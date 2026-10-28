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

/**
 * Een groep taken (standaard Huis, Tuin en Werk), door de gebruiker in te stellen tot [MAX] stuks.
 * [icon] is een sleutel uit TaskIcon, [color] een index in het kleurenpalet van het thema.
 */
@Entity(tableName = "groups")
data class TaskGroup(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: Int,
    val position: Int,
) {
    val label get() = name

    companion object {
        const val MAX = 5
        const val HOME = "HOME"
        const val GARDEN = "GARDEN"
        const val WORK = "WORK"

        /** De groepen waarmee de app begint; de id's zijn gelijk aan de oude vaste groepen. */
        val DEFAULTS = listOf(
            TaskGroup(HOME, "Huis", "huis", 0, 0),
            TaskGroup(GARDEN, "Tuin", "tuin", 1, 1),
            TaskGroup(WORK, "Werk", "werk", 2, 2),
        )

        /** Voor een taak waarvan de groep (nog) niet geladen of verdwenen is. */
        val FALLBACK = TaskGroup("", "Overig", "", 7, 99)
    }
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Id van de [TaskGroup]; de kolom heet nog "group" uit de tijd van de vaste groepen. */
    @ColumnInfo(name = "group") val groupId: String = TaskGroup.HOME,
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

/**
 * Een eerder gescande barcode, met de namen die de gebruiker koos. Zo hoeft dezelfde barcode
 * de volgende keer niet opnieuw opgezocht te worden.
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val barcode: String,
    /** Precieze naam, bijv. "AH Halfvolle melk 1 L". */
    val specific: String,
    /** Ruime naam, bijv. "Melk". */
    val broad: String,
    /** Of de ruime naam op het lijstje komt. */
    val useBroad: Boolean,
    val aisle: Aisle = Aisle.OTHER,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** De naam die op het lijstje komt; valt terug op de andere als de gekozen leeg is. */
    val chosen get() = (if (useBroad) broad.ifBlank { specific } else specific.ifBlank { broad }).trim()
}

/** Afgevinkte taak van vandaag, voor de doorgestreepte regels op het Vandaag-scherm. */
data class DoneToday(
    val completionId: Long,
    val taskId: Long,
    val title: String,
    val groupId: String,
    val repeatUnit: RepeatUnit,
    val repeatEvery: Int,
    val weekdays: Int,
    val icon: String,
) {
    val repeat get() = Repeat(repeatUnit, repeatEvery, weekdays)
}

class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun unitToString(u: RepeatUnit): String = u.name
    @TypeConverter fun stringToUnit(s: String): RepeatUnit = RepeatUnit.valueOf(s)
    @TypeConverter fun aisleToString(a: Aisle): String = a.name
    @TypeConverter fun stringToAisle(s: String): Aisle = runCatching { Aisle.valueOf(s) }.getOrDefault(Aisle.OTHER)
}
