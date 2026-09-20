package com.clockmods.ultimate.clock

import com.clockmods.test.MemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WorldClockRepositoryTest {
    @Test
    fun restoreDefaultsDisablesFeatureAndRestoresDefaultCities() {
        val repository = WorldClockRepository(MemorySharedPreferences())
        repository.setEnabled(true)
        repository.save(WorldClockCatalog.all().takeLast(2))

        repository.restoreDefaults()

        assertFalse(repository.isEnabled())
        assertEquals(WorldClockCatalog.defaults(), repository.getSelected())
    }
}
