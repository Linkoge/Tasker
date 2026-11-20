package dev.tasker.data

import android.content.Context
import androidx.room.*
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
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE,
    )],
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

data class CountRow(val categoryId: Long, val count: Int)

@Dao
interface TaskDao {
    @Query("SELECT * FROM categories ORDER BY position, id")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position, id")
    suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId")
    fun observeTasks(categoryId: Long): Flow<List<TaskEntity>>

    @Query("SELECT categoryId, COUNT(*) AS count FROM tasks WHERE completed = 0 GROUP BY categoryId")
    fun observeOpenCounts(): Flow<List<CountRow>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: Long): TaskEntity?

    @Insert suspend fun insertCategory(category: CategoryEntity): Long
    @Update suspend fun updateCategories(categories: List<CategoryEntity>)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: Long)
    @Insert suspend fun insertTask(task: TaskEntity): Long
    @Update suspend fun updateTask(task: TaskEntity)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)
}

@Database(entities = [CategoryEntity::class, TaskEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasker.db")
                .build().also { instance = it }
        }
    }
}

private fun CategoryEntity.toModel() = Category(id, name, position)
private fun Category.toEntity() = CategoryEntity(id, name, position)
private fun TaskEntity.toModel() = Task(id, categoryId, title, notes, priority, completed, createdAt, completedAt)
private fun Task.toEntity() = TaskEntity(id, categoryId, title, notes, priority, completed, createdAt, completedAt)

class RoomTaskStore(private val dao: TaskDao) : TaskStore {
    override fun observeCategories() = dao.observeCategories().map { l -> l.map { it.toModel() } }
    override fun observeTasks(categoryId: Long) = dao.observeTasks(categoryId).map { l -> l.map { it.toModel() } }
    override fun observeOpenCounts() = dao.observeOpenCounts().map { l -> l.associate { it.categoryId to it.count } }
    override suspend fun categories() = dao.categories().map { it.toModel() }
    override suspend fun task(id: Long) = dao.task(id)?.toModel()
    override suspend fun insertCategory(category: Category) = dao.insertCategory(category.toEntity())
    override suspend fun updateCategories(categories: List<Category>) = dao.updateCategories(categories.map { it.toEntity() })
    override suspend fun deleteCategory(id: Long) = dao.deleteCategory(id)
    override suspend fun insertTask(task: Task) = dao.insertTask(task.toEntity())
    override suspend fun updateTask(task: Task) = dao.updateTask(task.toEntity())
    override suspend fun deleteTask(id: Long) = dao.deleteTask(id)
}
