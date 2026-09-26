package github.detrig.feature.room.presentation.model

import github.detrig.feature.room.domain.model.HousePosition

internal enum class HouseRoom { PLAYROOM, BEDROOM, HALL, KITCHEN }

internal enum class HouseObjectArt {
    DRAWING, MUSIC, WORKSHOP, PUZZLE, BALL, FOOTBALL, RACING, THEATER,
    FISHING, GARDEN, SPACE, SCIENCE, FLIGHT,
    BED, NIGHTSTAND, WINDOW, WARDROBE, MIRROR,
    SOFA, PIGGY_BANK, DOOR, CALENDAR, TASK_BOARD, CABINET, PHONE,
    FRIDGE, SINK, COUNTER, STOVE, DINING_TABLE,
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
    private const val SCENE_WIDTH = 2048f
    private const val SCENE_HEIGHT = 685f
    const val WORLD_WIDTH = 6.65f
    const val VIEWPORT_WIDTH = 1f
    const val INITIAL_CAMERA_X = 3.05f
    const val IMAGE_ASPECT = 2169f / 725f
    // The hamster artwork occupies about 88% of its square canvas. This size
    // makes the visible pet height match the floor-scale reference on mobile.
    const val PET_WIDTH = 0.67f
    const val PET_FLOOR_BASELINE = 0.90f
    const val PET_SPEED = 0.9f
    const val PET_SLOWDOWN_DISTANCE = 0.1f
    const val PET_STOP_DISTANCE = 0.02f
    const val PET_WALK_MARGIN = 0.36f
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

    /** Fits the supplied transparent sprite into its former scene area without distorting it. */
    private fun asset(
        id: String,
        art: HouseObjectArt,
        sourceWidth: Float,
        sourceHeight: Float,
        frameLeft: Float,
        frameTop: Float,
        frameRight: Float,
        frameBottom: Float,
        zoneId: String? = null,
        opensMarket: Boolean = false,
        layer: Float = 1f,
        interactive: Boolean = false,
    ): HouseObjectPlacement {
        val scale = minOf(
            (frameRight - frameLeft) / sourceWidth,
            (frameBottom - frameTop) / sourceHeight,
        )
        val width = sourceWidth * scale
        val height = sourceHeight * scale
        val left = frameLeft + (frameRight - frameLeft - width) / 2f
        return referenceObject(
            id = id,
            art = art,
            left = left / SCENE_WIDTH,
            top = (frameBottom - height) / SCENE_HEIGHT,
            right = (left + width) / SCENE_WIDTH,
            bottom = frameBottom / SCENE_HEIGHT,
            zoneId = zoneId,
            opensMarket = opensMarket,
            layer = layer,
            interactive = interactive,
        )
    }

    /** Every room sprite is placed separately; only gameplay objects are interactive. */
    val objects = listOf(
        asset("decor_rug_bedroom", HouseObjectArt.DOOR, 741f, 227f, 341f, 465f, 660f, 563f, layer = -1f),
        asset("decor_rug_living", HouseObjectArt.DOOR, 1245f, 212f, 871f, 499f, 1245f, 563f, layer = -1f),
        asset("decor_rug_kitchen", HouseObjectArt.DOOR, 523f, 200f, 1719f, 444f, 2030f, 563f, layer = -1f),

        asset("decor_window", HouseObjectArt.WINDOW, 499f, 340f, 379f, 117f, 605f, 304f, layer = 0f),
        asset("decor_bedside_table", HouseObjectArt.NIGHTSTAND, 171f, 186f, 332f, 424f, 406f, 504f, layer = 0f),
        asset("decor_lamp", HouseObjectArt.NIGHTSTAND, 118f, 166f, 345f, 355f, 395f, 435f, layer = 1f),
        asset("decor_mirror", HouseObjectArt.MIRROR, 203f, 439f, 760f, 285f, 841f, 502f, layer = 0f, interactive = true),
        asset("decor_sofa", HouseObjectArt.SOFA, 907f, 451f, 884f, 333f, 1229f, 507f, layer = 0f),
        asset("decor_coffee_table", HouseObjectArt.DINING_TABLE, 482f, 197f, 968f, 474f, 1144f, 550f, layer = 2f),
        asset("decor_cabinet", HouseObjectArt.CABINET, 421f, 273f, 1235f, 403f, 1385f, 507f, layer = 0f),
        asset("decor_shelf_phone", HouseObjectArt.PHONE, 459f, 124f, 1153f, 259f, 1337f, 308f, layer = 0f),
        asset("decor_plant", HouseObjectArt.PHONE, 244f, 320f, 1281f, 200f, 1325f, 266f, layer = 2f),
        asset("decor_notice_board", HouseObjectArt.TASK_BOARD, 365f, 332f, 1425f, 202f, 1530f, 324f, layer = 0f),
        asset("decor_range_hood", HouseObjectArt.STOVE, 268f, 291f, 1890f, 100f, 2035f, 278f, layer = 0f),
        asset("decor_stove", HouseObjectArt.STOVE, 280f, 393f, 1890f, 301f, 2035f, 505f, layer = 0f),

        asset("decor_shelf_airplane", HouseObjectArt.FLIGHT, 406f, 124f, 22f, 176f, 158f, 217f, layer = 0f),
        asset("decor_shelf_keyboard", HouseObjectArt.MUSIC, 394f, 122f, 160f, 226f, 300f, 269f, layer = 0f),
        asset("decor_shelf_fishing", HouseObjectArt.FISHING, 404f, 124f, 145f, 342f, 283f, 385f, layer = 0f),
        asset("flight", HouseObjectArt.FLIGHT, 351f, 192f, 42f, 131f, 142f, 187f, zoneId = "flight", interactive = true),
        asset("music", HouseObjectArt.MUSIC, 405f, 187f, 173f, 193f, 288f, 247f, zoneId = "music", interactive = true),
        asset("fishing", HouseObjectArt.FISHING, 377f, 281f, 157f, 278f, 269f, 361f, zoneId = "fishing", interactive = true),
        asset("drawing", HouseObjectArt.DRAWING, 415f, 550f, 18f, 345f, 159f, 548f, zoneId = "drawing", interactive = true),
        asset("ball", HouseObjectArt.BALL, 424f, 317f, 171f, 429f, 291f, 535f, zoneId = "ball", interactive = true),

        asset("bed", HouseObjectArt.BED, 509f, 433f, 390f, 342f, 615f, 543f, interactive = true),
        asset("wardrobe", HouseObjectArt.WARDROBE, 348f, 561f, 612f, 216f, 764f, 505f, interactive = true),
        asset("piggy_bank", HouseObjectArt.PIGGY_BANK, 292f, 220f, 1277f, 356f, 1350f, 422f, interactive = true),
        asset("phone", HouseObjectArt.PHONE, 412f, 369f, 1151f, 177f, 1273f, 290f, opensMarket = true, interactive = true, layer = 2f),
        asset("calendar", HouseObjectArt.CALENDAR, 236f, 304f, 1345f, 189f, 1418f, 290f, interactive = true),
        asset("task_board", HouseObjectArt.TASK_BOARD, 408f, 172f, 1021f, 476f, 1091f, 505f, interactive = true, layer = 3f),
        asset("decor_pencil", HouseObjectArt.TASK_BOARD, 197f, 129f, 1095f, 482f, 1128f, 504f, layer = 4f),
        asset("fridge", HouseObjectArt.FRIDGE, 631f, 1214f, 1568f, 196f, 1695f, 502f, interactive = true, layer = 2f),
        asset("sink", HouseObjectArt.SINK, 326f, 291f, 1700f, 334f, 1888f, 502f),
        asset("decor_cutting_board", HouseObjectArt.SINK, 1477f, 364f, 1720f, 331f, 1830f, 361f, layer = 2f),
        asset("decor_chair", HouseObjectArt.DINING_TABLE, 266f, 361f, 1708f, 438f, 1789f, 547f, layer = 1f),
        asset("dining_table", HouseObjectArt.DINING_TABLE, 523f, 304f, 1785f, 430f, 1970f, 551f, interactive = true, layer = 2f),
    )

    fun initialPosition() = HousePosition(VERSION, INITIAL_CAMERA_X, INITIAL_CAMERA_X + 0.5f)

    fun clampCamera(x: Float): Float = x.coerceIn(0f, WORLD_WIDTH - VIEWPORT_WIDTH)
    fun clampPet(x: Float): Float = x.coerceIn(PET_WALK_MARGIN, WORLD_WIDTH - PET_WALK_MARGIN)

    /** The three visible partitions split the room into four soft flight arenas. */
    fun petFlightBounds(x: Float): Pair<Float, Float> {
        val partitions = floatArrayOf(0f, 318f, 854f, 1548f, 2048f)
        val referenceX = x / WORLD_WIDTH * 2048f
        val index = (0 until partitions.lastIndex).firstOrNull {
            referenceX <= partitions[it + 1]
        } ?: partitions.lastIndex - 1
        val radius = PET_WIDTH * 0.36f
        return (partitions[index] / 2048f * WORLD_WIDTH + radius) to
            (partitions[index + 1] / 2048f * WORLD_WIDTH - radius)
    }

    fun restored(position: HousePosition?): HousePosition =
        position?.takeIf { it.layoutVersion == VERSION && it.cameraLeftX.isFinite() && it.petX.isFinite() }
            ?.copy(cameraLeftX = clampCamera(position.cameraLeftX), petX = clampPet(position.petX))
            ?: initialPosition()
}
