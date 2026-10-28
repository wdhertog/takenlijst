package nl.wdhertog.takenlijst.ui

// Gegenereerd uit een selectie Lucide-iconen (https://lucide.dev, ISC-licentie).
// Een icoon toevoegen: tekenbestand in res/drawable plus een regel in ALL hieronder.

import nl.wdhertog.takenlijst.data.TaskGroup
import androidx.annotation.DrawableRes
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.Task

/** Een icoon voor een taak. De [key] wordt in de database bewaard; leeg betekent "kies op basis van de naam". */
data class TaskIcon(val key: String, @DrawableRes val drawable: Int, val label: String, val category: String, val words: List<String>) {
    companion object {
        val CATEGORIES = listOf("Huishouden", "Keuken & eten", "Tuin & buiten", "Dieren", "Boodschappen & geld", "Werk & administratie", "Onderweg", "Gezondheid & gezin", "Klussen")

        val ALL = listOf(
            TaskIcon("huis", R.drawable.ic_icon_house, "Huis", "Huishouden", listOf("huis", "thuis")),
            TaskIcon("schoonmaak", R.drawable.ic_icon_sparkles, "Schoonmaken", "Huishouden", listOf("schoonmaak", "poetsen", "stof", "opruim")),
            TaskIcon("reinigen", R.drawable.ic_icon_spray_can, "Reinigen", "Huishouden", listOf("spray", "ontvet", "ontkalk", "reinig")),
            TaskIcon("stofzuigen", R.drawable.ic_task_vacuum, "Stofzuigen", "Huishouden", listOf("stofzuig", "dweil", "vloer")),
            TaskIcon("ramen", R.drawable.ic_task_window, "Ramen", "Huishouden", listOf("raam", "ramen")),
            TaskIcon("wc", R.drawable.ic_icon_toilet, "Wc", "Huishouden", listOf("wc", "toilet")),
            TaskIcon("badkamer", R.drawable.ic_icon_bath, "Badkamer", "Huishouden", listOf("badkamer", "bad ")),
            TaskIcon("douche", R.drawable.ic_icon_shower_head, "Douche", "Huishouden", listOf("douche")),
            TaskIcon("was", R.drawable.ic_icon_washing_machine, "Was", "Huishouden", listOf("was ", "wassen", "wasmachine", "droger")),
            TaskIcon("strijken", R.drawable.ic_icon_shirt, "Kleding", "Huishouden", listOf("strijk", "kleding", "kleren", "opvouw")),
            TaskIcon("bed", R.drawable.ic_icon_bed_double, "Bed", "Huishouden", listOf("bed ", "bedden", "lakens", "beddengoed", "slaapkamer")),
            TaskIcon("afval", R.drawable.ic_icon_trash_2, "Afval", "Huishouden", listOf("afval", "vuilnis", "kliko", "container")),
            TaskIcon("recyclen", R.drawable.ic_icon_recycle, "Recyclen", "Huishouden", listOf("papier", "glas", "plastic", "pmd", "recycl")),
            TaskIcon("woonkamer", R.drawable.ic_icon_sofa, "Woonkamer", "Huishouden", listOf("woonkamer", "bank ", "kussens")),
            TaskIcon("lampen", R.drawable.ic_icon_lightbulb, "Lampen", "Huishouden", listOf("lamp")),
            TaskIcon("verwarming", R.drawable.ic_icon_heater, "Verwarming", "Huishouden", listOf("verwarming", "cv", "radiator")),
            TaskIcon("sleutels", R.drawable.ic_icon_key, "Sleutels", "Huishouden", listOf("sleutel", "slot")),
            TaskIcon("deur", R.drawable.ic_icon_door_open, "Deur", "Huishouden", listOf("deur")),
            TaskIcon("koken", R.drawable.ic_icon_cooking_pot, "Koken", "Keuken & eten", listOf("koken", "kook", "eten", "maaltijd", "avondeten")),
            TaskIcon("recept", R.drawable.ic_icon_chef_hat, "Recept", "Keuken & eten", listOf("recept", "bakken")),
            TaskIcon("afwas", R.drawable.ic_icon_utensils, "Afwas", "Keuken & eten", listOf("afwas", "vaatwas", "vaat", "bestek")),
            TaskIcon("koelkast", R.drawable.ic_icon_refrigerator, "Koelkast", "Keuken & eten", listOf("koelkast", "vriezer", "diepvries")),
            TaskIcon("oven", R.drawable.ic_icon_microwave, "Oven", "Keuken & eten", listOf("oven", "magnetron")),
            TaskIcon("koffie", R.drawable.ic_icon_coffee, "Koffie", "Keuken & eten", listOf("koffie")),
            TaskIcon("lunch", R.drawable.ic_icon_soup, "Lunch", "Keuken & eten", listOf("lunch", "soep")),
            TaskIcon("brood", R.drawable.ic_icon_sandwich, "Brood", "Keuken & eten", listOf("brood", "lunchpakket", "broodtrommel")),
            TaskIcon("drinken", R.drawable.ic_icon_glass_water, "Drinken", "Keuken & eten", listOf("drinken", "water drinken")),
            TaskIcon("melk", R.drawable.ic_icon_milk, "Melk", "Keuken & eten", listOf("melk")),
            TaskIcon("fruit", R.drawable.ic_icon_apple, "Fruit", "Keuken & eten", listOf("fruit", "appel")),
            TaskIcon("planten", R.drawable.ic_icon_sprout, "Planten", "Tuin & buiten", listOf("plant", "zaaien", "moestuin", "kiem")),
            TaskIcon("bloemen", R.drawable.ic_icon_flower_2, "Bloemen", "Tuin & buiten", listOf("bloem")),
            TaskIcon("snoeien", R.drawable.ic_icon_scissors, "Snoeien", "Tuin & buiten", listOf("heg", "snoei", "knip")),
            TaskIcon("bomen", R.drawable.ic_icon_trees, "Bomen", "Tuin & buiten", listOf("boom", "bomen", "bladeren")),
            TaskIcon("tuin", R.drawable.ic_icon_leaf, "Tuin", "Tuin & buiten", listOf("tuin", "onkruid")),
            TaskIcon("maaien", R.drawable.ic_icon_tractor, "Gras maaien", "Tuin & buiten", listOf("gras", "maai", "gazon")),
            TaskIcon("spitten", R.drawable.ic_icon_shovel, "Spitten", "Tuin & buiten", listOf("spit", "schep", "graaf")),
            TaskIcon("water geven", R.drawable.ic_icon_droplets, "Water geven", "Tuin & buiten", listOf("water geven", "sproei", "gieten")),
            TaskIcon("schutting", R.drawable.ic_icon_fence, "Schutting", "Tuin & buiten", listOf("schutting", "hek")),
            TaskIcon("terras", R.drawable.ic_icon_sun, "Terras", "Tuin & buiten", listOf("terras", "tuinmeubel", "zon")),
            TaskIcon("winter", R.drawable.ic_icon_snowflake, "Winter", "Tuin & buiten", listOf("sneeuw", "strooi", "winter")),
            TaskIcon("haard", R.drawable.ic_icon_flame, "Haard en bbq", "Tuin & buiten", listOf("haard", "hout", "bbq", "barbecue")),
            TaskIcon("dakgoot", R.drawable.ic_icon_cloud_rain, "Dakgoot", "Tuin & buiten", listOf("dakgoot", "goot", "regen")),
            TaskIcon("hond", R.drawable.ic_icon_dog, "Hond", "Dieren", listOf("hond", "uitlaten", "puppy")),
            TaskIcon("kat", R.drawable.ic_icon_cat, "Kat", "Dieren", listOf("kat", "poes", "kattenbak")),
            TaskIcon("kip", R.drawable.ic_icon_egg, "Kippen", "Dieren", listOf("kip", "kippen", "eieren", "hok")),
            TaskIcon("vogel", R.drawable.ic_icon_bird, "Vogels", "Dieren", listOf("vogel", "parkiet")),
            TaskIcon("vis", R.drawable.ic_icon_fish, "Vissen", "Dieren", listOf("vis ", "vissen", "aquarium")),
            TaskIcon("konijn", R.drawable.ic_icon_rabbit, "Konijn", "Dieren", listOf("konijn", "cavia", "knaagdier")),
            TaskIcon("huisdier", R.drawable.ic_icon_paw_print, "Huisdier", "Dieren", listOf("huisdier", "dierenarts", "vlo", "ontworm")),
            TaskIcon("boodschappen", R.drawable.ic_icon_shopping_cart, "Boodschappen", "Boodschappen & geld", listOf("boodschap", "supermarkt")),
            TaskIcon("markt", R.drawable.ic_icon_shopping_basket, "Markt", "Boodschappen & geld", listOf("markt", "mand")),
            TaskIcon("winkelen", R.drawable.ic_icon_shopping_bag, "Winkelen", "Boodschappen & geld", listOf("winkel", "kopen")),
            TaskIcon("bonnetjes", R.drawable.ic_icon_receipt, "Bonnetjes", "Boodschappen & geld", listOf("bonnetje", "kassabon", "declar")),
            TaskIcon("geld", R.drawable.ic_icon_wallet, "Geldzaken", "Boodschappen & geld", listOf("rekening", "betalen", "geld", "budget", "belasting")),
            TaskIcon("sparen", R.drawable.ic_icon_piggy_bank, "Sparen", "Boodschappen & geld", listOf("sparen", "spaar")),
            TaskIcon("betaalpas", R.drawable.ic_icon_credit_card, "Betaalpas", "Boodschappen & geld", listOf("pinpas", "creditcard", "abonnement")),
            TaskIcon("cadeau", R.drawable.ic_icon_gift, "Cadeau", "Boodschappen & geld", listOf("cadeau", "kado")),
            TaskIcon("pakket", R.drawable.ic_icon_package, "Pakketje", "Boodschappen & geld", listOf("pakket", "retour", "bezorg")),
            TaskIcon("planning", R.drawable.ic_icon_calendar, "Planning", "Werk & administratie", listOf("plan", "agenda", "afspraak")),
            TaskIcon("deadline", R.drawable.ic_icon_calendar_check, "Deadline", "Werk & administratie", listOf("deadline", "inleveren")),
            TaskIcon("overleg", R.drawable.ic_icon_users, "Overleg", "Werk & administratie", listOf("overleg", "vergader", "meeting", "team")),
            TaskIcon("werk", R.drawable.ic_icon_briefcase, "Werk", "Werk & administratie", listOf("werk", "kantoor")),
            TaskIcon("computer", R.drawable.ic_icon_laptop, "Computer", "Werk & administratie", listOf("laptop", "computer", "update", "backup")),
            TaskIcon("post", R.drawable.ic_icon_mail, "Post en mail", "Werk & administratie", listOf("post", "mail", "brief")),
            TaskIcon("printen", R.drawable.ic_icon_printer, "Printen", "Werk & administratie", listOf("print")),
            TaskIcon("document", R.drawable.ic_icon_file_text, "Document", "Werk & administratie", listOf("document", "rapport", "verslag", "formulier")),
            TaskIcon("checklist", R.drawable.ic_icon_clipboard_list, "Checklist", "Werk & administratie", listOf("checklist", "inventaris")),
            TaskIcon("bellen", R.drawable.ic_icon_phone, "Bellen", "Werk & administratie", listOf("bellen", "telefoon")),
            TaskIcon("wekker", R.drawable.ic_icon_alarm_clock, "Wekker", "Werk & administratie", listOf("wekker")),
            TaskIcon("school", R.drawable.ic_icon_graduation_cap, "School", "Werk & administratie", listOf("school", "huiswerk", "studie")),
            TaskIcon("lezen", R.drawable.ic_icon_book, "Lezen", "Werk & administratie", listOf("boek", "lezen", "bibliotheek")),
            TaskIcon("auto", R.drawable.ic_icon_car, "Auto", "Onderweg", listOf("auto", "apk")),
            TaskIcon("tanken", R.drawable.ic_icon_fuel, "Tanken", "Onderweg", listOf("tanken", "benzine", "laadpaal")),
            TaskIcon("fiets", R.drawable.ic_icon_bike, "Fiets", "Onderweg", listOf("fiets", "band plakken")),
            TaskIcon("ov", R.drawable.ic_icon_bus, "Openbaar vervoer", "Onderweg", listOf("bus", "trein", "ov-")),
            TaskIcon("vakantie", R.drawable.ic_icon_plane, "Vakantie", "Onderweg", listOf("vakantie", "reis", "vlieg")),
            TaskIcon("kamperen", R.drawable.ic_icon_tent, "Kamperen", "Onderweg", listOf("kamperen", "camping", "tent")),
            TaskIcon("medicijn", R.drawable.ic_icon_pill, "Medicijnen", "Gezondheid & gezin", listOf("medicijn", "pil", "vitamine")),
            TaskIcon("dokter", R.drawable.ic_icon_stethoscope, "Dokter", "Gezondheid & gezin", listOf("dokter", "huisarts", "tandarts", "controle")),
            TaskIcon("sport", R.drawable.ic_icon_dumbbell, "Sport", "Gezondheid & gezin", listOf("sport", "fitness", "trainen")),
            TaskIcon("gezondheid", R.drawable.ic_icon_heart_pulse, "Gezondheid", "Gezondheid & gezin", listOf("gezond", "bloeddruk")),
            TaskIcon("baby", R.drawable.ic_icon_baby, "Kinderen", "Gezondheid & gezin", listOf("baby", "luier", "kinderen", "kids")),
            TaskIcon("verjaardag", R.drawable.ic_icon_cake, "Verjaardag", "Gezondheid & gezin", listOf("verjaardag", "taart", "feest")),
            TaskIcon("muziek", R.drawable.ic_icon_music, "Muziek", "Gezondheid & gezin", listOf("muziek", "piano", "gitaar")),
            TaskIcon("tv", R.drawable.ic_icon_tv, "Tv", "Gezondheid & gezin", listOf("tv", "film", "serie")),
            TaskIcon("zwemmen", R.drawable.ic_icon_waves, "Zwemmen", "Gezondheid & gezin", listOf("zwem")),
            TaskIcon("bril", R.drawable.ic_icon_glasses, "Bril", "Gezondheid & gezin", listOf("bril", "lenzen", "opticien")),
            TaskIcon("zorg", R.drawable.ic_icon_heart_handshake, "Zorg en bezoek", "Gezondheid & gezin", listOf("mantelzorg", "oppas", "bezoek", "oma", "opa")),
            TaskIcon("repareren", R.drawable.ic_icon_wrench, "Repareren", "Klussen", listOf("repar", "kapot", "onderhoud")),
            TaskIcon("klussen", R.drawable.ic_icon_hammer, "Klussen", "Klussen", listOf("klus", "timmer", "ophang")),
            TaskIcon("boren", R.drawable.ic_icon_drill, "Boren", "Klussen", listOf("boor", "boren")),
            TaskIcon("verven", R.drawable.ic_icon_paint_roller, "Verven", "Klussen", listOf("verf", "schilder", "muur")),
            TaskIcon("lakken", R.drawable.ic_icon_paintbrush, "Lakken", "Klussen", listOf("kwast", "lak")),
            TaskIcon("stroom", R.drawable.ic_icon_plug, "Elektra", "Klussen", listOf("stopcontact", "elektr", "stroom")),
            TaskIcon("opladen", R.drawable.ic_icon_battery_charging, "Opladen", "Klussen", listOf("batterij", "oplad")),
            TaskIcon("thermostaat", R.drawable.ic_icon_thermometer, "Thermostaat", "Klussen", listOf("thermostaat", "temperatuur")),
            TaskIcon("ventilatie", R.drawable.ic_icon_fan, "Ventilatie", "Klussen", listOf("ventilat", "filter", "afzuig")),
        )

        /** Algemene iconen die alleen gekozen worden als er niets specifiekers past. */
        private val GENERIC = setOf("schoonmaak", "tuin", "huis", "werk")

        fun byKey(key: String) = ALL.firstOrNull { it.key == key }

        /** Raadt een icoon bij de naam van de taak: het langste passende trefwoord wint, "Oven schoonmaken" wordt dus een oven. */
        fun guess(title: String): TaskIcon? {
            val text = " ${title.lowercase()} "
            val hits = ALL.flatMap { icon -> icon.words.map { icon to it } }.filter { (_, word) -> text.contains(word) }
            return (hits.filter { it.first.key !in GENERIC }.ifEmpty { hits }).maxByOrNull { (_, word) -> word.length }?.first
        }

        /** Zoeken op label, categorie en trefwoorden. */
        fun search(query: String): List<TaskIcon> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return ALL
            return ALL.filter { icon ->
                icon.label.lowercase().contains(q) || icon.category.lowercase().contains(q) || icon.key.contains(q) ||
                    icon.words.any { it.trim().contains(q) || q.contains(it.trim()) }
            }
        }
    }
}

/** Het gekozen icoon, anders een geraden icoon, anders dat van de groep. */
@DrawableRes
fun Task.iconRes(group: TaskGroup): Int = taskIconRes(icon, title, group)

@DrawableRes
fun taskIconRes(icon: String, title: String, group: TaskGroup): Int = (TaskIcon.byKey(icon) ?: TaskIcon.guess(title))?.drawable ?: group.icon()
