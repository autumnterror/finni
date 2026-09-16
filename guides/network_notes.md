# Network core

Этот блок core повторяет минимальную идею из эталонного core.

Важно: в core **нет** `baseUrl`, `NetworkConfig`, `ApiFactory` и сборки Retrofit. Core не знает адрес backend и не создает конкретные API.

Core отвечает за общие сетевые вещи:

- проверить, есть ли сеть;
- превратить сетевые `Throwable` в `AppException`;
- дать общие сетевые ошибки;
- дать marker-аннотацию для измерения времени ответа API.

## Общая схема

```text
App / Mediator layer
    -> создает Retrofit / OkHttp / FeatureRemoteApi

FeatureDependencies
    -> получает FeatureRemoteApi

FeatureModule
    -> создает Repository
    -> создает UseCase
    -> создает ViewModel

Repository
    -> вызывает FeatureRemoteApi
    -> не проверяет NetworkManager вручную

Throwable из repository
    -> CoroutineErrorHandler
    -> CoreErrorHandler.mapToAppException(...)
    -> CoreExceptionMapper
    -> NetworkConnectionException / ServerException / UnknownNetworkException
```

Главная мысль: `NetworkManager` не прокидывается в каждый repository. Он нужен `CoreExceptionMapper`, чтобы понять, есть ли интернет, когда прилетела сетевая ошибка.

## Что лежит в core

В текущем core для сети есть:

```text
core/infrastructure/network/NetworkManager.kt
core/infrastructure/network/NetworkManagerImpl.kt

core/exception/mapper/CoreExceptionMapper.kt
core/exception/NetworkConnectionException.kt
core/exception/UnknownNetworkException.kt
core/exception/ServerException.kt

core/network/interceptor/LogResponseTime.kt
```

## NetworkManager

`NetworkManager` проверяет, есть ли сеть на устройстве.

```kotlin
interface NetworkManager {
    fun isNetworkAvailable(): Boolean
}
```

Он доступен через `CoreComponent`:

```kotlin
interface CoreComponent {
    val networkManager: NetworkManager
}
```

Но обычно feature/repository не берут его напрямую. Его использует `CoreExceptionMapper`.

## NetworkManagerImpl

`NetworkManagerImpl` - Android-реализация.

```kotlin
internal class NetworkManagerImpl(
    private val context: Context,
) : NetworkManager
```

Что делает:

- берет `ConnectivityManager`;
- проверяет активную сеть;
- возвращает `true`, если интернет доступен.

## CoreExceptionMapper

`CoreExceptionMapper` превращает обычный `Throwable` в `AppException`.

```kotlin
open class CoreExceptionMapper(
    private val networkManager: NetworkManager,
) {

    open fun map(from: Throwable): AppException {
        return when (from) {
            is AppException -> from
            is HttpException -> from.toAppException()
            is SSLException, is SocketTimeoutException -> UnknownNetworkException(from)
            is UnknownHostException, is ConnectException, is ErrnoException -> checkInternetConnection(from)
            else -> UnknownApplicationException(from)
        }
    }
}
```

Вот тут и используется `NetworkManager`:

```kotlin
private fun checkInternetConnection(from: Throwable): AppException {
    if (networkManager.isNetworkAvailable().not()) {
        return NetworkConnectionException(from)
    }
    return UnknownNetworkException(from)
}
```

То есть repository не спрашивает сеть сам. Он бросает/пропускает ошибку, а core уже мапит ее правильно.

## CoreModule

В эталонном core `NetworkManager` и `CoreExceptionMapper` создаются в обычном `CoreModule`.

```kotlin
override val networkManager: NetworkManager by lazy {
    NetworkManagerImpl(context)
}

override val coreExceptionMapper: CoreExceptionMapper by lazy {
    CoreExceptionMapper(networkManager)
}
```

`CoreComponent` отдает оба объекта наружу:

```kotlin
interface CoreComponent {
    val networkManager: NetworkManager
    val coreExceptionMapper: CoreExceptionMapper
}
```

## CoreErrorHandler

`CoreErrorHandler` использует `CoreExceptionMapper`.

```kotlin
CoreErrorHandler.init(
    handler = { exception ->
        // дефолтная обработка ошибки
    },
    exceptionMapper = coreComponent.coreExceptionMapper,
    recorder = { appException ->
        // логирование ошибки
    }
)
```

После этого `CoroutineErrorHandler` может сделать так:

```kotlin
val appException = CoreErrorHandler.mapToAppException(exception)
```

## ServerException

`ServerException` - HTTP-ошибка сервера.

```kotlin
open class ServerException(
    val code: Int,
    val body: String,
    inner: Throwable,
) : AppException(ExceptionType.Network, inner)
```

Есть helpers для Retrofit:

```kotlin
fun HttpException.toAppException(): AppException

fun <T> Response<T>.toAppException(): AppException
```

Обычно repository не вызывает их вручную. `HttpException` долетает до `CoreExceptionMapper`, а mapper уже вызывает `toAppException()`.

## NetworkConnectionException

`NetworkConnectionException` - ошибка "нет сети".

```kotlin
NetworkConnectionException(inner)
```

Ее создает `CoreExceptionMapper`, когда прилетела сетевая ошибка и `NetworkManager` сказал, что интернета нет.

## UnknownNetworkException

`UnknownNetworkException` - неизвестная сетевая ошибка.

```kotlin
UnknownNetworkException(inner)
```

Например, если ошибка похожа на сетевую, но интернет на устройстве есть.

## LogResponseTime

`LogResponseTime` - marker-аннотация для API-методов.

```kotlin
@LogResponseTime(eventName = "profile_load", paramType = "profile")
@GET("profile")
suspend fun loadProfile(): ProfileResponse
```

Сама по себе аннотация ничего не делает. Ее должен прочитать код, который создает или оборачивает `RestApi`.

## Где создается Retrofit

В эталонном core Retrofit не создается.

Значит, в новом приложении Retrofit должен жить выше:

```text
app module
    или app/di
    или mediator layer
```

Пример условного app-модуля:

```kotlin
class AppNetworkModule {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .build()
    }

    val profileRemoteApi: ProfileRemoteApi by lazy {
        retrofit.create(ProfileRemoteApi::class.java)
    }
}
```

Это не core. Это уровень приложения, потому что только приложение знает `baseUrl`.

## Что писать в feature dependencies

Feature просит снаружи конкретный API и другие нужные зависимости:

```kotlin
interface ProfileDependencies {

    fun profileRemoteApi(): ProfileRemoteApi

    fun globalNavigator(): GlobalNavigator
}
```

`ProfileRemoteApi` приходит из app/mediator слоя:

```kotlin
appNetworkModule.profileRemoteApi
```

`NetworkManager` сюда обычно не добавляем. Он уже находится внутри core exception mapping.

## Как app/mediator связывает feature

App-слой реализует зависимости feature:

```kotlin
class ProfileDependenciesImpl(
    private val coreComponent: CoreComponent,
    private val appNetworkModule: AppNetworkModule,
) : ProfileDependencies {

    override fun profileRemoteApi(): ProfileRemoteApi {
        return appNetworkModule.profileRemoteApi
    }

    override fun globalNavigator(): GlobalNavigator {
        return coreComponent.globalNavigator
    }
}
```

Потом эти зависимости передаются в feature через `dependenciesProvider`.

```kotlin
ProfileFeature.dependenciesProvider = ModuleDependenciesProvider {
    ProfileDependenciesImpl(
        coreComponent = coreComponent,
        appNetworkModule = appNetworkModule,
    )
}
```

## Что писать в feature module

`ProfileModule` создает repository/use case/ViewModel.

```kotlin
internal class ProfileModule(
    private val dependencies: ProfileDependencies,
) : ProfileComponent {

    private val repository: ProfileRepository by lazy {
        ProfileRepositoryImpl(
            api = dependencies.profileRemoteApi(),
        )
    }

    private val loadProfileUseCase: LoadProfileUseCase by lazy {
        LoadProfileUseCase(repository)
    }

    override val router: ProfileRouter by lazy {
        ProfileRouterImpl(
            navigator = dependencies.globalNavigator()
        )
    }

    override fun getProfileViewModel(): ProfileViewModel {
        return ProfileViewModel(
            loadProfileUseCase = loadProfileUseCase,
            router = router,
        )
    }
}
```

## Что писать в repository

Repository делает запрос и возвращает domain-модель.

```kotlin
internal class ProfileRepositoryImpl(
    private val api: ProfileRemoteApi,
) : ProfileRepository {

    override suspend fun loadProfile(): Profile {
        return api.loadProfile().toDomain()
    }
}
```

Repository не должен знать про `NetworkManager`, если мы идем по эталонной схеме.

Что произойдет при ошибке:

```text
api.loadProfile() throws HttpException / UnknownHostException / SocketTimeoutException
    -> ошибка вылетает из repository
    -> ошибка вылетает из use case
    -> ошибка попадает в launchCoroutine
    -> CoroutineErrorHandler
    -> CoreExceptionMapper
    -> AppException
```

## Что писать во ViewModel

ViewModel не знает про Retrofit и `NetworkManager`.

```kotlin
class ProfileViewModel(
    private val loadProfileUseCase: LoadProfileUseCase,
    private val router: ProfileRouter,
) : CoreViewModel<ProfileState, ProfileEvent>(
    initialState = ProfileState.loading()
) {

    override fun perform(viewEvent: ProfileEvent) {
        when (viewEvent) {
            ProfileEvent.Load -> loadProfile()
            ProfileEvent.BackClicked -> router.back()
        }
    }

    private fun loadProfile() = launchCoroutine(
        handleAction = ExceptionConsumer { error ->
            updateState(ProfileState.error(error))
            true
        }
    ) {
        updateState(ProfileState.loading())

        val profile = loadProfileUseCase()

        updateState(ProfileState.content(profile))
    }
}
```

Если repository бросит сетевую ошибку, она попадет в `CoroutineErrorHandler`, затем в `CoreExceptionMapper`, затем в `ExceptionConsumer` уже как `AppException`.

## Где писать baseUrl

`baseUrl` не пишется в core.

Его место:

```text
app module
    -> BuildConfig
    -> AppNetworkModule
    -> flavor config
    -> mediator config
```

Например:

```kotlin
private const val BASE_URL = "https://api.example.com/"
```

или позже:

```kotlin
BuildConfig.BASE_URL
```

## Коротко

- Core дает `NetworkManager`.
- `NetworkManager` нужен `CoreExceptionMapper`, а не каждому repository.
- Core дает общие сетевые ошибки.
- Core не знает `baseUrl`.
- Core не создает Retrofit API.
- App/mediator создает Retrofit и конкретные `RemoteApi`.
- Feature получает `RemoteApi` через `Dependencies`.
- Module передает `RemoteApi` в repository.
- Repository просто делает запрос и возвращает domain-модель.
- ViewModel работает только с use case и ловит `AppException` через `launchCoroutine`.
