package com.ninewer.schedulewidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter

class FullscreenImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dayIndex = intent.getIntExtra("dayIndex", -1)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FullscreenImageScreen(dayIndex = dayIndex) {
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenImageScreen(dayIndex: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val uriState = remember { mutableStateOf(ImageStore.loadUri(ctx, dayIndex)) }

    // актуальный Uri при каждом входе
    LaunchedEffect(dayIndex) {
        uriState.value = ImageStore.loadUri(ctx, dayIndex)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uriState.value != null) {
            Image(
                painter = rememberAsyncImagePainter(uriState.value),
                contentDescription = "Fullscreen image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "Нет картинки для этого дня",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
