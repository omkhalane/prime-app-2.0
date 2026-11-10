package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class PrimeTask(val id: String, val title: String, val done: Boolean)
private data class PrimeHabit(val id: String, val title: String, val done: Boolean)

/**
 * Offline-first planner. User-created records only: a fresh install starts empty and never
 * injects sample tasks or habits. Invalid/corrupt stored JSON is treated as empty, not demo data.
 */
@Composable
fun ProductivityHubCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prime_productivity", Context.MODE_PRIVATE) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }
    val tasks = remember {
        mutableStateListOf<PrimeTask>().apply {
            val raw = prefs.getString("tasks", "[]") ?: "[]"
            runCatching {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val title = item.optString("title").trim().takeIf(String::isNotBlank) ?: continue
                    add(PrimeTask(id, title, item.optBoolean("done")))
                }
            }
        }
    }
    val habits = remember {
        mutableStateListOf<PrimeHabit>().apply {
            val savedDay = prefs.getString("habit_day", null)
            if (savedDay == today) {
                val raw = prefs.getString("habits", "[]") ?: "[]"
                runCatching {
                    val array = JSONArray(raw)
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                        val title = item.optString("title").trim().takeIf(String::isNotBlank) ?: continue
                        add(PrimeHabit(id, title, item.optBoolean("done")))
                    }
                }
            }
        }
    }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showHabitDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var habitTitle by remember { mutableStateOf("") }
    var taskError by remember { mutableStateOf<String?>(null) }
    var habitError by remember { mutableStateOf<String?>(null) }

    fun saveTasks() {
        val array = JSONArray()
        tasks.forEach { array.put(JSONObject().put("id", it.id).put("title", it.title).put("done", it.done)) }
        prefs.edit().putString("tasks", array.toString()).apply()
    }
    fun saveHabits() {
        val array = JSONArray()
        habits.forEach { array.put(JSONObject().put("id", it.id).put("title", it.title).put("done", it.done)) }
        prefs.edit().putString("habits", array.toString()).putString("habit_day", today).apply()
    }
    fun addTask() {
        val title = taskTitle.trim()
        taskError = when {
            title.isEmpty() -> "Task title is required."
            title.length < 2 -> "Enter at least 2 characters."
            tasks.any { it.title.equals(title, ignoreCase = true) } -> "A task with this title already exists."
            tasks.size >= 100 -> "You have reached the 100-task limit."
            else -> null
        }
        if (taskError == null) {
            tasks.add(PrimeTask(UUID.randomUUID().toString(), title, false))
            saveTasks()
            taskTitle = ""
            showTaskDialog = false
        }
    }
    fun addHabit() {
        val title = habitTitle.trim()
        habitError = when {
            title.isEmpty() -> "Habit name is required."
            title.length < 2 -> "Enter at least 2 characters."
            habits.any { it.title.equals(title, ignoreCase = true) } -> "This habit already exists."
            habits.size >= 30 -> "You can track up to 30 habits."
            else -> null
        }
        if (habitError == null) {
            habits.add(PrimeHabit(UUID.randomUUID().toString(), title, false))
            saveHabits()
            habitTitle = ""
            showHabitDialog = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B29))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = Color(0xFF8EBBFF))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Daily Planner", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Your tasks and routines, stored on this device", color = Color(0xFF9BA8BD), fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color(0xFFFFC477), modifier = Modifier.size(18.dp))
                Text("  TASKS · ${tasks.count { it.done }}/${tasks.size} completed", color = Color(0xFFCAD5E8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { taskTitle = ""; taskError = null; showTaskDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add task", tint = Color.White)
                }
            }
            if (tasks.isEmpty()) {
                Text("No tasks yet. Add a task to start planning.", color = Color(0xFF9BA8BD), fontSize = 13.sp)
            } else {
                tasks.indices.toList().forEach { index ->
                    val task = tasks[index]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.done, onCheckedChange = { checked ->
                            tasks[index] = task.copy(done = checked)
                            saveTasks()
                        })
                        Text(task.title, color = if (task.done) Color(0xFF7E8BA1) else Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { tasks.removeAt(index); saveTasks() }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${task.title}", tint = Color(0xFF9BA8BD))
                        }
                    }
                }
            }
            androidx.compose.material3.HorizontalDivider(color = Color(0xFF30394A))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF7BE0B0), modifier = Modifier.size(18.dp))
                Text("  HABITS · ${habits.count { it.done }}/${habits.size} completed today", color = Color(0xFFCAD5E8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { habitTitle = ""; habitError = null; showHabitDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add habit", tint = Color.White)
                }
            }
            if (habits.isEmpty()) {
                Text("No habits configured. Add the routines you want to track.", color = Color(0xFF9BA8BD), fontSize = 13.sp)
            } else {
                habits.indices.toList().forEach { index ->
                    val habit = habits[index]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = habit.done, onCheckedChange = { checked ->
                            habits[index] = habit.copy(done = checked)
                            saveHabits()
                        })
                        Text(habit.title, color = if (habit.done) Color(0xFF7E8BA1) else Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { habits.removeAt(index); saveHabits() }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${habit.title}", tint = Color(0xFF9BA8BD))
                        }
                    }
                }
            }
            Text("No sample records are inserted. Tasks persist; habit check-ins are tracked per calendar day.", color = Color(0xFF7E8BA1), fontSize = 10.sp)
        }
    }

    if (showTaskDialog) {
        AlertDialog(
            onDismissRequest = { showTaskDialog = false; taskTitle = ""; taskError = null },
            title = { Text("Add a task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = taskTitle, onValueChange = { taskTitle = it.take(80); taskError = null }, label = { Text("Task title") }, singleLine = true, isError = taskError != null)
                    taskError?.let { Text(it, color = Color(0xFFFF7777), fontSize = 12.sp) }
                }
            },
            confirmButton = { Button(onClick = { addTask() }) { Text("Add task") } },
            dismissButton = { TextButton(onClick = { showTaskDialog = false; taskTitle = ""; taskError = null }) { Text("Cancel") } }
        )
    }
    if (showHabitDialog) {
        AlertDialog(
            onDismissRequest = { showHabitDialog = false; habitTitle = ""; habitError = null },
            title = { Text("Add a habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = habitTitle, onValueChange = { habitTitle = it.take(60); habitError = null }, label = { Text("Habit name") }, singleLine = true, isError = habitError != null)
                    habitError?.let { Text(it, color = Color(0xFFFF7777), fontSize = 12.sp) }
                }
            },
            confirmButton = { Button(onClick = { addHabit() }) { Text("Add habit") } },
            dismissButton = { TextButton(onClick = { showHabitDialog = false; habitTitle = ""; habitError = null }) { Text("Cancel") } }
        )
    }
}
