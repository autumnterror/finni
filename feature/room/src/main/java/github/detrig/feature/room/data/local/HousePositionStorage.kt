package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.domain.model.HousePositionRepository

internal class HousePositionStorage(preferences: SharedPreferences) :
    SharedStorage(preferences), HousePositionRepository {

    override fun load(): HousePosition? {
        val fields = try {
            readString(POSITION_KEY).split('|')
        } catch (_: ClassCastException) {
            return null
        }
        if (fields.size != 5) return null
        val version = fields[0].toIntOrNull() ?: return null
        val camera = fields[1].toFloatOrNull()?.takeIf { it.isFinite() } ?: return null
        val pet = fields[2].toFloatOrNull()?.takeIf { it.isFinite() } ?: return null
        val right = fields[3].toBooleanStrictOrNull() ?: return null
        val hint = fields[4].toBooleanStrictOrNull() ?: return null
        return HousePosition(version, camera, pet, right, hint)
    }

    override fun save(position: HousePosition) {
        // Одна атомарная запись; SharedStorage использует асинхронный apply.
        putString(POSITION_KEY, with(position) {
            "$layoutVersion|$cameraLeftX|$petX|$facingRight|$hintSeen"
        })
    }

    private companion object {
        const val POSITION_KEY = "house_position"
    }
}
