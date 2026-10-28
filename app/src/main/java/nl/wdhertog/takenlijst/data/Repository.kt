package nl.wdhertog.takenlijst.data

import androidx.room.withTransaction
import nl.wdhertog.takenlijst.domain.Aisle
import nl.wdhertog.takenlijst.domain.ParsedItem
import nl.wdhertog.takenlijst.domain.Recurrence
import nl.wdhertog.takenlijst.domain.Repeat
import nl.wdhertog.takenlijst.domain.RepeatUnit
import java.time.DayOfWeek
import java.time.LocalDate

class Repository(
    private val db: AppDatabase,
    /** Wordt aangeroepen na elke wijziging die herinneringen beïnvloedt. */
    private val onTasksChanged: () -> Unit,
) {
    private val tasks = db.tasks()
    private val shopping = db.shopping()

    val allTasks = tasks.observeAll()
    fun doneOn(day: LocalDate) = tasks.observeDoneOn(day)
    val shoppingItems = shopping.observeAll()
    val openShoppingCount = shopping.observeOpenCount()

    suspend fun allTasksOnce() = tasks.all()
    suspend fun task(id: Long) = tasks.get(id)
    suspend fun lastDone(id: Long) = tasks.lastDone(id)

    suspend fun save(task: Task): Long {
        val id = if (task.id == 0L) tasks.insert(task) else task.id.also { tasks.update(task) }
        onTasksChanged()
        return id
    }

    suspend fun delete(task: Task) {
        tasks.delete(task)
        onTasksChanged()
    }

    suspend fun complete(taskId: Long, today: LocalDate = LocalDate.now()): Long? {
        val id = db.withTransaction {
            val task = tasks.get(taskId) ?: return@withTransaction null
            val id = tasks.insertCompletion(Completion(taskId = task.id, doneOn = today, previousDue = task.nextDue))
            tasks.update(task.copy(nextDue = Recurrence.next(task.repeat, task.nextDue, today, task.fromCompletion)))
            id
        }
        onTasksChanged()
        return id
    }

    suspend fun undo(completionId: Long) {
        db.withTransaction {
            val c = tasks.completion(completionId) ?: return@withTransaction
            tasks.get(c.taskId)?.let { tasks.update(it.copy(nextDue = c.previousDue)) }
            tasks.deleteCompletion(c.id)
        }
        onTasksChanged()
    }

    /** Taak "op afroep" (of een andere) op een dag zetten. */
    suspend fun schedule(taskId: Long, day: LocalDate) {
        tasks.get(taskId)?.let { tasks.update(it.copy(nextDue = day)) }
        onTasksChanged()
    }

    // --- Boodschappen ---

    suspend fun addShopping(items: List<ParsedItem>) {
        shopping.insertAll(items.map { ShoppingItem(name = it.name, quantity = it.quantity, aisle = Aisle.guess(it.name)) })
    }

    suspend fun updateShopping(item: ShoppingItem) = shopping.update(item)
    suspend fun deleteShopping(item: ShoppingItem) = shopping.delete(item)

    /** Afgevinkte producten weghalen en de gekoppelde boodschappentaak afvinken als die aan de beurt is. */
    suspend fun finishShopping(today: LocalDate = LocalDate.now()): Boolean {
        shopping.deleteChecked()
        val task = tasks.all()
            .filter { it.linkShopping && it.nextDue != null && !it.nextDue.isAfter(today) }
            .minByOrNull { it.nextDue!! } ?: return false
        complete(task.id, today)
        return true
    }

    // --- Voorbeelden ---

    suspend fun isEmpty() = tasks.count() == 0

    suspend fun addExamples(today: LocalDate = LocalDate.now()) {
        fun t(title: String, group: TaskGroup, repeat: Repeat, inDays: Long = 0, reminder: Int? = null, shoppingTask: Boolean = false, fromDone: Boolean = false) =
            Task(
                title = title, group = group,
                repeatUnit = repeat.unit, repeatEvery = repeat.every, weekdays = repeat.weekdays,
                nextDue = Recurrence.firstDue(repeat, today.plusDays(inDays)),
                reminderMinutes = reminder, linkShopping = shoppingTask, fromCompletion = fromDone,
            )
        val saturday = Repeat(RepeatUnit.WEEK, 1, Repeat.bit(DayOfWeek.SATURDAY))
        tasks.insertAll(
            listOf(
                t("Honden uitlaten", TaskGroup.HOME, Repeat.DAILY, reminder = 7 * 60 + 30),
                t("Koken", TaskGroup.HOME, Repeat.DAILY),
                t("Boodschappen doen", TaskGroup.HOME, saturday, shoppingTask = true),
                t("WC schoonmaken", TaskGroup.HOME, saturday),
                t("Was doen", TaskGroup.HOME, Repeat(RepeatUnit.WEEK, 1, Repeat.bit(DayOfWeek.MONDAY) or Repeat.bit(DayOfWeek.THURSDAY))),
                t("Stofzuigen", TaskGroup.HOME, Repeat.WEEKLY, inDays = 2),
                t("Ramen lappen", TaskGroup.HOME, Repeat.QUARTERLY, inDays = 20),
                t("Voorjaarsschoonmaak", TaskGroup.HOME, Repeat.YEARLY, inDays = daysUntilMarch(today)),
                t("Oven schoonmaken", TaskGroup.HOME, Repeat.ON_DEMAND),
                t("Kippenvoer bijvullen", TaskGroup.GARDEN, Repeat.MONTHLY, inDays = 3, fromDone = true),
                t("Heg snoeien", TaskGroup.GARDEN, Repeat.QUARTERLY, inDays = 5),
                t("Gras maaien", TaskGroup.GARDEN, Repeat(RepeatUnit.WEEK, 2), inDays = 4),
                t("Weekplanning maken", TaskGroup.WORK, Repeat(RepeatUnit.WEEK, 1, Repeat.bit(DayOfWeek.MONDAY))),
            )
        )
        onTasksChanged()
    }

    private fun daysUntilMarch(today: LocalDate): Long {
        var march = LocalDate.of(today.year, 3, 15)
        if (!march.isAfter(today)) march = march.plusYears(1)
        return march.toEpochDay() - today.toEpochDay()
    }
}
