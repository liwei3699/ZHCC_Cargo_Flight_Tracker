package com.zzcargo.flightboard.export

import com.zzcargo.flightboard.data.model.Flight
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class XlsxWriterTest {

    private val headers = listOf(
        "航班信息", "起飞机场", "降落机场", "计划起飞时间", "实际起飞时间",
        "计划降落时间", "实际降落时间", "航班日期", "航班状态", "机型",
        "机号", "航站楼", "机位", "代理", "航班任务", "延误原因",
        "航线IATA", "航线ICAO", "国内/国际"
    )

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                result[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    private fun parse(xml: String): Document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

    private fun cell(doc: Document, ref: String): Element? {
        val cells = doc.getElementsByTagName("c")
        for (index in 0 until cells.length) {
            val element = cells.item(index) as Element
            if (element.getAttribute("r") == ref) return element
        }
        return null
    }

    private fun text(doc: Document, ref: String): String? {
        val element = cell(doc, ref) ?: return null
        val texts = element.getElementsByTagName("t")
        if (texts.length > 0) return texts.item(0).textContent
        val values = element.getElementsByTagName("v")
        if (values.length > 0) return values.item(0).textContent
        return null
    }

    private fun style(doc: Document, ref: String): String? =
        cell(doc, ref)?.getAttribute("s")

    private fun columnName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A' + rem))
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private val arrival = Flight(
        id = 1,
        airlinesIataCd = "CZ",
        flightNo = "6376",
        ioAttr = "A",
        execDate = "2026-11-05",
        startStationCn = "乌鲁木齐",
        terminalStationCn = "郑州",
        scheTakeOffTime = "21:00",
        actlTakeOffTime = "21:12",
        scheLandingTime = "00:50",
        actlLandingTime = "00:40",
        normalStateCn = "到达",
        agency = "南航",
        flightTaskCn = "正班",
        craftType = "B738",
        craftNo = "B1400",
        terminal = "T2",
        stand = "223",
        airrouteIataCd = "URC-CGO",
        airrouteIcaoCd = "ZWWW-ZHCC",
        regionAttr = "DOM"
    )

    private val delayed = Flight(
        id = 2,
        airlinesIataCd = "K4",
        flightNo = "819",
        ioAttr = "D",
        execDate = "2026-11-06",
        startStationCn = "郑州",
        terminalStationCn = "墨西哥城菲利普安吉利斯",
        scheTakeOffTime = "03:00",
        scheLandingTime = "01:30",
        abnormalStateCn = "延误",
        absreasonCn = "航空公司",
        agency = "南航",
        flightTaskCn = "货班",
        craftType = "B744",
        craftNo = "N401KZ",
        terminal = "T2",
        stand = "223",
        regionAttr = "INT"
    )

    @Test
    fun `creates all required ooxml parts`() {
        val files = unzip(XlsxWriter.write(listOf(arrival)))
        val required = listOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml"
        )
        required.forEach { assertTrue("missing $it", files.containsKey(it)) }
        assertTrue(files.getValue("xl/workbook.xml").contains("航班动态"))
    }

    @Test
    fun `writes 19 header columns in the specified order`() {
        val doc = parse(unzip(XlsxWriter.write(listOf(arrival))).getValue("xl/worksheets/sheet1.xml"))
        headers.forEachIndexed { index, expected ->
            assertEquals(expected, text(doc, columnName(index + 1) + "1"))
        }
        assertEquals("1", style(doc, "A1"))
        assertNull(text(doc, "T1"))
    }

    @Test
    fun `writes text numeric date and time cells with correct styles`() {
        val doc = parse(unzip(XlsxWriter.write(listOf(arrival))).getValue("xl/worksheets/sheet1.xml"))

        assertEquals("CZ6376", text(doc, "A2"))
        assertEquals("乌鲁木齐", text(doc, "B2"))
        assertEquals("郑州", text(doc, "C2"))
        assertEquals(0.875, text(doc, "D2")!!.toDouble(), 0.0000001)
        assertEquals(0.8833333333333333, text(doc, "E2")!!.toDouble(), 0.0000001)
        assertEquals(46331.0, text(doc, "H2")!!.toDouble(), 0.0000001)
        assertEquals("到达", text(doc, "I2"))
        assertEquals("国内", text(doc, "S2"))
        assertEquals("2", style(doc, "A2"))
        assertEquals("3", style(doc, "D2"))
        assertEquals("4", style(doc, "H2"))
        assertEquals("A1:S2", (doc.getElementsByTagName("autoFilter").item(0) as Element).getAttribute("ref"))
        val pane = doc.getElementsByTagName("pane").item(0) as Element
        assertEquals("1", pane.getAttribute("ySplit"))
        assertEquals("frozen", pane.getAttribute("state"))
    }

    @Test
    fun `uses abnormal status and writes all extra columns`() {
        val doc = parse(unzip(XlsxWriter.write(listOf(delayed))).getValue("xl/worksheets/sheet1.xml"))

        assertEquals("K4819", text(doc, "A2"))
        assertEquals("延误", text(doc, "I2"))
        assertEquals("B744", text(doc, "J2"))
        assertEquals("N401KZ", text(doc, "K2"))
        assertEquals("T2", text(doc, "L2"))
        assertEquals("223", text(doc, "M2"))
        assertEquals("南航", text(doc, "N2"))
        assertEquals("货班", text(doc, "O2"))
        assertEquals("航空公司", text(doc, "P2"))
        assertEquals("", text(doc, "Q2") ?: "")
        assertEquals("", text(doc, "R2") ?: "")
        assertEquals("国际", text(doc, "S2"))
    }

    @Test
    fun `escapes xml special characters and keeps the file parseable`() {
        val special = arrival.copy(agency = "A&B<C>\"D'")
        val doc = parse(unzip(XlsxWriter.write(listOf(special))).getValue("xl/worksheets/sheet1.xml"))
        assertEquals("A&B<C>\"D'", text(doc, "N2"))
    }

    @Test
    fun `writes a valid header-only sheet when there are no flights`() {
        val doc = parse(unzip(XlsxWriter.write(emptyList())).getValue("xl/worksheets/sheet1.xml"))

        assertEquals("航班信息", text(doc, "A1"))
        assertEquals("国内/国际", text(doc, "S1"))
        assertNull(cell(doc, "A2"))
        assertEquals("A1:S1", (doc.getElementsByTagName("autoFilter").item(0) as Element).getAttribute("ref"))
    }

    @Test
    fun `writes a blank cell for missing actual times`() {
        val doc = parse(unzip(XlsxWriter.write(listOf(arrival.copy(actlTakeOffTime = "", actlLandingTime = "")))).getValue("xl/worksheets/sheet1.xml"))
        assertNull(text(doc, "E2"))
        assertNull(text(doc, "G2"))
        assertEquals("3", style(doc, "E2"))
        assertEquals("3", style(doc, "G2"))
    }
}
