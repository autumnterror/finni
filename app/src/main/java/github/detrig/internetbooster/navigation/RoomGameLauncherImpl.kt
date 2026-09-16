package github.detrig.internetbooster.navigation

import android.content.res.Resources
import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.feature.room.api.RoomGameLauncher
import github.detrig.internetbooster.R

/** Здесь подключаются публичные API мини-игр по их идентификаторам. */
internal class RoomGameLauncherImpl(
    private val messageController: GlobalMessageController,
    private val resources: Resources,
) : RoomGameLauncher {
    override fun openGame(gameId: String) {
        when (gameId) {
            "fishing" -> github.detrig.minigames.fishing.FishingFeature.getApi().open()
            "flight" -> github.detrig.minigames.flight.FlightFeature.getApi().open()
            else -> messageController.showMessage(resources.getString(R.string.room_game_coming_soon))
        }
    }
}
