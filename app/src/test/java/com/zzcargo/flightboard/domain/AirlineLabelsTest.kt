package com.zzcargo.flightboard.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AirlineLabelsTest {

    @Test
    fun `official airline name wins`() {
        assertEquals("CZ 中国南方航空", AirlineLabels.label("CZ", "中国南方航空"))
    }

    @Test
    fun `curated cargo name is used when official name is missing`() {
        assertEquals("K4 卡利塔航空", AirlineLabels.label("K4", ""))
        assertEquals("O3 顺丰航空", AirlineLabels.label("O3", null))
    }

    @Test
    fun `unknown code is shown as the code`() {
        assertEquals("2G", AirlineLabels.label("2G", ""))
    }
}
