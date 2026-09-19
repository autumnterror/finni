package github.detrig.feature.room.presentation.model

import github.detrig.feature.room.domain.model.HousePosition

internal enum class HouseRoom { PLAYROOM, BEDROOM, HALL, KITCHEN }

internal enum class HouseObjectArt {
    DRAWING, MUSIC, WORKSHOP, PUZZLE, BALL, FOOTBALL, RACING, THEATER,
    FISHING, GARDEN, SPACE, SCIENCE, FLIGHT,
    BED, NIGHTSTAND, WINDOW, WARDROBE, MIRROR,
    SOFA, PIGGY_BANK, DOOR, CALENDAR, TASK_BOARD, CABINET, PHONE,
    FRIDGE, SINK, COUNTER, STOVE, BOWLS, DINING_TABLE,
}

/** Координаты кликабельной зоны в долях исходного wide-ассета комнаты. */
internal data class HouseObjectBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f

    init {
        require(left in 0f..1f && top in 0f..1f)
        require(right in 0f..1f && bottom in 0f..1f)
        require(right > left && bottom > top)
    }
}

/** x и ширина — в единицах ширины viewport; bounds — в координатах референсной картинки. */
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
    val bounds: HouseObjectBounds? = null,
    val interactive: Boolean = true,
)

/** Reference composition, rendered as resolution-independent surfaces and individual sprites. */
internal object HouseLayout {
    const val VERSION = 3
    const val WORLD_WIDTH = 6.65f
    const val VIEWPORT_WIDTH = 1f
    const val INITIAL_CAMERA_X = 3.05f
    const val IMAGE_ASPECT = 2169f / 725f
    const val PET_WIDTH = 0.29f
    const val PET_FLOOR_BASELINE = 0.90f
    const val PET_SPEED = 0.9f
    const val PET_SLOWDOWN_DISTANCE = 0.1f
    const val PET_STOP_DISTANCE = 0.02f
    const val PET_WALK_MARGIN = 0.2f
    const val WALK_CYCLE_DISTANCE = 0.18f
    const val LOCK_SIZE_FRACTION = 0.34f
    const val SAVE_DELAY_MILLIS = 350L

    private fun referenceObject(
        id: String,
        art: HouseObjectArt,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        zoneId: String? = null,
        opensMarket: Boolean = false,
        layer: Float = 1f,
        interactive: Boolean = true,
    ): HouseObjectPlacement {
        val bounds = HouseObjectBounds(left, top, right, bottom)
        return HouseObjectPlacement(
            id = id,
            art = art,
            centerX = bounds.centerX * WORLD_WIDTH,
            width = bounds.width * WORLD_WIDTH,
            height = bounds.height * WORLD_WIDTH,
            zoneId = zoneId,
            opensMarket = opensMarket,
            layer = layer,
            bounds = bounds,
            interactive = interactive,
        )
    }

    private fun game(id: String, art: HouseObjectArt, bounds: HouseObjectBounds) =
        referenceObject(
            id = id,
            art = art,
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            zoneId = id,
        )

    private fun decoration(id: String, art: HouseObjectArt, left: Float, top: Float, right: Float, bottom: Float) =
        referenceObject(id, art, left / 2048f, top / 685f, right / 2048f, bottom / 685f,
            layer = 0f, interactive = false)

    /** Every piece of furniture uses the same asset quality and renderer. */
    val objects = listOf(
        decoration("decor_window", HouseObjectArt.WINDOW, 379f, 117f, 605f, 304f),
        decoration("decor_nightstand", HouseObjectArt.NIGHTSTAND, 332f, 351f, 406f, 504f),
        decoration("decor_mirror", HouseObjectArt.MIRROR, 760f, 285f, 841f, 502f),
        decoration("decor_sofa", HouseObjectArt.SOFA, 884f, 333f, 1229f, 507f),
        decoration("decor_coffee_table", HouseObjectArt.DINING_TABLE, 944f, 479f, 1168f, 550f),
        decoration("decor_cabinet", HouseObjectArt.CABINET, 1235f, 403f, 1385f, 507f),
        decoration("decor_shelf", HouseObjectArt.CABINET, 1157f, 199f, 1331f, 303f),
        decoration("decor_notice_board", HouseObjectArt.TASK_BOARD, 1425f, 202f, 1530f, 324f),
        decoration("decor_stove", HouseObjectArt.STOVE, 1840f, 147f, 1985f, 505f),
        game("flight", HouseObjectArt.FLIGHT, HouseObjectBounds(0.011230f, 0.195620f, 0.076660f, 0.299270f)),
        game("music", HouseObjectArt.MUSIC, HouseObjectBounds(0.079590f, 0.281752f, 0.146484f, 0.382482f)),
        game("fishing", HouseObjectArt.FISHING, HouseObjectBounds(0.071289f, 0.407299f, 0.137695f, 0.548905f)),
        game("drawing", HouseObjectArt.DRAWING, HouseObjectBounds(0.008789f, 0.503650f, 0.077637f, 0.800000f)),
        game("ball", HouseObjectArt.BALL, HouseObjectBounds(0.083496f, 0.626277f, 0.142090f, 0.781022f)),
        referenceObject("bed", HouseObjectArt.BED, 0.190430f, 0.499270f, 0.300293f, 0.792701f),
        referenceObject("wardrobe", HouseObjectArt.WARDROBE, 0.298828f, 0.315328f, 0.373047f, 0.737226f),
        referenceObject("piggy_bank", HouseObjectArt.PIGGY_BANK, 0.623535f, 0.508029f, 0.659180f, 0.604380f),
        referenceObject("phone", HouseObjectArt.PHONE, 0.592773f, 0.272993f, 0.623047f, 0.398540f, opensMarket = true),
        referenceObject("calendar", HouseObjectArt.CALENDAR, 0.656738f, 0.275912f, 0.692383f, 0.423358f),
        referenceObject("task_board", HouseObjectArt.TASK_BOARD, 0.484863f, 0.686131f, 0.553223f, 0.750365f),
        referenceObject("fridge", HouseObjectArt.FRIDGE, 0.766602f, 0.332847f, 0.828613f, 0.738686f),
        referenceObject("sink", HouseObjectArt.SINK, 0.823242f, 0.487591f, 0.900879f, 0.732847f),
        referenceObject("dining_table", HouseObjectArt.DINING_TABLE, 0.854492f, 0.598540f, 0.979004f, 0.804380f, layer = 2f, interactive = false),
        referenceObject("bowls", HouseObjectArt.BOWLS, 0.917969f, 0.601460f, 0.948730f, 0.665693f, layer = 3f),
    )

    fun initialPosition() = HousePosition(VERSION, INITIAL_CAMERA_X, INITIAL_CAMERA_X + 0.5f)

    fun clampCamera(x: Float): Float = x.coerceIn(0f, WORLD_WIDTH - VIEWPORT_WIDTH)
    fun clampPet(x: Float): Float = x.coerceIn(PET_WALK_MARGIN, WORLD_WIDTH - PET_WALK_MARGIN)

    fun restored(position: HousePosition?): HousePosition =
        position?.takeIf { it.layoutVersion == VERSION && it.cameraLeftX.isFinite() && it.petX.isFinite() }
            ?.copy(cameraLeftX = clampCamera(position.cameraLeftX), petX = clampPet(position.petX))
            ?: initialPosition()
}
