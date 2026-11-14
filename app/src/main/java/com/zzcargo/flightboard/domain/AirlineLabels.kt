package com.zzcargo.flightboard.domain

object AirlineLabels {

    // Built-in names keep the filter readable even when the optional airline
    // directory request fails. The directory name still takes precedence.
    private val officialNames = mapOf(
        "CZ" to "中国南方航空",
        "CA" to "中国国际航空",
        "NX" to "澳门航空",
        "GX" to "北部湾航空",
        "EU" to "成都航空",
        "9C" to "春秋航空",
        "KE" to "大韩航空",
        "MU" to "东方航空",
        "DZ" to "东海航空",
        "FU" to "福州航空",
        "CX" to "国泰货运",
        "HU" to "海南航空",
        "NS" to "河北航空",
        "G5" to "华夏航空",
        "AE" to "华信航空",
        "HO" to "吉祥航空",
        "BL" to "捷星太平洋航空",
        "KY" to "昆明航空",
        "MF" to "厦门航空",
        "SC" to "山东航空",
        "FM" to "上海航空",
        "ZH" to "深圳航空",
        "JD" to "首都航空",
        "3U" to "四川航空",
        "GS" to "天津航空",
        "ZA" to "天空吴哥航空",
        "UQ" to "乌鲁木齐航空",
        "PN" to "西部航空",
        "8L" to "祥鹏航空",
        "JR" to "幸福航空",
        "Y8" to "金鹏航空",
        "VN" to "越南航空",
        "GJ" to "长龙航空",
        "BR" to "长荣航空",
        "GT" to "桂林航空",
        "QW" to "青岛航空",
        "BK" to "奥凯航空",
        "RY" to "江西航空",
        "AQ" to "九元航空",
        "A6" to "红土航空",
        "TV" to "西藏航空",
        "GY" to "贵州多彩航空",
        "EK" to "阿联酋航空",
        "JQ" to "澳大利亚捷星航空",
        "TR" to "新加坡酷虎航空",
        "WE" to "泰国微笑航空",
        "SL" to "印尼狮子航空",
        "GA" to "印尼鹰航 *",
        "VJ" to "越捷航空"
    )

    // The official airline page only lists passenger carriers; these are
    // commonly used cargo carriers seen in the flight data.
    private val cargoNames = mapOf(
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

    private val extraNames = mapOf(
        "TK" to "土耳其航空",
        "N8" to "国家航空",
        "2G" to "全球货运航空",
        "TH" to "泛亚航空"
    )

    private val builtInNames = officialNames + cargoNames + extraNames

    fun label(code: String, officialName: String?): String {
        val name = officialName?.takeIf { it.isNotBlank() } ?: builtInNames[code]
        return if (name.isNullOrBlank()) code else "$code $name"
    }
}
