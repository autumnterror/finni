package github.detrig.feature.economy.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationIdsTest {

    @Test
    fun reusesBaseIdOnlyWhenNoPreviousTransactionUsesIt() = runBlocking {
        val takenIds = setOf("help:quick", "help:quick:repeat:1", "help:quick:repeat:2")

        val id = firstAvailableOperationId("help:quick") { it in takenIds }

        assertEquals("help:quick:repeat:3", id)
    }

    @Test
    fun firstTransactionKeepsTheStableBaseId() = runBlocking {
        val id = firstAvailableOperationId("help:payoff") { false }

        assertEquals("help:payoff", id)
    }
}
