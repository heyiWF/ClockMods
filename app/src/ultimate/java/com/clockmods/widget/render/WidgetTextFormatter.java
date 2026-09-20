package com.clockmods.widget.render;
import com.clockmods.calendar.LunarCalendar;
import com.clockmods.pro.LunarAlmanac;
import com.clockmods.weather.WeatherModels;
import java.text.SimpleDateFormat;
import java.util.*;
public final class WidgetTextFormatter {
    private WidgetTextFormatter() { }
    public static String format(long now,TimeZone zone,Locale locale,String pattern) {
        SimpleDateFormat f=new SimpleDateFormat(pattern,locale); f.setTimeZone(zone); return f.format(new Date(now));
    }
    public static String formatGregorianDate(long now,TimeZone zone,Locale locale) {
        return format(now,zone,locale,"zh".equals(locale.getLanguage()) ? "M月d日" : "MMM d");
    }
    public static String formatWeekday(long now,TimeZone zone,Locale locale) { return format(now,zone,locale,"EEEE"); }
    public static String timePattern(boolean use24,boolean seconds) { return (use24 ? "HH:mm" : "h:mm")+(seconds ? ":ss" : "")+(use24 ? "" : " a"); }
    /**
     * Locale-aware 12/24-hour pattern for {@code TextClock}.
     *
     * <p>The day-period marker does not sit in the same place in every language: Chinese writes
     * "上午10:16" while English writes "10:16 AM". Asking ICU for the best pattern of the matching
     * skeleton ("Hm"/"hm", plus an "s" when seconds are shown) returns exactly the locale's
     * arrangement, including where the marker goes and whether it is separated by a space.
     */
    public static String timePattern(Locale locale,boolean use24,boolean seconds) {
        String skeleton=(use24 ? "H" : "h")+"m"+(seconds ? "s" : "");
        String pattern=android.text.format.DateFormat.getBestDateTimePattern(locale,skeleton);
        return pattern==null || pattern.trim().isEmpty() ? timePattern(use24,seconds) : pattern;
    }
    public static String formatLunar(long now,TimeZone zone,Locale locale) {
        Calendar cal=Calendar.getInstance(zone); cal.setTimeInMillis(now);
        // Match the application's almanac: lunar labels remain Chinese in every locale.
        return LunarCalendar.formatNatural(cal);
    }
    public static String formatHolidayAndSolarTerm(long now,TimeZone zone,Locale locale) {
        Calendar c=Calendar.getInstance(zone); c.setTimeInMillis(now);
        return join(LunarAlmanac.of(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).festivals().toArray(new String[0]));
    }
    public static String formatWeatherSummary(WeatherModels.WeatherDisplayData data,boolean includeLocation) {
        if(data==null) return "";
        return join(includeLocation ? WeatherModels.locationText(data.city,data.district) : "",data.text);
    }
    public static String withHolidayStatus(String festivals,String name,String status) {
        List<String> labels=new ArrayList<>();
        if(festivals!=null) labels.addAll(Arrays.asList(festivals.split(" · ")));
        labels.add(name); labels.add(status);
        return join(labels.toArray(new String[0]));
    }
    public static String join(String... values) {
        List<String> nonempty=new ArrayList<>();
        for(String value:values) if(value!=null && !value.trim().isEmpty() && !nonempty.contains(value.trim())) nonempty.add(value.trim());
        return String.join(" · ",nonempty);
    }
}
