package com.ninewer.schedulewidget

import android.R.color.white
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.ninewer.schedulewidget.ui.theme.ScheduleWidgetTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScheduleWidgetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeekGridScreen()
                }
            }
        }
    }
}

@Composable
fun WeekGridScreen() {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var uris by remember { mutableStateOf(ImageStore.loadAll(ctx)) }

    var pickIndex by remember { mutableStateOf(-1) }


    val todayIndex = remember { getTodayIndexForWeekdays() }

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember {
        mutableStateOf(prefs.getInt("updateHour", ScheduleWidget.updateHour))
    }
    var selectedMinute by remember {
        mutableStateOf(prefs.getInt("updateMinute", ScheduleWidget.updateMinute))
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ImageStore.saveUri(ctx, pickIndex, it)
            uris = ImageStore.loadAll(ctx)
            ScheduleWidget.updateAllWidgets(ctx, showTomorrow = hasAlarmTimePassed(selectedHour, selectedMinute))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape = RoundedCornerShape(24.dp))
        ) {
            Text(
                "Виджет Расписания",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .shadow(8.dp, shape = RoundedCornerShape(size= 24.dp)))
                {
                    Column (modifier = Modifier.padding(12.dp)) {
                        Text("Принудительно обновить виджет на: ", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {ScheduleWidget.updateAllWidgets(ctx, showTomorrow = false)},
                                modifier = Modifier.weight(1f)
                            ) {Text("Сегодня")}
                            OutlinedButton(
                                onClick = {ScheduleWidget.updateAllWidgets(ctx, showTomorrow = true)},
                                modifier = Modifier.weight(1f)
                            ) {Text("Завтра") }
                        }
                    }
                }

        // карточка выбора времени
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .shadow(8.dp, shape = RoundedCornerShape(size= 24.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Время перехода на следующий день", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Выбрать время")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedHour = 0
                            selectedMinute = 0
                            ScheduleWidget.updateHour = 0
                            ScheduleWidget.updateMinute = 0
                            ScheduleWidget.scheduleNextUpdate(ctx)
                            ScheduleWidget.updateAllWidgets(ctx, showTomorrow = hasAlarmTimePassed(selectedHour, selectedMinute))

                            prefs.edit()
                                .putInt("updateHour", 0)
                                .putInt("updateMinute", 0)
                                .apply()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сбросить")
                    }
                }
            }
        }

        // диалог выбора времени
        if (showTimePicker) {
            val isDark = isSystemInDarkTheme()
            val textColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("OK")
                    }
                },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AndroidView(
                            factory = { context ->
                                NumberPicker(context).apply {
                                    minValue = 0
                                    maxValue = 23
                                    value = selectedHour
                                    setTextColor(textColor)
                                    setOnValueChangedListener { _, _, newVal ->
                                        selectedHour = newVal
                                        ScheduleWidget.updateHour = newVal
                                        ScheduleWidget.scheduleNextUpdate(ctx)
                                        ScheduleWidget.updateAllWidgets(ctx, showTomorrow = hasAlarmTimePassed(selectedHour, selectedMinute))
                                        prefs.edit()
                                            .putInt("updateHour", newVal)
                                            .apply()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        AndroidView(
                            factory = { context ->
                                NumberPicker(context).apply {
                                    minValue = 0
                                    maxValue = 59
                                    value = selectedMinute
                                    setTextColor(textColor)
                                    setOnValueChangedListener { _, _, newVal ->
                                        selectedMinute = newVal
                                        ScheduleWidget.updateMinute = newVal
                                        ScheduleWidget.scheduleNextUpdate(ctx)
                                        ScheduleWidget.updateAllWidgets(ctx, showTomorrow = hasAlarmTimePassed(selectedHour, selectedMinute))
                                        prefs.edit()
                                            .putInt("updateMinute", newVal)
                                            .apply()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Нажмите плитку, чтобы открыть фото. Долгое нажатие — выбрать фото",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // список плиток
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val rows = WEEK_DAYS.chunked(2)
            items(rows.size) { rowIndex ->
                val row = rows[rowIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEachIndexed { i, dayName ->
                        val index = rowIndex * 2 + i
                        val isToday = index == todayIndex
                        Box(modifier = Modifier.weight(if (isToday) 2f else 1f)) {
                            DayTile(
                                dayName = dayName,
                                uri = uris.getOrNull(index),
                                isToday = isToday,
                                onClick = {
                                    ctx.startActivity(
                                        Intent(ctx, FullscreenImageActivity::class.java).apply {
                                            putExtra("dayIndex", index)
                                        }
                                    )
                                },
                                onLongClick = {
                                    pickIndex = index
                                    launcher.launch(arrayOf("image/*"))
                                }
                            )
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayTile(
    dayName: String,
    uri: Uri?,
    isToday: Boolean,
    onClick: (Uri?) -> Unit,
    onLongClick: () -> Unit
) {
    val cornerRadius = 24.dp

    Card(
        shape = RoundedCornerShape(cornerRadius),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(
                elevation = if (isToday) 16.dp else 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick(uri) },
                    onLongPress = { onLongClick() }
                )
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = "$dayName image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет фото", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text(
                text = dayName,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = colorResource(white)
            )
        }
    }
}

fun hasAlarmTimePassed(hour: Int, minute: Int): Boolean {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return now.after(target)
}


fun getTodayIndexForWeekdays(): Int? {
    val c = Calendar.getInstance()
    return when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 0
        else -> null
    }
}
