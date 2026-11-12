package com.zzcargo.flightboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Flight(
    val id: Long,
    val airlinesIataCd: String = "",
    val flightNo: String = "",
    val ioAttr: String = "",
    val execDate: String = "",
    val startStationCn: String = "",
    val terminalStationCn: String = "",
    val airrouteCn: String = "",
    val airrouteIataCd: String = "",
    val airrouteIcaoCd: String = "",
    val scheTakeOffTime: String = "",
    val actlTakeOffTime: String = "",
    val scheLandingTime: String = "",
    val actlLandingTime: String = "",
    val normalStateCn: String = "",
    val abnormalStateCn: String = "",
    val absreasonCn: String = "",
    val agency: String = "",
    val flightTaskCn: String = "",
    val craftType: String = "",
    val craftNo: String = "",
    val terminal: String = "",
    val stand: String = "",
    val carousel: String = "",
    val regionAttr: String = ""
) {
    val displayFlightNo: String get() = airlinesIataCd + flightNo
    val displayStatus: String get() = abnormalStateCn.ifBlank { normalStateCn }
    val isArrival: Boolean get() = ioAttr == "A"
    val isDeparture: Boolean get() = ioAttr == "D"
    val regionLabel: String get() = when (regionAttr) {
        "DOM" -> "国内"
        "INT" -> "国际"
        else -> regionAttr
    }
}
