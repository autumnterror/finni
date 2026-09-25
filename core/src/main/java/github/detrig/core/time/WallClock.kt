package github.detrig.core.time

fun interface WallClock {
    fun nowMillis(): Long
}

object SystemWallClock : WallClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
