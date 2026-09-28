package github.detrig.feature.gamestate.domain.model

/** Committed pet effects used to reconcile wishes and their one-time celebration. */
data class PetWishActivity(
    val id: String,
    val source: String,
    val sourceOperationId: String,
    val appliedAtMillis: Long,
)
