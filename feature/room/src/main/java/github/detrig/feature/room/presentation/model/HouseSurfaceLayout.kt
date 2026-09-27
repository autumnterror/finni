package github.detrig.feature.room.presentation.model

import androidx.compose.ui.graphics.ImageBitmap

/** Native coordinates shared by the room, its store preview, and replacement surface assets. */
internal object HouseSurfaceLayout {
    const val SCENE_WIDTH = 3128
    const val SCENE_HEIGHT = 685
    const val WALL_HEIGHT = 474
    const val FLOOR_HEIGHT = SCENE_HEIGHT - WALL_HEIGHT

    enum class Room(val id: String, val left: Int, val right: Int) {
        PLAYROOM("playroom", 0, 322),
        BEDROOM("bedroom", 322, 854),
        LIVING("living", 854, 1548),
        KITCHEN("kitchen", 1548, 2048),
        BATHROOM("bathroom", 2048, SCENE_WIDTH),
        ;

        val width: Int get() = right - left

        companion object {
            fun fromId(id: String): Room? = entries.firstOrNull { it.id == id }
        }
    }
}

/** An absent image keeps the current hand-drawn wall or floor exactly as it is. */
internal data class HouseSurfaceTextures(
    val walls: Map<HouseSurfaceLayout.Room, ImageBitmap> = emptyMap(),
    val floors: Map<HouseSurfaceLayout.Room, ImageBitmap> = emptyMap(),
) {
    companion object {
        val EMPTY = HouseSurfaceTextures()
    }
}
