package nl.wdhertog.takenlijst.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class RepeatUnit { NONE, DAY, WEEK, MONTH, YEAR }

/**
 * Hoe vaak een taak terugkomt. [unit] NONE betekent "op afroep".
 * [weekdays] is een bitmasker (maandag = bit 0) en geldt alleen bij WEEK.
 */
data class Repeat(val unit: RepeatUnit, val every: Int = 1, val weekdays: Int = 0) {

    val isOnDemand get() = unit == RepeatUnit.NONE

    fun describe(): String = when (unit) {
        RepeatUnit.NONE -> "Op afroep"
        RepeatUnit.DAY -> if (every == 1) "Dagelijks" else "Elke $every dagen"
        RepeatUnit.WEEK -> {
            val base = if (every == 1) "Wekelijks" else "Elke $every weken"
            if (weekdays == 0) base else "$base · ${weekdayLabels(weekdays)}"
        }
        RepeatUnit.MONTH -> when (every) {
            1 -> "Maandelijks"
            3 -> "Per kwartaal"
            6 -> "Elk half jaar"
            else -> "Elke $every maanden"
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

        val SHORT_DAYS = listOf("ma", "di", "wo", "do", "vr", "za", "zo")

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
            RepeatUnit.NONE -> from
            RepeatUnit.DAY -> from.plusDays(every)
            RepeatUnit.WEEK -> if (repeat.weekdays == 0) from.plusWeeks(every) else nextWeekday(repeat, from)
            RepeatUnit.MONTH -> from.plusMonths(every)
            RepeatUnit.YEAR -> from.plusYears(every)
        }
    }

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
