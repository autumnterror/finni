package github.detrig.feature.week.di

import github.detrig.feature.week.WeekDependencies
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.data.WeekRepository

internal class WeekModule(private val dependencies: WeekDependencies) : WeekComponent {
    override val api: WeekApi by lazy {
        WeekRepository(
            dependencies.weekDao(), dependencies.economyApi(),
            dependencies.transactionRunner(), dependencies.petDayEffects(),
        )
    }
}
