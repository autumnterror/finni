---
name: android-core-data-network-storage
description: Писать data layer Android feature-модулей через Retrofit API, repository, AppException, CoreExceptionMapper, SharedStorage, GlobalSessionMemory и Room helpers проекта.
metadata:
  short-description: Android data/network/storage
---

# Android Core Data, Network And Storage

Используй этот skill, когда нужно добавить Retrofit API, repository, mapper, локальное хранение, SharedPreferences storage, Room entity/dao/database или обработку ошибок data layer.

## Перед правками

| Файл | Зачем читать |
|------|--------------|
| `core/src/main/java/github/detrig/core/network_notes.md` | как устроена сеть в core |
| `core/src/main/java/github/detrig/core/storage_notes.md` | SharedPreferences и session memory |
| `core/src/main/java/github/detrig/core/room_notes.md` | Room helpers и размещение базы |
| `core/src/main/java/github/detrig/core/exception_notes.md` | ошибки приложения |
| `core/src/main/java/github/detrig/core/exception/mapper/CoreExceptionMapper.kt` | маппинг Throwable -> AppException |
| `core/src/main/java/github/detrig/core/infrastructure/preferences/SharedStorage.kt` | базовый preferences storage |
| `core/src/main/java/github/detrig/core/memory/GlobalSessionMemory.kt` | RAM-хранилище сессии |
| `core/src/main/java/github/detrig/core/database/RoomDatabaseFactory.kt` | создание Room database |
| `core/src/main/java/github/detrig/core/database/RoomTransactionRunner.kt` | транзакции Room |

## Network boundary

Core не создает Retrofit и не знает `baseUrl`. Retrofit, OkHttp и конкретные `RemoteApi` создаются в app/mediator слое и приходят в feature через `Dependencies`.

```text
app/mediator
    -> создает Retrofit и ProfileRemoteApi
    -> реализует ProfileDependencies

feature
    -> получает ProfileRemoteApi
    -> передает API в repository
```

Не прокидывай `NetworkManager` в каждый repository. Repository просто вызывает API; ошибки долетают до `CoreExceptionMapper` через `launchCoroutine`.

## Retrofit API и repository

```kotlin
interface ProfileRemoteApi {

    @GET("profile/{id}")
    suspend fun loadProfile(
        @Path("id") id: String,
    ): ProfileResponse
}
```

```kotlin
internal class ProfileRepositoryImpl(
    private val api: ProfileRemoteApi,
    private val dao: ProfileDao,
) : ProfileRepository {

    override suspend fun loadProfile(id: String): Profile {
        val cached = dao.getProfile(id)
        if (cached != null) return cached.toDomain()

        val remote = api.loadProfile(id)
        dao.saveProfile(remote.toEntity())
        return remote.toDomain()
    }
}
```

Если DTO и domain модель реально совпадают, можно не плодить отдельный DTO. Добавляй DTO, когда API-контракт отличается от domain или нужен явный mapper.

## Ошибки

Нормальная цепочка ошибок:

```text
Retrofit / IO Throwable
    -> repository не ловит без причины
    -> use case пропускает выше
    -> CoreViewModel.launchCoroutine
    -> CoroutineErrorHandler
    -> CoreErrorHandler.mapToAppException(...)
    -> CoreExceptionMapper
```

Для ошибок приложения используй `AppException` и наследников. Не делай `data class SomeException(val throwable: Throwable)` вместо exception-класса.

## SharedPreferences storage

Для маленьких key-value данных используй `SharedStorage`.

```kotlin
internal class SearchHistoryStorage(
    sharedPreferences: SharedPreferences,
) : SharedStorage(sharedPreferences) {

    fun getHistory(): List<String> {
        return readStringList(QUERY_HISTORY_KEY)
    }

    fun saveHistory(history: List<String>) {
        saveStringList(QUERY_HISTORY_KEY, history)
    }

    private companion object {
        const val QUERY_HISTORY_KEY = "query_history"
    }
}
```

`CorePreferences` используй только для общих app/core настроек. Feature-only флаги держи в feature storage.

## GlobalSessionMemory

Для данных, которые живут только в текущей сессии, используй `GlobalSessionMemory`.

```kotlin
internal class AuthSessionStorage(
    sessionMemory: GlobalSessionMemory,
) {
    var accessToken: String? by sessionMemory
    var currentUserId: String? by sessionMemory
}
```

Это RAM-хранилище. Не используй его для offline-cache или данных, которые должны переживать убийство процесса.

## Room

Core дает только helpers. Конкретные `AppDatabase`, `Entity`, `Dao` и migrations создаются в app/feature слоях.

```kotlin
@Database(
    entities = [ProfileEntity::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
```

```kotlin
class AppDatabaseModule(
    private val context: Context,
) {

    val database: AppDatabase by lazy {
        RoomDatabaseFactory.create<AppDatabase>(
            context = context,
            databaseName = "internet_booster.db",
        )
    }

    val profileDao: ProfileDao by lazy {
        database.profileDao()
    }

    val transactionRunner: RoomTransactionRunner by lazy {
        RoomTransactionRunner(database)
    }
}
```

В feature dependencies проси только нужный DAO или local data source:

```kotlin
interface ProfileDependencies {
    fun profileRemoteApi(): ProfileRemoteApi
    fun profileDao(): ProfileDao
}
```

## Gradle для Room

Модуль с `@Database`, `@Dao` или `@Entity` должен подключить KSP и compiler:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
```

## Что нельзя

| Нельзя | Почему |
|--------|--------|
| Добавлять `baseUrl` в core | core не знает backend приложения |
| Создавать Retrofit API в core | конкретные API принадлежат app/feature слою |
| Проверять сеть вручную в каждом repository | это задача `CoreExceptionMapper` |
| Хранить большие списки в SharedPreferences | для этого нужен Room |
| Класть feature-only настройки в `CorePreferences` | core не должен разрастаться от фич |
| Делать Room database singleton внутри feature | база создается в app/mediator слое |

## Чеклист

- [ ] Remote API приходит через `FeatureDependencies`.
- [ ] Repository зависит от API/DAO, а не от `NetworkManager`.
- [ ] Ошибки не глушатся без причины.
- [ ] DTO не выходит в Presentation.
- [ ] SharedPreferences storage наследуется от `SharedStorage`.
- [ ] Room database создается через `RoomDatabaseFactory`.
- [ ] Несколько DAO-операций в одной операции идут через `RoomTransactionRunner`.
