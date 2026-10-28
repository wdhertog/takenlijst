package nl.wdhertog.takenlijst.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskIconTest {
    @Test fun guessesFromTitle() {
        assertEquals("hond", TaskIcon.guess("Honden uitlaten")?.key)
        assertEquals("oven", TaskIcon.guess("Oven schoonmaken")?.key)
        assertEquals("wc", TaskIcon.guess("WC schoonmaken")?.key)
        assertEquals("kip", TaskIcon.guess("Kippenvoer bijvullen")?.key)
        assertEquals("snoeien", TaskIcon.guess("Heg snoeien")?.key)
        assertEquals("schoonmaak", TaskIcon.guess("Voorjaarsschoonmaak")?.key)
        assertEquals("was", TaskIcon.guess("Was doen")?.key)
    }

    @Test fun keysAreUnique() = assertEquals(TaskIcon.ALL.size, TaskIcon.ALL.map { it.key }.toSet().size)

    @Test fun searchFindsByKeywordAndCategory() {
        assertTrue(TaskIcon.search("hond").any { it.key == "hond" })
        assertTrue(TaskIcon.search("tuin").size > 5)
    }
}
