# Exceptions core

Этот блок core нужен, чтобы ошибки в приложении проходили по одному понятному пути:

```text
Throwable
    -> CoreErrorHandler.mapToAppException(...)
    -> CoreExceptionMapper
    -> AppException
    -> ExceptionConsumer
    -> CoreErrorHandler.handleException(...) / recordException(...)
```

То есть ViewModel не должна каждый раз сама решать, как превращать `Throwable` в ошибку приложения. Этим занимается core.

## AppException

`AppException` - базовая ошибка приложения.

```kotlin
abstract class AppException(
    val type: ExceptionType,
    val innerException: Throwable? = null,
) : RuntimeException(innerException) {

    abstract val moduleCode: String
    abstract val localCode: String
}
```

- Наследуется от `RuntimeException`, поэтому ее можно `throw`.
- Хранит тип ошибки через `ExceptionType`.
- Хранит исходную ошибку в `innerException`.
- Имеет `moduleCode` и `localCode` для будущих кодов ошибок.

Пример конкретной ошибки:

```kotlin
class NetworkConnectionException(
    inner: Throwable? = null,
) : AppException(ExceptionType.Network, inner) {

    override val moduleCode: String
        get() = ""

    override val localCode: String
        get() = ""
}
```

В текущем core уже есть базовые наследники:

- `UnknownApplicationException` - неизвестная ошибка приложения;
- `NetworkConnectionException` - нет подключения к сети;
- `UnknownNetworkException` - неизвестная сетевая ошибка.
- `ServerException` - сервер ответил HTTP-ошибкой.

## ExceptionType

`ExceptionType` - категория ошибки.

```kotlin
sealed class ExceptionType(val shortCode: String) {
    data object Network : ExceptionType("NET")
    data object AndroidInternal : ExceptionType("AI")
    data object Database : ExceptionType("DB")
    data object Application : ExceptionType("APP")
    data object BusinessLogic : ExceptionType("BL")
}
```

Зачем это нужно:

- сетевые ошибки можно обрабатывать отдельно;
- ошибки базы данных можно логировать отдельно;
- бизнес-ошибки можно показывать пользователю иначе;
- в логах видно, к какой группе относится ошибка.

## UnknownApplicationException

`UnknownApplicationException` - fallback-ошибка.

```kotlin
data class UnknownApplicationException(
    val inner: Throwable? = null,
) : AppException(ExceptionType.Application, inner)
```

Она нужна на случай, если в coroutine прилетел обычный `Throwable`, а не `AppException`.

Например:

```kotlin
throw IllegalStateException("Unexpected state")
```

Core превратит это в:

```kotlin
UnknownApplicationException(inner = originalThrowable)
```

## NetworkConnectionException

`NetworkConnectionException` - ошибка для ситуации, когда у пользователя нет сети.

```kotlin
throw NetworkConnectionException()
```

Обычно такую ошибку будет создавать mapper, когда сетевой слой поймет, что интернета нет.

## UnknownNetworkException

`UnknownNetworkException` - fallback для сетевых ошибок, которые не удалось распознать точнее.

```kotlin
throw UnknownNetworkException(originalThrowable)
```

## ServerException

`ServerException` - ошибка ответа сервера.

```kotlin
throw ServerException(
    code = 500,
    errorBody = responseBody,
    inner = httpException,
)
```

Обычно руками ее не бросают. Сетевой слой может создавать ее из `HttpException`.

## CoreErrorHandler

`CoreErrorHandler` - глобальная точка обработки ошибок.

```kotlin
object CoreErrorHandler {

    fun mapToAppException(exception: Throwable): AppException {
        return exception as? AppException ?: UnknownApplicationException(exception)
    }

    fun handleException(exception: Throwable) {
        exceptionHandler?.invoke(exception)
    }

    fun recordException(exception: AppException) {
        exceptionRecorder?.invoke(exception)
    }
}
```

За что отвечает:

- `mapToAppException()` приводит любую ошибку к `AppException`;
- `handleException()` запускает дефолтную обработку;
- `recordException()` отправляет ошибку в recorder, например в Crashlytics в будущем.

Инициализировать его удобно на старте приложения:

```kotlin
CoreErrorHandler.init(
    handler = { exception ->
        // дефолтная обработка ошибки
    },
    recorder = { appException ->
        // логирование ошибки
    }
)
```

## CoroutineErrorHandler

`CoroutineErrorHandler` - обработчик ошибок coroutine.

Он подключается внутри `CoreViewModel.launchCoroutine`:

```kotlin
fun launchCoroutine(
    handleAction: ExceptionConsumer = ExceptionConsumer { false },
    function: suspend CoroutineScope.() -> Unit,
): Job {
    return viewModelScope.launch(CoroutineErrorHandler(handleAction)) {
        function()
    }
}
```

Когда внутри coroutine происходит ошибка:

```kotlin
private fun loadProfile() = launchCoroutine {
    val profile = loadProfileUseCase()
    updateState(ProfileState.Content(profile))
}
```

цепочка такая:

```text
loadProfileUseCase() throws Throwable
    -> CoroutineErrorHandler.handleException(...)
    -> CoreErrorHandler.mapToAppException(...)
    -> ExceptionConsumer.consume(...)
```

## ExceptionConsumer

`ExceptionConsumer` - локальный обработчик ошибки конкретного экрана.

```kotlin
fun interface ExceptionConsumer {
    fun consume(appException: AppException): Boolean
}
```

Он возвращает `Boolean`:

- `true` - экран сам обработал ошибку, дефолтная обработка не нужна;
- `false` - экран не обработал ошибку, нужно отдать ее в `CoreErrorHandler`.

## Локальная обработка

Если экран сам показывает ошибку:

```kotlin
private fun loadProfile() = launchCoroutine(
    handleAction = ExceptionConsumer { error ->
        updateState(ProfileState.Error(error))
        true
    }
) {
    updateState(ProfileState.Loading)

    val profile = loadProfileUseCase()

    updateState(ProfileState.Content(profile))
}
```

Здесь `true` означает: "я уже обработал ошибку в этой ViewModel".

Что произойдет:

```text
Throwable
    -> AppException
    -> ExceptionConsumer
    -> true
    -> CoreErrorHandler.recordException(...)
```

## Дефолтная обработка

Если экран не знает, что делать с ошибкой:

```kotlin
private fun loadProfile() = launchCoroutine(
    handleAction = ExceptionConsumer { false }
) {
    val profile = loadProfileUseCase()
    updateState(ProfileState.Content(profile))
}
```

Или короче:

```kotlin
private fun loadProfile() = launchCoroutine {
    val profile = loadProfileUseCase()
    updateState(ProfileState.Content(profile))
}
```

Что произойдет:

```text
Throwable
    -> AppException
    -> ExceptionConsumer
    -> false
    -> CoreErrorHandler.handleException(...)
```

## Зачем это нужно

Без общей схемы каждый экран начал бы писать свой `try/catch`:

```kotlin
try {
    loadProfileUseCase()
} catch (e: Throwable) {
    // что-то делаем
}
```

С core-подходом экран пишет только то, что относится к нему:

```kotlin
launchCoroutine(
    handleAction = ExceptionConsumer { error ->
        updateState(ProfileState.Error(error))
        true
    }
) {
    loadProfileUseCase()
}
```

А общие вещи остаются в core:

- маппинг ошибок;
- дефолтная обработка;
- логирование;
- единый тип ошибки приложения.

## Коротко

- `AppException` - базовая ошибка приложения.
- `ExceptionType` - категория ошибки.
- `UnknownApplicationException` - fallback для обычного `Throwable`.
- `CoreErrorHandler` - глобальная обработка и логирование.
- `CoroutineErrorHandler` - ловит ошибки coroutine.
- `ExceptionConsumer` - локальная обработка ошибки во ViewModel.
- `true` в `ExceptionConsumer` - обработали локально.
- `false` в `ExceptionConsumer` - отдаем в общий обработчик.

