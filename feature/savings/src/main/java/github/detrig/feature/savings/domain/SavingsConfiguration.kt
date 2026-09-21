package github.detrig.feature.savings.domain

import github.detrig.feature.savings.api.SavingsGoalDraft

data class SavingsConfiguration(
    val starterGoals: List<SavingsGoalDraft> = listOf(
        SavingsGoalDraft("room-zone:drawing", "Рисование", 200, "source=room-zone;zoneId=drawing"),
        SavingsGoalDraft("room-zone:music", "Музыка", 350, "source=room-zone;zoneId=music"),
        SavingsGoalDraft("room-zone:fishing", "Рыбалка", 400, "source=room-zone;zoneId=fishing"),
        SavingsGoalDraft("starter:big-dream", "Большая мечта", 800, "source=starter"),
    ),
)
