package github.detrig.feature.savings.di

import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.presentation.SavingsViewModel

internal interface SavingsComponent {
    val api: SavingsApi
    fun viewModel(): SavingsViewModel
}
