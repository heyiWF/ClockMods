package com.clockmods.background;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/** Independent East Asian family and weight for each theme and interface language. */
public final class ScriptFontPreferences {
    private final SharedPreferences preferences;
    public ScriptFontPreferences(SharedPreferences preferences) { this.preferences = preferences; }
    private String key(String scope, String language) {
        String normalized = scope == null ? "" : scope.trim();
        return "script_font__" + normalized.substring(0, Math.min(160, normalized.length())) + "__" + language;
    }
    public String family(String scope, String language) {
        String id = preferences.getString(key(scope, language), ClockPreferences.FONT_SYSTEM);
        return supports(id, language) ? id : ClockPreferences.FONT_SYSTEM;
    }
    public int weight(String scope, String language) {
        return FontCatalog.optionForLanguage(family(scope, language), language).nearestWeight(preferences.getInt(key(scope, language) + "__weight", 400));
    }
    public void family(String scope, String language, String id) {
        if (supports(id, language)) preferences.edit().putString(key(scope, language), id).apply();
    }
    public void weight(String scope, String language, int weight) {
        preferences.edit().putInt(key(scope, language) + "__weight", FontCatalog.optionForLanguage(family(scope, language), language).nearestWeight(weight)).apply();
    }
    public static List<FontCatalog.FontOption> options(String language) {
        List<FontCatalog.FontOption> result = new ArrayList<>();
        for (FontCatalog.FontOption option : FontCatalog.options()) if (supports(option.id, language)) result.add(FontCatalog.optionForLanguage(option.id, language));
        return result;
    }
    public static boolean supports(String id, String language) {
        if (ClockPreferences.FONT_SYSTEM.equals(id)) return true;
        UserFontStore.Family family = UserFontStore.family(id);
        if (family == null) return "en".equals(language) && FontCatalog.isAvailable(id);
        for (UserFontStore.Face face : family.faces) if (supportsFace(face.font, language)) return true;
        return false;
    }
    public static boolean supportsFace(OpenTypeFont font, String language) {
        String probe = "en".equals(language) ? "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                : "ja".equals(language) ? "日本語年月日時分設定あいうえおアイウエオ"
                : "ko".equals(language) ? "시간날짜설정월화수목금토일"
                : "zh-Hant".equals(language) ? "年月日星期時間日期設置宜忌農曆" : "年月日星期时间日期设置宜忌农历";
        return font.coverage(probe) == 100;
    }
    public static boolean hasSeparateScript(String language) { return !"en".equals(language); }
}
