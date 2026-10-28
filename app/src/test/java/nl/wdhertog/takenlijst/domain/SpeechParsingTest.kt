package nl.wdhertog.takenlijst.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechParsingTest {
    @Test fun spokenListIsSplit() = assertEquals(
        listOf(ParsedItem("Melk"), ParsedItem("Yoghurt", "2 pakken"), ParsedItem("Hondenbrokken", "10 kilo"), ParsedItem("Appels")),
        ListParser.parseSpoken("Melk, twee pakken yoghurt, tien kilo hondenbrokken en appels."),
    )

    @Test fun oneOnlyBecomesDigitBeforeUnit() {
        assertEquals("1 zak uien", ListParser.digitsForNumberWords("een zak uien"))
        assertEquals("een lekker brood", ListParser.digitsForNumberWords("een lekker brood"))
    }
}
