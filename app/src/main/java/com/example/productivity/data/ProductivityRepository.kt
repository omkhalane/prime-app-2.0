package com.example.productivity.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow

enum class TaskRecurrence { NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY }
enum class TaskPriority(val value: Int) { LOW(1), NORMAL(2), HIGH(3), URGENT(4) }

sealed class ProductivityResult {
    data class Success(val id: String) : ProductivityResult()
    data class Invalid(val reason: String) : ProductivityResult()
    data object Duplicate : ProductivityResult()
    data object NotFound : ProductivityResult()
}

/** Validated persistence boundary. No seed data or synthetic activity is generated here. */
class ProductivityRepository(private val dao: ProductivityDao) {
    val tasks: Flow<List<ProductivityTaskEntity>> = dao.observeTasks()
    val projects: Flow<List<ProductivityProjectEntity>> = dao.observeProjects()
    val habits: Flow<List<ProductivityHabitEntity>> = dao.observeHabits()
    val completedTasks: Flow<Int> = dao.observeCompletedTaskCount()
    val taskCount: Flow<Int> = dao.observeTaskCount()

    suspend fun createTask(titleInput: String, notesInput: String = "", priority: TaskPriority = TaskPriority.NORMAL, dueAt: Long? = null, recurrence: TaskRecurrence = TaskRecurrence.NONE, projectId: String? = null): ProductivityResult {
        val title = titleInput.trim()
        val notes = notesInput.trim()
        if (title.length !in 2..80) return ProductivityResult.Invalid("Task title must contain 2–80 characters.")
        if (notes.length > 2000) return ProductivityResult.Invalid("Notes cannot exceed 2,000 characters.")
        if (dueAt != null && dueAt < 0L) return ProductivityResult.Invalid("Due date is invalid.")
        if (recurrence != TaskRecurrence.NONE && dueAt == null) return ProductivityResult.Invalid("A due date is required for recurring tasks.")
        if (dao.countTasksWithTitle(title) > 0) return ProductivityResult.Duplicate
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        return try {
            dao.insertTask(ProductivityTaskEntity(id, title, notes, priority.value, dueAt, recurrence.name, projectId, null, now, now))
            ProductivityResult.Success(id)
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            ProductivityResult.Duplicate
        }
    }

    suspend fun setTaskDone(id: String, done: Boolean): ProductivityResult {
        val existing = dao.task(id) ?: return ProductivityResult.NotFound
        val now = System.currentTimeMillis()
        if (existing.completedAt != null && done) return ProductivityResult.Success(id)
        if (dao.setTaskCompleted(id, if (done) now else null, now) == 0) return ProductivityResult.NotFound
        if (done && existing.recurrence != TaskRecurrence.NONE.name) {
            val nextDue = nextOccurrence(existing.dueAt ?: now, TaskRecurrence.valueOf(existing.recurrence))
            val nextId = UUID.randomUUID().toString()
            dao.insertTask(existing.copy(id = nextId, dueAt = nextDue, completedAt = null, createdAt = now, updatedAt = now))
        }
        return ProductivityResult.Success(id)
    }

    private fun nextOccurrence(from: Long, recurrence: TaskRecurrence): Long {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = from }
        when (recurrence) {
            TaskRecurrence.NONE -> Unit
            TaskRecurrence.DAILY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            TaskRecurrence.WEEKDAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                while (calendar.get(java.util.Calendar.DAY_OF_WEEK) in listOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY)) {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            TaskRecurrence.WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            TaskRecurrence.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }

    suspend fun deleteTask(id: String): ProductivityResult =
        if (dao.deleteTask(id) > 0) ProductivityResult.Success(id) else ProductivityResult.NotFound

    suspend fun createHabit(titleInput: String, activeDays: Set<Int>): ProductivityResult {
        val title = titleInput.trim()
        if (title.length !in 2..60) return ProductivityResult.Invalid("Habit name must contain 2–60 characters.")
        if (activeDays.isEmpty() || activeDays.any { it !in 0..6 }) return ProductivityResult.Invalid("Select at least one valid weekday.")
        if (dao.countHabitsWithTitle(title) > 0) return ProductivityResult.Duplicate
        val now = System.currentTimeMillis()
        val mask = activeDays.fold(0) { acc, day -> acc or (1 shl day) }
        val id = UUID.randomUUID().toString()
        return try {
            dao.insertHabit(ProductivityHabitEntity(id, title, mask, now))
            ProductivityResult.Success(id)
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            ProductivityResult.Duplicate
        }
    }

    suspend fun setHabitDone(habitId: String, localDate: String, done: Boolean): ProductivityResult {
        if (!isValidIsoDate(localDate)) return ProductivityResult.Invalid("Date must be a valid yyyy-MM-dd calendar date.")
        val habit = dao.habit(habitId) ?: return ProductivityResult.NotFound
        val date = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(localDate) }.getOrNull()
            ?: return ProductivityResult.Invalid("Calendar date is invalid.")
        val calendar = java.util.Calendar.getInstance().apply { time = date }
        val dayIndex = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
        if (done && habit.activeDaysMask and (1 shl dayIndex) == 0) {
            return ProductivityResult.Invalid("This habit is not scheduled for the selected weekday.")
        }
        if (done) dao.putCheckIn(ProductivityHabitCheckInEntity(habitId, localDate, System.currentTimeMillis()))
        else dao.removeCheckIn(habitId, localDate)
        return ProductivityResult.Success(habitId)
    }

    suspend fun archiveHabit(id: String): ProductivityResult =
        if (dao.archiveHabit(id) > 0) ProductivityResult.Success(id) else ProductivityResult.NotFound

    fun observeTodayCheckIns(): Flow<List<ProductivityHabitCheckInEntity>> =
        dao.observeCheckInsForDay(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()))

    private fun isValidIsoDate(value: String): Boolean = runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        format.parse(value) != null
    }.getOrDefault(false)
}
