package github.detrig.feature.room.domain.model

/** Логические координаты дома, независимые от плотности экрана. */
internal data class HousePosition(
    val layoutVersion: Int,
    val cameraLeftX: Float,
    val petX: Float,
    val facingRight: Boolean = true,
    val hintSeen: Boolean = false,
)

internal interface HousePositionRepository {
    fun load(): HousePosition?
    fun save(position: HousePosition)
}
