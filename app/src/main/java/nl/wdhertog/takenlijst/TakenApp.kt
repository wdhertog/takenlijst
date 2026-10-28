package nl.wdhertog.takenlijst

import android.app.Application
import android.content.Context
import nl.wdhertog.takenlijst.data.AppDatabase
import nl.wdhertog.takenlijst.data.Repository
import nl.wdhertog.takenlijst.domain.Aisle
import nl.wdhertog.takenlijst.domain.Lexicon
import nl.wdhertog.takenlijst.ocr.OcrEngine
import nl.wdhertog.takenlijst.reminders.ReminderScheduler
import nl.wdhertog.takenlijst.voice.SpeechEngine

class TakenApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { Repository(database) { ReminderScheduler.rescheduleAsync(this) } }
    val ocr by lazy { OcrEngine(this) }
    val speech by lazy { SpeechEngine(this) }

    private val productWords by lazy {
        assets.open("producten.txt").bufferedReader().readLines() + Aisle.keywordList()
    }

    /** Woordenlijst voor het verbeteren van handschrift, aangevuld met wat er nu op je lijst staat. */
    fun lexicon(extra: List<String>) = Lexicon(productWords + extra)

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        ReminderScheduler.rescheduleAsync(this)
    }

    companion object {
        fun from(context: Context) = context.applicationContext as TakenApp
    }
}
