package nl.wdhertog.takenlijst.domain

/**
 * Verbetert herkende woorden naar bekende Nederlandse productnamen. TrOCR is op Engels handschrift
 * getraind en maakt van "eieren" soms "eleven" of van "appels" "appeals"; een woord dat dicht bij een
 * bekend product ligt wordt daarom vervangen.
 */
class Lexicon(words: Collection<String>) {
    private val known: Set<String> = words.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    private val singleWords: List<String> = known.flatMap { it.split(' ') }.filter { it.length >= 3 }.distinct()

    data class Result(val text: String, val changed: Boolean)

    fun correct(text: String): Result {
        val lower = text.trim().lowercase()
        if (lower.isEmpty() || lower in known) return Result(text.trim(), false)

        // Eerst de hele regel ("volkoren brood"), dan woord voor woord.
        if (lower.none { it.isDigit() }) closest(lower, known)?.let { return Result(it, true) }
        var changed = false
        val words = text.trim().split(Regex("\\s+")).map { word ->
            val core = word.lowercase().trim('.', ',', ';', ':')
            if (core.length < 4 || core.any { it.isDigit() } || core in singleWords) word
            else closest(core, singleWords)?.also { changed = true } ?: word
        }
        return Result(words.joinToString(" "), changed)
    }

    private fun closest(word: String, candidates: Collection<String>): String? {
        // Korte woorden lijken snel op elkaar ("brie" → "bier"), dus daar weinig ruimte.
        val max = when {
            word.length < 4 -> 0
            word.length <= 5 -> 1
            word.length <= 8 -> 2
            else -> 3
        }
        if (max == 0) return null
        var best: String? = null
        var bestDistance = max + 1
        for (c in candidates) {
            if (kotlin.math.abs(c.length - word.length) > max) continue
            val d = distance(word, c, bestDistance)
            if (d < bestDistance) { best = c; bestDistance = d }
        }
        return best
    }

    companion object {
        /** Levenshtein-afstand, met vroegtijdig stoppen zodra [limit] overschreden wordt. */
        fun distance(a: String, b: String, limit: Int = Int.MAX_VALUE): Int {
            var prev = IntArray(b.length + 1) { it }
            var cur = IntArray(b.length + 1)
            for (i in 1..a.length) {
                cur[0] = i
                var rowMin = cur[0]
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                    rowMin = minOf(rowMin, cur[j])
                }
                if (rowMin >= limit) return limit
                val t = prev; prev = cur; cur = t
            }
            return prev[b.length]
        }
    }
}
