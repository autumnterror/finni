---
name: android-core-nav3-routing
description: Писать навигацию Android feature-модулей через Compose Navigation 3, Nav3Activity, типизированные route, feature router и GlobalNavigator из core.
metadata:
  short-description: Android Nav3 routing
---

# Android Core Nav3 Routing

Используй этот skill, когда нужно добавить экран, route, graph, router, nested graph или Activity на базе локального Nav3 core.

Не создавай и не обновляй README или документацию модуля/feature автоматически после реализации. Пиши их только по явному запросу пользователя; существующие документы можно читать как контекст.

Если задача добавляет или меняет визуальный Compose UI, используй `android-core-compose-ui`; для ViewModel и UI-state — `android-core-ui-mvvm`. Правка только route/router/graph не требует этих скиллов.

## Перед правками

| Файл | Зачем читать |
|------|--------------|
| `guides/navigation_notes.md` | карта navigation core |
| `core/src/main/java/github/detrig/core/presentation/navigation/GlobalNavigator.kt` | глобальный вход для router |
| `core/src/main/java/github/detrig/core/presentation/navigation/v3/Nav3GraphSpec.kt` | host и nested graph |
| `core/src/main/java/github/detrig/core/presentation/navigation/v3/Nav3Entries.kt` | `composable` и `nestedGraph` DSL |
| `core/src/main/java/github/detrig/core/presentation/navigation/v3/Nav3NavigationHandler.kt` | команды back stack |
| `core/src/main/java/github/detrig/core/view/Nav3Activity.kt` | Activity-host для Nav3 |

## Короткая схема

```text
Screen
    -> ViewModel.perform(event)
    -> Router.openSomething(...)
    -> GlobalNavigator.navigate(route)
    -> NavigationHandler активной Activity
    -> Nav3 back stack
```

Проект использует Activity + Compose + Navigation 3. Не добавляй fragment navigation или XML nav graph без прямой просьбы пользователя.

## Route

```kotlin
@Serializable
sealed interface ProfileRoute : NavKey {

    @Serializable
    data object Home : ProfileRoute

    @Serializable
    data class Details(
        val userId: String,
    ) : ProfileRoute
}
```

Route хранит параметры экрана прямо в объекте. В `composable` параметры читаются из lambda-аргумента.

## Graph

```kotlin
internal fun profileGraph(
    router: ProfileRouter,
) = Nav3HostGraphSpec(
    startRoute = ProfileRoute.Home,
) {
    composable<ProfileRoute.Home> {
        ProfileHomeScreen(
            onOpenDetails = { userId -> router.openDetails(userId) },
        )
    }

    composable<ProfileRoute.Details> { route ->
        ProfileDetailsScreen(
            userId = route.userId,
            onBack = router::back,
        )
    }
}
```

## Router

```kotlin
internal interface ProfileRouter {
    fun openDetails(userId: String)
    fun back()
}
```

```kotlin
internal class ProfileRouterImpl(
    private val navigator: GlobalNavigator,
) : ProfileRouter {

    override fun openDetails(userId: String) {
        navigator.navigate(ProfileRoute.Details(userId))
    }

    override fun back() {
        navigator.back()
    }
}
```

Router скрывает route от UI и ViewModel. Не размазывай `navigator.navigate(...)` по экранам.

## Activity

```kotlin
class MainActivity : Nav3Activity(
    navHostSpec = mainGraph(),
)
```

`Nav3Activity` сама создает `NavDisplay`, `Nav3NavigationHandler` и привязывает handler к `GlobalNavigator` в lifecycle Activity.

## Nested graph

Используй nested graph, когда внутри текущего graph открывается отдельный flow со своим внутренним back stack.

```kotlin
nestedGraph<MainRoute.ProfileFlow> {
    profileNestedGraph(router)
}
```

Для обычных переходов между экранами одной feature чаще нужен простой `composable<Route>` и метод router.

## Что нельзя

| Нельзя | Используй |
|--------|-----------|
| `sealed class Route` | `sealed interface Route : NavKey` |
| строковые route | типизированные `NavKey` route |
| `backStackEntry.toRoute<T>()` | прямой аргумент `composable<Route> { route -> }` |
| `NavHostGraphSpec` | `Nav3HostGraphSpec` |
| `navigation {}` | `nestedGraph {}` |
| `ComposeActivity` для Nav3 screen flow | `Nav3Activity` |
| импортировать route другой feature напрямую | public `Api` или dependency из mediator/app слоя |

## Чеклист

- [ ] Route реализует `NavKey`.
- [ ] Route с параметрами помечен `@Serializable`.
- [ ] Graph собран через `Nav3HostGraphSpec` или `Nav3NestedGraphSpec`.
- [ ] Экран зарегистрирован через `composable<Route>`.
- [ ] Навигация из feature идет через router.
- [ ] Cross-feature переход идет через public API другой feature, а не через ее internal route.
