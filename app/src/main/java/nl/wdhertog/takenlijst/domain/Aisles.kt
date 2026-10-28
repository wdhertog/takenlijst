package nl.wdhertog.takenlijst.domain

/** Afdelingen in de winkel, in de volgorde waarin je er meestal langs loopt. */
enum class Aisle(val label: String) {
    FRUIT_VEG("Groente & fruit"),
    BREAD("Brood"),
    MEAT_FISH("Vlees & vis"),
    DAIRY("Zuivel & eieren"),
    CHEESE("Kaas & beleg"),
    PANTRY("Houdbaar"),
    FROZEN("Diepvries"),
    DRINKS("Dranken"),
    SNACKS("Snoep & snacks"),
    CARE("Verzorging"),
    HOUSEHOLD("Huishouden"),
    PETS("Dieren"),
    OTHER("Overig");

    companion object {
        private val keywords: List<Pair<Aisle, List<String>>> = listOf(
            PETS to listOf("hond", "kat", "kippen", "legkorrel", "legmeel", "graan", "brokken", "kattenbak", "voer", "strooisel", "vogel"),
            FRUIT_VEG to listOf(
                "appel", "peer", "banaan", "bananen", "sinaasappel", "mandarijn", "citroen", "limoen", "druif", "druiven",
                "aardbei", "framboos", "bes", "kiwi", "mango", "ananas", "meloen", "avocado", "tomaat", "tomaten",
                "komkommer", "paprika", "ui", "uien", "knoflook", "prei", "wortel", "winterpeen", "sla", "spinazie",
                "broccoli", "bloemkool", "kool", "courgette", "aubergine", "champignon", "paddenstoel", "aardappel",
                "krieltjes", "bonen", "sperzie", "andijvie", "rucola", "biet", "gember", "peterselie", "basilicum",
                "fruit", "groente", "venkel", "selderij", "radijs", "pompoen", "asperge"
            ),
            BREAD to listOf("brood", "bolletjes", "pistolet", "croissant", "beschuit", "crackers", "wrap", "stokbrood", "krentenbol", "tijgerbrood"),
            MEAT_FISH to listOf("kip", "gehakt", "worst", "rookworst", "spek", "ham", "biefstuk", "vlees", "vis", "zalm", "tonijn", "garnaal", "kipfilet", "shoarma", "hamburger", "speklap", "lapjes"),
            DAIRY to listOf("melk", "yoghurt", "kwark", "vla", "boter", "room", "slagroom", "ei", "eieren", "karnemelk", "margarine", "creme fraiche"),
            CHEESE to listOf("kaas", "smeerkaas", "salami", "filet americain", "hummus", "pindakaas", "hagelslag", "jam", "chocopasta", "honing", "beleg", "vlokken", "stroop"),
            FROZEN to listOf("diepvries", "ijs", "pizza", "friet", "patat", "doperwten"),
            DRINKS to listOf("water", "sap", "frisdrank", "cola", "bier", "wijn", "koffie", "thee", "limonade", "siroop", "spa"),
            SNACKS to listOf("chips", "koek", "koekjes", "snoep", "chocola", "chocolade", "drop", "nootjes", "pinda", "noten", "popcorn", "ontbijtkoek", "stroopwafel"),
            CARE to listOf("shampoo", "tandpasta", "tandenborstel", "deo", "zeep", "douchegel", "scheer", "luiers", "maandverband", "tampons", "creme", "zonnebrand", "pleister", "paracetamol"),
            HOUSEHOLD to listOf(
                "wc-papier", "wcpapier", "toiletpapier", "keukenrol", "wasmiddel", "wasverzachter", "afwasmiddel",
                "vaatwas", "schoonmaak", "allesreiniger", "vuilniszak", "afvalzak", "spons", "borstel", "batterij",
                "lamp", "aluminiumfolie", "folie", "bakpapier", "ontkalker", "bleek", "tissues", "zakdoek"
            ),
            PANTRY to listOf(
                "pasta", "spaghetti", "macaroni", "rijst", "noedels", "meel", "bloem", "suiker", "zout", "peper",
                "olie", "azijn", "saus", "ketchup", "mayonaise", "mosterd", "soep", "bouillon", "blik", "pesto",
                "kruiden", "muesli", "cornflakes", "havermout", "linzen", "kikkererwten", "tomatenpuree", "pastasaus"
            ),
        )

        /** Raadt de afdeling op basis van woorden in de productnaam; langste trefwoord wint. */
        fun guess(name: String): Aisle {
            val words = name.lowercase().split(Regex("[^\\p{L}-]+")).filter { it.isNotBlank() }
            val text = name.lowercase()
            var best: Aisle = OTHER
            var bestLength = 0
            for ((aisle, keys) in keywords) {
                for (key in keys) {
                    val hit = if (key.length <= 3) words.any { it == key } else words.any { it.contains(key) } || text.contains(key)
                    if (hit && key.length > bestLength) {
                        best = aisle
                        bestLength = key.length
                    }
                }
            }
            return best
        }
    }
}
