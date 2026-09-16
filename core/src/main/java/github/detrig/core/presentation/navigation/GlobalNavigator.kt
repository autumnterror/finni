package github.detrig.core.presentation.navigation

import github.detrig.core.presentation.navigation.v3.Nav3NavigationHandler

/**
 * Глобальная точка входа для навигации.
 *
 * Feature/router обращаются к GlobalNavigator, а не к Activity напрямую.
 * Активная Activity при старте передает сюда свой [NavigationHandler].
 */
interface GlobalNavigator : Nav3NavigationHandler {

    /**
     * Привязывает текущий navigation handler.
     *
     * Обычно вызывается из Activity, когда NavDisplay и back stack уже готовы.
     *
     * @param navigationHandler handler активной Activity.
     * @param executePendingCommands если true, выполняет команды, которые пришли до готовности Activity.
     */
    fun setNavigationHandler(
        navigationHandler: NavigationHandler,
        executePendingCommands: Boolean = true,
    )

    /**
     * Сбрасывает текущий navigation handler.
     *
     * Обычно вызывается в onPause, чтобы GlobalNavigator не держал прямую ссылку на Activity.
     */
    fun resetNavigationHandler()

    /**
     * Возвращает handler активной Activity, если он сейчас есть.
     */
    fun currentNavigationHandler(): NavigationHandler?
}
