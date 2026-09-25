package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentHelpPromptStorageTest {

    @Test
    fun promptIsRecordedForTheDisplayedWeekAndCanAppearInANewWeek() {
        val preferences = preferencesWithOldPromptMarkers()
        val storage = ParentHelpPromptStorage(preferences)

        assertFalse(storage.wasShownInWeek(3))
        storage.markShownInWeek(3)
        assertTrue(ParentHelpPromptStorage(preferences).wasShownInWeek(3))
        assertFalse(storage.wasShownInWeek(4))

        storage.markShownInWeek(4)
        assertTrue(storage.wasShownInWeek(4))
    }

    private fun preferencesWithOldPromptMarkers(): SharedPreferences {
        val values = mutableMapOf<String, Any>(
            "parent_help_automatic_prompt_shown_v1" to true,
            "parent_help_automatic_prompt_shown_week_v2" to 3L,
        )
        val editor = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "putLong" -> {
                    values[args!![0] as String] = args[1] as Long
                    proxy
                }
                "apply" -> null
                "commit" -> true
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getLong" -> values[args!![0] as String] as? Long ?: args[1] as Long
                "edit" -> editor
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
    }
}
