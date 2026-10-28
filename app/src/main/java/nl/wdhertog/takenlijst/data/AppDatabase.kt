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
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        """SELECT c.id AS completionId, t.id AS taskId, t.title, t."group" AS groupId, t.repeatUnit, t.repeatEvery, t.weekdays
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

    @Query("UPDATE tasks SET \"group\" = :to WHERE \"group\" = :from")
    suspend fun moveGroup(from: String, to: String)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY position, name")
    fun observeAll(): Flow<List<TaskGroup>>

    @Query("SELECT * FROM groups ORDER BY position, name")
    suspend fun all(): List<TaskGroup>

    @Upsert suspend fun upsert(group: TaskGroup)
    @Upsert suspend fun upsertAll(groups: List<TaskGroup>)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: String)
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

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun get(barcode: String): Product?

    @Upsert suspend fun upsert(product: Product)

    @Query("SELECT COUNT(*) FROM products")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Database(entities = [Task::class, Completion::class, ShoppingItem::class, TaskGroup::class, Product::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun shopping(): ShoppingDao
    abstract fun groups(): GroupDao
    abstract fun products(): ProductDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "takenlijst.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) = insertDefaultGroups(db)
                })
                .build()

        private fun insertDefaultGroups(db: SupportSQLiteDatabase) {
            TaskGroup.DEFAULTS.forEach { g ->
                db.execSQL(
                    "INSERT OR IGNORE INTO groups (id, name, icon, color, position) VALUES (?, ?, ?, ?, ?)",
                    arrayOf<Any>(g.id, g.name, g.icon, g.color, g.position),
                )
            }
        }

        /** Versie 2: taken die meerdere keren per dag moeten gebeuren. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN timesPerDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN slotTimes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN slotsDone INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE completions ADD COLUMN previousSlots INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Versie 4: instelbare groepen in plaats van vast Huis, Tuin en Werk. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, " +
                        "`color` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                insertDefaultGroups(db)
            }
        }

        /** Versie 5: onthouden gescande barcodes. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `products` (`barcode` TEXT NOT NULL, `specific` TEXT NOT NULL, `broad` TEXT NOT NULL, " +
                        "`useBroad` INTEGER NOT NULL, `aisle` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`barcode`))"
                )
            }
        }

        /** Versie 3: zelfgekozen icoon per taak. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
