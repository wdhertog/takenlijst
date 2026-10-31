package nl.wdhertog.takenlijst.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** NONE is "op afroep", ONCE "eenmalig": beide zonder vast schema, maar ONCE is na afvinken afgerond. */
enum class RepeatUnit { NONE, DAY, WEEK, MONTH, YEAR, ONCE }

/**
 * Hoe vaak een taak terugkomt. [unit] NONE betekent "op afroep".
 * [weekdays] is een bitmasker (maandag = bit 0). Bij WEEK zijn het de gekozen dagen; bij MONTH met
 * [monthWeek] is het één dag: [monthWeek] 1..4 is de eerste tot vierde, -1 de laatste van de maand.
 */
data class Repeat(val unit: RepeatUnit, val every: Int = 1, val weekdays: Int = 0, val monthWeek: Int = 0) {

    /** Zonder vast schema: op afroep of eenmalig; de taak staat alleen op een dag als je hem inplant. */
    val isOnDemand get() = unit == RepeatUnit.NONE || unit == RepeatUnit.ONCE

    /** Eenmalig: na afvinken afgerond en uit de lijsten. */
    val isOnce get() = unit == RepeatUnit.ONCE

    /** Maandelijks op een vaste weekdag, zoals "eerste zaterdag". */
    val isMonthWeekday get() = unit == RepeatUnit.MONTH && monthWeek != 0 && weekdays != 0

    /** De weekdag bij [isMonthWeekday]. */
    val monthDay: DayOfWeek get() = DayOfWeek.of(Integer.numberOfTrailingZeros(weekdays) + 1)

    fun describe(): String = when (unit) {
        RepeatUnit.NONE -> "Op afroep"
        RepeatUnit.ONCE -> "Eenmalig"
        RepeatUnit.DAY -> if (every == 1) "Dagelijks" else "Elke $every dagen"
        RepeatUnit.WEEK -> {
            val base = if (every == 1) "Wekelijks" else "Elke $every weken"
            if (weekdays == 0) base else "$base · ${weekdayLabels(weekdays)}"
        }
        RepeatUnit.MONTH -> {
            val base = when (every) {
                1 -> "Maandelijks"
                3 -> "Per kwartaal"
                6 -> "Elk half jaar"
                else -> "Elke $every maanden"
            }
            if (isMonthWeekday) "$base · ${ORDINALS[monthWeek]} ${DAY_NAMES[monthDay.value - 1]}" else base
        }
        RepeatUnit.YEAR -> if (every == 1) "Jaarlijks" else "Elke $every jaar"
    }

    companion object {
        val DAILY = Repeat(RepeatUnit.DAY)
        val WEEKLY = Repeat(RepeatUnit.WEEK)
        val MONTHLY = Repeat(RepeatUnit.MONTH)
        val QUARTERLY = Repeat(RepeatUnit.MONTH, 3)
        val YEARLY = Repeat(RepeatUnit.YEAR)
        val ON_DEMAND = Repeat(RepeatUnit.NONE)
        val ONCE = Repeat(RepeatUnit.ONCE)

        val SHORT_DAYS = listOf("ma", "di", "wo", "do", "vr", "za", "zo")
        val DAY_NAMES = listOf("maandag", "dinsdag", "woensdag", "donderdag", "vrijdag", "zaterdag", "zondag")
        /** Welke van de maand, voor [monthWeek]; -1 is de laatste. */
        val ORDINALS = linkedMapOf(1 to "eerste", 2 to "tweede", 3 to "derde", 4 to "vierde", -1 to "laatste")

        /** Welke [day] van de maand het is: 1..4, of -1 voor een vijfde (dat is altijd de laatste). */
        fun monthWeekOf(day: LocalDate) = ((day.dayOfMonth - 1) / 7 + 1).let { if (it > 4) -1 else it }

        fun bit(day: DayOfWeek) = 1 shl (day.value - 1)

        fun weekdayLabels(mask: Int) =
            DayOfWeek.entries.filter { mask and bit(it) != 0 }
                .joinToString(", ") { SHORT_DAYS[it.value - 1] }
    }
}

object Recurrence {

    /** De eerste keer dat een nieuwe taak aan de beurt is, gerekend vanaf [start]. */
    fun firstDue(repeat: Repeat, start: LocalDate): LocalDate? = when {
        repeat.isOnDemand -> null
        repeat.unit == RepeatUnit.WEEK && repeat.weekdays != 0 ->
            if (matches(repeat.weekdays, start)) start else nextWeekday(repeat, start)
        repeat.isMonthWeekday ->
            monthWeekday(repeat, start).takeUnless { it.isBefore(start) } ?: monthWeekday(repeat, start.plusMonths(1))
        else -> start
    }

    /**
     * De volgende keer na afvinken op [today]. Normaal telt het schema door vanaf de geplande datum
     * (een maandtaak die 2 dagen te laat is blijft op dezelfde dag van de maand); met
     * [fromCompletion] telt het vanaf de dag dat je hem echt deed. De uitkomst ligt altijd na vandaag.
     */
    fun next(repeat: Repeat, due: LocalDate?, today: LocalDate, fromCompletion: Boolean): LocalDate? {
        if (repeat.isOnDemand) return null
        // Vroeg afgevinkt telt als gedaan voor die geplande keer, dus niet terugvallen naar vandaag.
        val base = if (fromCompletion || due == null) today else due
        var next = step(repeat, base)
        while (!next.isAfter(today)) next = step(repeat, next)
        return next
    }

    fun step(repeat: Repeat, from: LocalDate): LocalDate {
        val every = repeat.every.coerceAtLeast(1).toLong()
        return when (repeat.unit) {
            RepeatUnit.NONE, RepeatUnit.ONCE -> from
            RepeatUnit.DAY -> from.plusDays(every)
            RepeatUnit.WEEK -> if (repeat.weekdays == 0) from.plusWeeks(every) else nextWeekday(repeat, from)
            RepeatUnit.MONTH -> if (repeat.isMonthWeekday) monthWeekday(repeat, from.plusMonths(every)) else from.plusMonths(every)
            RepeatUnit.YEAR -> from.plusYears(every)
        }
    }

    /** De gekozen weekdag ("eerste zaterdag") in de maand van [month]. */
    private fun monthWeekday(repeat: Repeat, month: LocalDate): LocalDate =
        month.with(TemporalAdjusters.dayOfWeekInMonth(repeat.monthWeek, repeat.monthDay))

    private fun matches(mask: Int, date: LocalDate) = mask and Repeat.bit(date.dayOfWeek) != 0

    /** Eerstvolgende gekozen weekdag na [from]; bij "elke N weken" wordt na de laatste dag van de week N-1 weken overgeslagen. */
    private fun nextWeekday(repeat: Repeat, from: LocalDate): LocalDate {
        var d = from.plusDays(1)
        while (d.dayOfWeek != DayOfWeek.MONDAY) {
            if (matches(repeat.weekdays, d)) return d
            d = d.plusDays(1)
        }
        val monday = d.plusWeeks(repeat.every.coerceAtLeast(1) - 1L)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var c = monday
        for (i in 0 until 7) {
            if (matches(repeat.weekdays, c)) return c
            c = c.plusDays(1)
        }
        return from.plusWeeks(1)
    }
}
