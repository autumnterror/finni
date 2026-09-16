# UI / MVVM core

Этот блок core нужен, чтобы все экраны в приложении писались одинаково:

- экран отправляет `Event` во `ViewModel`;
- `ViewModel` запускает работу, например запрос в сеть;
- результат кладется в `State`;
- экран подписан на `State` и перерисовывается;
- одноразовые действия уходят через `Command`.

Общая схема:

```text
Screen
    -> ViewModel.perform(Event)
    -> launchCoroutine { UseCase / Repository }
    -> updateState(...)
    -> LiveData<State>
    -> Screen renders State

One-time actions:
ViewModel.commands.onNext(Command)
    -> CommandsQueue / CommandsQueueEffect
    -> navigation, snackbar, dialog
```

## CoreViewState

`CoreViewState` - базовый marker-interface для состояния экрана.

```kotlin
data class ProfileState(
    val isLoading: Boolean,
    val name: String,
) : CoreViewState
```

- `State` описывает, что сейчас должно быть на экране.
- `State` хранит данные для UI.
- `State` не должен запускать логику сам.

## CoreViewEvent

`CoreViewEvent` - базовый marker-interface для событий, которые экран отправляет во `ViewModel`.

```kotlin
sealed interface ProfileEvent : CoreViewEvent {
    data object Load : ProfileEvent
    data class OpenDetails(val id: String) : ProfileEvent
}
```

- `Event` описывает действие пользователя или системное событие.
- Экран не вызывает use case напрямую.
- Экран говорит `ViewModel`: "произошло событие".

## CoreViewModel

`CoreViewModel` - базовый класс для всех ViewModel.

```kotlin
class ProfileViewModel(
    private val loadProfileUseCase: LoadProfileUseCase,
) : CoreViewModel<ProfileState, ProfileEvent>(
    initialState = ProfileState(isLoading = false, name = "")
) {

    override fun perform(viewEvent: ProfileEvent) {
        when (viewEvent) {
            ProfileEvent.Load -> loadProfile()
            is ProfileEvent.OpenDetails -> commands.onNext(ProfileCommand.OpenDetails(viewEvent.id))
        }
    }

    private fun loadProfile() = launchCoroutine {
        updateState { copy(isLoading = true) }
        val profile = loadProfileUseCase()
        updateState { copy(isLoading = false, name = profile.name) }
    }
}
```

- `perform()` принимает события от UI.
- `state()` отдает `LiveData<State>` наружу.
- `launchCoroutine()` запускает coroutine в `viewModelScope`.
- `updateState()` обновляет состояние на main thread.
- `updateStateFromIo()` обновляет состояние через `postValue`.
- `commands` используется для одноразовых UI-команд.

## ViewModelConfig

`ViewModelConfig` хранит настройки, которые нужны `ViewModel`.

```kotlin
interface ViewModelConfig<S> {
    val dispatcherProvider: CoroutinesDispatcherProvider
    val mutableLiveData: MutableLiveData<S>
}
```

- `dispatcherProvider` дает dispatchers для coroutine.
- `mutableLiveData` позволяет подменять `LiveData` при создании `ViewModel`.
- `DefaultViewModelConfig` дает стандартные значения.

## LiveDataExt

`LiveDataExt` - набор extension-функций для удобной работы с `LiveData`.

- `onNext(value)` - записать новое значение в `MutableLiveData`.
- `delegate()` - использовать `MutableLiveData` как Kotlin property.
- `requireValue()` - получить текущее значение или упасть, если его нет.
- `observeNotNull()` - подписаться только на non-null значения.
- `mapDistinct()` - сделать `map` и `distinctUntilChanged`.
- `filter()` - пропускать только значения по условию.
- `filterIsInstance()` - пропускать только значения нужного типа.

Пример:

```kotlin
protected var stateData: ProfileState by state.delegate()

private fun setName(name: String) {
    stateData = stateData.copy(name = name)
}
```

## ViewCommand

`ViewCommand` - marker-interface для одноразовых действий UI.

```kotlin
sealed interface ProfileCommand : ViewCommand {
    data class OpenDetails(val id: String) : ProfileCommand
    data class ShowError(val message: String) : ProfileCommand
}
```

- `State` хранит стабильное состояние экрана.
- `Command` хранит действие, которое нужно выполнить один раз.
- Навигация, snackbar, toast, dialog обычно идут через `Command`.

## CommandsQueue

`CommandsQueue` - очередь одноразовых команд на базе `LiveData`.

```kotlin
commands.onNext(ProfileCommand.ShowError("Ошибка загрузки"))
```

- Команды складываются в очередь.
- UI получает команду.
- После обработки команда удаляется из очереди.

Это нужно, чтобы одноразовое действие не повторялось после пересоздания экрана.

## ComposeCommandsQueue

`CommandsQueueEffect` - Compose-обертка для подписки на `CommandsQueue`.

```kotlin
CommandsQueueEffect(viewModel.commands()) { command ->
    when (command) {
        is ProfileCommand.OpenDetails -> router.openDetails(command.id)
        is ProfileCommand.ShowError -> snackbarHostState.showSnackbar(command.message)
    }
}
```

- Внутри используется `LocalLifecycleOwner`.
- Подписка создается через `DisposableEffect`.
- При уходе composable из composition observer удаляется.

## ExceptionConsumer

`ExceptionConsumer` - функция, которая позволяет ViewModel обработать ошибку локально.

```kotlin
launchCoroutine(
    handleAction = ExceptionConsumer { exception ->
        commands.onNext(ProfileCommand.ShowError(exception.message.orEmpty()))
        true
    }
) {
    loadProfileUseCase()
}
```

- Используется в `launchCoroutine`.
- Нужен, когда конкретный экран хочет сам решить, что делать с ошибкой.
- Например показать snackbar, dialog или специальный error-state.

## AppException

`AppException` - базовый тип ошибки приложения, сделан по аналогии с `RmException` из эталонного core.

```kotlin
abstract class AppException(
    val type: ExceptionType,
    val innerException: Throwable? = null,
) : RuntimeException(innerException) {

    abstract val moduleCode: String
    abstract val localCode: String
}
```

- `type` показывает категорию ошибки: сеть, база данных, бизнес-логика и т.д.
- `innerException` хранит исходную ошибку.
- `moduleCode` и `localCode` нужны, если позже появятся коды ошибок по модулям.

## ExceptionType

`ExceptionType` - тип ошибки приложения.

```kotlin
sealed class ExceptionType(val shortCode: String) {
    data object Network : ExceptionType("NET")
    data object AndroidInternal : ExceptionType("AI")
    data object Database : ExceptionType("DB")
    data object Application : ExceptionType("APP")
    data object BusinessLogic : ExceptionType("BL")
}
```

Это нужно, чтобы ошибки можно было группировать: сетевые отдельно, ошибки БД отдельно, ошибки бизнес-логики отдельно.

## CoroutineErrorHandler

`CoroutineErrorHandler` - обработчик ошибок coroutine.

- Подключается внутри `CoreViewModel.launchCoroutine`.
- Получает exception, если coroutine упала.
- Должен передать ошибку в `ExceptionConsumer` или общий обработчик ошибок.

## CoreErrorHandler

`CoreErrorHandler` - место для общей обработки ошибок приложения.

- `mapToAppException()` приводит любой `Throwable` к `AppException`.
- Сюда можно вынести маппинг сетевых ошибок.
- Сюда можно вынести логирование.
- Сюда можно вынести дефолтное поведение, если экран сам ошибку не обработал.

Подробнее вся цепочка описана в `exception_notes.md`.



## Global messages

Для всплывающих сообщений уровня приложения используй `GlobalMessageController`, а не локальные `CommandsQueue` в каждой feature.

Обычный путь:

```text
ViewModel
    -> Router.showMessage(...) / Router.showErrorMessage(...)
    -> GlobalMessageController
    -> GlobalMessageHost в Nav3Activity
```

Feature-router скрывает общий controller от ViewModel:

```kotlin
internal interface ProfileRouter {
    fun showErrorMessage(message: String)
}
```

```kotlin
internal class ProfileRouterImpl(
    private val messageController: GlobalMessageController,
) : ProfileRouter {

    override fun showErrorMessage(message: String) {
        messageController.showErrorMessage(message)
    }
}
```

`CommandsQueue` оставляй для одноразовых действий конкретного экрана, которые не являются общим app-message: например локальный dialog-result, scroll-to-item или screen-specific effect.

## SimpleLce

`SimpleLce` - модель состояния `Loading / Content / Error`.

```kotlin
SimpleLce.loading()
SimpleLce.content(profile)
SimpleLce.error(error)
```

- `Loading` - данные загружаются.
- `Content` - данные успешно получены.
- `Error` - загрузка завершилась ошибкой.

Внутри `SimpleLceImpl` это хранится через `Result<T?>`:

- `Result.success(null)` - loading;
- `Result.success(value)` - content;
- `Result.failure(error)` - error.



## SimpleLceViewState

`SimpleLceViewState` - готовый `CoreViewState` для простых экранов.

```kotlin
typealias ProfileState = SimpleLceViewState<Profile>

class ProfileViewModel(
    private val loadProfileUseCase: LoadProfileUseCase,
) : CoreViewModel<ProfileState, ProfileEvent>(
    initialState = ProfileState.loading()
) {

    override fun perform(viewEvent: ProfileEvent) {
        when (viewEvent) {
            ProfileEvent.Load -> loadProfile()
        }
    }

    private fun loadProfile() = launchCoroutine {
        updateState(ProfileState.loading())
        val profile = loadProfileUseCase()
        updateState(ProfileState.content(profile))
    }
}
```

- Удобен, когда экрану достаточно трех состояний.
- Не нужно каждый раз писать свой `LoadingState`, `ContentState`, `ErrorState`.
- Можно использовать `fold` или `handle`, чтобы разобрать состояние.



## SimpleLceExt

`SimpleLceExt` содержит конвертацию `Result<T>` в `SimpleLce<T>`.

```kotlin
val state = result.toLce()
```

- `Result.success(value)` станет `SimpleLce.content(value)`.
- `Result.failure(error)` станет `SimpleLce.error(error)`.



## SimpleLceException

`SimpleLceException` - wrapper для ошибки внутри `SimpleLce`.

- Нужен, чтобы error-state был отдельным типом ошибки.
- Позволяет хранить исходный `Throwable` в `cause`.

## ThrowableExt

`ThrowableExt` содержит helper для получения сообщения ошибки.

```kotlin
val message = throwable.getUiMessage("Что-то пошло не так")
```

- Если есть понятное сообщение, вернет его.
- Если сообщения нет, вернет дефолтный текст.

## Коротко

- `CoreViewState` - что рисовать.
- `CoreViewEvent` - что произошло.
- `CoreViewModel` - где живет логика экрана.
- `ViewCommand` - одноразовое действие.
- `CommandsQueue` - доставка одноразовых действий.
- `SimpleLce` - loading/content/error для данных.
- `SimpleLceViewState` - готовый state для простых экранов.
- `CoroutineErrorHandler` и `CoreErrorHandler` - будущая общая обработка ошибок.
