package com.zzcargo.flightboard.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimes {
    private val updatedFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    private val fileFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun formatUpdated(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(updatedFormatter)

    fun fileStamp(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(fileFormatter)
}
