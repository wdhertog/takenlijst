# Takenlijst

Een slim takenlijstje: huishoudhulp voor Android met terugkerende taken voor huis, tuin en werk, plus
een boodschappenlijst die je in de winkel afstreept. Alles blijft op je telefoon en de app gebruikt geen
Google-diensten, dus hij werkt ook op /e/OS en andere ontgoogelde Android-versies. Internet wordt alleen
gebruikt om de spraak- en handschriftmodellen eenmalig te downloaden.

## Wat het kan

- **Vandaag**: wat te laat is, wat vandaag moet en wat er binnenkort aankomt; filter op Huis, Tuin of Werk.
  Afvinken kan ongedaan gemaakt worden.
- **Herhaling**: dagelijks, wekelijks (eventueel op vaste dagen), maandelijks, per kwartaal, jaarlijks,
  op afroep of zelf in te stellen ("elke 2 weken"). Met *Plannen vanaf afvinken* telt de volgende keer
  vanaf het moment dat je de taak echt deed in plaats van vanaf de geplande datum.
- **Meerdere keren per dag**: bijvoorbeeld honden uitlaten om 07:30, 13:00 en 21:00; de taak staat op
  Vandaag tot alle keren gedaan zijn.
- **Herinneringen**: een melding op de dag zelf (bij meerdere keren per dag: per keer), met een knop *Gedaan*.
- **Boodschappen**: producten worden automatisch per afdeling gesorteerd (aanpasbaar door lang te drukken).
  *Winkelmodus* houdt het scherm aan en maakt de regels groter. *Klaar* haalt afgevinkte producten weg en
  vinkt de gekoppelde boodschappentaak af.
- **Inspreken**: zeg "melk, twee pakken yoghurt en appels" en [whisper.cpp](https://github.com/ggml-org/whisper.cpp)
  schrijft het op het toestel uit (Whisper small, ~190 MB eenmalige download).
- **Lijstje van foto**: handschrift wordt per regel gelezen met [TrOCR](https://huggingface.co/microsoft/trocr-small-handwritten)
  via ONNX Runtime (~67 MB eenmalige download); bekende producten worden verbeterd ("eleven" → "eieren").
  Gedrukte tekst gaat via [Tesseract](https://github.com/tesseract-ocr/tesseract). Twijfelgevallen
  worden oranje gemarkeerd zodat je ze kunt verbeteren.

## Bouwen

GitHub Actions bouwt bij elke push naar `main` een APK (zie *Actions*). Met de secrets
`KEYSTORE_BASE64` en `KEYSTORE_PASSWORD` (sleutelalias `takenlijst`) wordt hij ondertekend en als
release gepubliceerd, zodat updates over elkaar heen installeren.

Lokaal: JDK 17+, Android SDK 35 met NDK en CMake, Gradle 8.10, en whisper.cpp in `third_party/`:

```bash
git clone --depth 1 --branch v1.7.6 https://github.com/ggml-org/whisper.cpp third_party/whisper.cpp
gradle testDebugUnitTest assembleDebug
```

## Licenties van gebruikte onderdelen

- Tesseract en [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android) — Apache 2.0
- Nederlands taalmodel uit [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast) — Apache 2.0
- whisper.cpp (MIT) met het Whisper-model van OpenAI (MIT)
- TrOCR (Microsoft, MIT) en ONNX Runtime (MIT)
- Lettertype [Figtree](https://github.com/erikdkennedy/figtree) — SIL Open Font License 1.1
