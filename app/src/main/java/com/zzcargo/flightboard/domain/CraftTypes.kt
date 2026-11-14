package com.zzcargo.flightboard.domain

object CraftTypes {

    const val OTHER_FAMILY = "其他机型"

    private data class Family(val label: String, val prefixes: List<String>)

    private val families = listOf(
        Family("波音737", listOf("B73")),
        Family("波音747", listOf("B74")),
        Family("波音757", listOf("B75")),
        Family("波音767", listOf("B76")),
        Family("波音777", listOf("B77")),
        Family("波音787", listOf("B78")),
        Family("空客A320系列", listOf("A31", "A32")),
        Family("空客A330", listOf("A33")),
        Family("空客A340", listOf("A34")),
        Family("空客A350", listOf("A35")),
        Family("空客A380", listOf("A38")),
        Family("巴航工业E系列", listOf("E17", "E19")),
        Family("庞巴迪CRJ", listOf("CRJ")),
        Family("中国商飞ARJ21", listOf("ARJ")),
        Family("伊尔系列", listOf("IL")),
        Family("图系列", listOf("TU"))
    )

    fun familyOf(type: String): String {
        if (type.isBlank()) return OTHER_FAMILY
        return families.firstOrNull { family ->
            family.prefixes.any { type.startsWith(it, ignoreCase = true) }
        }?.label ?: OTHER_FAMILY
    }

    fun shortLabel(type: String): String {
        if (type.length >= 4 && (type[0] == 'B' || type[0] == 'b') && type[1].isDigit()) {
            return type.drop(1)
        }
        return type
    }
}
