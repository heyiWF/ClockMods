package com.clockmods.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.clockmods.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

open class BackgroundRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = ClockPreferences(appContext)
    open fun getCurrentColor() = preferences.getBackgroundColor(ContextCompat.getColor(appContext, R.color.clock_default_background))
    open fun getBackgroundMode() = preferences.getBackgroundMode()
    fun useColor() { preferences.setBackgroundMode(ClockPreferences.MODE_COLOR) }
    fun setCurrentColor(color: Int) { preferences.setBackgroundMode(ClockPreferences.MODE_COLOR); preferences.setBackgroundColor(color) }
    fun isDimBackground() = preferences.isDimBackground()
    fun setDimBackground(value: Boolean) = preferences.setDimBackground(value)
    fun isScheduleDimBackground() = preferences.isScheduleDimBackground()
    fun setScheduleDimBackground(value: Boolean) = preferences.setScheduleDimBackground(value)
    fun getDimStartMinutes() = preferences.getDimStartMinutes()
    fun setDimStartMinutes(value: Int) = preferences.setDimStartMinutes(value)
    fun getDimEndMinutes() = preferences.getDimEndMinutes()
    fun setDimEndMinutes(value: Int) = preferences.setDimEndMinutes(value)
    fun getTimeFontScale() = preferences.getTimeFontScale()
    fun setTimeFontScale(value: Float) = preferences.setTimeFontScale(value)
    fun getDateFontScale() = preferences.getDateFontScale()
    fun setDateFontScale(value: Float) = preferences.setDateFontScale(value)
    fun getTimeFontScale(scopeId: String?) = preferences.getTimeFontScale(scopeId)
    fun setTimeFontScale(scopeId: String?, value: Float) = preferences.setTimeFontScale(scopeId, value)
    fun getDateFontScale(scopeId: String?) = preferences.getDateFontScale(scopeId)
    fun setDateFontScale(scopeId: String?, value: Float) = preferences.setDateFontScale(scopeId, value)
    fun getSupportingFontScale(scopeId: String?) = preferences.getSupportingFontScale(scopeId)
    fun setSupportingFontScale(scopeId: String?, value: Float) = preferences.setSupportingFontScale(scopeId, value)
    open fun getTimeColor() = preferences.getTimeColor()
    fun setTimeColor(value: Int) = preferences.setTimeColor(value)
    fun getDateColor() = preferences.getDateColor()
    fun setDateColor(value: Int) = preferences.setDateColor(value)
    fun isShowStatusIcons() = preferences.isShowStatusIcons()
    fun setShowStatusIcons(value: Boolean) = preferences.setShowStatusIcons(value)
    fun getStatusIconScale() = preferences.getStatusIconScale()
    fun setStatusIconScale(value: Float) = preferences.setStatusIconScale(value)
    fun isBlinkColon() = preferences.isBlinkColon()
    fun setBlinkColon(value: Boolean) = preferences.setBlinkColon(value)
    fun isAnimateTimeChanges() = preferences.isAnimateTimeChanges()
    fun setAnimateTimeChanges(value: Boolean) = preferences.setAnimateTimeChanges(value)
    fun getTimeTransition() = preferences.getTimeTransition()
    fun setTimeTransition(value: String?) = preferences.setTimeTransition(value)
    fun isHourlyChimeEnabled() = preferences.isHourlyChimeEnabled()
    fun setHourlyChimeEnabled(value: Boolean) = preferences.setHourlyChimeEnabled(value)
    fun isHalfHourChimeEnabled() = preferences.isHalfHourChimeEnabled()
    fun setHalfHourChimeEnabled(value: Boolean) = preferences.setHalfHourChimeEnabled(value)
    fun isHourlyChimeQuietEnabled() = preferences.isHourlyChimeQuietEnabled()
    fun setHourlyChimeQuietEnabled(value: Boolean) = preferences.setHourlyChimeQuietEnabled(value)
    fun getHourlyChimeQuietStart() = preferences.getHourlyChimeQuietStart()
    fun setHourlyChimeQuietStart(value: Int) = preferences.setHourlyChimeQuietStart(value)
    fun getHourlyChimeQuietEnd() = preferences.getHourlyChimeQuietEnd()
    fun setHourlyChimeQuietEnd(value: Int) = preferences.setHourlyChimeQuietEnd(value)
    fun isBoldText() = preferences.isBoldText()
    fun setBoldText(value: Boolean) = preferences.setBoldText(value)
    fun getFontFamily() = preferences.getFontFamily()
    fun setFontFamily(value: String?) = preferences.setFontFamily(value)
    fun getFontFamily(scopeId: String?) = preferences.getFontFamily(scopeId)
    fun setFontFamily(scopeId: String?, value: String?) = preferences.setFontFamily(scopeId, value)
    fun getFontWeight(scopeId: String?) = preferences.getFontWeight(scopeId)
    fun setFontWeight(scopeId: String?, value: Int) = preferences.setFontWeight(scopeId, value)
    fun isShowSeconds() = preferences.isShowSeconds()
    fun setShowSeconds(value: Boolean) = preferences.setShowSeconds(value)
    fun isShowLunar() = preferences.isShowLunar()
    fun setShowLunar(value: Boolean) = preferences.setShowLunar(value)
    fun isAutoStart() = preferences.isAutoStart()
    fun setAutoStart(value: Boolean) = preferences.setAutoStart(value)
    fun getCalendarWeekStart() = preferences.getCalendarWeekStart()
    fun setCalendarWeekStart(value: Int) = preferences.setCalendarWeekStart(value)
    fun isCalendarHighlightWeekends() = preferences.isCalendarHighlightWeekends()
    fun setCalendarHighlightWeekends(value: Boolean) = preferences.setCalendarHighlightWeekends(value)
    fun getCalendarTheme() = preferences.getCalendarTheme()
    fun setCalendarTheme(value: String?) = preferences.setCalendarTheme(value)
    fun isSmallSeconds() = preferences.isSmallSeconds()
    fun setSmallSeconds(value: Boolean) = preferences.setSmallSeconds(value)
    fun isPortraitStacked() = preferences.isPortraitStacked()
    fun setPortraitStacked(value: Boolean) = preferences.setPortraitStacked(value)
    fun isDateLunarDualLine() = preferences.isDateLunarDualLine()
    fun setDateLunarDualLine(value: Boolean) = preferences.setDateLunarDualLine(value)
    fun isUse24Hour() = preferences.isUse24Hour()
    fun setUse24Hour(value: Boolean) = preferences.setUse24Hour(value)
    fun isClockUseEnglish() = preferences.isClockUseEnglish()
    fun setClockUseEnglish(value: Boolean) = preferences.setClockUseEnglish(value)
    fun getClockLanguage() = preferences.getClockLanguage()
    fun setClockLanguage(value: String?) = preferences.setClockLanguage(value)
    fun getCustomMessage() = preferences.getCustomMessage()
    fun setCustomMessage(value: String?) = preferences.setCustomMessage(value)
    fun getWeatherTemperatureUnit() = preferences.getWeatherTemperatureUnit()
    fun setWeatherTemperatureUnit(value: String?) = preferences.setWeatherTemperatureUnit(value)
    fun getDatePatternCn() = preferences.getDatePatternCn()
    fun setDatePatternCn(value: String?) = preferences.setDatePatternCn(value)
    fun getDatePatternEn() = preferences.getDatePatternEn()
    fun setDatePatternEn(value: String?) = preferences.setDatePatternEn(value)
    fun getDateCore(english: Boolean) = preferences.getDateCore(english)
    fun getDateCombo(english: Boolean) = preferences.getDateCombo(english)
    fun isDateCustomEnabled(english: Boolean) = preferences.isDateCustomEnabled(english)
    fun getDateCustomText(english: Boolean) = preferences.getDateCustomText(english)
    fun setDateFormatState(english: Boolean, core: String?, combo: String?, customEnabled: Boolean, customText: String?) = preferences.setDateFormatState(english, core, combo, customEnabled, customText)
    fun getScreenOrientation() = preferences.getScreenOrientation()
    fun setScreenOrientation(value: Int) = preferences.setScreenOrientation(value)
    fun isUseNetworkTime() = preferences.isUseNetworkTime()
    fun setUseNetworkTime(value: Boolean) = preferences.setUseNetworkTime(value)
    fun getSyncIntervalMinutes() = preferences.getSyncIntervalMinutes()
    fun setSyncIntervalMinutes(value: Int) = preferences.setSyncIntervalMinutes(value)
    fun getTimeZoneId() = preferences.getTimeZoneId()
    fun setTimeZoneId(value: String?) = preferences.setTimeZoneId(value)
    fun isWeatherEnabled() = preferences.isWeatherEnabled()
    fun setWeatherEnabled(value: Boolean) = preferences.setWeatherEnabled(value)
    fun isWeatherDetailed() = preferences.isWeatherDetailed()
    fun setWeatherDetailed(value: Boolean) = preferences.setWeatherDetailed(value)
    fun getWeatherIntervalMinutes() = preferences.getWeatherIntervalMinutes()
    fun setWeatherIntervalMinutes(value: Int) = preferences.setWeatherIntervalMinutes(value)
    fun getWeatherLocationMode() = preferences.getWeatherLocationMode()
    fun setWeatherLocationMode(value: String?) = preferences.setWeatherLocationMode(value)
    fun getWeatherLocationId() = preferences.getWeatherLocationId()
    fun getWeatherProvince() = preferences.getWeatherProvince()
    fun getWeatherCity() = preferences.getWeatherCity()
    fun getWeatherDistrict() = preferences.getWeatherDistrict()
    fun getWeatherLatitude() = preferences.getWeatherLatitude()
    fun getWeatherLongitude() = preferences.getWeatherLongitude()
    fun setManualWeatherLocation(locationId: String?, province: String?, city: String?, district: String?) = preferences.setManualWeatherLocation(locationId, province, city, district)
    fun setManualWeatherLocation(locationId: String?, province: String?, city: String?, district: String?, latitude: Double, longitude: Double) = preferences.setManualWeatherLocation(locationId, province, city, district, latitude, longitude)
    fun isWeatherIconFill() = preferences.isWeatherIconFill()
    fun setWeatherIconFill(value: Boolean) = preferences.setWeatherIconFill(value)
    fun isWeatherIconDynamicColor() = preferences.isWeatherIconDynamicColor()
    fun setWeatherIconDynamicColor(value: Boolean) = preferences.setWeatherIconDynamicColor(value)
    fun restoreDefaults() = preferences.restoreDefaults()
    fun hasImage() = imageFile.isFile
    fun useImage() { if (hasImage()) preferences.setBackgroundMode(ClockPreferences.MODE_IMAGE) }

    @Throws(IOException::class)
    fun saveImage(inputStream: InputStream, displayLongSide: Int) {
        val tempFile = File(appContext.filesDir, TEMP_FILE_NAME); val imageFile = imageFile; val backupFile = File(appContext.filesDir, BACKUP_FILE_NAME)
        copyToFile(inputStream, tempFile); compressForDisplay(tempFile, displayLongSide)
        if (backupFile.exists() && !backupFile.delete()) { tempFile.delete(); throw IOException("Unable to prepare background backup") }
        if (imageFile.exists() && !imageFile.renameTo(backupFile)) { tempFile.delete(); throw IOException("Unable to preserve current background image") }
        if (!tempFile.renameTo(imageFile)) { tempFile.delete(); if (backupFile.exists()) backupFile.renameTo(imageFile); throw IOException("Unable to commit background image") }
        backupFile.delete(); preferences.setBackgroundMode(ClockPreferences.MODE_IMAGE)
    }

    @Throws(IOException::class)
    fun loadImage(targetWidth: Int, targetHeight: Int): Bitmap? {
        val file = imageFile; if (!file.isFile || targetWidth <= 0 || targetHeight <= 0) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; decodeFile(file, bounds); if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight); inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = decodeFile(file, options) ?: return null; val rotation = readRotation(file.absolutePath); if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }; val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true); if (rotated !== bitmap) bitmap.recycle(); return rotated
    }

    private val imageFile get() = File(appContext.filesDir, IMAGE_FILE_NAME)
    companion object {
        private const val IMAGE_FILE_NAME = "clock_background.image"; private const val TEMP_FILE_NAME = "clock_background.tmp"; private const val BACKUP_FILE_NAME = "clock_background.backup"
        @JvmStatic fun calculateInSampleSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Int { var sample = 1; while (sourceWidth / (sample * 2) >= targetWidth && sourceHeight / (sample * 2) >= targetHeight) sample *= 2; return sample }
        @JvmStatic fun calculateCompressedShortSide(sourceWidth: Int, sourceHeight: Int, displayLongSide: Int): Int { if (sourceWidth <= 0 || sourceHeight <= 0 || displayLongSide <= 0) return 0; return minOf(minOf(sourceWidth, sourceHeight), displayLongSide) }
        private fun compressForDisplay(file: File, displayLongSide: Int) {
            if (displayLongSide <= 0) return
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; decodeFile(file, bounds); if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unsupported background image")
            val rotation = readRotation(file.absolutePath); val width = if (rotation == 90 || rotation == 270) bounds.outHeight else bounds.outWidth; val height = if (rotation == 90 || rotation == 270) bounds.outWidth else bounds.outHeight; val targetShort = calculateCompressedShortSide(width, height, displayLongSide); if (targetShort <= 0 || minOf(width, height) <= targetShort) return
            val scale = targetShort.toFloat() / minOf(width, height); val targetWidth = maxOf(1, kotlin.math.round(width * scale).toInt()); val targetHeight = maxOf(1, kotlin.math.round(height * scale).toInt()); val options = BitmapFactory.Options().apply { inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, if (rotation == 90 || rotation == 270) targetHeight else targetWidth, if (rotation == 90 || rotation == 270) targetWidth else targetHeight); inPreferredConfig = Bitmap.Config.ARGB_8888 }; var bitmap = decodeFile(file, options) ?: throw IOException("Unable to decode background image")
            try { if (rotation != 0) { val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation.toFloat()) }, true); if (rotated !== bitmap) { bitmap.recycle(); bitmap = rotated } }; val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true); if (scaled !== bitmap) { bitmap.recycle(); bitmap = scaled }; FileOutputStream(file, false).use { output -> if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw IOException("Unable to compress background image"); output.flush() } } finally { bitmap.recycle() }
        }
        private fun copyToFile(input: InputStream, outputFile: File) { FileOutputStream(outputFile).use { output -> input.copyTo(output, 16 * 1024); output.flush() } }
        private fun decodeFile(file: File, options: BitmapFactory.Options): Bitmap? = FileInputStream(file).use { BitmapFactory.decodeStream(it, null, options) }
        private fun readRotation(path: String): Int = try { when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) { ExifInterface.ORIENTATION_ROTATE_90 -> 90; ExifInterface.ORIENTATION_ROTATE_180 -> 180; ExifInterface.ORIENTATION_ROTATE_270 -> 270; else -> 0 } } catch (_: IOException) { 0 }
    }
}
