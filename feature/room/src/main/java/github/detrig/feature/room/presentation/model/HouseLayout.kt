package github.detrig.feature.room.presentation.model

import github.detrig.feature.room.domain.model.HousePosition

internal enum class HouseRoom { PLAYROOM, BEDROOM, HALL, KITCHEN }

internal data class HouseSection(val room: HouseRoom, val startX: Float, val width: Float) {
    val endX get() = startX + width
}

internal enum class HouseObjectArt {
    DRAWING, MUSIC, WORKSHOP, PUZZLE, BALL, FOOTBALL, RACING, THEATER,
    FISHING, GARDEN, SPACE, SCIENCE, FLIGHT,
    BED, NIGHTSTAND, WINDOW, WARDROBE, MIRROR,
    SOFA, PIGGY_BANK, DOOR, CALENDAR, TASK_BOARD, CABINET, PHONE,
    FRIDGE, SINK, COUNTER, STOVE, BOWLS, DINING_TABLE,
}

/** x и ширина — в единицах мира; baseY — смещение опоры от горизонта. */
internal data class HouseObjectPlacement(
    val id: String,
    val art: HouseObjectArt,
    val centerX: Float,
    val width: Float,
    val height: Float,
    val baseY: Float = 0.12f,
    val zoneId: String? = null,
    val opensMarket: Boolean = false,
    val layer: Float = 1f,
) {
    val leftX get() = centerX - width / 2
    val rightX get() = centerX + width / 2
}

/** Одна единица X соответствует ширине окна в портретной ориентации. */
internal object HouseLayout {
    const val VERSION = 2
    const val WORLD_WIDTH = 16f
    const val VIEWPORT_WIDTH = 1f
    const val INITIAL_CAMERA_X = 10.6f
    const val WALL_HEIGHT_FRACTION = 0.45f
    const val PET_WIDTH = 0.29f
    const val PET_FLOOR_OFFSET = 0.57f
    const val PET_SPEED = 0.9f
    const val PET_SLOWDOWN_DISTANCE = 0.1f
    const val PET_STOP_DISTANCE = 0.02f
    const val PET_WALK_MARGIN = 0.2f
    const val WALK_CYCLE_DISTANCE = 0.18f
    const val LOCK_SIZE_FRACTION = 0.34f
    const val SAVE_DELAY_MILLIS = 350L

    val sections = listOf(
        HouseSection(HouseRoom.PLAYROOM, 0f, 6f),
        HouseSection(HouseRoom.BEDROOM, 6f, 3f),
        HouseSection(HouseRoom.HALL, 9f, 4f),
        HouseSection(HouseRoom.KITCHEN, 13f, 3f),
    )

    private fun game(id: String, art: HouseObjectArt, x: Float, width: Float,
                     height: Float, baseY: Float = 0.12f) =
        HouseObjectPlacement(id, art, x, width, height, baseY, zoneId = id)

    val objects = listOf(
        game("drawing", HouseObjectArt.DRAWING, 0.36f, 0.36f, 0.57f),
        game("music", HouseObjectArt.MUSIC, 0.84f, 0.43f, 0.37f),
        game("workshop", HouseObjectArt.WORKSHOP, 1.35f, 0.42f, 0.47f),
        game("puzzle", HouseObjectArt.PUZZLE, 1.88f, 0.45f, 0.30f),
        game("ball", HouseObjectArt.BALL, 2.38f, 0.34f, 0.46f),
        game("football", HouseObjectArt.FOOTBALL, 2.88f, 0.44f, 0.34f),
        game("racing", HouseObjectArt.RACING, 3.40f, 0.46f, 0.27f),
        game("theater", HouseObjectArt.THEATER, 3.92f, 0.43f, 0.59f),
        game("fishing", HouseObjectArt.FISHING, 4.43f, 0.45f, 0.41f),
        game("garden", HouseObjectArt.GARDEN, 4.96f, 0.44f, 0.35f),
        game("space", HouseObjectArt.SPACE, 5.48f, 0.40f, 0.52f),
        game("science", HouseObjectArt.SCIENCE, 5.84f, 0.28f, 0.32f),
        game("flight", HouseObjectArt.FLIGHT, 5.35f, 0.36f, 0.23f, -0.46f),
        HouseObjectPlacement("bed", HouseObjectArt.BED, 6.57f, 0.85f, 0.65f, 0.20f),
        HouseObjectPlacement("nightstand", HouseObjectArt.NIGHTSTAND, 7.15f, 0.27f, 0.48f),
        HouseObjectPlacement("bedroom_window", HouseObjectArt.WINDOW, 7.42f, 0.55f, 0.62f, -0.29f),
        HouseObjectPlacement("wardrobe", HouseObjectArt.WARDROBE, 8.04f, 0.61f, 0.87f),
        HouseObjectPlacement("mirror", HouseObjectArt.MIRROR, 8.66f, 0.31f, 0.66f),
        HouseObjectPlacement("sofa", HouseObjectArt.SOFA, 9.65f, 0.92f, 0.50f),
        HouseObjectPlacement("piggy_bank", HouseObjectArt.PIGGY_BANK, 10.43f, 0.43f, 0.37f),
        HouseObjectPlacement("market", HouseObjectArt.DOOR, 11.10f, 0.56f, 0.78f, 0f, opensMarket = true),
        HouseObjectPlacement("calendar", HouseObjectArt.CALENDAR, 11.68f, 0.25f, 0.29f, -0.38f),
        HouseObjectPlacement("task_board", HouseObjectArt.TASK_BOARD, 12.11f, 0.40f, 0.28f, -0.40f),
        HouseObjectPlacement("cabinet", HouseObjectArt.CABINET, 12.34f, 0.81f, 0.34f),
        HouseObjectPlacement("phone", HouseObjectArt.PHONE, 12.44f, 0.11f, 0.19f, -0.18f),
        HouseObjectPlacement("fridge", HouseObjectArt.FRIDGE, 13.36f, 0.43f, 0.81f),
        HouseObjectPlacement("sink", HouseObjectArt.SINK, 13.94f, 0.62f, 0.51f),
        HouseObjectPlacement("counter", HouseObjectArt.COUNTER, 14.51f, 0.46f, 0.42f),
        HouseObjectPlacement("stove", HouseObjectArt.STOVE, 14.99f, 0.40f, 0.87f),
        HouseObjectPlacement("bowls", HouseObjectArt.BOWLS, 15.36f, 0.30f, 0.12f, 0.19f),
        HouseObjectPlacement("dining_table", HouseObjectArt.DINING_TABLE, 15.73f, 0.50f, 0.43f),
    )

    fun initialPosition() = HousePosition(VERSION, INITIAL_CAMERA_X, INITIAL_CAMERA_X + 0.5f)
    fun horizon(height: Float, width: Float): Float =
        maxOf(height * WALL_HEIGHT_FRACTION, minOf(height * 0.60f, width * 1.05f))
    fun clampCamera(x: Float): Float = x.coerceIn(0f, WORLD_WIDTH - VIEWPORT_WIDTH)
    fun clampPet(x: Float): Float = x.coerceIn(PET_WALK_MARGIN, WORLD_WIDTH - PET_WALK_MARGIN)
    fun restored(position: HousePosition?): HousePosition =
        position?.takeIf { it.layoutVersion == VERSION && it.cameraLeftX.isFinite() && it.petX.isFinite() }
            ?.copy(cameraLeftX = clampCamera(position.cameraLeftX), petX = clampPet(position.petX))
            ?: initialPosition()
}
