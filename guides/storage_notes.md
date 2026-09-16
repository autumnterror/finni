# Storage core

В эталонном core нет Room.

Для локального хранения там есть две основные идеи:

- `GlobalSessionMemory` - данные в памяти на время сессии;
- `SharedStorage` / `CorePreferences` - простые persistent-настройки через SharedPreferences.

Room в эталонном core не используется. Если в новом приложении нужно хранить много структурированных данных, Room стоит добавить отдельно как осознанное расширение архитектуры.

## GlobalSessionMemory

`GlobalSessionMemory` - память, которая живет только в RAM.

```kotlin
class GlobalSessionMemory : MapMemory()
```

Использование:

```kotlin
class AuthDataStorage(
    sessionMemory: GlobalSessionMemory,
) {
    var refreshToken: String? by sessionMemory
    var userId: String? by sessionMemory
}
```

Что происходит под капотом:

```text
var refreshToken by sessionMemory
    -> Kotlin вызывает sessionMemory.setValue(...)
    -> значение кладется в Map по имени property

val token = refreshToken
    -> Kotlin вызывает sessionMemory.getValue(...)
    -> значение достается из Map по имени property
```

То есть это обычная map-память, завернутая в Kotlin delegate.

## Когда использовать GlobalSessionMemory

Использовать для данных, которые:

- нужны только пока пользователь в текущей сессии;
- не должны переживать logout;
- не обязательно сохранять после убийства процесса;
- должны быть доступны разным feature через core.

Примеры:

```text
accessToken
refreshToken
currentUserId
session flags
temporary auth data
```

При завершении сессии вызывается:

```kotlin
sessionMemory.clear()
```

И все значения удаляются.

## SharedStorage

`SharedStorage` - базовая обертка над `SharedPreferences`.

```kotlin
open class SharedStorage(
    private val sharedPreferences: SharedPreferences,
)
```

Он содержит методы:

```kotlin
readString(key, defaultValue)
putString(key, value)

readBoolean(key, defaultValue)
putBoolean(key, value)

readInt(key, defaultValue)
putInt(key, value)

readStringList(key)
saveStringList(key, value)

remove(key)
clear()
forceClear()
```

Зачем это нужно: чтобы не писать `sharedPreferences.edit { ... }` в каждом storage-классе.

## CorePreferences

`CorePreferences` - интерфейс для общих настроек core.

```kotlin
interface CorePreferences {
    fun clear()
    fun forceClear()
}
```

В эталонном проекте этот интерфейс большой, потому что там много app/core-настроек: debug flags, server ids, push token, onboarding flags и т.д.

В новом проекте лучше не копировать все эти поля заранее. Добавляй в `CorePreferences` только то, что реально становится общим для всего приложения.

## Feature storage

Если настройка относится к конкретной feature, лучше сделать storage внутри feature.

Пример:

```kotlin
internal class SearchHistoryStorage(
    sharedPreferences: SharedPreferences,
) : SharedStorage(sharedPreferences) {

    companion object {
        private const val QUERY_HISTORY_KEY = "query_history"
    }

    fun getHistory(): List<String> {
        return readStringList(QUERY_HISTORY_KEY)
    }

    fun saveHistory(history: List<String>) {
        saveStringList(QUERY_HISTORY_KEY, history)
    }
}
```

И создать его в feature module:

```kotlin
private val searchHistoryStorage: SearchHistoryStorage by lazy {
    SearchHistoryStorage(
        dependencies.sharedPreferences("search_history")
    )
}
```

## Room

Room в эталонном core не найден.

Но если в приложении будет много локальных данных, Room нужен.

Обычно Room используют для:

- списков объектов;
- кеша ответов API;
- истории действий;
- offline-first сценариев;
- связей между сущностями.

Room лучше не смешивать с `GlobalSessionMemory` и `SharedPreferences`:

```text
GlobalSessionMemory - временные данные сессии в RAM
SharedPreferences - маленькие key-value настройки
Room - большие структурированные локальные данные
```

## Где держать Room

Если строго следовать эталонному core, Room не кладем в core.

Практичный вариант для нового приложения:

```text
app/database
    -> AppDatabase
    -> создание Room.databaseBuilder(...)

feature/data/local
    -> Entity
    -> Dao
    -> LocalDataSource
```

И app/mediator прокидывает нужный DAO в feature dependencies:

```kotlin
interface ProfileDependencies {
    fun profileDao(): ProfileDao
}
```

Feature module собирает repository:

```kotlin
private val repository: ProfileRepository by lazy {
    ProfileRepositoryImpl(
        remoteApi = dependencies.profileRemoteApi(),
        profileDao = dependencies.profileDao(),
    )
}
```

## Коротко

- В эталонном core Room нет.
- `GlobalSessionMemory` - временная память на сессию.
- `SharedStorage` - базовая обертка над SharedPreferences.
- `CorePreferences` - общие key-value настройки core.
- Для больших локальных данных нужен Room.
- Room лучше добавлять отдельно: database в app, dao/entity/local data source рядом с feature.
