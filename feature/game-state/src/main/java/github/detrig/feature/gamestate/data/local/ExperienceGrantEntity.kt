package github.detrig.feature.gamestate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "experience_grants",
    indices = [Index(value = ["profileId"])],
)
data class ExperienceGrantEntity(
    @PrimaryKey val grantId: String,
    val profileId: String,
    val amount: Int,
    val source: String,
    val grantedAtMillis: Long,
)
