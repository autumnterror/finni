# Navigation 3 в core

Краткая карта файлов, которые собирают Nav3-навигацию.

Основная идея:

```text
Route
    -> Nav3HostGraphSpec
    -> Nav3Activity
    -> NavDisplay
    -> Nav3NavigationHandler
    -> GlobalNavigator
    -> Router во feature
```

## NavGraphKey.kt

`NavGraphKey` - ключ навигационного графа.

Нужен, чтобы отличать один graph от другого, особенно когда есть вложенные graph.

```kotlin
interface NavGraphKey
```

Пример:

```text
HostGraph
ProfileGraph
SettingsGraph
```

Если нужно вернуться к конкретному graph, используется его key:

```kotlin
navigator.backToGraph(ProfileGraphKey)
```

## DefaultNavGraphKey.kt

`DefaultNavGraphKey` - стандартная реализация `NavGraphKey`.

Нужна, когда graph не передал свой key явно.

```kotlin
val backStackKey = spec.graphKey ?: DefaultNavGraphKey(backStack)
```

Как работает:

- берет первый route из стартового back stack;
- достает имя класса route;
- создает читаемый key вида `NavGraphKey-Home`.

## Nav3GraphSpec.kt

Файл описывает спецификации graph:

- `Nav3HostGraphSpec`;
- `Nav3NestedGraphSpec`;
- `EntryHostProviderInstaller`.

### Nav3HostGraphSpec

`Nav3HostGraphSpec` - главный graph для Activity.

Он хранит:

- `graphKey` - ключ graph;
- `startBackStack` - стартовый стек экранов;
- `builder` - DSL, где регистрируются экраны.

Пример:

```kotlin
Nav3HostGraphSpec(
    startRoute = ProfileRoute.Home,
) {
    composable<ProfileRoute.Home> {
        ProfileHomeScreen()
    }
}
```

### Nav3NestedGraphSpec

`Nav3NestedGraphSpec` - вложенный graph.

Нужен, когда один flow живет внутри другого graph.

Пример:

```text
MainGraph
    -> ProfileNestedGraph
```

## Nav3Entries.kt

Файл с DSL-функциями для graph.

### composable

`composable<Route>` регистрирует Compose-экран для route.

```kotlin
composable<ProfileRoute.Details> { params ->
    ProfileDetailsScreen(userId = params.userId)
}
```

Зачем нужен:

- связывает route с Composable-экраном;
- дает доступ к параметрам route;
- скрывает прямую работу с `entry<T>`.

### nestedGraph

`nestedGraph<Route>` подключает вложенный graph.

Нужен, когда экран текущего graph открывает отдельный flow со своим back stack.

```kotlin
nestedGraph<Route.ProfileFlow> {
    profileNestedGraph()
}
```

Внутри создается отдельный `NavDisplay` и отдельный back stack.

## Nav3NavigationHandler.kt

Файл отвечает за реальное изменение Nav3 back stack.

Главный интерфейс:

```kotlin
interface Nav3NavigationHandler
```

Основные команды:

- `navigate(route)` - добавить экран в стек;
- `replace(route)` - заменить текущий экран;
- `back()` - вернуться назад;
- `backTo(route)` - вернуться к конкретному route;
- `backToStartRoute()` - вернуться к первому экрану текущего graph;
- `backToGraph(key)` - вернуться к конкретному graph;
- `openNewStartRoute(route)` - очистить текущий graph и поставить новый стартовый route.

Реальная реализация:

```kotlin
Nav3NavigationHandlerImpl
```

Она хранит:

```kotlin
LinkedHashMap<NavGraphKey, MutableList<NavKey>>
```

То есть:

```text
GraphKey -> back stack этого graph
```

Пример:

```text
HostGraph -> [Home, Profile]
ProfileGraph -> [ProfileHome, Details]
```

## NavigationHandler.kt

`NavigationHandler` - общий handler для Activity.

Сейчас он просто расширяет `Nav3NavigationHandler`:

```kotlin
interface NavigationHandler : Nav3NavigationHandler
```

Нужен, чтобы `GlobalNavigator` работал не с конкретной реализацией, а с общим контрактом активной Activity.

Позже сюда можно добавить другие возможности:

- dialogs;
- messages;
- permissions;
- legacy fragment navigation.

## GlobalNavigator.kt

`GlobalNavigator` - глобальная точка входа для навигации.

Он нужен, чтобы feature и ViewModel не знали про Activity.

```kotlin
router.openDetails(id)
    -> globalNavigator.navigate(Route.Details(id))
```

Основные задачи:

- хранить текущий `NavigationHandler`;
- прокидывать команды в активную Activity;
- сбрасывать handler, когда Activity ушла в pause.

```kotlin
fun setNavigationHandler(navigationHandler: NavigationHandler)
fun resetNavigationHandler()
fun currentNavigationHandler(): NavigationHandler?
```

## GlobalNavigatorImpl.kt

`GlobalNavigatorImpl` - реализация `GlobalNavigator`.

Он не меняет back stack сам. Он только передает команды текущему `NavigationHandler`.

Если handler есть:

```text
navigate(route)
    -> navigationHandler.navigate(route)
```

Если handler еще нет:

```text
navigate(route)
    -> commandBuffer.add(command)
```

Зачем нужен `commandBuffer`:

- команда может прийти до полной готовности Activity;
- команда сохранится;
- после `setNavigationHandler()` она выполнится.

## Nav3Activity.kt

`Nav3Activity` - базовая Activity для Nav3.

Она делает основную связку:

```text
Nav3HostGraphSpec
    -> startBackStack
    -> rememberNavBackStack
    -> NavDisplay
    -> Nav3NavigationHandler
    -> GlobalNavigator
```

Что происходит в `onCreate`:

- включается edge-to-edge;
- создается `NavDisplay`;
- создается back stack;
- регистрируются экраны из graph;
- `navigationHandler` получает back stack;
- `GlobalNavigator` получает текущий handler.

Когда Activity активна:

```kotlin
navigator.setNavigationHandler(navigationHandler)
```

Когда Activity уходит:

```kotlin
navigator.resetNavigationHandler()
```

## ProfileRoute.kt

Учебный пример route для feature `Profile`.

```kotlin
sealed interface ProfileRoute : NavKey
```

Route описывает экраны:

```kotlin
data object Home : ProfileRoute

data class Details(
    val userId: String,
) : ProfileRoute
```

`Details` хранит параметры экрана прямо внутри route.

## ProfileRouter.kt

`ProfileRouter` - внутренний navigation contract feature.

ViewModel или экран вызывает не `navigator.navigate(...)`, а понятный метод:

```kotlin
router.openDetails(userId)
```

Так feature не размазывает route по всему коду.

## ProfileRouterImpl.kt

`ProfileRouterImpl` переводит действия feature в команды `GlobalNavigator`.

```kotlin
override fun openDetails(userId: String) {
    navigator.navigate(ProfileRoute.Details(userId))
}
```

То есть:

```text
openDetails(id)
    -> Route.Details(id)
    -> GlobalNavigator.navigate(route)
```

## ProfileGraph.kt

Первый пример graph.

Он связывает routes с Compose-экранами:

```kotlin
composable<ProfileRoute.Home> {
    ProfileHomeScreen()
}

composable<ProfileRoute.Details> { params ->
    ProfileDetailsScreen(userId = params.userId)
}
```

Здесь видно главное преимущество Nav3:

- route типизирован;
- параметры приходят как объект;
- не нужен `Bundle`;
- экран не знает, как устроен back stack.

## Коротко

- `NavGraphKey` - имя graph.
- `DefaultNavGraphKey` - key по умолчанию.
- `Nav3HostGraphSpec` - главный graph Activity.
- `Nav3NestedGraphSpec` - вложенный graph.
- `composable` - route -> Compose screen.
- `nestedGraph` - route -> вложенный flow.
- `Nav3NavigationHandler` - команды навигации.
- `Nav3NavigationHandlerImpl` - меняет back stack.
- `NavigationHandler` - общий handler активной Activity.
- `GlobalNavigator` - глобальный вход для router/ViewModel.
- `GlobalNavigatorImpl` - буферизует и отправляет команды.
- `Nav3Activity` - host для `NavDisplay`.

