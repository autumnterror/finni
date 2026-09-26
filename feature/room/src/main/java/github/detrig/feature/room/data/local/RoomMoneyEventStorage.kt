package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.room.domain.model.RoomMoneyEvent
import github.detrig.feature.room.domain.model.RoomMoneyEventSchedule
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** The whole week's draw is written before any event can be shown or paid. */
internal class RoomMoneyEventStorage(
    private val preferences: SharedPreferences,
) : SharedStorage(preferences) {

    @Synchronized
    fun events(weekNumber: Long): List<RoomMoneyEvent> {
        val key = "money_events_week_$weekNumber"
        val saved = readString(key, null)
        if (saved != null) return decode(saved, weekNumber)

        val selected = RoomMoneyEventSchedule.events(weekNumber, Random.Default)
        check(preferences.edit().putString(key, encode(selected)).commit()) {
            "Could not save money events for week $weekNumber"
        }
        return selected
    }

    private fun encode(events: List<RoomMoneyEvent>): String = JSONArray().apply {
        events.forEach { event ->
            put(JSONObject().apply {
                put("id", event.id)
                put("day", event.dayOfWeek)
                put("title", event.title)
                put("amount", event.amountRub)
                put("kind", event.kind.name)
            })
        }
    }.toString()

    private fun decode(saved: String, weekNumber: Long): List<RoomMoneyEvent> {
        val array = JSONArray(saved)
        return List(array.length()) { index ->
            val event = array.getJSONObject(index)
            RoomMoneyEvent(
                id = event.getString("id"),
                weekNumber = weekNumber,
                dayOfWeek = event.getInt("day"),
                title = event.getString("title"),
                amountRub = event.getLong("amount"),
                kind = RoomMoneyEvent.Kind.valueOf(event.getString("kind")),
            )
        }
    }
}
