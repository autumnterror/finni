package github.detrig.feature.inventory.data

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.inventory.domain.StagedFoodItem
import github.detrig.feature.inventory.domain.TableFoodConsumptionResult
import github.detrig.feature.inventory.domain.TableFoodExpiry
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import java.lang.reflect.Proxy
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class InventoryExpiryTest {
    private val apple = ProductId("apple")
    private val gameDay = MutableStateFlow(1L)
    private val values = mutableMapOf<String, Any>()
    private fun repository() = InventoryRepositoryImpl(
        SharedStorage(preferences()), { gameDay.value }, { gameDay },
    )

    @Test fun foodSpoilsOnThirdGameDayTransition() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 2)))
        repo.stageForTable(apple)
        gameDay.value += TableFoodExpiry.MAX_AGE_GAME_DAYS - 1
        assertFalse(repo.reconcileTableExpiry())
        assertEquals(1, repo.observeTable().first().size)
        gameDay.value++
        assertTrue(repo.reconcileTableExpiry())
        assertTrue(repo.observeTable().first().isEmpty())
        assertEquals(1, repo.observeStock().first().single().quantity)
    }

    @Test fun eachPortionHasItsOwnShelfLifeAndNoticeSurvivesRestart() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 2)))
        repo.stageForTable(apple)
        gameDay.value++
        repo.stageForTable(apple)
        gameDay.value += 2
        val restored = repository()
        assertTrue(restored.reconcileTableExpiry())
        assertEquals(1, restored.observeTable().first().size)
        val secondRestart = repository()
        assertTrue(secondRestart.reconcileTableExpiry())
        secondRestart.acknowledgeSpoiledTableFood()
        assertFalse(repository().reconcileTableExpiry())
    }

    @Test fun expiredFoodCannotApplyPetEffectEvenWithStalePortionId() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 1)))
        repo.stageForTable(apple)
        val id = repo.observeTable().first().single().id
        gameDay.value += TableFoodExpiry.MAX_AGE_GAME_DAYS
        var effects = 0
        assertEquals(TableFoodConsumptionResult.NotFound, repo.consumeTableItem(id) { effects++ })
        assertEquals(0, effects)
        assertTrue(repo.reconcileTableExpiry())
    }

    @Test fun failedEffectKeepsFoodForRetryAndDoubleConsumptionDoesNotRepeatEffect() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 1)))
        repo.stageForTable(apple)
        val id = repo.observeTable().first().single().id
        try {
            repo.consumeTableItem(id) { error("save failed") }
            fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertEquals(id, repo.observeTable().first().single().id)
        var effects = 0
        assertTrue(repo.consumeTableItem(id) { effects++ } is TableFoodConsumptionResult.Consumed)
        assertEquals(TableFoodConsumptionResult.NotFound, repo.consumeTableItem(id) { effects++ })
        assertEquals(1, effects)
    }

    @Test fun undatedSavesKeepFoodAndPersistPlacementGameDay() = runBlocking {
        values["inventory_food_table__SIZE"] = 2
        values["inventory_food_table__0"] = "old-id|apple"
        values["inventory_food_table__1"] = "banana"
        val upgraded = repository().observeTable().first()
        assertEquals(listOf("apple", "banana"), upgraded.map { it.productId.value })
        assertEquals(listOf(gameDay.value, gameDay.value), upgraded.map { it.stagedAtAbsoluteDay })
        gameDay.value += TableFoodExpiry.MAX_AGE_GAME_DAYS
        assertTrue(repository().reconcileTableExpiry())
        assertTrue(repository().observeTable().first().isEmpty())
    }

    @Test fun stagingFreshFoodRemovesExpiredPortionsAndKeepsTheirNotice() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 3)))
        repo.stageForTable(apple)
        gameDay.value += TableFoodExpiry.MAX_AGE_GAME_DAYS
        repo.stageForTable(apple)
        val fresh = repo.observeTable().first().single()
        assertEquals(gameDay.value, fresh.stagedAtAbsoluteDay)
        assertTrue(repo.reconcileTableExpiry())
        assertEquals(1, repo.observeStock().first().single().quantity)
    }

    @Test fun gameDayMovingBackwardsDoesNotSpoilFood() = runBlocking {
        gameDay.value = 10
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 1)))
        repo.stageForTable(apple)
        gameDay.value = 9
        assertFalse(repo.reconcileTableExpiry())
        assertEquals(1, repo.observeTable().first().size)
    }

    @Test fun realTimeSaveStartsFreshOnCurrentGameDayAndKeepsUnreadNotice() = runBlocking {
        gameDay.value = 27
        values["inventory_food_table_v2"] = "1\nold-id|apple|1000"
        val upgraded = repository()
        assertTrue(upgraded.reconcileTableExpiry())
        val portion = upgraded.observeTable().first().single()
        assertEquals("old-id", portion.id)
        assertEquals(27L, portion.stagedAtAbsoluteDay)
        upgraded.acknowledgeSpoiledTableFood()
        gameDay.value = 29
        assertFalse(repository().reconcileTableExpiry())
        gameDay.value = 30
        assertTrue(repository().reconcileTableExpiry())
    }

    @Test fun shelfLifeContinuesAcrossGameWeekBoundary() = runBlocking {
        gameDay.value = 6 // Saturday
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 1)))
        repo.stageForTable(apple)
        gameDay.value = 8 // Monday
        assertFalse(repo.reconcileTableExpiry())
        gameDay.value = 9 // Tuesday, three days since placement
        assertTrue(repo.reconcileTableExpiry())
    }

    @Test fun observingUnchangedGameDayNeverAgesFood() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 1)))
        repo.stageForTable(apple)
        repeat(10) {
            assertFalse(repository().reconcileTableExpiry())
            assertEquals(1L, repository().observeTable().first().single().stagedAtAbsoluteDay)
        }
    }

    @Test fun openRoomTableUpdatesAsGameDayAdvancesWithoutReopening() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 1)))
        repo.stageForTable(apple)
        val updates = Channel<List<StagedFoodItem>>(Channel.UNLIMITED)
        val observation = launch { repo.observeTable().collect { updates.send(it) } }
        try {
            assertEquals(1, withTimeout(2_000) { updates.receive() }.size)
            gameDay.value += TableFoodExpiry.MAX_AGE_GAME_DAYS
            assertTrue(withTimeout(2_000) { updates.receive() }.isEmpty())
            assertTrue(repo.reconcileTableExpiry())
        } finally {
            observation.cancelAndJoin()
            updates.close()
        }
    }

    private fun preferences(): SharedPreferences {
        val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "putString", "putInt" -> { values[args!![0] as String] = args[1]!!; proxy }
                "remove" -> { values.remove(args!![0] as String); proxy }
                "clear" -> { values.clear(); proxy }
                "apply" -> null
                "commit" -> true
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString", "getInt" -> values[args!![0] as String] ?: args[1]
                "contains" -> values.containsKey(args!![0] as String)
                "edit" -> editor
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
    }
}
