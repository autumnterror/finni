package github.detrig.feature.room.domain.model

import github.detrig.feature.economy.domain.canOfferParentHelp

internal fun shouldOfferAutomaticParentHelp(
    onboardingCompleted: Boolean,
    availableRub: Long,
    savingsRub: Long,
    debtRub: Long,
    hasActiveParentHelp: Boolean,
    minimumRequiredBalanceRub: Long,
    alreadyShownInWeek: Boolean,
): Boolean = onboardingCompleted && !alreadyShownInWeek && canOfferParentHelp(
    availableRub = availableRub,
    savingsRub = savingsRub,
    debtRub = debtRub,
    hasActiveParentHelp = hasActiveParentHelp,
    minimumRequiredBalanceRub = minimumRequiredBalanceRub,
)
