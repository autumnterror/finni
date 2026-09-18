package github.detrig.feature.planning.domain

/** Разрешено только для отдельного demo-профиля; реальная игра получает факты от фич. */
data class PlanningConfig(val seedDemoProgress: Boolean = false)
