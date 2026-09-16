# DI на примере feature `Profile`

## Feature

`ProfileFeature` - это входная точка и владелец lifecycle DI-графа.

```kotlin
object ProfileFeature {

    var dependenciesProvider: ModuleDependenciesProvider<ProfileDependencies>? = null

    private var component: ProfileComponent? by diDemand {
        ProfileModule(
            dependencies = requireNotNull(dependenciesProvider?.getDependencies())
        )
    }

    fun getApi(): ProfileApi = requireNotNull(component).api

    internal fun component(): ProfileComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
```

- `ProfileFeature` знает, когда создать `component`.
- `ProfileFeature` знает, когда уничтожить `component`.
- `ProfileFeature` наружу отдает только public API.

## Component

`ProfileComponent` - это контракт: что можно получить из DI-графа этой feature.

```kotlin
internal interface ProfileComponent {

    val api: ProfileApi
    val router: ProfileRouter

    fun getProfileViewModel(userId: String): ProfileViewModel
}
```

- `Component` описывает доступные зависимости.
- `Component` скрывает детали создания.
- `Component` отделяет "что есть" от "как создать".

Важно: `Component` обычно interface. Он ничего не создает сам. Он просто говорит: "у этой feature есть api, router и фабрика ViewModel".

## Module

`ProfileModule` - это реализация `component`. Именно тут создаются настоящие объекты.

```kotlin
internal class ProfileModule(
    private val dependencies: ProfileDependencies,
) : ProfileComponent {

    private val repository: ProfileRepository by lazy {
        ProfileRepositoryImpl(
            api = dependencies.profileRemoteApi()
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

    override val api: ProfileApi by lazy {
        ProfileApiImpl(router)
    }

    override fun getProfileViewModel(userId: String): ProfileViewModel {
        return ProfileViewModel(
            userId = userId,
            loadProfileUseCase = loadProfileUseCase,
            router = router,
        )
    }
}
```

- `Module` создает `Repository`.
- `Module` создает `UseCase` / `Interactor`.
- `Module` создает `Router`.
- `Module` создает `ViewModel`.
- `Module` решает, что singleton на graph, а что новый объект.

`by lazy` внутри модуля означает: один объект на один `component`.

```kotlin
private val repository by lazy { ProfileRepositoryImpl(...) }
```

Пока жив `ProfileComponent`, `repository` будет один и тот же.

## Dependencies

`ProfileDependencies` - это то, что feature просит снаружи.

```kotlin
interface ProfileDependencies {

    fun profileRemoteApi(): ProfileRemoteApi

    fun globalNavigator(): GlobalNavigator

    fun analytics(): ProfileAnalytics
}
```

- Feature не знает `appComponent`.
- Feature не знает другие feature-компоненты.
- Feature просит только узкие контракты.

## API

`ProfileApi` - это публичный API feature. То, что другие модули могут вызвать снаружи.

```kotlin
interface ProfileApi {

    fun openProfile(userId: String)

    fun openCurrentUserProfile()
}
```

- `Api` показывает, что feature разрешает делать извне.
- `Api` скрывает внутренние экраны, routes, `ViewModel`, `repository`.
- `Api` нужен для cross-feature взаимодействия.

Например, feature `Cards` не должна знать, что у `Profile` внутри есть `Route.ProfileDetails`. Она должна вызвать:

```kotlin
ProfileFeature.getApi().openProfile(userId)
```

А как именно откроется экран - это забота `Profile`.

## API Impl

`ProfileApiImpl` - реальная реализация публичного API.

```kotlin
internal class ProfileApiImpl(
    private val router: ProfileRouter,
) : ProfileApi {

    override fun openProfile(userId: String) {
        router.openProfile(userId)
    }

    override fun openCurrentUserProfile() {
        router.openCurrentUserProfile()
    }
}
```

- `ApiImpl` переводит внешний вызов во внутреннее действие feature.
- `ApiImpl` может использовать `router`, `useCase`, `actions`.
- `ApiImpl` остается internal, наружу виден только interface `ProfileApi`.

## Mediator

Медиатор живет в app-слое и соединяет модули между собой. Feature-модули не должны напрямую знать друг о друге.

## Коротко

- `Feature` - владелец `component`, точка входа.
- `Component` - список того, что можно получить из feature graph.
- `Module` - место, где создаются объекты.
- `Dependencies` - что feature требует снаружи.
- `Api` - что feature отдает наружу.
- `ApiImpl` - внутренняя реализация public Api.
