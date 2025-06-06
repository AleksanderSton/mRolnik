package com.example.mrolnik.screen

import Task
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mrolnik.R
import com.example.mrolnik.model.Planner
import com.example.mrolnik.service.PlannerService
import com.example.mrolnik.service.TaskService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@SuppressLint("NewApi")
@Composable
fun PlannerScreen(navController: NavController) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var taskText by remember { mutableStateOf("") }

    var taskName by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Stan edytowanego zadania
    var editTaskName by remember { mutableStateOf("") }
    var editTaskDate by remember { mutableStateOf("") }
    var editTaskDescription by remember { mutableStateOf("") }

    // Stan do wyświetlania info o zadaniu
    var infoTaskName by remember { mutableStateOf("") }
    var infoTaskDate by remember { mutableStateOf("") }
    var infoTaskDescription by remember { mutableStateOf("") }

    val backIcon = painterResource(R.drawable.baseline_arrow_back)
    val addIcon = painterResource(R.drawable.baseline_add)

    val context = LocalContext.current

    val plannerService = PlannerService()
    var planner by remember { mutableStateOf<Planner?>(null) }

    val taskService = TaskService()
    var taskList by remember { mutableStateOf<List<Task>>(emptyList()) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    // Add this LaunchedEffect to initialize the planner
    LaunchedEffect(Unit) {
        planner = plannerService.createOrReturnPlanner()
        Log.i("PlannerScreen", "Initialized planner: ${planner?.plannerId}, ${planner?.createDate}")
    }

    LaunchedEffect(selectedDate, planner?.plannerId) {
        if (planner != null) {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val formattedDate = selectedDate.format(dateFormatter)
            planner?.plannerId?.let { pid ->
                taskList = taskService.getTasksByDate(pid, formattedDate)
                Log.d("TaskService", "Fetched ${taskList.size} tasks for plannerId=${pid}, date=$formattedDate")
            }
        }
    }

    Log.i("PlannerScreen", "PlannerId: ${planner?.plannerId}, ${planner?.createDate}")

    fun onEditTask(name: String, date: String, description: String) {
        editTaskName = name
        editTaskDate = date
        editTaskDescription = description
        showEditDialog = true
    }

    fun onInfoTask(name: String, date: String, description: String) {
        infoTaskName = name
        infoTaskDate = date
        infoTaskDescription = description
        showInfoDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter = backIcon,
                        contentDescription = "Wróć",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Planer",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Text("<")
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    fontSize = 20.sp
                )
                Button(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Text(">")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DaysOfMonth(
                month = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = { date -> selectedDate = date }
            )

            Spacer(modifier = Modifier.height(16.dp))

            selectedDate?.let { date ->
                Text(
                    text = "Zadania na ${date.dayOfMonth}.${date.monthValue}.${date.year}:",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                TaskList(
                    tasks = taskList,
                    onEditTask = { task ->
                        editTaskName = task.taskName
                        editTaskDate = task.realizeDate
                        editTaskDescription = task.description
                        editingTask = task
                        showEditDialog = true
                    },
                    onInfoTask = { task ->
                        infoTaskName = task.taskName
                        infoTaskDate = task.realizeDate
                        infoTaskDescription = task.description
                        showInfoDialog = true
                    },
                    onDeleteTask = { task ->
                        CoroutineScope(Dispatchers.IO).launch {
                            taskService.deleteTask(task)
                            withContext(Dispatchers.Main) {
                                taskList = taskList.filterNot { it.taskId == task.taskId }
                            }
                        }
                    }
                )


            }
        }

        // Dolny pasek do dodawania zadania
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp, 0.dp, 0.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = taskText,
                onValueChange = { taskText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Dodaj zadanie...") }
            )
            IconButton(
                onClick = {
                    taskName = taskText  // ustaw taskName na to co jest wpisane w polu tekstowym
                    taskDate = selectedDate.toString()  // ustaw taskDate na wybraną datę w formacie yyyy-MM-dd
                    showAddDialog = true
                }
            ) {
                Icon(
                    painter = addIcon,
                    contentDescription = "Dodaj zadanie"
                )
            }
        }

        // Dialog dodawania zadania (przyjmujemy, że CustomModalDialog jest w innym pliku)
        if (showAddDialog) {
            CustomModalDialog(
                onDismiss = { showAddDialog = false },
                title = "Nowe zadanie",
                onConfirm = {
                    planner?.let {
                        val newTask = Task(
                            taskId = 0,
                            taskName = taskName,
                            realizeDate = taskDate,
                            description = taskDescription,
                            plannerId = it.plannerId
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            taskService.addTask(newTask)
                            val refreshed = taskService.getTasksByDate(it.plannerId, selectedDate.toString())
                            withContext(Dispatchers.Main) {
                                taskList = refreshed
                                showAddDialog = false
                                taskName = ""
                                taskDate = ""
                                taskDescription = ""
                                taskText = ""
                            }
                        }
                    }
                }


            ) {
                Column {
                    OutlinedTextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        label = { Text("Nazwa zadania") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()

                            if (taskDate.isNotEmpty()) {
                                val parsedDate = LocalDate.parse(taskDate)
                                calendar.set(
                                    parsedDate.year,
                                    parsedDate.monthValue - 1,
                                    parsedDate.dayOfMonth
                                )
                            }

                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                    taskDate = selected.toString()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (taskDate.isNotEmpty()) taskDate else "Wybierz datę")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskDescription,
                        onValueChange = { taskDescription = it },
                        label = { Text("Opis zadania") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )
                }
            }
        }


        // Dialog edycji zadania
        if (showEditDialog) {
            CustomModalDialog(
                onDismiss = { showEditDialog = false },
                title = "Edytuj zadanie",
                onConfirm = {
                    editingTask?.let {
                        val updated = it.copy(
                            taskName = editTaskName,
                            realizeDate = editTaskDate,
                            description = editTaskDescription
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            taskService.updateTask(updated)
                            val refreshed = taskService.getTasksByDate(updated.plannerId, selectedDate.toString())
                            withContext(Dispatchers.Main) {
                                taskList = refreshed
                                showEditDialog = false
                            }
                        }
                    }
                }

            ) {
                Column {
                    OutlinedTextField(
                        value = editTaskName,
                        onValueChange = { editTaskName = it },
                        label = { Text("Nazwa zadania") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            if (editTaskDate.isNotEmpty()) {
                                val parsedDate = LocalDate.parse(editTaskDate)
                                calendar.set(
                                    parsedDate.year,
                                    parsedDate.monthValue - 1,
                                    parsedDate.dayOfMonth
                                )
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                    editTaskDate = selected.toString()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (editTaskDate.isNotEmpty()) editTaskDate else "Wybierz datę")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editTaskDescription,
                        onValueChange = { editTaskDescription = it },
                        label = { Text("Opis zadania") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )
                }
            }
        }

        // Dialog z informacjami o zadaniu
        if (showInfoDialog) {
            InfoModalDialog(
                onDismiss = { showInfoDialog = false },
                title = "Informacje o zadaniu"
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "Nazwa:", fontWeight = FontWeight.Bold)
                    Text(text = infoTaskName, modifier = Modifier.padding(bottom = 8.dp))

                    Text(text = "Data:", fontWeight = FontWeight.Bold)
                    Text(text = infoTaskDate, modifier = Modifier.padding(bottom = 8.dp))

                    Text(text = "Opis:", fontWeight = FontWeight.Bold)
                    Text(text = infoTaskDescription)
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun DaysOfMonth(
    month: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfWeek = (month.atDay(1).dayOfWeek.value + 6) % 7 // Poniedziałek = 0

    val daysList = buildList {
        repeat(firstDayOfWeek) { add(null) }
        for (day in 1..daysInMonth) {
            add(day)
        }
        val remaining = 7 - (size % 7)
        if (remaining < 7) {
            repeat(remaining) { add(null) }
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd").forEach { day ->
            Text(
                text = day,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }

    daysList.chunked(7).forEach { week ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            week.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .aspectRatio(1f)
                        .then(
                            if (day != null && selectedDate == month.atDay(day)) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = colorResource(id = R.color.purple_500),
                                    shape = MaterialTheme.shapes.small
                                )
                            } else Modifier
                        )
                        .let {
                            if (day != null) it.clickable { onDateSelected(month.atDay(day)) }
                            else it
                        },
                    contentAlignment = Alignment.Center
                ) {
                    day?.let {
                        Text(
                            text = it.toString(),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun TaskList(
    tasks: List<Task>,
    onEditTask: (Task) -> Unit,
    onInfoTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    val editIcon = painterResource(id = R.drawable.baseline_edit)
    val deleteIcon = painterResource(id = R.drawable.baseline_delete)
    val infoIcon = painterResource(id = R.drawable.baseline_info)

    if (tasks.isEmpty()) {
        Text(text = "Brak zadań.")
    } else {
        LazyColumn {
            items(tasks) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.taskName,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { onEditTask(task) }) {
                            Icon(painter = editIcon, contentDescription = "EDIT")
                        }
                        IconButton(onClick = { onDeleteTask(task) }) {
                            Icon(painter = deleteIcon, contentDescription = "DELETE")
                        }
                        IconButton(onClick = { onInfoTask(task) }) {
                            Icon(painter = infoIcon, contentDescription = "INFO")
                        }
                    }
                }
            }
        }
    }
}

