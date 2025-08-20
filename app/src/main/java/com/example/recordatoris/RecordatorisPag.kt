package com.example.recordatoris

import android.text.format.DateUtils
import android.util.Log
import android.webkit.ConsoleMessage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlin.random.nextInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPostNotificationsIfNeeded() {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            onResult = { /* granted/denied -> no-op here */ }
        )
        LaunchedEffect(Unit) {
            if (!hasPostNotificationsPermission(context)) {
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatorisPag(){
    RequestPostNotificationsIfNeeded()
    val context = androidx.compose.ui.platform.LocalContext.current
    var mostrarModal by remember { mutableStateOf(false) }
    val repo = remember { RecordatoriRepo(DbProvider.get(context).recordatoriDao()) }
    val listRec by remember(repo) { repo.observeAll() }.collectAsState(initial = emptyList())
    var mostrarSelector by remember { mutableStateOf(false) }
    var mostrarSelectorDia by remember { mutableStateOf(false) }
    var titol by remember { mutableStateOf("") }
    var isRecurrent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val timeState = rememberTimePickerState(
        initialHour = if (Date.from(Instant.now()).hours < 23) Date.from(Instant.now()).hours + 1 else 0,
        initialMinute = 0,
        is24Hour = true // Format de 24 hores
    )
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    fun addRecordatori(title: String, date: Date, time: java.time.LocalTime, id:Int, recurrent:Boolean) {
        scope.launch {
            repo.add(Recordatori(id, title, date, time, recurrent))
        }
    }
    val deleteRecordatori: (Recordatori) -> Unit = { rec ->
        scope.launch {
            repo.deleteById(rec.id)
            cancelReminder(context, rec.id)
        }
    }
    if (mostrarModal) {
        AlertDialog(
            onDismissRequest = { mostrarModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if(isRecurrent){
                            val zone = ZoneId.systemDefault()
                            val millis: Long = dateState.selectedDateMillis
                                ?: (System.currentTimeMillis() + 60 * 60 * 1000) // if null → +1 hou
                            val id = Random.nextInt(1000, 99000)
                            addRecordatori(
                                titol,
                                Date(millis),
                                LocalTime.of(timeState.hour, timeState.minute),
                                id,
                                true
                            )
                            val baseDate: LocalDate = Instant.ofEpochMilli(millis)
                                .atZone(zone)
                                .toLocalDate() // IMPORTANT: DatePicker gives midnight UTC; convert with your ZoneId
                            val pickedTime: LocalTime = LocalTime.of(timeState.hour, timeState.minute)
                            scheduleMonthlyReminder(
                                context = context,
                                id = id,     // e.g., DB id or a unique number
                                dayOfMonth = baseDate.dayOfMonth,
                                hour = pickedTime.hour,
                                minute = pickedTime.minute,
                                title = "Recordatori",
                                text = titol
                            )
                            mostrarModal = false
                        }
                        else {
                            val zone = ZoneId.systemDefault()
                            val millis: Long = dateState.selectedDateMillis
                                ?: (System.currentTimeMillis() + 60 * 60 * 1000) // if null → +1 hou
                            val id = Random.nextInt(1000, 99000)
                            addRecordatori(
                                titol,
                                Date(millis),
                                LocalTime.of(timeState.hour, timeState.minute),
                                id,
                                false
                            )
                            val baseDate: LocalDate = Instant.ofEpochMilli(millis)
                                .atZone(zone)
                                .toLocalDate() // IMPORTANT: DatePicker gives midnight UTC; convert with your ZoneId
                            val pickedTime: LocalTime =
                                LocalTime.of(timeState.hour, timeState.minute)
                            val pickedDateTime: LocalDateTime =
                                LocalDateTime.of(baseDate, pickedTime)
                            val triggerAtMillis: Long = pickedDateTime
                                .atZone(zone)
                                .toInstant()
                                .toEpochMilli()
                            scheduleReminder(
                                context = context,
                                triggerAtMillis = triggerAtMillis,
                                id = id,     // e.g., DB id or a unique number
                                title = "Recordatori",
                                text = titol
                            )
                            mostrarModal = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModal = false }) {
                    Text("Cancel·lar")
                }
            },
            title = { Text("Afegir recordatori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titol,
                        onValueChange = { titol = it },
                        label = { Text("Títol") },
                        singleLine = true
                    )
                    Button(onClick = { mostrarSelector = true }) {
                        Text("Seleccionar hora")
                    }
                    Button(onClick = {mostrarSelectorDia= true}) {
                        Text(text = "Seleccionar dia")
                    }
                    Row (verticalAlignment = Alignment.CenterVertically){
                        Checkbox(
                        checked = isRecurrent,
                        onCheckedChange = {isRecurrent = it}
                        )
                        Text("Recurrent cada mes?")
                    }
                    val selectedMillis = dateState.selectedDateMillis
                    if (selectedMillis != null) {
                        val dataSeleccionada = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(java.util.Date(selectedMillis))
                        Text(text=(if(timeState.hour >= 10) timeState.hour.toString() else "0"+timeState.hour.toString())+":"+(if(timeState.minute >= 10) timeState.minute.toString() else "0"+timeState.minute.toString())+", "+dataSeleccionada)
                    }
                    else{
                        Text(text=(if(timeState.hour >= 10) timeState.hour.toString() else "0"+timeState.hour.toString())+":"+(if(timeState.minute >= 10) timeState.minute.toString() else "0"+timeState.minute.toString())+", "+ SimpleDateFormat("HH:mm, dd/MM/yy", Locale.ENGLISH).format(Date.from(Instant.now())))
                    }
                }
            }
        )
    }
    if (mostrarSelectorDia) {
        DatePickerDialog(
            onDismissRequest = { mostrarSelector = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = dateState.selectedDateMillis
                    if (selectedMillis != null) {
                        val dataSeleccionada = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(java.util.Date(selectedMillis))
                        println("Data seleccionada: $dataSeleccionada")
                    }
                    else{
                        dateState.selectedDateMillis = (System.currentTimeMillis())
                    }
                    mostrarSelectorDia = false
                }) {
                    Text("Acceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorDia = false }) {
                    Text("Cancel·lar")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
    if (mostrarSelector) {
        AlertDialog(
            onDismissRequest = { mostrarSelector = false },
            confirmButton = {
                TextButton(onClick = {
                    mostrarSelector = false
                }) {
                    Text("Acceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelector = false }) {
                    Text("Cancel·lar")
                }
            },
            title = { Text("Tria l'hora") },
            text = {
                TimePicker(state = timeState)
            }
        )
    }


    fun Date.isThisWeek(
        zoneId: ZoneId = ZoneId.systemDefault(),
        weekFields: WeekFields = WeekFields.of(Locale.getDefault()) // or WeekFields.ISO
    ): Boolean {
        val today = LocalDate.now(zoneId)
        val date = this.toInstant().atZone(zoneId).toLocalDate()
        if(date == today) return false
        val wToday = today.get(weekFields.weekOfWeekBasedYear())
        val yToday = today.get(weekFields.weekBasedYear())
        val wDate  = date.get(weekFields.weekOfWeekBasedYear())
        val yDate  = date.get(weekFields.weekBasedYear())

        return wToday == wDate && yToday == yDate
    }
    Column(modifier = Modifier.fillMaxSize().background(Color.DarkGray).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly){
            Button(onClick = {mostrarModal=true}, modifier = Modifier.fillMaxWidth().padding(top=24.dp, start = 8.dp, end = 8.dp), colors = ButtonColors(
                Color.Yellow, contentColor = Color.Black,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.White
            )) {Text(text="Afegir") }
        }
        Text(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), text = "Recordatoris per avui", color = Color.White)
        HorizontalDivider(modifier = Modifier.padding(8.dp), color = Color.White,thickness = 2.dp)
        LazyColumn{
            val todayList = listRec.filter { DateUtils.isToday(it.date.time) }
            if(todayList.isEmpty()){
                item {
                    Text("No hi han recordatoris per avui.", modifier = Modifier.padding(10.dp), color = Color.White)
                }
            }else{
                items(todayList.size) { index ->
                    if (todayList[index].recurring) {
                        RecordatoriItem(listRec[index], onDelete = deleteRecordatori, true)
                    } else {
                        RecordatoriItem(listRec[index], onDelete = deleteRecordatori, false)
                    }
                }
            }
        }
        Text(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), text = "Recordatoris per aquesta setmana", color = Color.White)
        HorizontalDivider(modifier = Modifier.padding(8.dp), color = Color.White,thickness = 2.dp)
        LazyColumn{
            val thisWeekList = listRec.filter {it.date.isThisWeek()}
            if(thisWeekList.isEmpty()){
                item {
                    Text("No hi han recordatoris per aquesta setmana.", modifier = Modifier.padding(10.dp), color = Color.White)
                }
            }else{
                items(thisWeekList.size) { index ->
                    if (thisWeekList[index].recurring) {
                        RecordatoriItem(listRec[index], onDelete = deleteRecordatori, true)
                    } else {
                        RecordatoriItem(listRec[index], onDelete = deleteRecordatori, false)
                    }
                }
            }
        }
        Text(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), text = "Tots els recordatoris", color = Color.White)
        HorizontalDivider(modifier = Modifier.padding(8.dp), color = Color.White,thickness = 2.dp)
        LazyColumn {
            items(listRec.size) { index ->
                if (listRec[index].recurring) {
                    RecordatoriItem(listRec[index], onDelete = deleteRecordatori, true)
                } else {
                    RecordatoriItem(listRec[index], onDelete = deleteRecordatori, false)
                }
            }
        }

    }
}


@Composable
fun RecordatoriItem(item: Recordatori, onDelete: (Recordatori) -> Unit, recurring: Boolean){
    Row (modifier = Modifier.fillMaxWidth()
        .padding(8.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.Blue)
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Column (modifier = Modifier.weight(1f)){
            Text(text = (if (item.hour.hour >= 10) item.hour.hour.toString() else "0"+item.hour.hour.toString())+":"+(if (item.hour.minute >= 10) item.hour.minute.toString() else "0"+item.hour.minute.toString())+", "+SimpleDateFormat("dd/MM/yy", Locale.ENGLISH).format(item.date),
                fontSize = 14.sp,
                color = Color.White)
            if(recurring){
                Text(text="Recurrent", color = Color.White, fontSize = 14.sp)
            }
            Text(text = item.title, fontSize = 20.sp, color = Color.White)
        }

        IconButton (onClick = {onDelete(item)}){
            Icon(painter = painterResource(id=R.drawable.baseline_delete_24),
                contentDescription = "Borrar",
                tint = Color.White)
        }
    }
}