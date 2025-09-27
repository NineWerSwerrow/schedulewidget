package com.ninewer.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.View
import android.widget.RemoteViews
import java.util.*

class ScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Автоматическое обновление в полночь и при смене времени/таймзоны
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val todayIndex = getTodayIndexForWeekdays() ?: 0
            val dayName = WEEK_DAYS[todayIndex]

            val views = RemoteViews(context.packageName, R.layout.schedule_widget)

            // достаём картинку из кэша
            val bitmap: Bitmap? = ImageStore.loadBitmapFromCache(context, todayIndex)

            if (bitmap != null) {
                // скругление углов
                val rounded = bitmap.withRoundedCorners(48f)

                views.setImageViewBitmap(R.id.widgetImage, rounded)
                views.setViewVisibility(R.id.widgetImage, View.VISIBLE)
                views.setViewVisibility(R.id.widgetText, View.GONE)
            } else {
                views.setTextViewText(R.id.widgetText, dayName)
                views.setViewVisibility(R.id.widgetText, View.VISIBLE)
                views.setViewVisibility(R.id.widgetImage, View.GONE)
            }

            // клик -> открыть приложение
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            // обновление виджета
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // метод для обновления всех виджетов (ало зачем вам несколько виджетов которые показывают одно и тоже, useless)
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ScheduleWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            ids.forEach { id ->
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}

// функция для скругления битмапы
fun Bitmap.withRoundedCorners(radius: Float): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint().apply {
        isAntiAlias = true
        shader = BitmapShader(this@withRoundedCorners, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, radius, radius, paint)

    return output
}
