package github.detrig.feature.savings.domain

import github.detrig.feature.savings.api.SavingsGoalDraft

data class SavingsConfiguration(
    val starterGoals: List<SavingsGoalDraft> = listOf(
        SavingsGoalDraft("starter:pet-toy", "Игрушка для питомца", 300, "source=starter"),
        SavingsGoalDraft("starter:drawing-set", "Набор для рисования", 500, "source=starter"),
        SavingsGoalDraft("starter:big-dream", "Большая мечта", 800, "source=starter"),
    ),
)
