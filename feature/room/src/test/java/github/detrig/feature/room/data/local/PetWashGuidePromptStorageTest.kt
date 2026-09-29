package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetWashGuidePromptStorageTest {
    @Test fun promptIsShownOnceUntilThePetIsCleanAgain() {
        val preferences = preferences()
        val storage = PetWashGuidePromptStorage(preferences)

        assertFalse(storage.wasShownForCurrentDirtEpisode())
        assertTrue(storage.tryMarkShownForCurrentDirtEpisode())
        assertTrue(storage.wasShownForCurrentDirtEpisode())
        assertFalse(storage.tryMarkShownForCurrentDirtEpisode())
        assertFalse(PetWashGuidePromptStorage(preferences).tryMarkShownForCurrentDirtEpisode())

        storage.resetForCleanPet()

        assertTrue(storage.tryMarkShownForCurrentDirtEpisode())
    }

    @Test fun completedBathGuidanceSurvivesCleaningForLaterDirtEpisodes() {
        val preferences = preferences()
        val storage = PetWashGuidePromptStorage(preferences)

        assertFalse(storage.wasBathGuidanceCompleted())
        storage.markBathGuidanceCompleted()
        storage.tryMarkShownForCurrentDirtEpisode()

        storage.resetForCleanPet()

        assertTrue(storage.wasBathGuidanceCompleted())
        assertFalse(storage.wasShownForCurrentDirtEpisode())
    }

    @Test fun fullResetClearsBothEpisodeAndCompletedGuidance() {
        val storage = PetWashGuidePromptStorage(preferences())
        storage.markBathGuidanceCompleted()
        storage.tryMarkShownForCurrentDirtEpisode()

        storage.resetAll()

        assertFalse(storage.wasBathGuidanceCompleted())
        assertFalse(storage.wasShownForCurrentDirtEpisode())
    }

    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any>()
        val editor = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "putBoolean" -> {
                    values[args!![0] as String] = args[1] as Boolean
                    proxy
                }
                "remove" -> {
                    values.remove(args!![0] as String)
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
                "getBoolean" -> values[args!![0] as String] as? Boolean ?: args[1] as Boolean
                "contains" -> values.containsKey(args!![0] as String)
                "edit" -> editor
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
    }
}
