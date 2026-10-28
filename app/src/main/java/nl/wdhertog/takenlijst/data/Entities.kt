package nl.wdhertog.takenlijst.data

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
) {
    val repeat get() = Repeat(repeatUnit, repeatEvery, weekdays)
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
