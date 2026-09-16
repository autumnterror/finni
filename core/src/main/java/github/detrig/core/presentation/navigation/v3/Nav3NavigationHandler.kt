package github.detrig.core.presentation.navigation.v3

import android.app.Activity
import androidx.navigation3.runtime.NavKey
import github.detrig.core.presentation.navigation.NavigationHandler
import kotlin.reflect.KClass

/**
 * Интерфейс для управления навигацией в Navigation 3.
 *
 * Работает с иерархическим back stack:
 *
 * ```text
 * GraphKey -> [RouteA, RouteB, RouteC]
 * ```
 *
 * Каждый graph хранит свой список route. Активным считается последний graph,
 * добавленный через [addBackStack].
 */
interface Nav3NavigationHandler {

    /**
     * Добавляет route в стек текущего graph.
     *
     * ```text
     * До:    [Home, Settings]
     * После: [Home, Settings, Details]
     * ```
     *
     * @param route экран, который нужно открыть.
     * @param launchSingleTop если true, удаляет такой же route из текущего стека перед добавлением.
     */
    fun navigate(route: NavKey, launchSingleTop: Boolean = false)

    /**
     * Заменяет последний route в текущем стеке на новый.
     *
     * ```text
     * До:    [Home, Confirm]
     * После: [Home, Success]
     * ```
     *
     * Удобно для экранов результата, когда назад не нужно возвращаться на предыдущий шаг.
     */
    fun replace(route: NavKey, launchSingleTop: Boolean = false)

    /**
     * Возвращает назад.
     *
     * Если в текущем graph больше одного route, удаляет последний route.
     * Если в nested graph остался один route, закрывает nested graph.
     * Если остался один route в host graph, закрывает Activity.
     */
    fun back()

    /**
     * Возвращает к конкретному route в текущем стеке.
     *
     * ```text
     * До:    [Home, Details, Edit]
     * После: [Home, Details]
     * ```
     */
    fun backTo(route: NavKey)

    /**
     * Возвращает к последнему route указанного типа в текущем стеке.
     *
     * Удобно, когда route содержит параметры и точный экземпляр заранее неизвестен.
     */
    fun backTo(type: KClass<out NavKey>)

    /**
     * Возвращает к первому route текущего graph.
     *
     * ```text
     * До:    [Home, Details, Edit]
     * После: [Home]
     * ```
     */
    fun backToStartRoute()

    /**
     * Очищает nested graph и возвращает host graph к стартовому route.
     *
     * Используется, когда нужно закрыть весь текущий flow и вернуться в начало Activity.
     */
    fun backToHostStartRoute()

    /**
     * Возвращает к указанному graph и удаляет все graph, которые были открыты после него.
     */
    fun backToGraph(key: NavGraphKey)

    /**
     * Очищает текущий graph и ставит переданный route как новый стартовый экран.
     */
    fun openNewStartRoute(route: NavKey)

    /**
     * Регистрирует back stack graph.
     *
     * Внутренний метод: вызывается из Nav3Activity или nestedGraph, когда создается NavDisplay.
     */
    fun addBackStack(key: NavGraphKey, stack: MutableList<NavKey>)

    /**
     * Возвращает имя текущего route.
     *
     * Используется для debug, логирования или тестовой инфраструктуры.
     */
    fun getCurrentScreenName(): String
}

/**
 * Удобная версия [Nav3NavigationHandler.backTo] для возврата по типу route.
 */
inline fun <reified T : NavKey> Nav3NavigationHandler.backTo() {
    backTo(T::class)
}

/**
 * Реальная реализация Nav3-навигации для Activity.
 *
 * Этот класс не рисует UI. Он только меняет списки route, а NavDisplay сам
 * реагирует на изменение back stack и показывает нужный экран.
 */
internal class Nav3NavigationHandlerImpl(
    private val activity: Activity,
) : NavigationHandler {

    /**
     * Храним несколько back stack: один для host graph и по одному для каждого nested graph.
     */
    private val backStack: LinkedHashMap<NavGraphKey, MutableList<NavKey>> = LinkedHashMap()

    private val currentGraphKey: NavGraphKey
        get() = backStack.keys.lastOrNull() ?: error("BackStack is empty")

    private val currentStack: MutableList<NavKey>
        get() = backStack.values.lastOrNull() ?: error("BackStack is empty")

    override fun navigate(route: NavKey, launchSingleTop: Boolean) {
        if (launchSingleTop) {
            currentStack.removeAll { it == route }
        }
        currentStack.add(route)
    }

    override fun replace(route: NavKey, launchSingleTop: Boolean) {
        currentStack.removeLastOrNull()
        if (launchSingleTop) {
            currentStack.removeAll { it == route }
        }
        currentStack.add(route)
    }

    override fun back() {
        when {
            backStack.size == 1 && currentStack.size == 1 -> {
                activity.finish()
            }

            currentStack.size == 1 -> {
                backStack.remove(currentGraphKey)
                currentStack.removeLastOrNull()
            }

            else -> currentStack.removeLastOrNull()
        }
    }

    override fun backTo(route: NavKey) {
        check(currentStack.contains(route)) {
            "Route $route not found in current back stack"
        }

        while (currentStack.last() != route) {
            currentStack.removeLastOrNull()
        }
    }

    override fun backTo(type: KClass<out NavKey>) {
        val index = currentStack.indexOfLast { route -> type.isInstance(route) }
        check(index >= 0) {
            "NavKey of type $type not found in current back stack"
        }

        while (currentStack.lastIndex > index) {
            currentStack.removeLastOrNull()
        }
    }

    override fun backToStartRoute() {
        openStartRoute(route = currentStack.first())
    }

    override fun backToHostStartRoute() {
        val firstEntry = backStack.entries.firstOrNull()
            ?: error("Activity backStack is empty")

        backStack.clear()
        backStack[firstEntry.key] = firstEntry.value
        openStartRoute(route = firstEntry.value.first())
    }

    override fun backToGraph(key: NavGraphKey) {
        check(backStack.containsKey(key)) {
            "Graph $key not found in backStack"
        }

        val keys = backStack.keys.toList()
        val index = keys.indexOf(key)
        keys.drop(index + 1).forEach { graphKey ->
            backStack.remove(graphKey)
        }
    }

    override fun openNewStartRoute(route: NavKey) {
        openStartRoute(route)
    }

    override fun addBackStack(key: NavGraphKey, stack: MutableList<NavKey>) {
        backStack[key] = stack
    }

    override fun getCurrentScreenName(): String {
        return currentStack.lastOrNull()?.toString().orEmpty()
    }

    private fun openStartRoute(route: NavKey) {
        currentStack.clear()
        currentStack.add(route)
    }
}
