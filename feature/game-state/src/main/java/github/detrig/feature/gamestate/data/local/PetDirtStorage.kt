package github.detrig.feature.gamestate.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.gamestate.domain.model.PetDirtAnchor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small independent save so adding pet dirt does not alter the existing Room schema. */
internal class PetDirtStorage(private val preferences: SharedPreferences) : SharedStorage(preferences) {
    private val mutableAnchor = MutableStateFlow(readAnchor())
    val anchor: StateFlow<PetDirtAnchor?> = mutableAnchor

    @Synchronized
    fun ensureInitialized(nowMillis: Long, completedGames: Long): PetDirtAnchor =
        mutableAnchor.value ?: PetDirtAnchor(nowMillis, completedGames).also(::save)

    @Synchronized
    fun reset(nowMillis: Long, completedGames: Long) {
        setAnchor(PetDirtAnchor(nowMillis, completedGames))
    }

    @Synchronized
    fun setAnchor(value: PetDirtAnchor) = save(value)

    private fun readAnchor(): PetDirtAnchor? {
        val parts = readString(ANCHOR_KEY, "").split(':')
        if (parts.size != 2) return null
        val time = parts[0].toLongOrNull() ?: return null
        val games = parts[1].toLongOrNull() ?: return null
        return PetDirtAnchor(time, games)
    }

    private fun save(value: PetDirtAnchor) {
        check(preferences.edit().putString(ANCHOR_KEY,
            "${value.cleanedAtMillis}:${value.completedGamesAtClean}").commit())
        mutableAnchor.value = value
    }

    private companion object {
        const val ANCHOR_KEY = "pet_dirt_anchor_v1"
    }
}
