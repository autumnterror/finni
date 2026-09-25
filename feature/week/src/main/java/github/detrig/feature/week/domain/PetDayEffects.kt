package github.detrig.feature.week.domain

/** Эффект питомца в той же транзакции, что и переход игрового дня. */
fun interface PetDayEffects {
    suspend fun afterSleep()
}
