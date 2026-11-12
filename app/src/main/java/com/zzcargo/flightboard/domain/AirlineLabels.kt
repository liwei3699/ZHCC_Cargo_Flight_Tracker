package com.zzcargo.flightboard.domain

object AirlineLabels {

    // The official airline page only lists passenger carriers; these are
    // commonly used cargo carriers seen in the flight data.
    private val fallbackNames = mapOf(
        "5X" to "UPS航空",
        "5Y" to "阿特拉斯航空",
        "7L" to "丝绸之路西部航空",
        "CF" to "中国邮政航空",
        "CK" to "中国货运航空",
        "CV" to "卢森堡国际货运航空",
        "DR" to "瑞丽航空",
        "ET" to "埃塞俄比亚航空",
        "GI" to "中原龙浩航空",
        "HT" to "天津货运航空",
        "K4" to "卡利塔航空",
        "M6" to "阿美捷航空",
        "O3" to "顺丰航空",
        "OD" to "峇迪航空",
        "OQ" to "重庆航空",
        "VI" to "伏尔加-第聂伯航空"
    )

    fun label(code: String, officialName: String?): String {
        val name = officialName?.takeIf { it.isNotBlank() } ?: fallbackNames[code]
        return if (name.isNullOrBlank()) code else "$code $name"
    }
}
