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

private data class PrimeTask(val id: String, val title: String, val done: Boolean)
private data class PrimeHabit(val id: String, val title: String, val done: Boolean)

/** Lightweight offline-first task and daily-habit foundation stored on-device. */
@Composable
fun ProductivityHubCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prime_productivity", Context.MODE_PRIVATE) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val tasks = remember {
        mutableStateListOf<PrimeTask>().apply {
            val raw = prefs.getString("tasks", "[]") ?: "[]"
            runCatching {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(PrimeTask(item.optString("id"), item.optString("title"), item.optBoolean("done")))
                }
            }
        }
    }
    val habits = remember {
        mutableStateListOf<PrimeHabit>().apply {
            val savedDay = prefs.getString("habit_day", null)
            val raw = if (savedDay == today) prefs.getString("habits", null) else null
            if (raw != null) runCatching {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(PrimeHabit(item.optString("id"), item.optString("title"), item.optBoolean("done")))
                }
            }
            if (isEmpty()) {
                addAll(listOf(
                    PrimeHabit("water", "Drink enough water", false),
                    PrimeHabit("read", "Read for 20 minutes", false),
                    PrimeHabit("move", "Move your body", false)
                ))
            }
        }
    }
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }

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
                    Text("Small steps, consistent progress", color = Color(0xFF9BA8BD), fontSize = 12.sp)
                }
                IconButton(onClick = { showTaskDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add task", tint = Color.White)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color(0xFFFFC477), modifier = Modifier.size(18.dp))
                Text("  TASKS  ·  ${tasks.count { it.done }}/${tasks.size} completed", color = Color(0xFFCAD5E8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (tasks.isEmpty()) Text("Add your first task for today.", color = Color(0xFF9BA8BD), fontSize = 13.sp)
            (0 until tasks.size).toList().forEach { index ->
                val task = tasks[index]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.done, onCheckedChange = { checked ->
                        tasks[index] = task.copy(done = checked)
                        saveTasks()
                    })
                    Text(task.title, color = if (task.done) Color(0xFF7E8BA1) else Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { tasks.removeAt(index); saveTasks() }) { Text("Remove", color = Color(0xFF9BA8BD), fontSize = 11.sp) }
                }
            }
            androidx.compose.material3.HorizontalDivider(color = Color(0xFF30394A))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF7BE0B0), modifier = Modifier.size(18.dp))
                Text("  DAILY HABITS  ·  ${habits.count { it.done }}/${habits.size}", color = Color(0xFFCAD5E8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            habits.forEachIndexed { index, habit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = habit.done, onCheckedChange = { checked ->
                        habits[index] = habit.copy(done = checked)
                        saveHabits()
                    })
                    Text(habit.title, color = if (habit.done) Color(0xFF7E8BA1) else Color.White, fontSize = 14.sp)
                }
            }
            Text("Tasks and habit check-ins are saved locally on this device.", color = Color(0xFF7E8BA1), fontSize = 10.sp)
        }
    }

    if (showTaskDialog) {
        AlertDialog(
            onDismissRequest = { showTaskDialog = false; taskTitle = "" },
            title = { Text("Add a task") },
            text = {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it.take(80) },
                    label = { Text("What do you want to finish?") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val title = taskTitle.trim()
                    if (title.isNotEmpty() && tasks.none { it.title.equals(title, true) }) {
                        tasks.add(PrimeTask(java.util.UUID.randomUUID().toString(), title, false))
                        saveTasks()
                        taskTitle = ""
                        showTaskDialog = false
                    }
                }, enabled = taskTitle.trim().isNotEmpty()) { Text("Add task") }
            },
            dismissButton = { TextButton(onClick = { showTaskDialog = false; taskTitle = "" }) { Text("Cancel") } }
        )
    }
}
