package com.clockmods.background;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import com.clockmods.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BackgroundRepository {
    private static final String IMAGE_FILE_NAME = "clock_background.image";
    private static final String TEMP_FILE_NAME = "clock_background.tmp";
    private static final String BACKUP_FILE_NAME = "clock_background.backup";

    private final Context appContext;
    private final ClockPreferences preferences;

    public BackgroundRepository(Context context) {
        appContext = context.getApplicationContext();
        preferences = new ClockPreferences(appContext);
    }

    public int getCurrentColor() {
        return preferences.getBackgroundColor(appContext.getResources().getColor(R.color.clock_default_background));
    }

    public String getBackgroundMode() {
        return preferences.getBackgroundMode();
    }

    public void setCurrentColor(int color) {
        preferences.setBackgroundMode(ClockPreferences.MODE_COLOR);
        preferences.setBackgroundColor(color);
    }

    public boolean isDimBackground() {
        return preferences.isDimBackground();
    }

    public void setDimBackground(boolean dimBackground) {
        preferences.setDimBackground(dimBackground);
    }

    public boolean isScheduleDimBackground() {
        return preferences.isScheduleDimBackground();
    }

    public void setScheduleDimBackground(boolean schedule) {
        preferences.setScheduleDimBackground(schedule);
    }

    public int getDimStartMinutes() {
        return preferences.getDimStartMinutes();
    }

    public void setDimStartMinutes(int minutes) {
        preferences.setDimStartMinutes(minutes);
    }

    public int getDimEndMinutes() {
        return preferences.getDimEndMinutes();
    }

    public void setDimEndMinutes(int minutes) {
        preferences.setDimEndMinutes(minutes);
    }

    public float getTimeFontScale() {
        return preferences.getTimeFontScale();
    }

    public void setTimeFontScale(float scale) {
        preferences.setTimeFontScale(scale);
    }

    public float getDateFontScale() {
        return preferences.getDateFontScale();
    }

    public void setDateFontScale(float scale) {
        preferences.setDateFontScale(scale);
    }

    public int getTimeColor() {
        return preferences.getTimeColor();
    }

    public void setTimeColor(int color) {
        preferences.setTimeColor(color);
    }

    public int getDateColor() {
        return preferences.getDateColor();
    }

    public void setDateColor(int color) {
        preferences.setDateColor(color);
    }

    public boolean isShowStatusIcons() {
        return preferences.isShowStatusIcons();
    }

    public void setShowStatusIcons(boolean show) {
        preferences.setShowStatusIcons(show);
    }

    public boolean isBlinkColon() {
        return preferences.isBlinkColon();
    }

    public void setBlinkColon(boolean blinkColon) {
        preferences.setBlinkColon(blinkColon);
    }

    public boolean isAnimateTimeChanges() {
        return preferences.isAnimateTimeChanges();
    }

    public void setAnimateTimeChanges(boolean animate) {
        preferences.setAnimateTimeChanges(animate);
    }

    public boolean isBoldText() {
        return preferences.isBoldText();
    }

    public void setBoldText(boolean boldText) {
        preferences.setBoldText(boldText);
    }

    public boolean isShowSeconds() {
        return preferences.isShowSeconds();
    }

    public void setShowSeconds(boolean showSeconds) {
        preferences.setShowSeconds(showSeconds);
    }

    public boolean isShowLunar() {
        return preferences.isShowLunar();
    }

    public void setShowLunar(boolean showLunar) {
        preferences.setShowLunar(showLunar);
    }

    public boolean isSmallSeconds() {
        return preferences.isSmallSeconds();
    }

    public void setSmallSeconds(boolean smallSeconds) {
        preferences.setSmallSeconds(smallSeconds);
    }

    public boolean isUse24Hour() {
        return preferences.isUse24Hour();
    }

    public void setUse24Hour(boolean use24Hour) {
        preferences.setUse24Hour(use24Hour);
    }

    public boolean isClockUseEnglish() {
        return preferences.isClockUseEnglish();
    }

    public void setClockUseEnglish(boolean useEnglish) {
        preferences.setClockUseEnglish(useEnglish);
    }

    public boolean isUseNetworkTime() {
        return preferences.isUseNetworkTime();
    }

    public void setUseNetworkTime(boolean useNetworkTime) {
        preferences.setUseNetworkTime(useNetworkTime);
    }

    public int getSyncIntervalMinutes() {
        return preferences.getSyncIntervalMinutes();
    }

    public void setSyncIntervalMinutes(int minutes) {
        preferences.setSyncIntervalMinutes(minutes);
    }

    public String getTimeZoneId() {
        return preferences.getTimeZoneId();
    }

    public void setTimeZoneId(String timeZoneId) {
        preferences.setTimeZoneId(timeZoneId);
    }

    /** Resets background, font size and font colors to defaults (black background, white text). */
    public void restoreDefaults() {
        preferences.restoreDefaults();
    }

    public boolean hasImage() {
        return getImageFile().isFile();
    }

    public void useImage() {
        if (hasImage()) {
            preferences.setBackgroundMode(ClockPreferences.MODE_IMAGE);
        }
    }

    public void saveImage(InputStream inputStream) throws IOException {
        File tempFile = new File(appContext.getFilesDir(), TEMP_FILE_NAME);
        File imageFile = getImageFile();
        File backupFile = new File(appContext.getFilesDir(), BACKUP_FILE_NAME);
        copyToFile(inputStream, tempFile);

        if (backupFile.exists() && !backupFile.delete()) {
            tempFile.delete();
            throw new IOException("Unable to prepare background backup");
        }
        if (imageFile.exists() && !imageFile.renameTo(backupFile)) {
            tempFile.delete();
            throw new IOException("Unable to preserve current background image");
        }
        if (!tempFile.renameTo(imageFile)) {
            tempFile.delete();
            if (backupFile.exists()) {
                backupFile.renameTo(imageFile);
            }
            throw new IOException("Unable to commit background image");
        }
        backupFile.delete();
        preferences.setBackgroundMode(ClockPreferences.MODE_IMAGE);
    }

    public Bitmap loadImage(int targetWidth, int targetHeight) throws IOException {
        File imageFile = getImageFile();
        if (!imageFile.isFile() || targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        decodeFile(imageFile, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = decodeFile(imageFile, options);
        if (bitmap == null) {
            return null;
        }

        int rotation = readRotation(imageFile.getAbsolutePath());
        if (rotation == 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    static int calculateInSampleSize(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        int sampleSize = 1;
        while (sourceWidth / (sampleSize * 2) >= targetWidth
                && sourceHeight / (sampleSize * 2) >= targetHeight) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private File getImageFile() {
        return new File(appContext.getFilesDir(), IMAGE_FILE_NAME);
    }

    private static void copyToFile(InputStream inputStream, File outputFile) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            outputStream.flush();
        }
    }

    private static Bitmap decodeFile(File file, BitmapFactory.Options options) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(inputStream, null, options);
        }
    }

    private static int readRotation(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
        } catch (IOException ignored) {
            return 0;
        }
        return 0;
    }
}