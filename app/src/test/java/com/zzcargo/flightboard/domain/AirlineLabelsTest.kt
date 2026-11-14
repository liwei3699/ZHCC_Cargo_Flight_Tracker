package com.zzcargo.flightboard.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AirlineLabelsTest {

    @Test
    fun `directory airline name wins`() {
        assertEquals("CZ 测试名称", AirlineLabels.label("CZ", "测试名称"))
    }

    @Test
    fun `built-in official name is used without directory data`() {
        assertEquals("CZ 中国南方航空", AirlineLabels.label("CZ", null))
        assertEquals("CA 中国国际航空", AirlineLabels.label("CA", ""))
    }

    @Test
    fun `curated cargo name is used when directory name is missing`() {
        assertEquals("K4 卡利塔航空", AirlineLabels.label("K4", ""))
        assertEquals("O3 顺丰航空", AirlineLabels.label("O3", null))
        assertEquals("2G 全球货运航空", AirlineLabels.label("2G", ""))
    }

    @Test
    fun `unknown code is shown as the code`() {
        assertEquals("ZZ", AirlineLabels.label("ZZ", ""))
    }
}
