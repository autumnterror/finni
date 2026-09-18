package github.detrig.feature.week.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "week_state")
data class WeekStateEntity(
    @PrimaryKey val id: String = "current",
    val absoluteDay: Long,
)
