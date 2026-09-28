package github.detrig.internetbooster.time

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.internetbooster.MainActivity
import github.detrig.internetbooster.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class HungerNotificationDispatcher(
    private val context: Context,
    private val gameStateApi: GameStateApi,
) {
    private val mutex = Mutex()

    suspend fun dispatch() = mutex.withLock {
        withContext(Dispatchers.Main.immediate) { dispatchInBackground() }
    }

    private suspend fun dispatchInBackground() {
        val state = gameStateApi.hungerAlertState()
        val notifications = NotificationManagerCompat.from(context)
        if (isForeground() || state == null || state.hunger > 0) {
            notifications.cancel(NOTIFICATION_ID)
            return
        }
        val episode = state.pendingEpisode ?: return
        if (!canNotify(notifications)) return

        val openGame = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hunger)
            .setContentTitle(context.getString(R.string.pet_hungry_title))
            .setContentText(context.getString(R.string.pet_hungry_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openGame)
            .setAutoCancel(true)
            .build()
        try {
            notifications.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            return // Permission may have been revoked between the check and notify().
        }
        gameStateApi.markHungerAlertDelivered(episode)
    }

    private fun isForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun canNotify(notifications: NotificationManagerCompat): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED) && notifications.areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    companion object {
        private const val CHANNEL_ID = "pet_care"
        private const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID, context.getString(R.string.pet_care_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
