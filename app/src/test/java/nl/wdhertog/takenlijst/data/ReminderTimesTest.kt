package nl.wdhertog.takenlijst.data

import nl.wdhertog.takenlijst.domain.RepeatUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReminderTimesTest {
    private val due = LocalDate.of(2026, 11, 10)
    private fun at(t: LocalDateTime) = t.toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun Task.times() = reminderTimes(ZoneOffset.UTC)

    @Test fun onTheDayItself() =
        assertEquals(listOf(at(due.atTime(9, 0))), Task(title = "Was", nextDue = due, reminderMinutes = 9 * 60).times())

    @Test fun dayBefore() =
        assertEquals(
            listOf(at(due.minusDays(1).atTime(19, 0))),
            Task(title = "APK", nextDue = due, reminderMinutes = 19 * 60, reminderDaysBefore = 1).times(),
        )

    @Test fun laterReplacesTheReminder() {
        val later = at(due.atTime(20, 0))
        assertEquals(listOf(later), Task(title = "Medicijn", nextDue = due, reminderMinutes = 9 * 60, snoozedUntil = later).times())
    }

    @Test fun laterOnlyMovesTheNextSlot() {
        val later = at(due.atTime(8, 30))
        val dogs = Task(
            title = "Honden uitlaten", repeatUnit = RepeatUnit.DAY, nextDue = due, reminderMinutes = 7 * 60 + 30,
            timesPerDay = 3, slotTimes = "450,780,1260", snoozedUntil = later,
        )
        assertEquals(listOf(later, at(due.atTime(13, 0)), at(due.atTime(21, 0))), dogs.times())
    }

    @Test fun noReminderNoTimes() =
        assertEquals(emptyList<Long>(), Task(title = "Stofzuigen", nextDue = due).times())
}
