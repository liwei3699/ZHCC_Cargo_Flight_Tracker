package com.zzcargo.flightboard.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightModelTest {

    @Test
    fun `display flight number keeps airline prefix and leading zeros`() {
        val flight = Flight(id = 1, airlinesIataCd = "CX", flightNo = "051")
        assertEquals("CX051", flight.displayFlightNo)
    }

    @Test
    fun `abnormal status wins over normal status`() {
        val flight = Flight(id = 1, normalStateCn = "到达", abnormalStateCn = "延误")
        assertEquals("延误", flight.displayStatus)
    }

    @Test
    fun `normal status is used when abnormal is blank`() {
        val flight = Flight(id = 1, normalStateCn = "起飞", abnormalStateCn = "")
        assertEquals("起飞", flight.displayStatus)
    }

    @Test
    fun `blank statuses stay blank`() {
        val flight = Flight(id = 1, normalStateCn = "", abnormalStateCn = "")
        assertEquals("", flight.displayStatus)
    }

    @Test
    fun `region label maps DOM and INT`() {
        assertEquals("国内", Flight(id = 1, regionAttr = "DOM").regionLabel)
        assertEquals("国际", Flight(id = 1, regionAttr = "INT").regionLabel)
        assertEquals("OTHER", Flight(id = 1, regionAttr = "OTHER").regionLabel)
    }

    @Test
    fun `io flags identify arrival and departure`() {
        assertTrue(Flight(id = 1, ioAttr = "A").isArrival)
        assertFalse(Flight(id = 1, ioAttr = "A").isDeparture)
        assertTrue(Flight(id = 1, ioAttr = "D").isDeparture)
    }
}
