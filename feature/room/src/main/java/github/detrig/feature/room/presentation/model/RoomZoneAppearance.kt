package github.detrig.feature.room.presentation.model

import androidx.annotation.StringRes
import github.detrig.feature.room.R

internal enum class RoomZoneAppearance(
    val key: String,
    @StringRes val titleRes: Int,
) {
    BALL("ball", R.string.zone_ball),
    DRAWING("drawing", R.string.zone_drawing),
    MUSIC("music", R.string.zone_music),
    WORKSHOP("workshop", R.string.zone_workshop),
    PUZZLE("puzzle", R.string.zone_puzzle),
    FISHING("fishing", R.string.zone_fishing),
    GARDEN("garden", R.string.zone_garden),
    FOOTBALL("football", R.string.zone_football),
    FLIGHT("flight", R.string.zone_flight),
    SPACE("space", R.string.zone_space),
    RACING("racing", R.string.zone_racing),
    THEATER("theater", R.string.zone_theater),
    SCIENCE("science", R.string.zone_science);

    companion object {
        fun fromKey(key: String): RoomZoneAppearance = entries.first { it.key == key }
    }
}
