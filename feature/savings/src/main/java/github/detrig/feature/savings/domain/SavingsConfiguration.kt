package github.detrig.feature.savings.domain

import github.detrig.feature.savings.api.SavingsGoalDraft

data class SavingsConfiguration(
    val starterGoals: List<SavingsGoalDraft> = listOf(
        SavingsGoalDraft("room-zone:drawing", "Рисование", 200, "source=room-zone;zoneId=drawing"),
        SavingsGoalDraft("room-zone:music", "Музыка", 350, "source=room-zone;zoneId=music"),
        SavingsGoalDraft("room-zone:fishing", "Рыбалка", 400, "source=room-zone;zoneId=fishing"),
        SavingsGoalDraft("starter:toy", "Новая игрушка", 300, "source=starter"),
        SavingsGoalDraft("starter:room-item", "Предмет для комнаты", 450, "source=starter"),
        SavingsGoalDraft("starter:big-dream", "Большая мечта", 800, "source=starter"),
    ),
)
