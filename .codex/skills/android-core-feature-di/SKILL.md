---
name: android-core-feature-di
description: Создавать и менять Android feature-модули в стиле проекта ручным DI через Feature, Dependencies, Component, Module, Api, ApiImpl и ModuleDependenciesProvider.
metadata:
  short-description: Android feature DI
---

# Android Core Feature DI

Используй этот skill, когда нужно создать feature-модуль, дописать DI, подключить зависимости feature или оформить public API модуля.

Не создавай и не обновляй README или документацию модуля/feature автоматически после реализации. Пиши их только по явному запросу пользователя; существующие документы можно читать как контекст.

Если задача затрагивает визуальный Compose UI, используй `android-core-compose-ui`; если ViewModel и UI-state — `android-core-ui-mvvm`. Не загружай их для правки только DI.

## Перед правками

Сначала проверь актуальные файлы проекта:

| Файл | Зачем читать |
|------|--------------|
| `guides/di_notes.md` | общая схема DI |
| `core/src/main/java/github/detrig/core/di/DiComponentFieldDemand.kt` | как работает `diDemand` |
| `core/src/main/java/github/detrig/core/di/ModuleDependenciesProvider.kt` | как feature получает зависимости |
| `core/src/main/java/github/detrig/core/di/CoreComponent.kt` | что core отдает наружу |
| `core/src/main/java/github/detrig/core/di/module/CoreModule.kt` | как core создает зависимости |

## Базовая структура feature

```text
feature/profile/
├── ProfileFeature.kt
├── ProfileDependencies.kt
├── api/
│   ├── ProfileApi.kt
│   └── ProfileApiImpl.kt
├── di/
│   ├── ProfileComponent.kt
│   └── ProfileModule.kt
├── navigation/
│   └── ProfileRouter.kt
└── presentation/
    └── ProfileViewModel.kt
```

## Ответственность классов

| Класс | За что отвечает |
|------|------------------|
| `*Feature` | владеет lifecycle component и отдает public API |
| `*Dependencies` | описывает, что feature просит снаружи |
| `*Component` | внутренний контракт DI-графа |
| `*Module` | создает реальные зависимости |
| `*Api` | публичные возможности feature для других модулей |
| `*ApiImpl` | internal-реализация public API |

## Пример Feature

```kotlin
object ProfileFeature {

    var dependenciesProvider: ModuleDependenciesProvider<ProfileDependencies>? = null

    private var component: ProfileComponent? by diDemand {
        ProfileModule(
            dependencies = requireNotNull(dependenciesProvider?.getDependencies()),
        )
    }

    fun getApi(): ProfileApi = requireNotNull(component).api

    internal fun component(): ProfileComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
```

## Пример Dependencies

```kotlin
interface ProfileDependencies {
    fun profileRemoteApi(): ProfileRemoteApi
    fun globalNavigator(): GlobalNavigator
    fun profileDao(): ProfileDao
}
```

Feature просит только узкие контракты. Не передавай в feature весь `AppComponent`, если нужен один API или один DAO.

## Пример Component и Module

```kotlin
internal interface ProfileComponent {
    val api: ProfileApi
    val router: ProfileRouter

    fun getProfileViewModel(userId: String): ProfileViewModel
}
```

```kotlin
internal class ProfileModule(
    private val dependencies: ProfileDependencies,
) : ProfileComponent {

    private val repository: ProfileRepository by lazy {
        ProfileRepositoryImpl(
            api = dependencies.profileRemoteApi(),
            dao = dependencies.profileDao(),
        )
    }

    private val loadProfileUseCase: LoadProfileUseCase by lazy {
        LoadProfileUseCase(repository)
    }

    override val router: ProfileRouter by lazy {
        ProfileRouterImpl(
            navigator = dependencies.globalNavigator(),
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

## Что нельзя

| Нельзя | Почему |
|--------|--------|
| Подключать Hilt, Dagger или Koin | проект использует ручной DI |
| Делать `*Module` public | наружу должен смотреть `*Feature` и `*Api` |
| Импортировать internal-классы другой feature | ломает границы модулей |
| Создавать ViewModel через `by lazy` в Module | ViewModel должна создаваться новым экземпляром |
| Тянуть зависимости из глобальных singleton внутри repository | зависимости должны приходить через `Dependencies` |

## Чеклист

- [ ] `*Feature` public object.
- [ ] `dependenciesProvider` использует `ModuleDependenciesProvider`.
- [ ] Component создается через `diDemand`.
- [ ] `*Component` и `*Module` internal.
- [ ] Public API вынесен в `*Api`.
- [ ] `*ApiImpl` internal.
- [ ] ViewModel создается фабричным методом в component/module.
