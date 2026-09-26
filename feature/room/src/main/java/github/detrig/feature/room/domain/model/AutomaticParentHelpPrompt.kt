package github.detrig.feature.room.domain.model

import github.detrig.feature.economy.domain.ParentHelpRequestResult
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

internal fun shouldRetainParentHelpDialog(
    hasActiveParentHelp: Boolean,
    isRequestingParentHelp: Boolean,
    helpIsAvailable: Boolean,
): Boolean = hasActiveParentHelp || isRequestingParentHelp || helpIsAvailable

internal fun shouldCloseAutomaticParentHelpDialog(
    requestResult: ParentHelpRequestResult,
    hasActiveParentHelp: Boolean,
): Boolean = requestResult is ParentHelpRequestResult.Accepted ||
    requestResult is ParentHelpRequestResult.AlreadyActive ||
    hasActiveParentHelp
