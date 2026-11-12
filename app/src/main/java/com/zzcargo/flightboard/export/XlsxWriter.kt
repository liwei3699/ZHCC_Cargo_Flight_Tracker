package com.zzcargo.flightboard.export

import com.zzcargo.flightboard.data.model.Flight
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxWriter {

    private val headers = listOf(
        "航班信息", "起飞机场", "降落机场", "计划起飞时间", "实际起飞时间",
        "计划降落时间", "实际降落时间", "航班日期", "航班状态", "机型",
        "机号", "航站楼", "机位", "代理", "航班任务", "延误原因",
        "航线IATA", "航线ICAO", "国内/国际"
    )

    fun write(flights: List<Flight>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            put(zip, "[Content_Types].xml", CONTENT_TYPES)
            put(zip, "_rels/.rels", ROOT_RELS)
            put(zip, "xl/workbook.xml", WORKBOOK)
            put(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            put(zip, "xl/styles.xml", STYLES)
            put(zip, "xl/worksheets/sheet1.xml", buildSheet(flights))
        }
        return out.toByteArray()
    }

    private fun buildSheet(flights: List<Flight>): String {
        val lastRow = flights.size + 1
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("""<dimension ref="A1:S$lastRow"/>""")
        sb.append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/><selection pane="bottomLeft" activeCell="A2" sqref="A2"/></sheetView></sheetViews>""")
        sb.append("""<sheetFormatPr defaultRowHeight="15"/>""")
        sb.append(COLS)
        sb.append("<sheetData>")
        sb.append(buildRow(1, headers.map { Cell.Text(it) }, header = true))
        flights.forEachIndexed { index, flight ->
            sb.append(buildRow(index + 2, cellsFor(flight)))
        }
        sb.append("</sheetData>")
        sb.append("""<autoFilter ref="A1:S$lastRow"/>""")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun cellsFor(flight: Flight): List<Cell> = listOf(
        Cell.Text(flight.displayFlightNo),
        Cell.Text(flight.startStationCn),
        Cell.Text(flight.terminalStationCn),
        Cell.Time(flight.scheTakeOffTime),
        Cell.Time(flight.actlTakeOffTime),
        Cell.Time(flight.scheLandingTime),
        Cell.Time(flight.actlLandingTime),
        Cell.Date(flight.execDate),
        Cell.Text(flight.displayStatus),
        Cell.Text(flight.craftType),
        Cell.Text(flight.craftNo),
        Cell.Text(flight.terminal),
        Cell.Text(flight.stand),
        Cell.Text(flight.agency),
        Cell.Text(flight.flightTaskCn),
        Cell.Text(flight.absreasonCn),
        Cell.Text(flight.airrouteIataCd),
        Cell.Text(flight.airrouteIcaoCd),
        Cell.Text(flight.regionLabel)
    )

    private fun buildRow(row: Int, cells: List<Cell>, header: Boolean = false): String {
        val sb = StringBuilder()
        sb.append("""<row r="$row" spans="1:19">""")
        cells.forEachIndexed { index, cell ->
            val ref = columnName(index + 1) + row
            val style = when {
                header -> 1
                cell is Cell.Text -> 2
                cell is Cell.Time -> 3
                else -> 4
            }
            when (cell) {
                is Cell.Text -> {
                    if (cell.value.isBlank()) {
                        sb.append("""<c r="$ref" s="$style"/>""")
                    } else {
                        sb.append(textCell(ref, cell.value, style))
                    }
                }
                is Cell.Time -> {
                    val value = cell.excelValue()
                    if (value == null) {
                        if (cell.value.isBlank()) sb.append("""<c r="$ref" s="$style"/>""")
                        else sb.append(textCell(ref, cell.value, style))
                    } else {
                        sb.append("""<c r="$ref" s="$style"><v>${formatNumber(value)}</v></c>""")
                    }
                }
                is Cell.Date -> {
                    val value = cell.excelValue()
                    if (value == null) {
                        if (cell.value.isBlank()) sb.append("""<c r="$ref" s="$style"/>""")
                        else sb.append(textCell(ref, cell.value, style))
                    } else {
                        sb.append("""<c r="$ref" s="$style"><v>${formatNumber(value)}</v></c>""")
                    }
                }
            }
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun textCell(ref: String, value: String, style: Int): String =
        """<c r="$ref" s="$style" t="inlineStr"><is><t xml:space="preserve">${escape(value)}</t></is></c>"""

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

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

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun put(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private sealed interface Cell {
        val value: String

        data class Text(override val value: String) : Cell
        data class Time(override val value: String) : Cell {
            fun excelValue(): Double? {
                val parts = value.split(":")
                if (parts.size < 2) return null
                val hour = parts[0].toIntOrNull() ?: return null
                val minute = parts[1].toIntOrNull() ?: return null
                if (hour !in 0..23 || minute !in 0..59) return null
                return (hour * 3600 + minute * 60) / 86400.0
            }
        }

        data class Date(override val value: String) : Cell {
            fun excelValue(): Double? {
                val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
                return date.toEpochDay() + 25569.0
            }
        }
    }

    private const val COLS =
        """<cols><col min="1" max="1" width="12" customWidth="1"/><col min="2" max="3" width="16" customWidth="1"/><col min="4" max="7" width="12" customWidth="1"/><col min="8" max="8" width="12" customWidth="1"/><col min="9" max="9" width="10" customWidth="1"/><col min="10" max="10" width="8" customWidth="1"/><col min="11" max="11" width="10" customWidth="1"/><col min="12" max="12" width="9" customWidth="1"/><col min="13" max="13" width="8" customWidth="1"/><col min="14" max="15" width="10" customWidth="1"/><col min="16" max="16" width="14" customWidth="1"/><col min="17" max="17" width="12" customWidth="1"/><col min="18" max="18" width="16" customWidth="1"/><col min="19" max="19" width="10" customWidth="1"/></cols>"""

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""

    private const val ROOT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private const val WORKBOOK =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="航班动态" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""

    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="2"><numFmt numFmtId="164" formatCode="hh:mm"/><numFmt numFmtId="165" formatCode="yyyy-mm-dd"/></numFmts><fonts count="2"><font><sz val="11"/><name val="等线"/><family val="2"/></font><font><b/><sz val="11"/><name val="等线"/><family val="2"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFDCE9F7"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color auto="1"/></left><right style="thin"><color auto="1"/></right><top style="thin"><color auto="1"/></top><bottom style="thin"><color auto="1"/></bottom><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="5"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf><xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf></cellXfs><cellStyles count="1"><cellStyle name="常规" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""
}
