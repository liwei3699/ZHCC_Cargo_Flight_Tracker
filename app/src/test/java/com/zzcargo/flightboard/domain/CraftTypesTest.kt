package com.zzcargo.flightboard.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CraftTypesTest {

    @Test
    fun `maps aircraft codes to families`() {
        assertEquals("波音737", CraftTypes.familyOf("B733"))
        assertEquals("波音737", CraftTypes.familyOf("B738M"))
        assertEquals("波音747", CraftTypes.familyOf("B744"))
        assertEquals("波音777", CraftTypes.familyOf("B772LR"))
        assertEquals("空客A320系列", CraftTypes.familyOf("A320NEO"))
        assertEquals("空客A350", CraftTypes.familyOf("A359"))
        assertEquals("中国商飞ARJ21", CraftTypes.familyOf("ARJ217"))
        assertEquals("巴航工业E系列", CraftTypes.familyOf("E190"))
        assertEquals("伊尔系列", CraftTypes.familyOf("IL76"))
        assertEquals(CraftTypes.OTHER_FAMILY, CraftTypes.familyOf("C919"))
    }

    @Test
    fun `short labels strip the Boeing prefix only`() {
        assertEquals("733", CraftTypes.shortLabel("B733"))
        assertEquals("738", CraftTypes.shortLabel("B738"))
        assertEquals("739", CraftTypes.shortLabel("B739"))
        assertEquals("738M", CraftTypes.shortLabel("B738M"))
        assertEquals("772LR", CraftTypes.shortLabel("B772LR"))
        assertEquals("A320NEO", CraftTypes.shortLabel("A320NEO"))
        assertEquals("ARJ217", CraftTypes.shortLabel("ARJ217"))
    }
}
