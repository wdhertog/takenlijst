package nl.wdhertog.takenlijst.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.wdhertog.takenlijst.TakenApp
import nl.wdhertog.takenlijst.data.DoneToday
import nl.wdhertog.takenlijst.data.ShoppingItem
import nl.wdhertog.takenlijst.data.Task
import nl.wdhertog.takenlijst.data.TaskGroup
import nl.wdhertog.takenlijst.domain.Aisle
import nl.wdhertog.takenlijst.domain.ListParser
import nl.wdhertog.takenlijst.domain.ParsedItem
import nl.wdhertog.takenlijst.voice.SAMPLE_RATE
import nl.wdhertog.takenlijst.voice.VoiceRecorder
import java.time.LocalDate

data class TodayState(
    val loaded: Boolean = false,
    val hasTasks: Boolean = false,
    val late: List<Task> = emptyList(),
    val due: List<Task> = emptyList(),
    val done: List<DoneToday> = emptyList(),
    val upcoming: List<Task> = emptyList(),
) {
    val total get() = late.size + due.size + done.size
}

data class ScanRow(val key: Int, val name: String, val quantity: String, val selected: Boolean, val uncertain: Boolean, val seen: String)

sealed interface ScanState {
    data object Idle : ScanState
    data object Recording : ScanState
    data class Working(val message: String) : ScanState
    data class Result(val rows: List<ScanRow>, val heard: String? = null) : ScanState
    data class Failed(val message: String) : ScanState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = TakenApp.from(application)
    private val repo = app.repository

    private val today = MutableStateFlow(LocalDate.now())
    /** Id van de groep waarop Vandaag gefilterd is, of null voor alles. */
    val groupFilter = MutableStateFlow<String?>(null)

    val groups: StateFlow<List<TaskGroup>> = repo.groups.stateIn(viewModelScope, SharingStarted.Eagerly, TaskGroup.DEFAULTS)

    fun saveGroup(group: TaskGroup) = viewModelScope.launch { repo.saveGroup(group) }
    fun deleteGroup(id: String, moveTo: String) = viewModelScope.launch {
        repo.deleteGroup(id, moveTo)
        if (groupFilter.value == id) groupFilter.value = null
    }
    fun moveGroup(id: String, delta: Int) = viewModelScope.launch { repo.moveGroup(id, delta) }

    /** Bij terugkeren naar de app kan het intussen een nieuwe dag zijn. */
    fun refreshDate() = today.update { LocalDate.now() }

    val tasks: StateFlow<List<Task>?> = repo.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayState: StateFlow<TodayState> = combine(
        repo.allTasks, today.flatMapLatest { repo.doneOn(it) }, today, groupFilter,
    ) { all, done, day, filter ->
        val visible = if (filter == null) all else all.filter { it.groupId == filter }
        val dated = visible.filter { it.nextDue != null }.sortedWith(compareBy({ it.nextDue }, { it.nextSlotTime ?: Int.MAX_VALUE }))
        val dueToday = dated.filter { it.nextDue == day }.map { it.id }.toSet()
        // Een taak van meerdere keren per dag staat pas bij "gedaan" als alle keren gedaan zijn, en dan één keer.
        val finished = (if (filter == null) done else done.filter { it.groupId == filter })
            .filter { it.taskId !in dueToday }
            .associateBy { it.taskId }.values.toList()
        TodayState(
            loaded = true,
            hasTasks = all.isNotEmpty(),
            late = dated.filter { it.nextDue!!.isBefore(day) },
            due = dated.filter { it.nextDue == day },
            done = finished,
            upcoming = dated.filter { it.nextDue!!.isAfter(day) && !it.nextDue.isAfter(day.plusDays(7)) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayState())

    val currentDate: StateFlow<LocalDate> = today.asStateFlow()

    val shopping: StateFlow<List<ShoppingItem>> = repo.shoppingItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val openShopping: StateFlow<Int> = repo.openShoppingCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val shopMode = MutableStateFlow(false)

    // --- Taken ---

    suspend fun complete(taskId: Long): Long? = repo.complete(taskId, today.value)
    fun undo(completionId: Long) = viewModelScope.launch { repo.undo(completionId) }
    fun schedule(taskId: Long, day: LocalDate = today.value) = viewModelScope.launch { repo.schedule(taskId, day) }
    suspend fun task(id: Long) = repo.task(id)
    suspend fun lastDone(id: Long) = repo.lastDone(id)
    fun save(task: Task) = viewModelScope.launch { repo.save(task) }
    fun delete(task: Task) = viewModelScope.launch { repo.delete(task) }
    fun addExamples() = viewModelScope.launch { repo.addExamples(today.value) }

    // --- Boodschappen ---

    fun addShopping(text: String) = viewModelScope.launch {
        ListParser.parseLine(text)?.let { repo.addShopping(listOf(it)) }
    }

    fun toggle(item: ShoppingItem) = viewModelScope.launch { repo.updateShopping(item.copy(checked = !item.checked)) }
    fun updateShopping(item: ShoppingItem) = viewModelScope.launch { repo.updateShopping(item) }
    fun deleteShopping(item: ShoppingItem) = viewModelScope.launch { repo.deleteShopping(item) }
    suspend fun finishShopping(): Boolean = repo.finishShopping(today.value)

    // --- Foto uitlezen en inspreken ---

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan = _scan.asStateFlow()
    val recorder = VoiceRecorder()

    fun startScan(uri: Uri) = viewModelScope.launch {
        _scan.value = ScanState.Working("Tekst herkennen…")
        _scan.value = runCatching { app.ocr.recognize(uri) }.fold(
            onSuccess = { lines ->
                val rows = lines.mapNotNull { line ->
                    ListParser.parseLine(line.text)?.let { item ->
                        // Lage zekerheid of een afgebroken woord ("hagelsl.") laten controleren.
                        val doubtful = line.confidence < 70f || line.text.trim().endsWith(".")
                        ScanRow(0, item.name, item.quantity, selected = true, uncertain = doubtful, seen = line.text.trim())
                    }
                }.mapIndexed { i, r -> r.copy(key = i) }
                if (rows.isEmpty()) ScanState.Failed("Er is geen tekst gevonden. Probeer een scherpere foto met meer licht.")
                else ScanState.Result(rows)
            },
            onFailure = { ScanState.Failed(it.message ?: "Uitlezen is mislukt") },
        )
    }

    fun startRecording() = viewModelScope.launch {
        _scan.value = ScanState.Recording
        val samples = runCatching { recorder.record() }.getOrElse {
            _scan.value = ScanState.Failed(it.message ?: "Opnemen is mislukt")
            return@launch
        }
        if (samples.size < SAMPLE_RATE / 2) {
            _scan.value = ScanState.Idle
            return@launch
        }
        _scan.value = ScanState.Working("Uitschrijven…")
        _scan.value = runCatching { app.speech.transcribe(samples) }.fold(
            onSuccess = { text ->
                val rows = ListParser.parseSpoken(text).mapIndexed { i, item ->
                    ScanRow(i, item.name, item.quantity, selected = true, uncertain = false, seen = "")
                }
                if (rows.isEmpty()) ScanState.Failed("Ik heb geen producten verstaan. Probeer het nog eens, bijvoorbeeld: “melk, twee pakken yoghurt en appels”.")
                else ScanState.Result(rows, heard = text)
            },
            onFailure = { ScanState.Failed(it.message ?: "Uitschrijven is mislukt") },
        )
    }

    fun stopRecording() = recorder.stop()

    fun editScanRow(row: ScanRow) = _scan.update { s ->
        if (s is ScanState.Result) s.copy(rows = s.rows.map { if (it.key == row.key) row else it }) else s
    }

    fun commitScan() = viewModelScope.launch {
        val s = _scan.value as? ScanState.Result ?: return@launch
        repo.addShopping(s.rows.filter { it.selected && it.name.isNotBlank() }.map { ParsedItem(it.name.trim(), it.quantity.trim()) })
        _scan.value = ScanState.Idle
    }

    fun resetScan() { _scan.value = ScanState.Idle }
}

/** Groepeert producten per afdeling in winkelvolgorde; afgevinkte producten onderaan elke afdeling. */
fun List<ShoppingItem>.byAisle(): List<Pair<Aisle, List<ShoppingItem>>> =
    groupBy { it.aisle }.toSortedMap(compareBy { it.ordinal })
        .map { (aisle, items) -> aisle to items.sortedWith(compareBy({ it.checked }, { it.addedAt })) }

val TodayState.progress get() = if (total == 0) 0f else done.size / total.toFloat()
