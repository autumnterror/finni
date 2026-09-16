package github.detrig.core.infrastructure.preferences

/**
 * Core key-value storage.
 *
 * Сюда добавляются только общие настройки приложения, которые действительно
 * принадлежат core. Feature-specific настройки лучше хранить в storage своей feature.
 */
interface CorePreferences {

    fun clear()

    fun forceClear()
}
