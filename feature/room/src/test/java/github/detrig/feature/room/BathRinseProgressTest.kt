package github.detrig.feature.room

import github.detrig.feature.room.domain.model.remainingDirtAfterRinse
import org.junit.Assert.assertEquals
import org.junit.Test

class BathRinseProgressTest {
    @Test fun dirtFadesAsFoamIsRinsed() {
        var dirt = 1f
        listOf(80f, 50f, 20f, 0f).forEach { remaining ->
            dirt = remainingDirtAfterRinse(dirt, remaining, 100f)
            assertEquals(remaining / 100f, dirt, 0.001f)
        }
    }

    @Test fun dirtFadesBeforeAFoamSpotDisappears() {
        assertEquals(0.85f, remainingDirtAfterRinse(1f, 8.5f, 10f), 0.001f)
    }

    @Test fun addingMoreSoapCannotBringDirtBack() {
        val partial = remainingDirtAfterRinse(1f, 20f, 100f)
        assertEquals(0.2f, remainingDirtAfterRinse(partial, 120f, 120f), 0.001f)
        val clean = remainingDirtAfterRinse(partial, 0f, 120f)
        assertEquals(0f, remainingDirtAfterRinse(clean, 120f, 120f), 0.001f)
    }
}
