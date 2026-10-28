package nl.wdhertog.takenlijst.domain

/**
 * Maakt van productgegevens uit Open Food Facts twee namen voor het lijstje: een precieze
 * ("AH Halfvolle melk 1 L") en een ruime ("Melk").
 */
object ProductNames {
    private val sizePattern = Regex(
        """(?i)\b\d+([.,]\d+)?\s*(x\s*\d+([.,]\d+)?\s*)?(ml|cl|dl|l|liter|g|gr|gram|kg|kilo|st|stuks|stk|rollen|tabs|pack)\b\.?"""
    )

    /** Het eerste merk uit een kommagescheiden lijst. */
    fun brand(brands: String): String = brands.split(',').firstOrNull()?.trim().orEmpty()

    /** Merk, productnaam en inhoud, zonder dat iets dubbel komt te staan. */
    fun specific(name: String, brands: String, quantity: String): String {
        val brand = brand(brands)
        var result = clean(name)
        if (brand.isNotEmpty() && !Regex("""(?i)(^|\W)${Regex.escape(brand)}(\W|$)""").containsMatchIn(result)) result = "$brand $result".trim()
        val size = clean(quantity)
        if (size.isNotEmpty() && !squash(result).contains(squash(size))) result = "$result $size".trim()
        return capitalize(result)
    }

    /**
     * De soort product: het eerste bekende productwoord van achter naar voren ("Halfvolle melk" → "Melk"),
     * anders de algemene naam, anders de productnaam zonder merk en inhoud.
     */
    fun broad(name: String, brands: String, generic: String = ""): String {
        val bare = withoutBrandAndSize(name, brands)
        productWord(bare)?.let { return it }
        val gen = withoutBrandAndSize(generic, brands)
        productWord(gen)?.let { return it }
        // Een korte algemene naam is meestal ruimer dan de productnaam ("Hazelnootpasta" in plaats van "Nutella").
        if (gen.isNotEmpty() && (bare.isEmpty() || gen.split(' ').size <= 3)) return capitalize(gen)
        return capitalize(bare)
    }

    private fun productWord(text: String): String? =
        text.split(Regex("""[^\p{L}-]+""")).filter { it.length > 1 }.lastOrNull { Aisle.isProductWord(it) }
            ?.let { capitalize(it.lowercase()) }

    private fun withoutBrandAndSize(name: String, brands: String): String {
        var s = clean(name)
        brands.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { b ->
            s = s.replace(Regex("""(?i)^${Regex.escape(b)}\s+"""), "")
        }
        return clean(s.replace(sizePattern, " "))
    }

    private fun clean(s: String) = s.replace(Regex("""\s+"""), " ").trim().trim(',', '-', '.').trim()
    private fun squash(s: String) = s.lowercase().replace(Regex("""[\s.]"""), "").replace(',', '.')
    private fun capitalize(s: String) = s.replaceFirstChar { it.uppercase() }
}
