package nl.wdhertog.takenlijst.ui

import androidx.annotation.DrawableRes
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.Task

/** Icoontjes voor taken. De sleutel wordt in de database bewaard; leeg betekent "kies op basis van de naam". */
enum class TaskIcon(val key: String, @DrawableRes val drawable: Int, val label: String, val words: List<String>) {
    DOG("hond", R.drawable.ic_task_dog, "Hond", listOf("hond", "uitlaten", "puppy")),
    CAT("kat", R.drawable.ic_task_cat, "Kat", listOf("kat", "poes", "kattenbak")),
    CHICKEN("kip", R.drawable.ic_task_egg, "Kippen", listOf("kip", "kippen", "eieren", "hok")),
    COOK("koken", R.drawable.ic_task_cook, "Koken", listOf("koken", "kook", "eten", "maaltijd", "avondeten", "lunch")),
    SHOPPING("boodschappen", R.drawable.ic_cart, "Boodschappen", listOf("boodschap", "winkel", "supermarkt")),
    TOILET("wc", R.drawable.ic_task_toilet, "Wc", listOf("wc", "toilet", "badkamer", "douche")),
    LAUNDRY("was", R.drawable.ic_task_laundry, "Was", listOf("was ", "wassen", "wasmachine", "droger", "beddengoed")),
    IRON("strijken", R.drawable.ic_task_iron, "Strijken", listOf("strijk")),
    VACUUM("stofzuigen", R.drawable.ic_task_vacuum, "Stofzuigen", listOf("stofzuig", "dweil", "vloer")),
    WINDOW("ramen", R.drawable.ic_task_window, "Ramen", listOf("raam", "ramen")),
    OVEN("oven", R.drawable.ic_task_oven, "Keuken", listOf("oven", "koelkast", "magnetron", "keuken", "fornuis")),
    DISHES("afwas", R.drawable.ic_task_dishes, "Afwas", listOf("afwas", "vaatwas", "vaat")),
    BED("bed", R.drawable.ic_task_bed, "Bed", listOf("bed ", "bedden", "slaapkamer", "lakens")),
    CLEAN("schoonmaak", R.drawable.ic_task_clean, "Schoonmaak", listOf("schoonmaak", "poetsen", "stof", "opruim")),
    TRASH("afval", R.drawable.ic_task_trash, "Afval", listOf("afval", "vuilnis", "container", "kliko", "papier", "glas")),
    HEDGE("snoeien", R.drawable.ic_task_hedge, "Snoeien", listOf("heg", "snoei", "knip")),
    PLANT("planten", R.drawable.ic_task_plant, "Planten", listOf("plant", "water geven", "tuin", "gras", "maai", "onkruid", "bloem")),
    CAR("auto", R.drawable.ic_task_car, "Auto", listOf("auto", "tanken", "apk", "fiets")),
    MAIL("post", R.drawable.ic_task_mail, "Post", listOf("post", "mail", "brief", "rapport")),
    MONEY("geld", R.drawable.ic_task_money, "Geldzaken", listOf("rekening", "betalen", "bank", "belasting", "budget")),
    HEALTH("medicijn", R.drawable.ic_task_pill, "Medicijnen", listOf("medicijn", "pil", "vitamine", "dokter", "tandarts", "vlo", "ontworm")),
    CALENDAR("planning", R.drawable.ic_calendar, "Planning", listOf("plan", "agenda", "overleg", "vergader", "afspraak"));

    companion object {
        fun byKey(key: String) = entries.firstOrNull { it.key == key }

        /**
         * Raadt een icoon bij de naam van de taak; het langste passende trefwoord wint. "Schoonmaak" is
         * algemeen, dus bij "Oven schoonmaken" of "WC schoonmaken" gaat het voorwerp voor.
         */
        fun guess(title: String): TaskIcon? {
            val text = " ${title.lowercase()} "
            val hits = entries.flatMap { icon -> icon.words.map { icon to it } }.filter { (_, word) -> text.contains(word) }
            return (hits.filter { it.first != CLEAN }.ifEmpty { hits }).maxByOrNull { (_, word) -> word.length }?.first
        }
    }
}

/** Het gekozen icoon, anders een geraden icoon, anders dat van de groep. */
@DrawableRes
fun Task.iconRes(): Int = (TaskIcon.byKey(icon) ?: TaskIcon.guess(title))?.drawable ?: group.icon()
