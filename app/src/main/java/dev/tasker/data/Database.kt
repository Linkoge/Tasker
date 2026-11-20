package dev.tasker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.tasker.domain.Category
import dev.tasker.domain.Task
import dev.tasker.domain.TaskStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val notes: String,
    val priority: Int,
    val completed: Boolean,
    val createdAt: Long,
    val completedAt: Long?,
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM categories ORDER BY position, id")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position, id")
    suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Insert suspend fun insertCategory(category: CategoryEntity): Long
    @Query("UPDATE categories SET name = :name WHERE id = :id") suspend fun renameCategory(id: Long, name: String)
    @Query("UPDATE categories SET position = :position WHERE id = :id") suspend fun setPosition(id: Long, position: Int)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: Long)

    @Insert suspend fun insertTask(task: TaskEntity): Long
    @Update suspend fun updateTask(task: TaskEntity)
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun task(id: Long): TaskEntity?
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)
}

@Database(entities = [CategoryEntity::class, TaskEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasker.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Default categories on first launch.
                        listOf("Daily", "Weekly", "Monthly").forEachIndexed { index, name ->
                            db.execSQL("INSERT INTO categories (name, position) VALUES (?, ?)", arrayOf<Any>(name, index))
                        }
                    }
                })
                .build()
    }
}

private fun CategoryEntity.toDomain() = Category(id, name, position)
private fun TaskEntity.toDomain() = Task(id, categoryId, title, notes, priority, completed, createdAt, completedAt)
private fun Task.toEntity() = TaskEntity(id, categoryId, title, notes, priority, completed, createdAt, completedAt)

class RoomTaskStore(private val db: AppDatabase) : TaskStore {
    private val dao = db.dao()

    override fun observeCategories(): Flow<List<Category>> = dao.observeCategories().map { l -> l.map { it.toDomain() } }
    override fun observeTasks(): Flow<List<Task>> = dao.observeTasks().map { l -> l.map { it.toDomain() } }
    override suspend fun categories(): List<Category> = dao.categories().map { it.toDomain() }
    override suspend fun insertCategory(name: String, position: Int): Long = dao.insertCategory(CategoryEntity(name = name, position = position))
    override suspend fun renameCategory(id: Long, name: String) = dao.renameCategory(id, name)
    override suspend fun updatePositions(categories: List<Category>) {
        categories.forEach { dao.setPosition(it.id, it.position) }
    }
    override suspend fun deleteCategory(id: Long) = dao.deleteCategory(id)
    override suspend fun insertTask(task: Task): Long = dao.insertTask(task.toEntity())
    override suspend fun updateTask(task: Task) = dao.updateTask(task.toEntity())
    override suspend fun task(id: Long): Task? = dao.task(id)?.toDomain()
    override suspend fun deleteTask(id: Long) = dao.deleteTask(id)
}
