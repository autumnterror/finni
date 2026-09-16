# Room

В эталонном core Room не используется: локальное хранение там сделано через `GlobalSessionMemory` и `SharedPreferences`.

В этом проекте Room нужен часто, поэтому добавляем только базовые helpers в core. Конкретные `Database`, `Dao` и `Entity` остаются в app/feature слоях.

## Что лежит в core

```text
core/database/RoomDatabaseFactory.kt
core/database/RoomTransactionRunner.kt
```

Core не должен знать:

- какие есть таблицы;
- какие есть DAO;
- какие feature используют базу;
- какой конкретный `AppDatabase` у приложения.

## Dependencies

Для модулей, где объявлены `@Database`, `@Dao` или `@Entity`, нужны Room и KSP.

В version catalog добавлены:

```toml
room = "2.8.4"
ksp = "2.2.21-2.0.5"

androidx-room-runtime
androidx-room-ktx
androidx-room-compiler
```

В модуле с Room annotations нужно подключить:

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

## RoomDatabaseFactory

`RoomDatabaseFactory` создает конкретную Room database.

```kotlin
val database = RoomDatabaseFactory.create<AppDatabase>(
    context = context,
    databaseName = "internet_booster.db",
)
```

С migrations:

```kotlin
val database = RoomDatabaseFactory.create<AppDatabase>(
    context = context,
    databaseName = "internet_booster.db",
    migrations = arrayOf(MIGRATION_1_2),
)
```

`fallbackToDestructiveMigration` лучше держать `false`. Включать его стоит только осознанно, обычно для debug или прототипа.

## Где создавать AppDatabase

В стиле проекта лучше создать database в app/mediator слое:

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
        RoomDatabaseFactory.create(
            context = context,
            databaseName = "internet_booster.db",
        )
    }

    val profileDao: ProfileDao by lazy {
        database.profileDao()
    }
}
```

## Entity

Entity - таблица в базе.

```kotlin
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
)
```

Entity можно держать рядом с feature:

```text
feature/profile/data/local/ProfileEntity.kt
```

## Dao

Dao - методы работы с таблицей.

```kotlin
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfile(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileEntity)
}
```

## FeatureDependencies

Feature получает только нужный DAO.

```kotlin
interface ProfileDependencies {
    fun profileRemoteApi(): ProfileRemoteApi
    fun profileDao(): ProfileDao
    fun globalNavigator(): GlobalNavigator
}
```

App/mediator реализует dependencies:

```kotlin
class ProfileDependenciesImpl(
    private val coreComponent: CoreComponent,
    private val appNetworkModule: AppNetworkModule,
    private val appDatabaseModule: AppDatabaseModule,
) : ProfileDependencies {

    override fun profileRemoteApi(): ProfileRemoteApi {
        return appNetworkModule.profileRemoteApi
    }

    override fun profileDao(): ProfileDao {
        return appDatabaseModule.profileDao
    }

    override fun globalNavigator(): GlobalNavigator {
        return coreComponent.globalNavigator
    }
}
```

## Repository

Repository объединяет remote и local источники.

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

## RoomTransactionRunner

`RoomTransactionRunner` нужен, если надо выполнить несколько DAO-операций атомарно.

```kotlin
class ProfileLocalDataSource(
    private val dao: ProfileDao,
    private val transactionRunner: RoomTransactionRunner,
) {

    suspend fun replaceProfiles(profiles: List<ProfileEntity>) {
        transactionRunner.runInTransaction {
            dao.clearProfiles()
            dao.saveProfiles(profiles)
        }
    }
}
```

Создается в app database module:

```kotlin
val transactionRunner: RoomTransactionRunner by lazy {
    RoomTransactionRunner(database)
}
```

## Коротко

- Core дает только `RoomDatabaseFactory` и `RoomTransactionRunner`.
- `AppDatabase` создается в app/mediator слое.
- `Entity` и `Dao` лучше держать рядом с feature.
- Feature получает DAO через `Dependencies`.
- Repository работает с DAO и remote API.
- Для маленьких key-value настроек Room не нужен, используй `SharedStorage`.
