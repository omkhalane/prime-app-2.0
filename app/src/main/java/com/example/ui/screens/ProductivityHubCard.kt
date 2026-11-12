package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.productivity.data.ProductivityDatabase
import com.example.productivity.data.ProductivityRepository
import com.example.productivity.data.ProductivityResult
import com.example.productivity.data.ProductivityTaskEntity
import com.example.productivity.data.TaskPriority
import com.example.productivity.data.TaskRecurrence
import com.example.ui.theme.PrimeAccent
import com.example.ui.theme.PrimeBackground
import com.example.ui.theme.PrimeSubtleBorder
import com.example.ui.theme.PrimeSurface
import com.example.ui.theme.PrimeSurfaceElevated
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import com.example.ui.theme.PrimeTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** Live Room-backed planner. Empty state means the user has not created records. */
@Composable
fun ProductivityHubCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { ProductivityRepository(ProductivityDatabase.get(context).productivityDao()) }
    val tasks by repository.tasks.collectAsState(initial = emptyList())
    val habits by repository.habits.collectAsState(initial = emptyList())
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }
    val checkIns by repository.observeTodayCheckIns().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showTaskDialog by remember { mutableStateOf(false) }
    var showHabitDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.NORMAL) }
    var recurrence by remember { mutableStateOf(TaskRecurrence.NONE) }
    var habitDays by remember { mutableStateOf(setOf(0,1,2,3,4,5,6)) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var priorityMenu by remember { mutableStateOf(false) }
    var recurrenceMenu by remember { mutableStateOf(false) }

    fun parseDueDate(input: String): Long? {
        if (input.isBlank()) return null
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        val parsed = runCatching { format.parse(input) }.getOrNull() ?: return Long.MIN_VALUE
        return parsed.time
    }

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, PrimeSubtleBorder), colors = CardDefaults.cardColors(containerColor = PrimeSurface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = PrimeAccent)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Daily Planner", color = PrimeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Tasks, priorities and daily habits", color = PrimeTextSecondary, fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = PrimeAccent, modifier = Modifier.size(18.dp))
                Text("  TASKS · ${tasks.count { it.completedAt != null }}/${tasks.size}", color = PrimeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { title=""; notes=""; dueDate=""; error=null; showTaskDialog=true }) { Icon(Icons.Rounded.Add, contentDescription="Add task", tint=PrimeTextPrimary) }
            }
            if (tasks.isEmpty()) Text("No tasks yet. Add your first task.", color=PrimeTextSecondary, fontSize=13.sp)
            tasks.forEach { task ->
                TaskRow(task, onToggle = { done -> scope.launch { repository.setTaskDone(task.id, done) } }, onDelete = { scope.launch { repository.deleteTask(task.id) } })
            }
            androidx.compose.material3.HorizontalDivider(color=PrimeSubtleBorder)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription=null, tint=PrimeAccent, modifier=Modifier.size(18.dp))
                Text("  HABITS · ${checkIns.count { check -> habits.any { it.id == check.habitId } }}/${habits.size} TODAY", color=Color(0xFFCAD5E8), fontSize=11.sp, fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick={ title=""; error=null; habitDays=setOf(0,1,2,3,4,5,6); showHabitDialog=true }) { Icon(Icons.Rounded.Add, contentDescription="Add habit", tint=PrimeTextPrimary) }
            }
            if (habits.isEmpty()) Text("No habits configured. Add routines you want to track.", color=PrimeTextSecondary, fontSize=13.sp)
            habits.forEach { habit ->
                val checked = checkIns.any { it.habitId == habit.id }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=checked, onCheckedChange={ done -> scope.launch { repository.setHabitDone(habit.id, today, done) } })
                    Column(Modifier.weight(1f)) {
                        Text(habit.title, color=if(checked) Color(0xFF7E8BA1) else Color.White, fontSize=14.sp)
                        Text("Scheduled days: ${(0..6).filter { habit.activeDaysMask and (1 shl it) != 0 }.joinToString(",") { listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")[it] }}", color=PrimeTextTertiary, fontSize=10.sp)
                    }
                    IconButton(onClick={ scope.launch { repository.archiveHabit(habit.id) } }) { Icon(Icons.Rounded.DeleteOutline, contentDescription="Archive habit", tint=PrimeTextSecondary) }
                }
            }
            Text("Saved on this device. No sample activity or placeholder records are generated.", color=PrimeTextTertiary, fontSize=10.sp)
        }
    }

    if (showTaskDialog) AlertDialog(
        onDismissRequest={ if(!saving) showTaskDialog=false },
        title={ Text("Create task") },
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=title,onValueChange={ title=it.take(80); error=null },label={ Text("Title (2–80 characters)") },singleLine=true)
                OutlinedTextField(value=notes,onValueChange={ notes=it.take(2000) },label={ Text("Notes (optional)") })
                OutlinedTextField(value=dueDate,onValueChange={ dueDate=it.take(10); error=null },label={ Text("Due date YYYY-MM-DD (optional)") },singleLine=true)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Priority: ${priority.name}", color=PrimeTextPrimary)
                    TextButton(onClick={ priorityMenu=true }) { Text("Change") }
                    DropdownMenu(expanded=priorityMenu,onDismissRequest={priorityMenu=false}) {
                        TaskPriority.entries.forEach { value -> DropdownMenuItem(text={Text(value.name)},onClick={priority=value;priorityMenu=false}) }
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Repeat: ${recurrence.name}", color=PrimeTextPrimary)
                    TextButton(onClick={recurrenceMenu=true}) { Text("Change") }
                    DropdownMenu(expanded=recurrenceMenu,onDismissRequest={recurrenceMenu=false}) {
                        TaskRecurrence.entries.forEach { value -> DropdownMenuItem(text={Text(value.name)},onClick={recurrence=value;recurrenceMenu=false}) }
                    }
                }
                error?.let { Text(it,color=Color(0xFFFF5B66),fontSize=12.sp) }
            }
        },
        confirmButton={ Button(enabled=!saving,onClick={
            val due = parseDueDate(dueDate)
            if(due == Long.MIN_VALUE) { error="Enter a valid date in YYYY-MM-DD format." }
            else {
                saving=true
                scope.launch {
                    when(val result=repository.createTask(title,notes,priority,due,recurrence)) {
                        is ProductivityResult.Success -> { showTaskDialog=false; title=""; notes=""; dueDate="" }
                        is ProductivityResult.Invalid -> error=result.reason
                        ProductivityResult.Duplicate -> error="A task with this title already exists."
                        ProductivityResult.NotFound -> error="The task could not be found."
                    }
                    saving=false
                }
            }
        }) { Text(if(saving) "Saving…" else "Create") } },
        dismissButton={ TextButton(onClick={showTaskDialog=false}) { Text("Cancel") } }
    )

    if(showHabitDialog) AlertDialog(
        onDismissRequest={if(!saving) showHabitDialog=false},
        title={Text("Create habit")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value=title,onValueChange={title=it.take(60);error=null},label={Text("Habit name (2–60 characters)")},singleLine=true)
                Text("Repeat on",color=PrimeTextPrimary,fontSize=13.sp)
                val days=listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
                days.forEachIndexed { index, day -> Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=index in habitDays,onCheckedChange={checked -> habitDays=if(checked) habitDays+index else habitDays-index})
                    Text(day,color=PrimeTextPrimary,fontSize=13.sp)
                } }
                error?.let { Text(it,color=Color(0xFFFF5B66),fontSize=12.sp) }
            }
        },
        confirmButton={Button(enabled=!saving,onClick={
            saving=true
            scope.launch {
                when(val result=repository.createHabit(title,habitDays)) {
                    is ProductivityResult.Success -> {showHabitDialog=false;title=""}
                    is ProductivityResult.Invalid -> error=result.reason
                    ProductivityResult.Duplicate -> error="A habit with this name already exists."
                    ProductivityResult.NotFound -> error="The habit could not be found."
                }
                saving=false
            }
        }) {Text(if(saving) "Saving…" else "Create")} },
        dismissButton={TextButton(onClick={showHabitDialog=false}) {Text("Cancel")}}
    )
}

@Composable
private fun TaskRow(task: ProductivityTaskEntity, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Checkbox(checked=task.completedAt!=null,onCheckedChange=onToggle)
        Column(Modifier.weight(1f)) {
            Text(task.title,color=if(task.completedAt!=null) Color(0xFF7E8BA1) else Color.White,fontSize=14.sp)
            val due = task.dueAt?.let { SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(Date(it)) }
            Text(listOf("Priority: ${TaskPriority.entries.firstOrNull { it.value==task.priority }?.name ?: "NORMAL"}",due?.let{"Due $it"},task.recurrence.takeIf{it!="NONE"}?.let{"Repeat $it"}).filterNotNull().joinToString(" · "),color=PrimeTextTertiary,fontSize=10.sp)
        }
        IconButton(onClick=onDelete) { Icon(Icons.Rounded.DeleteOutline,contentDescription="Delete task",tint=PrimeTextSecondary) }
    }
}
