package github.detrig.internetbooster.mediators

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import kotlin.random.Random

/** One shared daily draw for wishes, shop offers, and suspicious phone messages. */
internal class DailyRoomEventSchedule(
    private val preferences: SharedPreferences,
) : SharedStorage(preferences) {
    enum class Kind { NONE, WISH, PROMOTION, SECURITY }

    private data class Draw(
        val day: Long = 0L,
        val kind: Kind = Kind.NONE,
        val lastSecurityDay: Long = 0L,
    )

    @Synchronized
    fun eventForDay(absoluteDay: Long): Kind {
        require(absoluteDay >= 1L)
        val seed = randomSeed()
        val stored = Draw(
            day = readLong(DAY_KEY, 0L),
            kind = Kind.valueOf(readString(KIND_KEY, Kind.NONE.name)),
            lastSecurityDay = readLong(SECURITY_DAY_KEY, 0L),
        )
        if (stored.day == absoluteDay) return stored.kind

        // An older observer can read its day without overwriting a newer saved draw.
        var draw = if (stored.day < absoluteDay) stored else Draw()
        while (draw.day < absoluteDay) {
            val day = draw.day + 1L
            val kind = drawKind(day, draw, seed)
            draw = Draw(day, kind, if (kind == Kind.SECURITY) day else draw.lastSecurityDay)
        }
        if (absoluteDay > stored.day) {
            check(preferences.edit()
                .putLong(DAY_KEY, draw.day)
                .putString(KIND_KEY, draw.kind.name)
                .putLong(SECURITY_DAY_KEY, draw.lastSecurityDay)
                .commit()) { "Failed to persist the daily room event" }
        }
        return draw.kind
    }

    @Synchronized
    fun resetProgress() {
        check(preferences.edit().clear().commit()) { "Failed to reset the daily room events" }
    }

    private fun randomSeed(): Long {
        if (hasKey(SEED_KEY)) return readLong(SEED_KEY, 0L)
        return Random.nextLong().also { seed ->
            check(preferences.edit().putLong(SEED_KEY, seed).commit()) {
                "Failed to persist the daily room event seed"
            }
        }
    }

    private fun drawKind(day: Long, previous: Draw, seed: Long): Kind {
        // Keep the first-day guide quiet and preserve the two content introductions.
        if (day == 1L) return Kind.NONE
        if (day == FIRST_WISH_DAY) return Kind.WISH
        if (day == FIRST_PROMOTION_DAY) return Kind.PROMOTION

        val random = Random(seed xor day)
        if (previous.kind != Kind.NONE && random.nextDouble() >= NEXT_DAY_EVENT_PROBABILITY) {
            return Kind.NONE
        }
        val candidates = buildList {
            add(Kind.WISH to WISH_WEIGHT)
            if (day >= FIRST_PROMOTION_DAY) add(Kind.PROMOTION to PROMOTION_WEIGHT)
            if (day - previous.lastSecurityDay >= MINIMUM_SECURITY_INTERVAL_DAYS) {
                add(Kind.SECURITY to SECURITY_WEIGHT)
            }
        }
        var roll = random.nextInt(candidates.sumOf { it.second })
        candidates.forEach { (kind, weight) ->
            if (roll < weight) return kind
            roll -= weight
        }
        error("No daily room event selected")
    }

    companion object {
        const val PREFERENCES_NAME = "finpet_daily_room_events"
        const val FIRST_WISH_DAY = 2L
        const val FIRST_PROMOTION_DAY = 5L
        const val MINIMUM_SECURITY_INTERVAL_DAYS = 3
        private const val NEXT_DAY_EVENT_PROBABILITY = 0.65
        private const val WISH_WEIGHT = 60
        private const val PROMOTION_WEIGHT = 25
        private const val SECURITY_WEIGHT = 15
        private const val SEED_KEY = "seed"
        private const val DAY_KEY = "day"
        private const val KIND_KEY = "kind"
        private const val SECURITY_DAY_KEY = "last_security_day"
    }
}
