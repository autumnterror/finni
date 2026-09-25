package github.detrig.feature.phone.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetFeedbackSurface
import github.detrig.designsystem.component.FinPetFeedbackTone
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.phone.R
import github.detrig.feature.phone.domain.MessageKind
import github.detrig.feature.phone.domain.MessageSenderId
import github.detrig.feature.phone.domain.MessageThread
import github.detrig.feature.phone.domain.PhoneMessage
import github.detrig.feature.phone.domain.SecurityMessageEvent
import github.detrig.feature.phone.domain.SecurityMessageScenario
import github.detrig.feature.phone.domain.SecurityResponseChoice
import github.detrig.feature.room.presentation.component.ParentHelpDialog

@Composable
internal fun MessagesApp(
    state: MessagesViewState,
    onBack: () -> Unit,
    onThreadOpened: (MessageSenderId) -> Unit,
    onThreadClosed: () -> Unit,
    onSuspiciousInteraction: (String, SecurityResponseChoice) -> Unit,
    onParentHelpOfferOpened: () -> Unit = {},
    onParentHelpAccepted: (String) -> Unit = {},
    onParentHelpPaidOff: () -> Unit = {},
    onParentHelpDismissed: () -> Unit = {},
) {
    val selectedThread = state.selectedSenderId?.let { sender ->
        state.inbox.threads.firstOrNull { it.senderId == sender }
    }
    BackHandler(enabled = selectedThread != null, onBack = onThreadClosed)

    if (selectedThread == null) {
        MessagesThreadList(
            threads = state.inbox.threads,
            onBack = onBack,
            onThreadOpened = onThreadOpened,
        )
    } else {
        MessageThreadContent(
            thread = selectedThread,
            isResponding = state.isResponding,
            errorMessage = state.errorMessage,
            onBack = onThreadClosed,
            onSuspiciousMessageOpened = onSuspiciousInteraction,
            onParentHelpOfferOpened = onParentHelpOfferOpened,
        )
    }
    state.parentHelpDialog?.let { help ->
        ParentHelpDialog(
            state = help,
            isRequesting = help.isSubmitting,
            onOfferSelected = onParentHelpAccepted,
            onPayOffNow = onParentHelpPaidOff,
            onDismiss = onParentHelpDismissed,
        )
    }
}

@Composable
private fun MessagesThreadList(
    threads: List<MessageThread>,
    onBack: () -> Unit,
    onThreadOpened: (MessageSenderId) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.currencyContainer)
            .padding(AppTheme.spacing.md),
    ) {
        MessagesHeader(
            title = stringResource(R.string.messages_app_name),
            onBack = onBack,
        )
        Spacer(Modifier.height(AppTheme.spacing.md))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            items(threads, key = { it.senderId.name }) { thread ->
                MessageThreadRow(thread = thread, onClick = { onThreadOpened(thread.senderId) })
            }
        }
    }
}

@Composable
private fun MessagesHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        FinPetBackButton(
            onClick = onBack,
            contentDescription = stringResource(R.string.messages_back),
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MessageThreadRow(
    thread: MessageThread,
    onClick: () -> Unit,
) {
    val senderName = thread.senderId.displayName()
    FinPetCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.storefrontControl,
        containerColor = AppTheme.colors.storefront.surface,
        borderColor = AppTheme.colors.storefront.outline,
        borderWidth = AppTheme.sizes.borderStrong,
        elevation = AppTheme.elevation.low,
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                SenderAvatar(thread.senderId)
                if (thread.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.statusCritical.accent)
                            .semantics {
                                contentDescription = "Непрочитано"
                            },
                    )
                }
            }
            Spacer(Modifier.width(AppTheme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = senderName,
                        modifier = Modifier.weight(1f),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.storefront.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    thread.messages.lastOrNull()?.let { message ->
                        Text(
                            text = stringResource(R.string.messages_day, message.absoluteDay),
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                }
                Text(
                    text = thread.messages.lastOrNull()?.displayText()
                        ?: stringResource(R.string.messages_empty_preview),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "›",
                modifier = Modifier.padding(start = AppTheme.spacing.xs),
                style = AppTheme.typography.screenTitle,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SenderAvatar(senderId: MessageSenderId) {
    val senderName = senderId.displayName()
    FinPetCard(
        modifier = Modifier
            .size(58.dp)
            .semantics {
                contentDescription = "Аватар отправителя $senderName"
            },
        shape = AppTheme.shapes.storefrontControl,
        containerColor = if (senderId == MessageSenderId.BANK) {
            AppTheme.colors.statusInfo.container
        } else {
            AppTheme.colors.surfaceInteractive
        },
        borderColor = AppTheme.colors.storefront.outline,
        borderWidth = AppTheme.sizes.borderStrong,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (senderId == MessageSenderId.BANK) "₽" else "?",
                style = AppTheme.typography.screenTitle,
                color = if (senderId == MessageSenderId.BANK) {
                    AppTheme.colors.statusInfo.accent
                } else {
                    AppTheme.colors.statusWarning.accent
                },
            )
        }
    }
}

@Composable
private fun MessageThreadContent(
    thread: MessageThread,
    isResponding: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSuspiciousMessageOpened: (String, SecurityResponseChoice) -> Unit,
    onParentHelpOfferOpened: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.currencyContainer)
            .padding(AppTheme.spacing.md),
    ) {
        MessagesHeader(title = thread.senderId.displayName(), onBack = onBack)
        Spacer(Modifier.height(AppTheme.spacing.md))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            if (thread.messages.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.messages_no_messages),
                        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.xl),
                        textAlign = TextAlign.Center,
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            items(thread.messages, key = { it.id }) { message ->
                val event = thread.latestEvent?.takeIf {
                    it.id == message.eventId && it.response == null && !isResponding
                }
                val unsafeChoice = event?.let {
                    when (message.kind) {
                        MessageKind.REQUEST_CONFIRMATION_CODE -> SecurityResponseChoice.SHARE_CODE
                        MessageKind.UNKNOWN_LINK -> SecurityResponseChoice.OPEN_LINK
                        MessageKind.BANK_CONFIRMATION_CODE -> null
                        MessageKind.PARENT_HELP_OFFER,
                        MessageKind.PARENT_HELP_REPAYMENT,
                        -> null
                    }
                }
                IncomingMessageBubble(
                    message = message,
                    onParentHelpOfferOpened = if (
                        message.kind == MessageKind.PARENT_HELP_OFFER ||
                        message.kind == MessageKind.PARENT_HELP_REPAYMENT
                    ) onParentHelpOfferOpened else null,
                    onSuspiciousInteraction = unsafeChoice?.let { choice ->
                        { onSuspiciousMessageOpened(message.eventId, choice) }
                    },
                )
            }
            errorMessage?.let { message ->
                item {
                    FinPetFeedbackSurface(
                        tone = FinPetFeedbackTone.Critical,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(message, Modifier.padding(AppTheme.spacing.md))
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingMessageBubble(
    message: PhoneMessage,
    onParentHelpOfferOpened: (() -> Unit)?,
    onSuspiciousInteraction: (() -> Unit)?,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        FinPetCard(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .then(
                    if (onParentHelpOfferOpened != null) {
                        Modifier.clickable(onClick = onParentHelpOfferOpened)
                    } else if (onSuspiciousInteraction != null) {
                        Modifier.clickable(
                            onClickLabel = stringResource(
                                if (message.kind == MessageKind.UNKNOWN_LINK) {
                                    R.string.messages_open_link
                                } else {
                                    R.string.messages_share_code
                                },
                            ),
                            onClick = onSuspiciousInteraction,
                        )
                    } else {
                        Modifier
                    },
                ),
            shape = AppTheme.shapes.storefrontControl,
            containerColor = AppTheme.colors.storefront.surface,
            borderColor = AppTheme.colors.storefront.outline,
        ) {
            Column(Modifier.padding(AppTheme.spacing.md)) {
                Text(
                    text = message.displayText(),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.storefront.onSurface,
                )
                Text(
                    text = stringResource(R.string.messages_day, message.absoluteDay),
                    modifier = Modifier.align(Alignment.End),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun MessageSenderId.displayName(): String = stringResource(
    when (this) {
        MessageSenderId.BANK -> R.string.messages_bank
        MessageSenderId.MOM -> R.string.messages_mom
        MessageSenderId.UNKNOWN_1 -> R.string.messages_unknown_1
        MessageSenderId.UNKNOWN_2 -> R.string.messages_unknown_2
    },
)

@Composable
private fun PhoneMessage.displayText(): String = when (kind) {
    MessageKind.BANK_CONFIRMATION_CODE -> stringResource(R.string.messages_bank_code, payload)
    MessageKind.REQUEST_CONFIRMATION_CODE -> stringResource(R.string.messages_code_request)
    MessageKind.UNKNOWN_LINK -> stringResource(R.string.messages_unknown_link, payload)
    MessageKind.PARENT_HELP_OFFER -> stringResource(R.string.messages_parent_help_offer)
    MessageKind.PARENT_HELP_REPAYMENT -> {
        val details = payload.split('|')
        val paymentsRemaining = details.getOrNull(0)?.toIntOrNull()
        val remainingRub = details.getOrNull(1)?.toLongOrNull()
        if (paymentsRemaining != null && remainingRub != null) {
            stringResource(R.string.messages_parent_help_repayment, paymentsRemaining, remainingRub)
        } else {
            stringResource(R.string.messages_parent_help_repayment_fallback)
        }
    }
}


@Composable
internal fun MessagesPetDialogue(
    state: MessagesViewState,
    portrait: @Composable (Modifier) -> Unit,
    onGuidanceDismissed: (String) -> Unit,
    onFeedbackDismissed: (String) -> Unit,
) {
    val guidanceEvent = state.selectedSenderId
        ?.let { selected -> state.inbox.threads.firstOrNull { it.senderId == selected } }
        ?.latestEvent
    val feedbackEvent = state.dialogueEventId?.let { eventId ->
        state.inbox.threads.mapNotNull { it.latestEvent }.firstOrNull { it.id == eventId }
    }
    val guidancePending = guidanceEvent?.let { event ->
        event.guidanceVisible && !event.guidanceAcknowledged && event.response == null
    } == true
    val feedbackPending = feedbackEvent?.let { event ->
        event.response != null &&
            !event.feedbackAcknowledged &&
            (event.response.isSafe || event.penaltyApplied)
    } == true
    if (!guidancePending && !feedbackPending) return
    val event = if (guidancePending) checkNotNull(guidanceEvent) else checkNotNull(feedbackEvent)

    val text = if (guidancePending) {
        stringResource(
            if (event.scenario == SecurityMessageScenario.CONFIRMATION_CODE) {
                R.string.messages_guidance_code
            } else {
                R.string.messages_guidance_link
            },
        )
    } else {
        val safe = event.response?.isSafe == true
        when (event.scenario) {
            SecurityMessageScenario.CONFIRMATION_CODE -> if (safe) {
                stringResource(R.string.messages_safe_code_feedback)
            } else {
                stringResource(R.string.messages_unsafe_code_feedback, event.penaltyRub ?: 0L)
            }
            SecurityMessageScenario.UNKNOWN_LINK -> if (safe) {
                stringResource(R.string.messages_safe_link_feedback)
            } else {
                stringResource(R.string.messages_unsafe_link_feedback, event.penaltyRub ?: 0L)
            }
        }
    }
    FinPetDialogueDialog(
        speakerName = stringResource(R.string.messages_pet_name),
        cards = listOf(text),
        portrait = portrait,
        onFinished = {
            if (guidancePending) onGuidanceDismissed(event.id) else onFeedbackDismissed(event.id)
        },
    )
}

@Preview(name = "Сообщения", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun MessagesAppPreview() {
    val event = SecurityMessageEvent(
        id = "security-event-2",
        absoluteDay = 2,
        scenario = SecurityMessageScenario.CONFIRMATION_CODE,
        senderId = MessageSenderId.UNKNOWN_1,
        guidanceVisible = true,
    )
    val message = PhoneMessage(
        id = "security-event-2-request",
        eventId = event.id,
        senderId = MessageSenderId.UNKNOWN_1,
        absoluteDay = 2,
        kind = MessageKind.REQUEST_CONFIRMATION_CODE,
    )
    val state = MessagesViewState(
        inbox = github.detrig.feature.phone.domain.MessagesInbox(
            threads = listOf(
                MessageThread(MessageSenderId.BANK, emptyList(), 0, null),
                MessageThread(MessageSenderId.UNKNOWN_1, listOf(message), 1, event),
                MessageThread(MessageSenderId.UNKNOWN_2, emptyList(), 0, null),
            ),
            unreadCount = 1,
            firstRoomPromptPending = true,
        ),
        selectedSenderId = MessageSenderId.UNKNOWN_1,
    )
    FinPetTheme {
        Box {
            MessagesApp(
                state = state,
                onBack = {},
                onThreadOpened = {},
                onThreadClosed = {},
                onSuspiciousInteraction = { _, _ -> },
                onParentHelpOfferOpened = {},
                onParentHelpAccepted = {},
                onParentHelpDismissed = {},
            )
            MessagesPetDialogue(
                state = state,
                portrait = { modifier ->
                    Box(modifier.background(AppTheme.colors.currencyContainer))
                },
                onGuidanceDismissed = {},
                onFeedbackDismissed = {},
            )
        }
    }
}
