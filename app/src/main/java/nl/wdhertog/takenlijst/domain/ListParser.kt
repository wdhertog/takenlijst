package nl.wdhertog.takenlijst.domain

data class ParsedItem(val name: String, val quantity: String = "")

/** Zet een regel tekst ("- eieren (12)", "2x melk", "Kaas 500 g") om in een product met hoeveelheid. */
object ListParser {

    private val bullet = Regex("^\\s*(?:[-–—•*·>□☐☑✓✔+]|o\\s|\\d{1,2}[.)])\\s*")
    private val parenQuantity = Regex("\\(([^)]{1,15})\\)")
    private val unit = "(?:g|gr|gram|kg|kilo|l|liter|ltr|ml|cl|st|stuks?|pak|pakken|zak|zakken|fles|flessen|blik|blikken|bak|bakken|doos|dozen|bos|bossen|net|netten|tros|rol|rollen|pot|potten)"
    private val leadingQuantity = Regex("^(\\d+(?:[.,]\\d+)?\\s*(?:x\\b|$unit\\b)?)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val trailingQuantity = Regex("^(.+?)\\s+((?:x\\s*)?\\d+(?:[.,]\\d+)?\\s*(?:x\\b|$unit\\b)?)$", RegexOption.IGNORE_CASE)
    private val headings = setOf("boodschappen", "boodschappenlijst", "lijst", "lijstje", "todo", "te halen")

    fun parseLine(raw: String): ParsedItem? {
        var line = raw.replace(bullet, "").trim().trimEnd(',', ';', ':')
        if (line.isEmpty()) return null

        var quantity = ""
        parenQuantity.find(line)?.let {
            quantity = it.groupValues[1].trim()
            line = line.removeRange(it.range).trim()
        }
        if (quantity.isEmpty()) {
            leadingQuantity.find(line)?.let {
                quantity = it.groupValues[1].trim()
                line = it.groupValues[2].trim()
            } ?: trailingQuantity.find(line)?.let {
                quantity = it.groupValues[2].trim()
                line = it.groupValues[1].trim()
            }
        }
        // "2x", "2 x" en "x2" worden allemaal "2×".
        quantity = quantity
            .replace(Regex("^x\\s*(\\d+)$", RegexOption.IGNORE_CASE), "$1×")
            .replace(Regex("^(\\d+)\\s*x$", RegexOption.IGNORE_CASE), "$1×")

        val name = line.trim().trimEnd('.', ',', '-').trim()
        if (name.count { it.isLetter() } < 2) return null
        if (name.lowercase() in headings) return null
        return ParsedItem(name.replaceFirstChar { it.uppercase() }, quantity)
    }

    fun parse(text: String): List<ParsedItem> = text.lines().mapNotNull(::parseLine)
}
