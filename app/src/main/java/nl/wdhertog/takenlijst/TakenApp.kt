package nl.wdhertog.takenlijst

import android.app.Application
import android.content.Context
import nl.wdhertog.takenlijst.data.AppDatabase
import nl.wdhertog.takenlijst.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.wdhertog.takenlijst.models.ModelManager
import nl.wdhertog.takenlijst.ocr.OcrEngine
import nl.wdhertog.takenlijst.reminders.ReminderScheduler
import nl.wdhertog.takenlijst.voice.SpeechEngine

class TakenApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { Repository(database) { ReminderScheduler.rescheduleAsync(this) } }
    val ocr by lazy { OcrEngine(this) }
    val speech by lazy { SpeechEngine(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        ReminderScheduler.rescheduleAsync(this)
        CoroutineScope(Dispatchers.IO).launch { ModelManager.cleanUp(this@TakenApp) }
    }

    companion object {
        fun from(context: Context) = context.applicationContext as TakenApp
    }
}
