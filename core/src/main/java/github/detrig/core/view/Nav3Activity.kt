package github.detrig.core.view

import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import github.detrig.core.CoreApplication
import github.detrig.core.presentation.message.GlobalMessageHost
import github.detrig.core.presentation.navigation.NavigationHandler
import github.detrig.core.presentation.navigation.v3.DefaultNavGraphKey
import github.detrig.core.presentation.navigation.v3.Nav3HostGraphSpec
import github.detrig.core.presentation.navigation.v3.Nav3NavigationHandlerImpl

/**
 * Базовый класс для Activity, которые используют Navigation 3.
 *
 * Автоматически создает [Nav3NavigationHandlerImpl], настраивает [NavDisplay]
 * и регистрирует [NavigationHandler] в глобальном навигаторе.
 *
 * Поддерживает два варианта графа:
 *
 * - статический graph через constructor(navHostSpec: Nav3HostGraphSpec);
 * - динамический graph через функцию (Bundle) -> Nav3HostGraphSpec.
 *
 * Пример:
 * ```kotlin
 * class MainActivity : Nav3Activity(mainGraph()) {
 *
 *     override fun onNavigationHandlerBind(navigationHandler: NavigationHandler) {
 *         MainFeature.component().router.setNavigationHandler(navigationHandler)
 *     }
 * }
 * ```
 *
 * Lifecycle:
 *
 * - onCreate: создает compose content и первично регистрирует NavigationHandler;
 * - LaunchedEffect: добавляет back stack графа в NavigationHandler;
 * - onResume: снова привязывает NavigationHandler к GlobalNavigator;
 * - onPause: отвязывает NavigationHandler от GlobalNavigator.
 *
 * @param navHostSpec функция, которая создает Nav3HostGraphSpec из аргументов Activity.
 */
abstract class Nav3Activity(
    private val navHostSpec: (Bundle) -> Nav3HostGraphSpec,
) : AppCompatActivity() {

    /**
     * Конструктор для Activity со статическим графом.
     *
     * Используется, если стартовый graph не зависит от Bundle/Intent аргументов.
     *
     * @param navHostSpec готовая спецификация graph.
     */
    constructor(navHostSpec: Nav3HostGraphSpec) : this({ navHostSpec })

    /**
     * NavigationHandler конкретной Activity.
     *
     * Хранит back stack этой Activity и выполняет команды navigate/back/replace.
     */
    private val navigationHandler = Nav3NavigationHandlerImpl(this)

    /**
     * Глобальный навигатор приложения.
     *
     * Router'ы feature обычно обращаются именно к нему, а он уже передает команды
     * в активный [navigationHandler].
     */
    private val navigator by lazy { CoreApplication.app.coreComponent.globalNavigator }

    private val messageController by lazy { CoreApplication.app.coreComponent.globalMessageController }

    /**
     * Флаг, что NavDisplay уже создал back stack и handler можно безопасно использовать.
     */
    private var navigationIsReady = false

    /**
     * Обработчик системной кнопки назад.
     *
     * Делегирует back в NavigationHandler, чтобы логика закрытия экранов и Activity
     * была в одном месте.
     */
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigationHandler.back()
        }
    }

    /**
     * Вызывается, когда [NavigationHandler] готов к использованию.
     *
     * Можно переопределить в Activity, если нужно передать handler в router feature.
     *
     * Пример:
     * ```kotlin
     * override fun onNavigationHandlerBind(navigationHandler: NavigationHandler) {
     *     ProfileFeature.component().router.setNavigationHandler(navigationHandler)
     * }
     * ```
     *
     * @param navigationHandler handler текущей Activity.
     */
    open fun onNavigationHandlerBind(navigationHandler: NavigationHandler) = Unit

    /**
     * Оборачивает весь Compose-контент Activity в тему приложения.
     *
     * Core оставляет реализацию темы app-модулю и не зависит от design system.
     */
    @Composable
    protected open fun ProvideAppContent(content: @Composable () -> Unit) {
        content()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Регистрируем handler сразу, но отложенные команды пока не выполняем:
        // back stack еще будет создан внутри setContent.
        navigator.setNavigationHandler(navigationHandler, executePendingCommands = false)

        setContent {
            ProvideAppContent {
                val args = intent?.extras ?: Bundle.EMPTY
                val spec = remember { navHostSpec(args) }
                val startBackStack = remember(args, spec) { spec.startBackStack(args) }
                val backStack = rememberNavBackStack(*startBackStack.toTypedArray())

                Box(modifier = Modifier.fillMaxSize()) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { navigationHandler.back() },
                        entryProvider = entryProvider { spec.builder(this) },
                        entryDecorators = listOf(
                            // Нужно для корректного сохранения состояния composable entry.
                            rememberSaveableStateHolderNavEntryDecorator(),
                            // Привязывает ViewModelStore к жизни nav entry.
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    )
                    GlobalMessageHost(controller = messageController)
                }

                LaunchedEffect(Unit) {
                    // После создания back stack регистрируем его в handler.
                    val backStackKey = spec.graphKey ?: DefaultNavGraphKey(backStack)
                    navigationHandler.addBackStack(backStackKey, backStack)

                    // Теперь навигация полностью готова, можно выполнить pending commands.
                    navigator.setNavigationHandler(navigationHandler)
                    onNavigationHandlerBind(navigationHandler)
                    navigationIsReady = true
                }
            }
        }

        // Системный back должен идти через NavigationHandler.
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    override fun onResume() {
        super.onResume()
        if (navigationIsReady) {
            // Activity снова активна, значит GlobalNavigator должен слать команды в ее handler.
            navigator.setNavigationHandler(navigationHandler)
            onNavigationHandlerBind(navigationHandler)
        }
    }

    override fun onPause() {
        // Activity больше не активна, нельзя держать ее handler в GlobalNavigator.
        navigator.resetNavigationHandler()
        super.onPause()
    }
}
