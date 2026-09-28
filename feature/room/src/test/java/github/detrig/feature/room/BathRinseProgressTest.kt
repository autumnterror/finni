package github.detrig.feature.room

import github.detrig.feature.room.domain.model.remainingDirtAfterRinse
import org.junit.Assert.assertEquals
import org.junit.Test

class BathRinseProgressTest {
    @Test fun dirtFadesAsFoamIsRinsed() {
        var dirt = 1f
        listOf(80, 50, 20, 0).forEach { remaining ->
            dirt = remainingDirtAfterRinse(dirt, remaining, 100)
            assertEquals(remaining / 100f, dirt, 0.001f)
        }
    }

    @Test fun addingMoreSoapCannotBringDirtBack() {
        val partial = remainingDirtAfterRinse(1f, 20, 100)
        assertEquals(0.2f, remainingDirtAfterRinse(partial, 120, 120), 0.001f)
        val clean = remainingDirtAfterRinse(partial, 0, 120)
        assertEquals(0f, remainingDirtAfterRinse(clean, 120, 120), 0.001f)
    }
}
