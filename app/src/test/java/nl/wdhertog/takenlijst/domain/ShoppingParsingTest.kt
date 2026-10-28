package nl.wdhertog.takenlijst.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingParsingTest {
    @Test fun bulletAndParenthesisQuantity() =
        assertEquals(ParsedItem("Eieren", "12"), ListParser.parseLine("- eieren (12)"))

    @Test fun leadingCount() =
        assertEquals(ParsedItem("Melk", "2×"), ListParser.parseLine("2x melk"))

    @Test fun leadingAmountWithUnit() =
        assertEquals(ParsedItem("Hondenbrokken", "10 kg"), ListParser.parseLine("10 kg hondenbrokken"))

    @Test fun trailingAmountWithUnit() =
        assertEquals(ParsedItem("Spinazie", "300 g"), ListParser.parseLine("• Spinazie 300 g"))

    @Test fun abbreviationKeepsWord() =
        assertEquals(ParsedItem("Hagelsl", ""), ListParser.parseLine("- hagelsl."))

    @Test fun headingAndNoiseIgnored() {
        assertNull(ListParser.parseLine("Boodschappen"))
        assertNull(ListParser.parseLine("- ."))
        assertNull(ListParser.parseLine(""))
    }

    @Test fun aisles() {
        assertEquals(Aisle.PETS, Aisle.guess("Kippenvoer"))
        assertEquals(Aisle.PETS, Aisle.guess("Legkorrel kippen"))
        assertEquals(Aisle.MEAT_FISH, Aisle.guess("Kipfilet"))
        assertEquals(Aisle.CHEESE, Aisle.guess("Pindakaas"))
        assertEquals(Aisle.PANTRY, Aisle.guess("Tomatenpuree"))
        assertEquals(Aisle.FRUIT_VEG, Aisle.guess("Uien"))
        assertEquals(Aisle.DAIRY, Aisle.guess("Melk"))
        assertEquals(Aisle.HOUSEHOLD, Aisle.guess("WC-papier"))
        assertEquals(Aisle.OTHER, Aisle.guess("Cadeautje"))
    }
}
