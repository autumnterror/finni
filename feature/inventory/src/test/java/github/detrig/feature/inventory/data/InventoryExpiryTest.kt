package github.detrig.feature.inventory.data

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.inventory.domain.TableFoodConsumptionResult
import github.detrig.feature.inventory.domain.TableFoodExpiry
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class InventoryExpiryTest {
    private val apple = ProductId("apple")
    private var now = 1_000L
    private val values = mutableMapOf<String, Any>()
    private fun repository() = InventoryRepositoryImpl(SharedStorage(preferences()), { now })

    @Test fun exactThreeDaysStayFreshButOlderFoodSpoils() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 2)))
        repo.stageForTable(apple)
        now += TableFoodExpiry.MAX_AGE_MILLIS
        assertFalse(repo.reconcileTableExpiry())
        assertEquals(1, repo.observeTable().first().size)
        now++
        assertTrue(repo.reconcileTableExpiry())
        assertTrue(repo.observeTable().first().isEmpty())
        assertEquals(1, repo.observeStock().first().single().quantity)
    }

    @Test fun eachPortionHasItsOwnShelfLifeAndNoticeSurvivesRestart() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 2)))
        repo.stageForTable(apple)
        now += 24 * 60 * 60 * 1_000L
        repo.stageForTable(apple)
        now += 2 * 24 * 60 * 60 * 1_000L + 1
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
        now += TableFoodExpiry.MAX_AGE_MILLIS + 1
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

    @Test fun olderSavesKeepFoodAndPersistUpgradeTimestamp() = runBlocking {
        values["inventory_food_table__SIZE"] = 2
        values["inventory_food_table__0"] = "old-id|apple"
        values["inventory_food_table__1"] = "banana"
        val upgraded = repository().observeTable().first()
        assertEquals(listOf("apple", "banana"), upgraded.map { it.productId.value })
        assertEquals(listOf(now, now), upgraded.map { it.stagedAtMillis })
        now += TableFoodExpiry.MAX_AGE_MILLIS + 1
        assertTrue(repository().reconcileTableExpiry())
        assertTrue(repository().observeTable().first().isEmpty())
    }

    @Test fun stagingFreshFoodRemovesExpiredPortionsAndKeepsTheirNotice() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 3)))
        repo.stageForTable(apple)
        now += TableFoodExpiry.MAX_AGE_MILLIS + 1
        repo.stageForTable(apple)
        val fresh = repo.observeTable().first().single()
        assertEquals(now, fresh.stagedAtMillis)
        assertTrue(repo.reconcileTableExpiry())
        assertEquals(1, repo.observeStock().first().single().quantity)
    }

    @Test fun clockMovingBackwardsDoesNotSpoilFood() = runBlocking {
        val repo = repository()
        repo.deliver("purchase", listOf(ProductQuantity(apple, 1)))
        repo.stageForTable(apple)
        now -= 1_000
        assertFalse(repo.reconcileTableExpiry())
        assertEquals(1, repo.observeTable().first().size)
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
