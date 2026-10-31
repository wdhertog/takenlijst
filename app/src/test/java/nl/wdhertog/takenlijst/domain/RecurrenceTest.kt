package nl.wdhertog.takenlijst.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceTest {
    private val wed = LocalDate.of(2026, 10, 28) // woensdag

    @Test fun dailyOnTime() =
        assertEquals(wed.plusDays(1), Recurrence.next(Repeat.DAILY, wed, wed, false))

    @Test fun dailyLateSkipsToTomorrow() =
        assertEquals(wed.plusDays(1), Recurrence.next(Repeat.DAILY, wed.minusDays(4), wed, false))

    @Test fun monthlyLateKeepsDayOfMonth() =
        assertEquals(LocalDate.of(2026, 11, 26), Recurrence.next(Repeat.MONTHLY, LocalDate.of(2026, 10, 26), wed, false))

    @Test fun monthlyFromCompletionCountsFromToday() =
        assertEquals(LocalDate.of(2026, 11, 28), Recurrence.next(Repeat.MONTHLY, LocalDate.of(2026, 10, 26), wed, true))

    @Test fun earlyCompletionMovesPastPlannedDate() =
        assertEquals(LocalDate.of(2027, 2, 2), Recurrence.next(Repeat.QUARTERLY, LocalDate.of(2026, 11, 2), wed, false))

    @Test fun onDemandHasNoNext() =
        assertNull(Recurrence.next(Repeat.ON_DEMAND, wed, wed, false))

    @Test fun weeklyOnChosenDays() {
        val monThu = Repeat(RepeatUnit.WEEK, 1, Repeat.bit(DayOfWeek.MONDAY) or Repeat.bit(DayOfWeek.THURSDAY))
        assertEquals(LocalDate.of(2026, 10, 29), Recurrence.next(monThu, wed, wed, false))
        assertEquals(LocalDate.of(2026, 11, 2), Recurrence.next(monThu, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 29), false))
    }

    @Test fun everyTwoWeeksOnSaturday() {
        val sat = LocalDate.of(2026, 10, 31)
        val r = Repeat(RepeatUnit.WEEK, 2, Repeat.bit(DayOfWeek.SATURDAY))
        assertEquals(LocalDate.of(2026, 11, 14), Recurrence.next(r, sat, sat, false))
    }

    @Test fun firstDueSnapsToWeekday() {
        val sat = Repeat(RepeatUnit.WEEK, 1, Repeat.bit(DayOfWeek.SATURDAY))
        assertEquals(LocalDate.of(2026, 10, 31), Recurrence.firstDue(sat, wed))
        assertNull(Recurrence.firstDue(Repeat.ON_DEMAND, wed))
    }

    private val firstSat = Repeat(RepeatUnit.MONTH, 1, Repeat.bit(DayOfWeek.SATURDAY), monthWeek = 1)

    @Test fun firstSaturdayOfMonth() {
        // 7 nov 2026 is de eerste zaterdag van november, 5 dec die van december.
        assertEquals(LocalDate.of(2026, 11, 7), Recurrence.firstDue(firstSat, wed))
        assertEquals(LocalDate.of(2026, 12, 5), Recurrence.next(firstSat, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 7), false))
    }

    @Test fun firstSaturdayAfterPostponing() =
        // Uitgesteld naar maandag 9 nov: de volgende keer is gewoon weer de eerste zaterdag van december.
        assertEquals(LocalDate.of(2026, 12, 5), Recurrence.next(firstSat, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9), false))

    @Test fun lastFridayEveryQuarter() {
        val r = Repeat(RepeatUnit.MONTH, 3, Repeat.bit(DayOfWeek.FRIDAY), monthWeek = -1)
        assertEquals(LocalDate.of(2026, 10, 30), Recurrence.firstDue(r, wed))
        assertEquals(LocalDate.of(2027, 1, 29), Recurrence.next(r, LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 30), false))
    }

    @Test fun monthWeekOfDate() {
        assertEquals(1, Repeat.monthWeekOf(LocalDate.of(2026, 10, 3)))
        assertEquals(4, Repeat.monthWeekOf(LocalDate.of(2026, 10, 24)))
        assertEquals(-1, Repeat.monthWeekOf(LocalDate.of(2026, 10, 31)))
    }

    @Test fun descriptions() {
        assertEquals("Maandelijks · eerste zaterdag", firstSat.describe())
        assertEquals("Per kwartaal", Repeat.QUARTERLY.describe())
        assertEquals("Wekelijks · ma, do", Repeat(RepeatUnit.WEEK, 1, 1 or 8).describe())
        assertEquals("Elke 2 weken", Repeat(RepeatUnit.WEEK, 2).describe())
    }
}
