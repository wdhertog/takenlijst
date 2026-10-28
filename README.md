# Takenlijst

Een slim takenlijstje: huishoudhulp voor Android met terugkerende taken voor huis, tuin en werk, plus
een boodschappenlijst die je in de winkel afstreept. Alles blijft op je telefoon — de app heeft geen
internettoegang en gebruikt geen Google-diensten, dus hij werkt ook op /e/OS en andere ontgoogelde
Android-versies.

## Wat het kan

- **Vandaag**: wat te laat is, wat vandaag moet en wat er binnenkort aankomt; filter op Huis, Tuin of Werk.
  Afvinken kan ongedaan gemaakt worden.
- **Herhaling**: dagelijks, wekelijks (eventueel op vaste dagen), maandelijks, per kwartaal, jaarlijks,
  op afroep of zelf in te stellen ("elke 2 weken"). Met *Plannen vanaf afvinken* telt de volgende keer
  vanaf het moment dat je de taak echt deed in plaats van vanaf de geplande datum.
- **Herinneringen**: een melding op de dag zelf, met een knop *Gedaan*.
- **Boodschappen**: producten worden automatisch per afdeling gesorteerd (aanpasbaar door lang te drukken).
  *Winkelmodus* houdt het scherm aan en maakt de regels groter. *Klaar* haalt afgevinkte producten weg en
  vinkt de gekoppelde boodschappentaak af.
- **Lijstje van foto**: tekstherkenning met [Tesseract](https://github.com/tesseract-ocr/tesseract)
  op het toestel. Werkt goed op gedrukte tekst en netjes handschrift; twijfelgevallen worden oranje
  gemarkeerd zodat je ze kunt verbeteren.

## Bouwen

GitHub Actions bouwt bij elke push naar `main` een APK (zie *Actions*). Met de secrets
`KEYSTORE_BASE64` en `KEYSTORE_PASSWORD` (sleutelalias `takenlijst`) wordt hij ondertekend en als
release gepubliceerd, zodat updates over elkaar heen installeren.

Lokaal: JDK 17+, Android SDK 35 en Gradle 8.10:

```bash
gradle testDebugUnitTest assembleDebug
```

## Licenties van gebruikte onderdelen

- Tesseract en [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android) — Apache 2.0
- Nederlands taalmodel uit [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast) — Apache 2.0
- Lettertype [Figtree](https://github.com/erikdkennedy/figtree) — SIL Open Font License 1.1
