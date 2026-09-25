package github.detrig.internetbooster.time

import android.content.Context
import github.detrig.core.infrastructure.preferences.SharedStorage

internal class NotificationPermissionStorage(context: Context) : SharedStorage(
    context.getSharedPreferences("pet_notifications", Context.MODE_PRIVATE),
) {
    fun wasRequested(): Boolean = readBoolean("requested", false)

    fun markRequested() = putBoolean("requested", true)
}
