package github.detrig.feature.phone.api

/** Narrow durable storage contract owned by the phone feature. */
interface PhoneMessagesStorage {
    fun readPayload(): String

    /** Returns only after the payload is durably written. */
    fun writePayload(payload: String)
}
