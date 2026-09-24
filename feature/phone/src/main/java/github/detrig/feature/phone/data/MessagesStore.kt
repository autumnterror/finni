package github.detrig.feature.phone.data

import github.detrig.feature.phone.api.PhoneMessagesStorage
import github.detrig.feature.phone.domain.StoredMessagesState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal interface MessagesStore {
    fun load(): StoredMessagesState
    fun save(state: StoredMessagesState)
}

internal class SharedPreferencesMessagesStore(
    private val storage: PhoneMessagesStorage,
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
) : MessagesStore {

    override fun load(): StoredMessagesState {
        val payload = storage.readPayload()
        if (payload.isBlank()) return StoredMessagesState()
        return runCatching { json.decodeFromString<StoredMessagesState>(payload) }
            .getOrDefault(StoredMessagesState())
    }

    override fun save(state: StoredMessagesState) {
        storage.writePayload(json.encodeToString(state))
    }
}
