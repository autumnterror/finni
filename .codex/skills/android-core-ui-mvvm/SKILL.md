---
name: android-core-ui-mvvm
description: Создавать и менять Android ViewModel, UI-state, события, одноразовые команды и обработку ошибок через CoreViewModel, CoreViewState, CoreViewEvent, CommandsQueue и SimpleLce. Для визуальной Compose-разметки и превью использовать android-core-compose-ui.
metadata:
  short-description: Android MVVM and UI state
---

# Android Core MVVM And UI State

Используй этот skill, когда нужно создать или изменить ViewModel, state, event, command, loading/content/error состояние, обработку ошибок или связать UI с состоянием.

Визуальная Compose-разметка, дизайн-система и превью описаны в `android-core-compose-ui`. Читай его, если задача затрагивает отрисовку; не дублируй здесь его правила.

Не создавай и не обновляй README или документацию модуля/feature автоматически после реализации. Пиши их только по явному запросу пользователя; существующие документы можно читать как контекст.

## Перед правками

| Файл | Зачем читать |
|------|--------------|
| `guides/ui_mvvm_notes.md` | общий гайд UI/MVVM |
| `core/src/main/java/github/detrig/core/mvvm/CoreViewModel.kt` | базовый ViewModel |
| `core/src/main/java/github/detrig/core/mvvm/CoreViewState.kt` | marker для state |
| `core/src/main/java/github/detrig/core/mvvm/CoreViewEvent.kt` | marker для event |
| `core/src/main/java/github/detrig/core/mvvm/command/ViewCommand.kt` | marker для command |
| `core/src/main/java/github/detrig/core/mvvm/command/CommandsQueue.kt` | очередь одноразовых команд |
| `core/src/main/java/github/detrig/core/mvvm/command/ComposeCommandsQueue.kt` | Compose effect для команд |
| `core/src/main/java/github/detrig/core/utils/lce/simple/SimpleLce.kt` | loading/content/error модель |

## Основной поток

```text
Composable
    -> onEvent(ProfileViewEvent.Load)
    -> ViewModel.perform(event)
    -> launchCoroutine { useCase() }
    -> updateState { ... }
    -> state(): LiveData<State>
```

UI не должен напрямую вызывать repository, Retrofit API, DAO или `GlobalNavigator`.

## State и Event

```kotlin
data class ProfileViewState(
    val isLoading: Boolean = false,
    val name: String = "",
    val errorMessage: String? = null,
) : CoreViewState
```

```kotlin
sealed interface ProfileViewEvent : CoreViewEvent {
    data object Load : ProfileViewEvent
    data object BackClicked : ProfileViewEvent
    data class DetailsClicked(val userId: String) : ProfileViewEvent
}
```

State описывает, что рисовать. Event описывает, что произошло.

## ViewModel

```kotlin
internal class ProfileViewModel(
    private val loadProfileUseCase: LoadProfileUseCase,
    private val router: ProfileRouter,
) : CoreViewModel<ProfileViewState, ProfileViewEvent>(
    initialState = ProfileViewState(),
) {

    override fun perform(viewEvent: ProfileViewEvent) {
        when (viewEvent) {
            ProfileViewEvent.Load -> loadProfile()
            ProfileViewEvent.BackClicked -> router.back()
            is ProfileViewEvent.DetailsClicked -> router.openDetails(viewEvent.userId)
        }
    }

    private fun loadProfile() = launchCoroutine(
        handleAction = ExceptionConsumer { exception ->
            updateState {
                copy(
                    isLoading = false,
                    errorMessage = exception.message,
                )
            }
            true
        },
    ) {
        updateState { copy(isLoading = true, errorMessage = null) }
        val profile = loadProfileUseCase()
        updateState { copy(isLoading = false, name = profile.name) }
    }
}
```

Используй `launchCoroutine`, чтобы ошибки проходили через `CoroutineErrorHandler`.

## Commands

Command нужен для одноразовых действий: snackbar, dialog, внешнее действие или навигационное событие, если feature выбрала такой стиль.

```kotlin
sealed interface ProfileCommand : ViewCommand {
    data class ShowMessage(val message: String) : ProfileCommand
}
```

```kotlin
commands.onNext(ProfileCommand.ShowMessage("Профиль обновлен"))
```

В Compose подписывайся через локальный helper:

```kotlin
CommandsQueueEffect(
    commands = ImmutableCommandsQueue(viewModel.commands<ProfileCommand>()),
) { command ->
    when (command) {
        is ProfileCommand.ShowMessage -> snackbarHostState.showSnackbar(command.message)
    }
}
```

## SimpleLce

Для простых экранов можно использовать готовое состояние `SimpleLceViewState<T>`.

```kotlin
typealias ProfileViewState = SimpleLceViewState<Profile>

private fun loadProfile() = launchCoroutine(
    handleAction = ExceptionConsumer { exception ->
        updateState(ProfileViewState.error(exception))
        true
    },
) {
    updateState(ProfileViewState.loading())
    updateState(ProfileViewState.content(loadProfileUseCase()))
}
```

## Что нельзя

| Нельзя | Используй |
|--------|-----------|
| `viewModelScope.launch` напрямую | `launchCoroutine { }` |
| хранить snackbar/navigation как state-флаг | `ViewCommand` |
| дергать repository из Composable | Event -> ViewModel -> UseCase |
| импортировать `Context`, `View`, `R` во ViewModel | передавай готовые данные/ресурсы через зависимости |
| вводить StateFlow/MVI framework без запроса | существующий LiveData core |

## Чеклист

- [ ] State реализует `CoreViewState`.
- [ ] Event реализует `CoreViewEvent`.
- [ ] ViewModel наследуется от `CoreViewModel<State, Event>`.
- [ ] Асинхронная работа идет через `launchCoroutine`.
- [ ] Ошибки экрана обрабатываются через `ExceptionConsumer`, если нужен локальный UI-state.
- [ ] Одноразовые действия идут через `commands.onNext(...)`.
- [ ] Если изменен визуальный Compose UI, применен `android-core-compose-ui`.
