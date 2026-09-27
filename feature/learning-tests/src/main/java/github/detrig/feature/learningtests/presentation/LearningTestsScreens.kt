package github.detrig.feature.learningtests.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetIconButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.learningtests.LearningTestsFeature
import github.detrig.feature.learningtests.R
import github.detrig.feature.learningtests.domain.DailyLearningTests
import github.detrig.feature.learningtests.domain.LearningTestAnswer
import github.detrig.feature.learningtests.domain.LearningTestAttempt
import github.detrig.feature.learningtests.domain.LearningTestDefinition
import github.detrig.feature.learningtests.domain.LearningTestDifficulty
import github.detrig.feature.learningtests.domain.LearningTestOffer
import github.detrig.feature.learningtests.domain.LearningTestOfferStatus
import github.detrig.feature.learningtests.domain.LearningTestQuestion
import github.detrig.feature.learningtests.domain.LearningTestSession

@Composable
internal fun LearningTestsHomeScreen(
    vm: LearningTestsHomeViewModel = viewModel { LearningTestsFeature.component().homeViewModel() },
) {
    val state by vm.state().observeAsState(LearningTestsHomeViewState())
    LaunchedEffect(vm) { vm.perform(LearningTestsHomeViewEvent.Load) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(vm, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.perform(LearningTestsHomeViewEvent.Refresh)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LearningTestsHomeContent(
        state = state,
        onClose = { vm.perform(LearningTestsHomeViewEvent.Close) },
        onRetry = { vm.perform(LearningTestsHomeViewEvent.Load) },
        onOpenTest = { vm.perform(LearningTestsHomeViewEvent.OpenTest(it)) },
    )
}

@Composable
internal fun LearningTestQuizScreen(
    testId: String,
    vm: LearningTestQuizViewModel = viewModel(key = "learning-test-$testId") {
        LearningTestsFeature.component().quizViewModel(testId)
    },
) {
    val state by vm.state().observeAsState(LearningTestQuizViewState())
    LaunchedEffect(vm) { vm.perform(LearningTestQuizViewEvent.Load) }
    BackHandler { vm.perform(LearningTestQuizViewEvent.Close) }
    LearningTestQuizContent(
        state = state,
        onClose = { vm.perform(LearningTestQuizViewEvent.Close) },
        onSelectOption = { vm.perform(LearningTestQuizViewEvent.SelectOption(it)) },
        onSubmit = { vm.perform(LearningTestQuizViewEvent.SubmitAnswer) },
        onContinue = { vm.perform(LearningTestQuizViewEvent.Continue) },
        onRetry = { vm.perform(LearningTestQuizViewEvent.Load) },
    )
}

@Composable
private fun LearningTestsHomeContent(
    state: LearningTestsHomeViewState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onOpenTest: (String) -> Unit,
) {
    NotebookPage(onClose = onClose) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 34.dp, end = 32.dp, top = maxHeight * 0.145f, bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.learning_tests_daily_title),
                    style = AppTheme.typography.brand,
                    color = AppTheme.colors.dialogue.onPanel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                val daily = state.dailyTests
                when {
                    state.isLoading -> Text(
                        text = stringResource(R.string.learning_tests_loading),
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textSecondary,
                    )
                    state.hasError -> {
                        Text(
                            text = stringResource(R.string.learning_tests_load_error),
                            style = AppTheme.typography.bodyStrong,
                            color = AppTheme.colors.dialogue.onPanel,
                            textAlign = TextAlign.Center,
                        )
                        QuizActionButton(stringResource(R.string.learning_tests_retry), onRetry, enabled = true)
                    }
                    daily?.offers.isNullOrEmpty() -> Text(
                        text = stringResource(R.string.learning_tests_all_mastered),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.dialogue.onPanel,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = AppTheme.spacing.xl),
                    )
                    else -> {
                        val availableTests = requireNotNull(daily)
                        Text(
                            text = stringResource(R.string.learning_tests_daily_count, availableTests.gameDay),
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(AppTheme.spacing.xs))
                        availableTests.offers.forEach { offer ->
                            DailyTestCard(offer = offer, onClick = { onOpenTest(offer.test.id) })
                        }
                        Text(
                            text = stringResource(R.string.learning_tests_try_again_later),
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTestCard(
    offer: LearningTestOffer,
    onClick: () -> Unit,
) {
    val isFinished = offer.status == LearningTestOfferStatus.COMPLETED_WITH_MISTAKES
    val enabled = !isFinished
    val isInProgress = offer.status == LearningTestOfferStatus.IN_PROGRESS
    FinPetCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .alpha(if (isFinished) 0.72f else 1f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        containerColor = if (isInProgress) AppTheme.colors.storefront.selectedSurface
        else AppTheme.colors.storefront.surface,
        borderColor = if (isInProgress) AppTheme.colors.statusPositive.accent
        else AppTheme.colors.storefront.outline,
        borderWidth = AppTheme.sizes.borderStrong,
        elevation = AppTheme.elevation.low,
        shape = AppTheme.shapes.storefrontControl,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DailyTestRadio(selected = isInProgress)
            Spacer(Modifier.width(AppTheme.spacing.sm))
            val statusText = when (offer.status) {
                LearningTestOfferStatus.NEW -> stringResource(R.string.learning_tests_start)
                LearningTestOfferStatus.IN_PROGRESS -> stringResource(
                    R.string.learning_tests_resume_question,
                    (offer.questionIndex ?: 0) + 1,
                    offer.test.questions.size,
                )
                LearningTestOfferStatus.COMPLETED_WITH_MISTAKES -> stringResource(
                    R.string.learning_tests_finished_for_today,
                    offer.mistakeCount,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
            ) {
                Text(
                    text = offer.test.title,
                    style = AppTheme.typography.sectionTitle,
                    color = AppTheme.colors.dialogue.onPanel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.learning_tests_difficulty_summary,
                        offer.test.difficulty.title,
                        offer.test.questions.size,
                    ) + " · " + stringResource(
                        R.string.learning_tests_reward_compact,
                        offer.test.difficulty.perfectRewardRub,
                        offer.test.maxXp,
                    ),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText,
                    style = AppTheme.typography.label,
                    color = if (enabled) AppTheme.colors.statusPositive.onContainer else AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DailyTestRadio(selected: Boolean) {
    val radioSurface = AppTheme.colors.storefront.surface
    val radioOutline = AppTheme.colors.storefront.outline
    val radioFill = AppTheme.colors.statusPositive.accent
    val radioBorderWidth = AppTheme.sizes.borderStrong
    Canvas(Modifier.size(34.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = radioSurface, radius = size.minDimension / 2f)
        drawCircle(
            color = radioOutline,
            radius = size.minDimension / 2f - radioBorderWidth.toPx() / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = radioBorderWidth.toPx()),
        )
        if (selected) {
            drawCircle(color = radioFill, radius = 10.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun LearningTestQuizContent(
    state: LearningTestQuizViewState,
    onClose: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
) {
    NotebookPage(onClose = onClose) {
        val session = state.session
        when {
            state.isLoading -> Text(
                text = stringResource(R.string.learning_tests_loading),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
            state.hasError -> Column(
                modifier = Modifier.align(Alignment.Center).padding(start = 56.dp, end = 28.dp),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Text(stringResource(R.string.learning_tests_load_error), style = AppTheme.typography.body)
                QuizActionButton(stringResource(R.string.learning_tests_retry), onRetry, enabled = true)
            }
            state.unavailable || session == null -> Text(
                text = stringResource(R.string.learning_tests_no_longer_available),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(start = 56.dp, end = 28.dp),
            )
            session.attempt.isComplete -> QuizResult(
                session = session,
                onBack = onClose,
                modifier = Modifier.fillMaxSize(),
            )
            else -> QuizQuestion(
                session = session,
                selectedOptionIndex = state.selectedOptionIndex,
                isBusy = state.isBusy,
                onSelectOption = onSelectOption,
                onSubmit = onSubmit,
                onContinue = onContinue,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun QuizQuestion(
    session: LearningTestSession,
    selectedOptionIndex: Int?,
    isBusy: Boolean,
    onSelectOption: (Int) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val pageHeight = maxHeight
        val horizontalInset = 34.dp
        val prompt = session.question?.prompt.orEmpty()
        val answerHeight = (pageHeight * 0.094f).coerceIn(58.dp, 76.dp)
        val answersRegionHeight = minOf(
            pageHeight * 0.425f,
            (pageHeight * 0.595f - 70.dp - pageHeight * 0.035f - AppTheme.spacing.lg)
                .coerceAtLeast(80.dp),
        )
        val answersScroll = rememberScrollState()
        LaunchedEffect(session.attempt.questionIndex) { answersScroll.scrollTo(0) }

        RewardChip(
            test = session.test,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = pageHeight * 0.073f),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = pageHeight * 0.166f),
        ) {
            QuestionProgress(session)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = pageHeight * 0.245f)
                .fillMaxWidth()
                .height(pageHeight * 0.158f)
                .padding(start = horizontalInset + 4.dp, end = horizontalInset),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = prompt,
                style = when {
                    prompt.length < 48 -> AppTheme.typography.brand
                    prompt.length < 76 -> AppTheme.typography.screenTitle
                    prompt.length < 106 -> AppTheme.typography.sectionTitle
                    else -> AppTheme.typography.bodyStrong
                },
                color = AppTheme.colors.dialogue.onPanel,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = pageHeight * 0.405f)
                .fillMaxWidth()
                .height(answersRegionHeight)
                .padding(start = horizontalInset, end = horizontalInset - 2.dp)
                .verticalScroll(answersScroll)
                .padding(bottom = AppTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            session.question?.options.orEmpty().forEachIndexed { index, option ->
                val answer = session.answer
                QuizAnswerCard(
                    text = option,
                    height = answerHeight,
                    selected = if (answer == null) selectedOptionIndex == index
                    else index == session.question?.correctOptionIndex,
                    incorrectSelection = answer != null && !answer.isCorrect && answer.selectedOptionIndex == index,
                    enabled = answer == null && !isBusy,
                    onClick = { onSelectOption(index) },
                )
            }
            session.answer?.let { answer ->
                val correctAnswer = session.question?.options?.getOrNull(session.question.correctOptionIndex).orEmpty()
                Text(
                    text = if (answer.isCorrect) stringResource(R.string.learning_tests_answer_correct)
                    else stringResource(R.string.learning_tests_correct_option, correctAnswer),
                    style = AppTheme.typography.bodyStrong,
                    color = if (answer.isCorrect) AppTheme.colors.statusPositive.onContainer
                    else AppTheme.colors.statusWarning.onContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.xs),
                )
            }
        }
        val hasAnswer = session.answer != null
        QuizActionButton(
            text = stringResource(if (hasAnswer) R.string.learning_tests_next_question else R.string.learning_tests_answer_button),
            onClick = if (hasAnswer) onContinue else onSubmit,
            enabled = !isBusy && (hasAnswer || selectedOptionIndex != null),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = pageHeight * 0.035f),
        )
    }
}

@Composable
private fun QuizResult(
    session: LearningTestSession,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        RewardChip(
            test = session.test,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = maxHeight * 0.073f),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * 0.265f)
                .fillMaxWidth()
                .padding(start = 36.dp, end = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xl),
        ) {
            Text(
                text = stringResource(
                    if (session.attempt.isPerfect) R.string.learning_tests_perfect_title
                    else R.string.learning_tests_completed_title,
                ),
                style = AppTheme.typography.brand,
                color = AppTheme.colors.dialogue.onPanel,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (session.attempt.isPerfect) stringResource(
                    R.string.learning_tests_perfect_reward,
                    session.test.difficulty.perfectRewardRub,
                    session.test.maxXp,
                ) else stringResource(
                    R.string.learning_tests_mistake_feedback,
                    session.attempt.mistakeCount,
                ),
                style = AppTheme.typography.bodyStrong,
                color = if (session.attempt.isPerfect) AppTheme.colors.statusPositive.onContainer
                else AppTheme.colors.dialogue.onPanel,
                textAlign = TextAlign.Center,
            )
        }
        QuizActionButton(
            text = stringResource(R.string.learning_tests_back_to_list),
            onClick = onBack,
            enabled = true,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = maxHeight * 0.035f),
        )
    }
}

@Composable
private fun QuestionProgress(session: LearningTestSession) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(
                R.string.learning_tests_question_count,
                session.attempt.questionIndex + 1,
                session.test.questions.size,
            ),
            style = AppTheme.typography.sectionTitle,
            color = AppTheme.colors.dialogue.onPanel,
        )
        Spacer(Modifier.height(AppTheme.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            session.test.questions.indices.forEach { index ->
                val answer = session.answers[index]
                val isCurrent = index == session.attempt.questionIndex
                val fillColor = when {
                    answer?.isCorrect == true -> AppTheme.colors.statusPositive.accent
                    answer != null -> AppTheme.colors.statusCritical.accent
                    isCurrent -> AppTheme.colors.storefront.primaryAction
                    else -> Color.Transparent
                }
                val borderColor = when {
                    answer?.isCorrect == true -> AppTheme.colors.statusPositive.accent
                    answer != null -> AppTheme.colors.statusCritical.accent
                    isCurrent -> AppTheme.colors.statusPositive.accent
                    else -> AppTheme.colors.storefront.outline
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(fillColor)
                        .then(
                            if (answer == null) {
                                Modifier.border(AppTheme.sizes.borderThin, borderColor, CircleShape)
                            } else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (answer?.isCorrect == true) {
                        Text("✓", style = AppTheme.typography.caption, color = AppTheme.colors.onActionPrimary)
                    } else if (answer != null) {
                        Text("×", style = AppTheme.typography.caption, color = AppTheme.colors.onActionPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardChip(test: LearningTestDefinition, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(194.dp).height(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.quiz_reward_chip),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = stringResource(R.string.learning_tests_reward_heading, test.difficulty.perfectRewardRub),
            style = AppTheme.typography.sectionTitle,
            color = AppTheme.colors.dialogue.onPanel,
            maxLines = 1,
            modifier = Modifier.padding(start = 42.dp, end = AppTheme.spacing.sm),
        )
    }
}

@Composable
private fun QuizAnswerCard(
    text: String,
    height: Dp,
    selected: Boolean,
    incorrectSelection: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accessibilityText = text
    val incorrectColors = AppTheme.colors.statusCritical
    FinPetCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = accessibilityText
            }
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = AppTheme.shapes.storefrontControl,
        containerColor = Color.Transparent,
        borderColor = null,
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(
                    if (selected) R.drawable.quiz_answer_selected else R.drawable.quiz_answer_normal,
                ),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
            if (incorrectSelection) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 13.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(incorrectColors.container)
                        .border(AppTheme.sizes.borderThin, incorrectColors.border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        val inset = 1.5.dp.toPx()
                        val stroke = 2.dp.toPx()
                        drawLine(
                            color = incorrectColors.onContainer,
                            start = Offset(inset, inset),
                            end = Offset(size.width - inset, size.height - inset),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = incorrectColors.onContainer,
                            start = Offset(size.width - inset, inset),
                            end = Offset(inset, size.height - inset),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            Text(
                text = text,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.dialogue.onPanel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 60.dp, end = AppTheme.spacing.md, top = AppTheme.spacing.xs, bottom = AppTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun QuizActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseStyle = FinPetButtonDefaults.primaryStyle()
    Box(modifier.fillMaxWidth(0.78f).height(70.dp).alpha(if (enabled) 1f else 0.65f)) {
        Image(
            painter = painterResource(R.drawable.quiz_action_button),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        FinPetButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            enabled = enabled,
            style = baseStyle.copy(
                containerColor = Color.Transparent,
                contentColor = AppTheme.colors.onActionPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = AppTheme.colors.onActionPrimary.copy(alpha = 0.7f),
                borderColor = null,
                disabledBorderColor = null,
                borderWidth = 0.dp,
                shape = AppTheme.shapes.storefrontControl,
                minHeight = AppTheme.sizes.minimumTouchTarget,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AppTheme.spacing.md),
                shadowElevation = AppTheme.elevation.none,
                disabledShadowElevation = AppTheme.elevation.none,
                shadowColor = Color.Transparent,
                textStyle = AppTheme.typography.brand,
            ),
        )
    }
}

@Composable
private fun NotebookPage(
    onClose: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.roomBackground),
    ) {
        Image(
            painter = painterResource(R.drawable.quiz_room_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(-0.45f, 0f),
            modifier = Modifier.fillMaxSize().blur(10.dp),
        )
        Box(Modifier.fillMaxSize().background(AppTheme.colors.roomBackground.copy(alpha = 0.16f)))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f)
                .align(Alignment.Center),
        ) {
            Image(
                painter = painterResource(R.drawable.quiz_notebook_panel),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
            content()
            val closeDescription = stringResource(R.string.learning_tests_close)
            FinPetIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .semantics { contentDescription = closeDescription },
            ) {
                Image(
                    painter = painterResource(R.drawable.quiz_close_button),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(name = "Тесты на сегодня", widthDp = 390, heightDp = 844)
@Composable
private fun LearningTestsHomePreview() {
    FinPetTheme {
        val test = previewTest()
        LearningTestsHomeContent(
            state = LearningTestsHomeViewState(
                isLoading = false,
                dailyTests = DailyLearningTests(
                    gameDay = 2,
                    offers = listOf(
                        LearningTestOffer(test, LearningTestOfferStatus.NEW),
                        LearningTestOffer(
                            test.copy(id = "saving-goal", title = "Копим на мечту"),
                            LearningTestOfferStatus.IN_PROGRESS,
                            questionIndex = 2,
                        ),
                    ),
                ),
            ),
            onClose = {},
            onRetry = {},
            onOpenTest = {},
        )
    }
}

@Preview(name = "Вопрос теста", widthDp = 390, heightDp = 844)
@Composable
private fun LearningTestQuizPreview() {
    FinPetTheme {
        val test = previewTest()
        val session = LearningTestSession(
            test = test,
            attempt = LearningTestAttempt(2, 1, 0, false, false),
            question = test.questions[1],
            answer = null,
            answers = mapOf(0 to LearningTestAnswer(1, true)),
        )
        LearningTestQuizContent(
            state = LearningTestQuizViewState(isLoading = false, session = session, selectedOptionIndex = 0),
            onClose = {},
            onSelectOption = {},
            onSubmit = {},
            onContinue = {},
            onRetry = {},
        )
    }
}

private fun previewTest() = LearningTestDefinition(
    id = "money-flow",
    title = "Куда уходят деньги?",
    difficulty = LearningTestDifficulty.SIMPLE,
    questions = listOf(
        LearningTestQuestion("Что такое расход?", listOf("Деньги, которые ты получил", "Деньги, которые ты потратил", "Деньги в копилке", "Любая монета"), 1),
        LearningTestQuestion("Сколько осталось после покупки?", listOf("120 ₽", "140 ₽", "160 ₽", "260 ₽"), 1),
        LearningTestQuestion("Что относится к доходу?", listOf("Покупка", "Оплата", "Карманные деньги", "Игрушка"), 2),
        LearningTestQuestion("Можно ли купить игрушку дороже бюджета?", listOf("Да", "Нет", "Да, останется", "Цена не важна"), 1),
        LearningTestQuestion("Зачем следить за расходами?", listOf("Быстрее потратить", "Понимать траты", "Снизить цены", "Сделать деньги одинаковыми"), 1),
    ),
)
