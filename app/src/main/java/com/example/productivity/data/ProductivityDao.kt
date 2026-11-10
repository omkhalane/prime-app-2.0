package com.example.productivity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductivityDao {
    @Query("SELECT * FROM productivity_tasks ORDER BY CASE WHEN completedAt IS NULL THEN 0 ELSE 1 END, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, priority DESC, createdAt DESC")
    fun observeTasks(): Flow<List<ProductivityTaskEntity>>

    @Query("SELECT * FROM productivity_tasks WHERE id = :id LIMIT 1")
    suspend fun task(id: String): ProductivityTaskEntity?

    @Query("SELECT COUNT(*) FROM productivity_tasks WHERE title = :title COLLATE NOCASE")
    suspend fun countTasksWithTitle(title: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: ProductivityTaskEntity)

    @Query("UPDATE productivity_tasks SET title=:title, notes=:notes, priority=:priority, dueAt=:dueAt, recurrence=:recurrence, projectId=:projectId, updatedAt=:updatedAt WHERE id=:id")
    suspend fun updateTask(id: String, title: String, notes: String, priority: Int, dueAt: Long?, recurrence: String, projectId: String?, updatedAt: Long): Int

    @Query("UPDATE productivity_tasks SET completedAt=:completedAt, updatedAt=:updatedAt WHERE id=:id")
    suspend fun setTaskCompleted(id: String, completedAt: Long?, updatedAt: Long): Int

    @Query("DELETE FROM productivity_tasks WHERE id=:id")
    suspend fun deleteTask(id: String): Int

    @Query("SELECT * FROM productivity_projects ORDER BY name COLLATE NOCASE")
    fun observeProjects(): Flow<List<ProductivityProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProductivityProjectEntity)

    @Query("DELETE FROM productivity_projects WHERE id=:id")
    suspend fun deleteProject(id: String): Int

    @Query("SELECT * FROM productivity_habits WHERE archived=0 ORDER BY createdAt")
    fun observeHabits(): Flow<List<ProductivityHabitEntity>>

    @Query("SELECT COUNT(*) FROM productivity_habits WHERE title = :title COLLATE NOCASE AND archived=0")
    suspend fun countHabitsWithTitle(title: String): Int

    @Query("SELECT * FROM productivity_habits WHERE id=:id AND archived=0 LIMIT 1")
    suspend fun habit(id: String): ProductivityHabitEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: ProductivityHabitEntity)

    @Query("UPDATE productivity_habits SET title=:title, activeDaysMask=:activeDaysMask WHERE id=:id AND archived=0")
    suspend fun updateHabit(id: String, title: String, activeDaysMask: Int): Int

    @Query("UPDATE productivity_habits SET archived=1 WHERE id=:id")
    suspend fun archiveHabit(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCheckIn(checkIn: ProductivityHabitCheckInEntity)

    @Query("DELETE FROM productivity_habit_checkins WHERE habitId=:habitId AND localDate=:localDate")
    suspend fun removeCheckIn(habitId: String, localDate: String): Int

    @Query("SELECT * FROM productivity_habit_checkins WHERE localDate=:localDate")
    fun observeCheckInsForDay(localDate: String): Flow<List<ProductivityHabitCheckInEntity>>

    @Query("SELECT * FROM productivity_habit_checkins WHERE habitId=:habitId ORDER BY localDate DESC")
    fun observeHabitHistory(habitId: String): Flow<List<ProductivityHabitCheckInEntity>>

    @Query("SELECT COUNT(*) FROM productivity_tasks WHERE completedAt IS NOT NULL")
    fun observeCompletedTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM productivity_tasks")
    fun observeTaskCount(): Flow<Int>

    @Transaction
    suspend fun completeTaskAndInsertNext(id: String, now: Long, nextTask: ProductivityTaskEntity): Boolean {
        if (setTaskCompleted(id, now, now) == 0) return false
        insertTask(nextTask)
        return true
    }
}
