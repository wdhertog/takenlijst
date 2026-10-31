package nl.wdhertog.takenlijst.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.wdhertog.takenlijst.MainActivity
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.TakenApp
import nl.wdhertog.takenlijst.data.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Er staat steeds één wekker: op het eerstvolgende herinneringsmoment. Als hij afgaat, krijgen alle
 * taken waarvan het moment sinds de vorige keer verstreken is een melding, en wordt de volgende gezet.
 * Inexacte wekkers volstaan (een paar minuten later is prima) en vragen geen extra toestemming.
 */
object ReminderScheduler {
    const val CHANNEL = "herinneringen"
    private const val PREFS = "herinneringen"
    private const val LAST_CHECK = "laatst_gecontroleerd"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Herinneringen voor taken", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun rescheduleAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { reschedule(app) }
    }

    /** Herinneringsmomenten van een taak; bij meerdere keren per dag één per keer die nog moet. */
    private fun triggerTimes(task: Task): List<Long> {
        val due = task.nextDue ?: return emptyList()
        task.reminderMinutes ?: return emptyList()
        val minutes = if (task.timesPerDay > 1) task.times.drop(task.slotsDone) else listOfNotNull(task.reminderMinutes)
        return minutes.map { due.atStartOfDay().plusMinutes(it.toLong()).millis() }
    }

    private fun LocalDateTime.millis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    suspend fun reschedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!prefs.contains(LAST_CHECK)) prefs.edit().putLong(LAST_CHECK, now).apply()

        val tasks = TakenApp.from(context).repository.let { repo -> repo.allTasksOnce() }
        val next = tasks.flatMap(::triggerTimes).filter { it > now }.minOrNull()

        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (next == null) alarm.cancel(pending)
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    }

    /** Meldingen tonen voor alles wat sinds de vorige controle aan de beurt kwam. */
    suspend fun fire(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val since = prefs.getLong(LAST_CHECK, now)
        val repo = TakenApp.from(context).repository
        val tasks = repo.allTasksOnce()
        val groups = repo.groupsOnce().associateBy { it.id }
        tasks.filter { t -> triggerTimes(t).any { it in (since + 1)..now } }
            .forEach { notify(context, it, groups[it.groupId]?.name ?: "") }
        prefs.edit().putLong(LAST_CHECK, now).apply()
        reschedule(context)
    }

    private fun notify(context: Context, task: Task, groupName: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context, task.id.toInt(), Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val done = PendingIntent.getBroadcast(
            context, task.id.toInt(),
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_DONE).putExtra(ReminderReceiver.EXTRA_TASK, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val tomorrow = PendingIntent.getBroadcast(
            context, task.id.toInt(),
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_POSTPONE).putExtra(ReminderReceiver.EXTRA_TASK, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF2F6B4F.toInt())
            .setContentTitle(if (task.timesPerDay > 1) "${task.title} (${task.slotsDone + 1} van ${task.timesPerDay})" else task.title)
            .setContentText(listOf(groupName, task.describeRepeat()).filter { it.isNotEmpty() }.joinToString(" · "))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Gedaan", done)
            .addAction(0, "Morgen", tomorrow)
            .build()
        NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE = "nl.wdhertog.takenlijst.FIRE"
        const val ACTION_DONE = "nl.wdhertog.takenlijst.DONE"
        const val ACTION_POSTPONE = "nl.wdhertog.takenlijst.POSTPONE"
        const val EXTRA_TASK = "taak"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> ReminderScheduler.fire(app)
                    ACTION_DONE -> {
                        val id = intent.getLongExtra(EXTRA_TASK, -1)
                        if (id > 0) {
                            TakenApp.from(app).repository.complete(id)
                            NotificationManagerCompat.from(app).cancel(id.toInt())
                        }
                    }
                    ACTION_POSTPONE -> {
                        val id = intent.getLongExtra(EXTRA_TASK, -1)
                        if (id > 0) {
                            TakenApp.from(app).repository.schedule(id, LocalDate.now().plusDays(1))
                            NotificationManagerCompat.from(app).cancel(id.toInt())
                        }
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.rescheduleAsync(context)
    }
}
