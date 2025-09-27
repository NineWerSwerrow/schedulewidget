package com.ninewer.schedulewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStore {
    private const val PREFS = "week_images"
    private const val KEY_PREFIX = "day_uri_"

    // сохранение uri и копия картинки в cacheDir
    fun saveUri(context: Context, dayIndex: Int, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREFIX + dayIndex, uri.toString()).apply()

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    saveBitmapToCache(context, dayIndex, bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // достаём Uri
    fun loadUri(context: Context, dayIndex: Int): Uri? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val str = prefs.getString(KEY_PREFIX + dayIndex, null) ?: return null
        return Uri.parse(str)
    }

    fun loadAll(context: Context): List<Uri?> {
        return (0 until WEEK_DAYS.size).map { loadUri(context, it) }
    }

    // сохранение картинки в cacheDir/day_X.png
    private fun saveBitmapToCache(context: Context, dayIndex: Int, bitmap: Bitmap) {
        val file = getCacheFile(context, dayIndex)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    // чтение картинки из кэша
    fun loadBitmapFromCache(context: Context, dayIndex: Int): Bitmap? {
        val file = getCacheFile(context, dayIndex)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private fun getCacheFile(context: Context, dayIndex: Int): File {
        return File(context.filesDir, "day_$dayIndex.png")
    }
}
