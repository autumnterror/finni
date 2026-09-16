package github.detrig.minigames.fishing.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.minigames.fishing.domain.FishingProgress
import github.detrig.minigames.fishing.domain.FishingRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString

internal class FishingRepositoryImpl(
    private val dao: FishingDao,
    private val transactions: RoomTransactionRunner,
    private val json: Json,
    private val rulesVersion: Int,
) : FishingRepository {
    override suspend fun load(profileId: String): FishingProgress = transactions.runInTransaction {
        read(profileId)
    }

    override suspend fun update(profileId: String, transform: (FishingProgress) -> FishingProgress): FishingProgress =
        transactions.runInTransaction {
            val updated = transform(read(profileId))
            require(updated.profileId == profileId)
            dao.write(FishingProgressEntity(profileId, json.encodeToString(updated)))
            updated
        }

    private suspend fun read(profileId: String): FishingProgress {
        val row = dao.read(profileId) ?: return FishingProgress(profileId = profileId)
        val root = json.parseToJsonElement(row.payload).jsonObject
        val schema = root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1
        require(schema in 1..5) { "Unsupported fishing save" }
        val session = root["session"]
        val savedRules = (session as? JsonObject)?.get("rulesVersion")?.jsonPrimitive?.intOrNull
        val incompatibleSession = savedRules != null && savedRules != rulesVersion
        val values = root.toMutableMap()
        if (schema == 1) {
            values["schemaVersion"] = JsonPrimitive(3)
            val preferences = (root["preferences"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            preferences["inputMode"] = JsonPrimitive("TAP")
            values["preferences"] = JsonObject(preferences)
            values["tutorialDone"] = JsonPrimitive(false)
        }
        if (schema < 3) {
            values["schemaVersion"] = JsonPrimitive(3)
            values.remove("album")
            values.remove("badges")
        }
        if (schema < 4) {
            values["schemaVersion"] = JsonPrimitive(4)
            values.remove("campaign")
        }
        if (schema < 5) {
            values["schemaVersion"] = JsonPrimitive(5)
            val preferences = (values["preferences"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            preferences.remove("inputMode")
            values["preferences"] = JsonObject(preferences)
        }
        if (incompatibleSession) {
            values["session"] = JsonNull
            values["migrationNotice"] = JsonPrimitive(true)
        }
        val migrated = JsonObject(values)
        val progress = json.decodeFromJsonElement(FishingProgress.serializer(), migrated)
        require(progress.profileId == profileId)
        if (migrated != root) dao.write(FishingProgressEntity(profileId, json.encodeToString(progress)))
        return progress
    }
}
