package github.detrig.core.memory

/**
 * Память, привязанная к жизненному циклу сессии.
 *
 * Важно: использовать только для данных, которые должны жить в памяти
 * на протяжении текущей сессии и очищаться при ее завершении.
 *
 * Пример:
 * ```kotlin
 * class AuthDataStorage(
 *     sessionMemory: GlobalSessionMemory,
 * ) {
 *     var refreshToken: String? by sessionMemory
 * }
 * ```
 */
class GlobalSessionMemory : MapMemory()
