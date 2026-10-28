package nl.wdhertog.takenlijst

import android.app.Application
import android.content.Context
import nl.wdhertog.takenlijst.data.AppDatabase
import nl.wdhertog.takenlijst.data.Repository
import nl.wdhertog.takenlijst.ocr.OcrEngine
import nl.wdhertog.takenlijst.reminders.ReminderScheduler

class TakenApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { Repository(database) { ReminderScheduler.rescheduleAsync(this) } }
    val ocr by lazy { OcrEngine(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        ReminderScheduler.rescheduleAsync(this)
    }

    companion object {
        fun from(context: Context) = context.applicationContext as TakenApp
    }
}
