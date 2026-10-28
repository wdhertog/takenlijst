package nl.wdhertog.takenlijst.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import nl.wdhertog.takenlijst.domain.Aisle
import nl.wdhertog.takenlijst.domain.Repeat
import nl.wdhertog.takenlijst.domain.RepeatUnit
import java.time.LocalDate

enum class TaskGroup(val label: String) {
    HOME("Huis"),
    GARDEN("Tuin"),
    WORK("Werk"),
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val group: TaskGroup = TaskGroup.HOME,
    val repeatUnit: RepeatUnit = RepeatUnit.WEEK,
    val repeatEvery: Int = 1,
    val weekdays: Int = 0,
    /** Null bij "op afroep" zolang de taak niet ingepland is. */
    val nextDue: LocalDate? = null,
    /** Minuten na middernacht, of null zonder herinnering. */
    val reminderMinutes: Int? = null,
    val fromCompletion: Boolean = false,
    val note: String = "",
    /** Opent de boodschappenlijst en wordt afgevinkt als je klaar bent met winkelen. */
    val linkShopping: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** Hoe vaak de taak op een dag moet gebeuren (honden uitlaten: 3). */
    @ColumnInfo(defaultValue = "1") val timesPerDay: Int = 1,
    /** Tijdstip per keer in minuten na middernacht, kommagescheiden; alleen bij [timesPerDay] > 1. */
    @ColumnInfo(defaultValue = "''") val slotTimes: String = "",
    /** Hoe vaak de taak op de geplande dag al gedaan is. */
    @ColumnInfo(defaultValue = "0") val slotsDone: Int = 0,
    /** Gekozen icoon (zie TaskIcon); leeg = automatisch op basis van de naam. */
    @ColumnInfo(defaultValue = "''") val icon: String = "",
) {
    val repeat get() = Repeat(repeatUnit, repeatEvery, weekdays)

    /** "Dagelijks", of "3× per dag" voor taken die vaker op een dag gebeuren. */
    fun describeRepeat(): String = when {
        timesPerDay <= 1 -> repeat.describe()
        repeatUnit == RepeatUnit.DAY && repeatEvery == 1 -> "${timesPerDay}× per dag"
        else -> "${repeat.describe()} · ${timesPerDay}× op de dag"
    }

    val times: List<Int> get() = slotTimes.split(',').mapNotNull { it.trim().toIntOrNull() }

    /** Tijdstip van de eerstvolgende keer die nog moet gebeuren, als dat bekend is. */
    val nextSlotTime: Int? get() = if (timesPerDay > 1) times.getOrNull(slotsDone) else reminderMinutes
}

@Entity(
    tableName = "completions",
    foreignKeys = [ForeignKey(entity = Task::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taskId"), Index("doneOn")],
)
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val doneOn: LocalDate,
    /** Geplande datum vóór het afvinken, zodat ongedaan maken hem kan terugzetten. */
    val previousDue: LocalDate?,
    /** Aantal keer dat de taak die dag al gedaan was, voor ongedaan maken. */
    @ColumnInfo(defaultValue = "0") val previousSlots: Int = 0,
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String = "",
    val aisle: Aisle = Aisle.OTHER,
    val checked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
)

/** Afgevinkte taak van vandaag, voor de doorgestreepte regels op het Vandaag-scherm. */
data class DoneToday(
    val completionId: Long,
    val taskId: Long,
    val title: String,
    val group: TaskGroup,
    val repeatUnit: RepeatUnit,
    val repeatEvery: Int,
    val weekdays: Int,
) {
    val repeat get() = Repeat(repeatUnit, repeatEvery, weekdays)
}

class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun groupToString(g: TaskGroup): String = g.name
    @TypeConverter fun stringToGroup(s: String): TaskGroup = TaskGroup.valueOf(s)
    @TypeConverter fun unitToString(u: RepeatUnit): String = u.name
    @TypeConverter fun stringToUnit(s: String): RepeatUnit = RepeatUnit.valueOf(s)
    @TypeConverter fun aisleToString(a: Aisle): String = a.name
    @TypeConverter fun stringToAisle(s: String): Aisle = runCatching { Aisle.valueOf(s) }.getOrDefault(Aisle.OTHER)
}
