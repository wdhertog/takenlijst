package nl.wdhertog.takenlijst.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun all(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): Task?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    @Insert suspend fun insert(task: Task): Long
    @Insert suspend fun insertAll(tasks: List<Task>)
    @Update suspend fun update(task: Task)
    @Delete suspend fun delete(task: Task)

    @Query(
        """SELECT c.id AS completionId, t.id AS taskId, t.title, t."group", t.repeatUnit, t.repeatEvery, t.weekdays
           FROM completions c JOIN tasks t ON t.id = c.taskId
           WHERE c.doneOn = :day ORDER BY c.id"""
    )
    fun observeDoneOn(day: LocalDate): Flow<List<DoneToday>>

    @Insert suspend fun insertCompletion(c: Completion): Long

    @Query("SELECT * FROM completions WHERE id = :id")
    suspend fun completion(id: Long): Completion?

    @Query("DELETE FROM completions WHERE id = :id")
    suspend fun deleteCompletion(id: Long)

    @Query("SELECT MAX(doneOn) FROM completions WHERE taskId = :taskId")
    suspend fun lastDone(taskId: Long): LocalDate?
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY checked, addedAt")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Insert suspend fun insert(item: ShoppingItem): Long
    @Insert suspend fun insertAll(items: List<ShoppingItem>)
    @Update suspend fun update(item: ShoppingItem)
    @Delete suspend fun delete(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun deleteChecked()

    @Query("SELECT COUNT(*) FROM shopping_items WHERE checked = 0")
    fun observeOpenCount(): Flow<Int>
}

@Database(entities = [Task::class, Completion::class, ShoppingItem::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun shopping(): ShoppingDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "takenlijst.db").build()
    }
}
