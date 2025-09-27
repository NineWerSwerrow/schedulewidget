package com.ninewer.schedulewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import java.util.*

class ScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // показывать сегодня при ручном обновлении
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, showTomorrow = false)
        }
        scheduleNextUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // обычные системные события = сегодня
                updateAllWidgets(context, showTomorrow = false)
                scheduleNextUpdate(context)
            }

            ACTION_UPDATE_WIDGET -> {
                // будильник = завтра
                updateAllWidgets(context, showTomorrow = true)
                scheduleNextUpdate(context)
            }
        }
    }

    companion object {
        var updateHour = 0      // по умолчанию 00:00
        var updateMinute = 0

        const val ACTION_UPDATE_WIDGET = "com.ninewer.schedulewidget.UPDATE_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            showTomorrow: Boolean
        ) {
            val todayIndex = getDayIndex(offset = if (showTomorrow) 1 else 0)
            val dayName = WEEK_DAYS[todayIndex]

            val views = RemoteViews(context.packageName, R.layout.schedule_widget)

            val bitmap: Bitmap? = ImageStore.loadBitmapFromCache(context, todayIndex)

            if (bitmap != null) {
                val rounded = bitmap.withRoundedCorners(48f)
                views.setImageViewBitmap(R.id.widgetImage, rounded)
                views.setViewVisibility(R.id.widgetImage, View.VISIBLE)
                views.setViewVisibility(R.id.widgetText, View.GONE)
            } else {
                views.setTextViewText(R.id.widgetText, dayName)
                views.setViewVisibility(R.id.widgetText, View.VISIBLE)
                views.setViewVisibility(R.id.widgetImage, View.GONE)
            }

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, showTomorrow: Boolean) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ScheduleWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            ids.forEach { id ->
                updateAppWidget(context, appWidgetManager, id, showTomorrow)
            }
        }

        fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ScheduleWidget::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, updateHour)
                set(Calendar.MINUTE, updateMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // ✅ Проверка на Android 12+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    val settingsIntent =
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}

// сегодня или завтра
fun getDayIndex(offset: Int = 0): Int {
    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
    return when (c.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 0
    }
}

// скругление картинки
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
