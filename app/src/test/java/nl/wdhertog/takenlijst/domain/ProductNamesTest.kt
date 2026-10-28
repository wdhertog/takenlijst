package nl.wdhertog.takenlijst.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductNamesTest {
    @Test fun preciesMetMerkEnInhoud() {
        assertEquals("AH Halfvolle melk 1 L", ProductNames.specific("Halfvolle melk", "AH, Albert Heijn", "1 L"))
    }

    @Test fun merkEnInhoudNietDubbel() {
        assertEquals("Calvé Pindakaas 350 g", ProductNames.specific("Calvé Pindakaas 350 g", "Calvé", "350g"))
    }

    @Test fun merkAlsLosWoord() {
        // "ah" zit in "Jahoo", maar het merk staat er niet als los woord.
        assertEquals("AH Jahoo", ProductNames.specific("Jahoo", "AH", ""))
    }

    @Test fun ruimIsDeProductsoort() {
        assertEquals("Melk", ProductNames.broad("Halfvolle melk", "AH"))
        assertEquals("Tijgerbrood", ProductNames.broad("AH Volkoren tijgerbrood heel", "AH"))
        assertEquals("Pindakaas", ProductNames.broad("Calvé Pindakaas", "Calvé"))
        assertEquals("Hondenvoer", ProductNames.broad("Pedigree hondenvoer adult 3 kg", "Pedigree"))
    }

    @Test fun ruimZonderBekendWoordGebruiktAlgemeneNaam() {
        assertEquals("Hazelnootpasta", ProductNames.broad("Nutella 400 g", "Ferrero", "Hazelnootpasta met cacao"))
        assertEquals("Vaatwastabletten", ProductNames.broad("Finish Quantum", "Finish", "Vaatwastabletten"))
    }

    @Test fun ruimZonderAlgemeneNaamLaatMerkEnInhoudWeg() {
        assertEquals("Zero sugar", ProductNames.broad("Coca-Cola Zero sugar 1,5 l", "Coca-Cola"))
    }
}
