# Takenlijst

Een slim takenlijstje: huishoudhulp voor Android met terugkerende taken voor huis, tuin en werk, plus
een boodschappenlijst die je in de winkel afstreept. Alles blijft op je telefoon en de app gebruikt geen
Google-diensten, dus hij werkt ook op /e/OS en andere ontgoogelde Android-versies. Internet wordt alleen
gebruikt om het spraakmodel te downloaden en, als je dat aanzet, om barcodes op te zoeken. Zie de
[privacyverklaring](PRIVACY.md).

## Installeren

Download de nieuwste APK onder [Releases](https://github.com/wdhertog/takenlijst/releases/latest).
Releases met een versienummer (v1.2.0) zijn de stabiele versies; *Testversie*-builds verschijnen bij elke
wijziging.

## Wat het kan

- **Vandaag**: wat te laat is, wat vandaag moet en wat er binnenkort aankomt; filter per groep.
  Afvinken kan ongedaan gemaakt worden.
- **Uitstellen**: druk lang op een taak om hem naar morgen, zaterdag, volgende week of een zelfgekozen dag
  te schuiven. Taken op afroep plan je zo ook in op een andere dag dan vandaag. Vanuit een herinnering kan
  het met de knop *Morgen*.
- **Swipen**: veeg naar links of rechts om tussen Vandaag, Taken, Boodschappen en Meer te wisselen.
- **Groepen**: tot vijf eigen groepen (standaard Huis, Tuin en Werk) met naam, kleur en icoon.
- **Iconen**: kies per taak uit bijna honderd iconen, met zoekfunctie; anders raadt de app er een.
- **Herhaling**: dagelijks, wekelijks (eventueel op vaste dagen), maandelijks, per kwartaal, jaarlijks,
  op afroep of zelf in te stellen ("elke 2 weken"). Maandelijks kan ook op een vaste weekdag, zoals
  de eerste zaterdag of de laatste vrijdag van de maand. Met *Plannen vanaf afvinken* telt de volgende keer
  vanaf het moment dat je de taak echt deed in plaats van vanaf de geplande datum.
- **Meerdere keren per dag**: bijvoorbeeld honden uitlaten om 07:30, 13:00 en 21:00; de taak staat op
  Vandaag tot alle keren gedaan zijn.
- **Herinneringen**: een melding op de dag zelf (bij meerdere keren per dag: per keer), met een knop *Gedaan*.
- **Boodschappen**: producten worden automatisch per afdeling gesorteerd (aanpasbaar door lang te drukken).
  *Winkelmodus* houdt het scherm aan, zet het rechtop en maakt de regels groter. *Klaar* haalt afgevinkte producten weg en
  vinkt de gekoppelde boodschappentaak af.
- **Inspreken**: zeg "melk, twee pakken yoghurt en appels" en [whisper.cpp](https://github.com/ggml-org/whisper.cpp)
  schrijft het op het toestel uit (Whisper small, ~190 MB eenmalige download).
- **Lijstje van foto**: gedrukte of getypte lijstjes lezen met [Tesseract](https://github.com/tesseract-ocr/tesseract)
  op het toestel. Twijfelgevallen worden oranje gemarkeerd zodat je ze kunt verbeteren. Voor
  handgeschreven lijstjes werkt inspreken beter.
- **Barcode scannen**: met [ZXing](https://github.com/zxing/zxing). Kies tussen een ruime naam ("Melk") en
  een precieze ("AH Halfvolle melk 1 L"); de app onthoudt de barcode en je keuze. Opzoeken bij
  [Open Food Facts](https://world.openfoodfacts.org) gebeurt alleen als je dat aanzet.
- **Thema**: licht, donker of volgens de telefoon.

## Bouwen

Nodig: JDK 17+, Android SDK 35 met NDK en CMake. whisper.cpp zit als submodule in `third_party/`.

```bash
git clone --recurse-submodules https://github.com/wdhertog/takenlijst
cd takenlijst
./gradlew testDebugUnitTest assembleDebug
```

GitHub Actions bouwt bij elke push naar `main` een testversie (pre-release `build-N`). Een tag `vX.Y.Z`
levert een echte release met APK en AAB; de tag moet gelijk zijn aan `versionName` in
`app/build.gradle.kts`. Ondertekenen gebeurt met de secrets `KEYSTORE_BASE64` en `KEYSTORE_PASSWORD`
(sleutelalias `takenlijst`).

Een nieuwe versie uitbrengen:

1. Verhoog `versionCode` en `versionName` in `app/build.gradle.kts` (1.2.0 → 10200).
2. Zet de wijzigingen in `fastlane/metadata/android/nl-NL/changelogs/<versionCode>.txt` (en `en-US`).
3. `git tag v1.2.0 && git push --tags`

Winkelteksten en screenshots staan in `fastlane/metadata/android/`, waar F-Droid en IzzyOnDroid ze lezen.

## Licentie

Takenlijst is vrije software onder de [GNU General Public License v3.0](LICENSE).

## Licenties van gebruikte onderdelen

- Tesseract en [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android) — Apache 2.0
- Nederlands taalmodel uit [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast) — Apache 2.0
- whisper.cpp (MIT) met het Whisper-model van OpenAI (MIT)
- [ZXing](https://github.com/zxing/zxing) en [zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded) — Apache 2.0
- Productgegevens van [Open Food Facts](https://world.openfoodfacts.org) — Open Database License
- Iconen van [Lucide](https://lucide.dev) — ISC
- Lettertype [Figtree](https://github.com/erikdkennedy/figtree) — SIL Open Font License 1.1
