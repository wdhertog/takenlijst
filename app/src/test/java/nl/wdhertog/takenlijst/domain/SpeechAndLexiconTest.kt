package nl.wdhertog.takenlijst.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechAndLexiconTest {
    @Test fun spokenListIsSplit() = assertEquals(
        listOf(ParsedItem("Melk"), ParsedItem("Yoghurt", "2 pakken"), ParsedItem("Hondenbrokken", "10 kilo"), ParsedItem("Appels")),
        ListParser.parseSpoken("Melk, twee pakken yoghurt, tien kilo hondenbrokken en appels."),
    )

    @Test fun oneOnlyBecomesDigitBeforeUnit() {
        assertEquals("1 zak uien", ListParser.digitsForNumberWords("een zak uien"))
        assertEquals("een lekker brood", ListParser.digitsForNumberWords("een lekker brood"))
    }

    private val lexicon = Lexicon(listOf("eieren", "appels", "kippenvoer", "pakken", "bier", "hagelslag", "volkoren brood"))

    @Test fun englishGuessesAreCorrected() {
        assertEquals(Lexicon.Result("eieren", true), lexicon.correct("eleven"))
        assertEquals(Lexicon.Result("appels", true), lexicon.correct("appeals"))
        assertEquals(Lexicon.Result("kippenvoer", true), lexicon.correct("kiypenvoer"))
        assertEquals("2 pakken", lexicon.correct("2 pakkken").text)
    }

    @Test fun knownAndShortWordsStay() {
        assertFalse(lexicon.correct("Hagelslag").changed)
        assertFalse(lexicon.correct("brie").changed)
        assertTrue(lexicon.correct("volkoren brod").changed)
    }
}
