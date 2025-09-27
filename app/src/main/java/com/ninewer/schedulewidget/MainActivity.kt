package com.ninewer.schedulewidget

import android.R.color.white
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.colorResource
import com.ninewer.schedulewidget.ui.theme.ScheduleWidgetTheme

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
    var uris by remember { mutableStateOf(ImageStore.loadAll(ctx)) }

    var pickIndex by remember { mutableStateOf(-1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ImageStore.saveUri(ctx, pickIndex, it)
            uris = ImageStore.loadAll(ctx)
            ScheduleWidget.updateAllWidgets(ctx)
        }
    }

    val todayIndex = remember { getTodayIndexForWeekdays() }

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
                //.padding(bottom = 8.dp) <- makes shadow ugly
        ) {
            Text(
                "Виджет Расписания",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Text(
            "Нажмите плитку, чтобы открыть фото. Долгое нажатие — выбрать фото",
            style = MaterialTheme.typography.titleMedium
        )

        // строки вручную
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val rows = WEEK_DAYS.chunked(2) // каждая строка = 2 элемента
            items(rows.size) { rowIndex ->
                val row = rows[rowIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEachIndexed { i, dayName ->
                        val index = rowIndex * 2 + i
                        val isToday = index == todayIndex
                        Box(
                            modifier = Modifier.weight(if (isToday) 2f else 1f)
                        ) {
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
                    // заполнение пустым местом нечётное кол-во дней <- useless + ugly but we'll not see it
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// \/ just composing
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
fun getTodayIndexForWeekdays(): Int? {
    val c = Calendar.getInstance()
    return when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> null
    }
}
