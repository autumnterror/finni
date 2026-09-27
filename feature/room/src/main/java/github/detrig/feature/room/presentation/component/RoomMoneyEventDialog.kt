package github.detrig.feature.room.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalVisibilityEffect
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.MoneyAllocation
import github.detrig.feature.room.domain.model.MoneyEventResolution
import github.detrig.feature.room.domain.model.RoomMoneyEvent

@Composable
internal fun RoomMoneyEventDialog(
    event: RoomMoneyEvent,
    availableRub: Long,
    savingsRub: Long,
    canAskParents: Boolean,
    hasActiveGoal: Boolean,
    error: MoneyEventResolution?,
    isResolving: Boolean,
    petName: String,
    petPortrait: @Composable (Modifier) -> Unit,
    onResolve: (MoneyAllocation?) -> Unit,
    onLater: () -> Unit,
    onOpenSavings: () -> Unit,
    onAskParents: () -> Unit,
    onParentCoverage: () -> Unit,
) {
    FinPetModalVisibilityEffect()
    val cards = buildList {
        add(stringResource(
            if (event.kind == RoomMoneyEvent.Kind.EXTRA_INCOME) R.string.money_event_income
            else R.string.money_event_expense,
            event.title, event.amountRub,
        ))
        if (event.kind == RoomMoneyEvent.Kind.KNOWN_EXPENSE) {
            add(stringResource(R.string.money_event_known_explanation))
        } else if (event.kind == RoomMoneyEvent.Kind.UNEXPECTED_EXPENSE) {
            add(stringResource(R.string.money_event_unexpected_explanation))
        } else {
            add(stringResource(R.string.money_event_income_choice))
        }
        when (error) {
            MoneyEventResolution.InsufficientFunds -> add(stringResource(
                R.string.money_event_not_enough, event.amountRub, availableRub))
            MoneyEventResolution.NoActiveGoal -> add(stringResource(R.string.money_event_no_goal))
            else -> Unit
        }
    }
    val actions = if (event.kind == RoomMoneyEvent.Kind.EXTRA_INCOME) {
        if (event.savedAllocation == MoneyAllocation.GOAL) {
            listOf(
                FinPetDialogueAction("goal", stringResource(R.string.money_event_goal)),
                FinPetDialogueAction("later", stringResource(R.string.money_event_later)),
            )
        } else buildList {
            add(FinPetDialogueAction("wants", stringResource(R.string.money_event_wants)))
            add(FinPetDialogueAction("reserve", stringResource(R.string.money_event_reserve)))
            if (hasActiveGoal) add(FinPetDialogueAction("goal", stringResource(R.string.money_event_goal)))
            add(FinPetDialogueAction("free", stringResource(R.string.money_event_free)))
            add(FinPetDialogueAction("later", stringResource(R.string.money_event_later)))
        }
    } else buildList {
        if (availableRub >= event.amountRub) {
            add(FinPetDialogueAction("pay", stringResource(R.string.money_event_pay)))
        }
        if (availableRub < event.amountRub && savingsRub > 0) {
            add(FinPetDialogueAction("savings", stringResource(R.string.money_event_open_savings)))
        }
        if (availableRub + savingsRub < event.amountRub && canAskParents) {
            add(FinPetDialogueAction("parents", stringResource(R.string.money_event_ask_parents)))
        }
        if (availableRub < event.amountRub && savingsRub == 0L && !canAskParents) {
            add(FinPetDialogueAction("coverage", stringResource(R.string.money_event_parent_coverage)))
        }
        add(FinPetDialogueAction("later", stringResource(R.string.money_event_later)))
    }
    FinPetDialogueDialog(
        speakerName = petName,
        cards = cards,
        portrait = petPortrait,
        advanceOnTap = false,
        actions = actions,
        onActionSelected = { action ->
            if (isResolving) return@FinPetDialogueDialog
            when (action.id) {
                "wants" -> onResolve(MoneyAllocation.WANTS)
                "reserve" -> onResolve(MoneyAllocation.RESERVE)
                "goal" -> onResolve(MoneyAllocation.GOAL)
                "free" -> onResolve(MoneyAllocation.FREE)
                "pay" -> onResolve(null)
                "savings" -> onOpenSavings()
                "parents" -> onAskParents()
                "coverage" -> onParentCoverage()
                else -> onLater()
            }
        },
        onFinished = onLater,
    )
}

@Preview(name = "Денежное событие", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun RoomMoneyEventDialogPreview() {
    FinPetTheme {
        RoomMoneyEventDialog(
            event = RoomMoneyEvent("preview", 2, 3, "Небольшой подарок", 50,
                RoomMoneyEvent.Kind.EXTRA_INCOME),
            availableRub = 400,
            savingsRub = 0,
            canAskParents = true,
            hasActiveGoal = true,
            error = null,
            isResolving = false,
            petName = "Финни",
            petPortrait = { Text("🐾") },
            onResolve = {},
            onLater = {},
            onOpenSavings = {},
            onAskParents = {},
            onParentCoverage = {},
        )
    }
}
