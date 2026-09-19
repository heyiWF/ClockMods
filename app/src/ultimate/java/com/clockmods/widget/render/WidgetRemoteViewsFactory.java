package com.clockmods.widget.render;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Icon;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import com.clockmods.R;
import com.clockmods.background.ClockPreferences;
import com.clockmods.calendar.HolidayRepository;
import com.clockmods.weather.*;
import com.clockmods.widget.model.*;
import com.clockmods.widget.store.WidgetConfigStore;
import java.util.*;
import static com.clockmods.widget.render.WidgetPendingIntentFactory.Action.*;
/** Only this class owns widget view IDs. No clock is painted into a bitmap. */
public final class WidgetRemoteViewsFactory {
    private static volatile HolidayRepository holidays;

    /**
     * {@code [tier][font column]} for each widget kind. Columns 0 to 2 are the theme's structural
     * variants (sans, serif, monospace); the rest are the families the app offers, in the app's
     * order and matching {@link WidgetFontRegistry#COLUMNS}. Every column is generated from the
     * same skeleton, so a structural change has to be applied to all of them at once.
     */
    private static final int[][] DIGITAL_LAYOUTS={
        {R.layout.widget_digital_compact,
         R.layout.widget_digital_compact_serif,
         R.layout.widget_digital_compact_mono,
         R.layout.widget_digital_compact_roboto,
         R.layout.widget_digital_compact_gsansdisplay,
         R.layout.widget_digital_compact_gsanstext,
         R.layout.widget_digital_compact_sfprodisplay,
         R.layout.widget_digital_compact_sfprorounded,
         R.layout.widget_digital_compact_inter,
         R.layout.widget_digital_compact_lato,
         R.layout.widget_digital_compact_lora,
         R.layout.widget_digital_compact_notosans,
         R.layout.widget_digital_compact_bitcount},
        {R.layout.widget_digital_wide,
         R.layout.widget_digital_wide_serif,
         R.layout.widget_digital_wide_mono,
         R.layout.widget_digital_wide_roboto,
         R.layout.widget_digital_wide_gsansdisplay,
         R.layout.widget_digital_wide_gsanstext,
         R.layout.widget_digital_wide_sfprodisplay,
         R.layout.widget_digital_wide_sfprorounded,
         R.layout.widget_digital_wide_inter,
         R.layout.widget_digital_wide_lato,
         R.layout.widget_digital_wide_lora,
         R.layout.widget_digital_wide_notosans,
         R.layout.widget_digital_wide_bitcount},
        {R.layout.widget_digital_tall,
         R.layout.widget_digital_tall_serif,
         R.layout.widget_digital_tall_mono,
         R.layout.widget_digital_tall_roboto,
         R.layout.widget_digital_tall_gsansdisplay,
         R.layout.widget_digital_tall_gsanstext,
         R.layout.widget_digital_tall_sfprodisplay,
         R.layout.widget_digital_tall_sfprorounded,
         R.layout.widget_digital_tall_inter,
         R.layout.widget_digital_tall_lato,
         R.layout.widget_digital_tall_lora,
         R.layout.widget_digital_tall_notosans,
         R.layout.widget_digital_tall_bitcount}};

    private static final int[][] ANALOG_LAYOUTS={
        {R.layout.widget_analog_compact,
         R.layout.widget_analog_compact_serif,
         R.layout.widget_analog_compact_mono,
         R.layout.widget_analog_compact_roboto,
         R.layout.widget_analog_compact_gsansdisplay,
         R.layout.widget_analog_compact_gsanstext,
         R.layout.widget_analog_compact_sfprodisplay,
         R.layout.widget_analog_compact_sfprorounded,
         R.layout.widget_analog_compact_inter,
         R.layout.widget_analog_compact_lato,
         R.layout.widget_analog_compact_lora,
         R.layout.widget_analog_compact_notosans,
         R.layout.widget_analog_compact_bitcount},
        {R.layout.widget_analog_medium,
         R.layout.widget_analog_medium_serif,
         R.layout.widget_analog_medium_mono,
         R.layout.widget_analog_medium_roboto,
         R.layout.widget_analog_medium_gsansdisplay,
         R.layout.widget_analog_medium_gsanstext,
         R.layout.widget_analog_medium_sfprodisplay,
         R.layout.widget_analog_medium_sfprorounded,
         R.layout.widget_analog_medium_inter,
         R.layout.widget_analog_medium_lato,
         R.layout.widget_analog_medium_lora,
         R.layout.widget_analog_medium_notosans,
         R.layout.widget_analog_medium_bitcount},
        {R.layout.widget_analog_large,
         R.layout.widget_analog_large_serif,
         R.layout.widget_analog_large_mono,
         R.layout.widget_analog_large_roboto,
         R.layout.widget_analog_large_gsansdisplay,
         R.layout.widget_analog_large_gsanstext,
         R.layout.widget_analog_large_sfprodisplay,
         R.layout.widget_analog_large_sfprorounded,
         R.layout.widget_analog_large_inter,
         R.layout.widget_analog_large_lato,
         R.layout.widget_analog_large_lora,
         R.layout.widget_analog_large_notosans,
         R.layout.widget_analog_large_bitcount}};

    private static final int[][] WEATHER_LAYOUTS={
        {R.layout.widget_weather_compact,
         R.layout.widget_weather_compact_serif,
         R.layout.widget_weather_compact_mono,
         R.layout.widget_weather_compact_roboto,
         R.layout.widget_weather_compact_gsansdisplay,
         R.layout.widget_weather_compact_gsanstext,
         R.layout.widget_weather_compact_sfprodisplay,
         R.layout.widget_weather_compact_sfprorounded,
         R.layout.widget_weather_compact_inter,
         R.layout.widget_weather_compact_lato,
         R.layout.widget_weather_compact_lora,
         R.layout.widget_weather_compact_notosans,
         R.layout.widget_weather_compact_bitcount},
        {R.layout.widget_weather_wide,
         R.layout.widget_weather_wide_serif,
         R.layout.widget_weather_wide_mono,
         R.layout.widget_weather_wide_roboto,
         R.layout.widget_weather_wide_gsansdisplay,
         R.layout.widget_weather_wide_gsanstext,
         R.layout.widget_weather_wide_sfprodisplay,
         R.layout.widget_weather_wide_sfprorounded,
         R.layout.widget_weather_wide_inter,
         R.layout.widget_weather_wide_lato,
         R.layout.widget_weather_wide_lora,
         R.layout.widget_weather_wide_notosans,
         R.layout.widget_weather_wide_bitcount},
        {R.layout.widget_weather_large,
         R.layout.widget_weather_large_serif,
         R.layout.widget_weather_large_mono,
         R.layout.widget_weather_large_roboto,
         R.layout.widget_weather_large_gsansdisplay,
         R.layout.widget_weather_large_gsanstext,
         R.layout.widget_weather_large_sfprodisplay,
         R.layout.widget_weather_large_sfprorounded,
         R.layout.widget_weather_large_inter,
         R.layout.widget_weather_large_lato,
         R.layout.widget_weather_large_lora,
         R.layout.widget_weather_large_notosans,
         R.layout.widget_weather_large_bitcount}};

    private static final int[][] CALENDAR_LAYOUTS={
        {R.layout.widget_calendar_compact,
         R.layout.widget_calendar_compact_serif,
         R.layout.widget_calendar_compact_mono,
         R.layout.widget_calendar_compact_roboto,
         R.layout.widget_calendar_compact_gsansdisplay,
         R.layout.widget_calendar_compact_gsanstext,
         R.layout.widget_calendar_compact_sfprodisplay,
         R.layout.widget_calendar_compact_sfprorounded,
         R.layout.widget_calendar_compact_inter,
         R.layout.widget_calendar_compact_lato,
         R.layout.widget_calendar_compact_lora,
         R.layout.widget_calendar_compact_notosans,
         R.layout.widget_calendar_compact_bitcount},
        {R.layout.widget_calendar_wide,
         R.layout.widget_calendar_wide_serif,
         R.layout.widget_calendar_wide_mono,
         R.layout.widget_calendar_wide_roboto,
         R.layout.widget_calendar_wide_gsansdisplay,
         R.layout.widget_calendar_wide_gsanstext,
         R.layout.widget_calendar_wide_sfprodisplay,
         R.layout.widget_calendar_wide_sfprorounded,
         R.layout.widget_calendar_wide_inter,
         R.layout.widget_calendar_wide_lato,
         R.layout.widget_calendar_wide_lora,
         R.layout.widget_calendar_wide_notosans,
         R.layout.widget_calendar_wide_bitcount},
        {R.layout.widget_calendar_large,
         R.layout.widget_calendar_large_serif,
         R.layout.widget_calendar_large_mono,
         R.layout.widget_calendar_large_roboto,
         R.layout.widget_calendar_large_gsansdisplay,
         R.layout.widget_calendar_large_gsanstext,
         R.layout.widget_calendar_large_sfprodisplay,
         R.layout.widget_calendar_large_sfprorounded,
         R.layout.widget_calendar_large_inter,
         R.layout.widget_calendar_large_lato,
         R.layout.widget_calendar_large_lora,
         R.layout.widget_calendar_large_notosans,
         R.layout.widget_calendar_large_bitcount}};

    private WidgetRemoteViewsFactory() { }
    private static HolidayRepository holidays(Context c) {
        if (holidays == null) synchronized (WidgetRemoteViewsFactory.class) {
            if (holidays == null) holidays = new HolidayRepository(c.getApplicationContext());
        }
        return holidays;
    }
    public static RemoteViews createResponsive(Context c,int id,WidgetKind kind) {
        WidgetConfig config=new WidgetConfigStore(c).getOrDefault(id,kind);
        Map<SizeF,RemoteViews> sizes=new LinkedHashMap<>();
        float[][] bounds={{80,56},{180,100},{260,100},{180,180},{260,180}};
        for(float[] b:bounds) sizes.put(new SizeF(b[0],b[1]),create(c,config,WidgetSizeClassResolver.resolve(b[0],b[1])));
        return new RemoteViews(sizes);
    }
    public static RemoteViews createForSize(Context c,int id,WidgetKind kind,WidgetSizeClass size) {
        return create(c,new WidgetConfigStore(c).getOrDefault(id,kind),size);
    }
    public static RemoteViews create(Context c,WidgetConfig config,WidgetSizeClass size) {
        WidgetThemeSpec theme=WidgetThemeRegistry.resolve(c,config.themeId);
        int tier=(size==WidgetSizeClass.COMPACT || size==WidgetSizeClass.SMALL) ? 0 : size==WidgetSizeClass.WIDE ? 1 : 2;
        // The theme picks a structural variant; an explicit per-instance font overrides that column.
        int themeColumn=theme.fontLayoutVariant==WidgetThemeSpec.Font.SERIF ? 1 : theme.fontLayoutVariant==WidgetThemeSpec.Font.MONOSPACE ? 2 : 0;
        int column=WidgetFontRegistry.columnOf(config.fontId,themeColumn);
        int[][] layouts=config.kind==WidgetKind.DIGITAL ? DIGITAL_LAYOUTS : config.kind==WidgetKind.ANALOG ? ANALOG_LAYOUTS
                : config.kind==WidgetKind.WEATHER ? WEATHER_LAYOUTS : CALENDAR_LAYOUTS;
        RemoteViews v=new RemoteViews(c.getPackageName(),layouts[tier][column]);
        int primary=config.themeId.equals("transparent.clean") && config.darkText ? 0xff16212c : theme.primaryTextColor;
        int secondary=config.themeId.equals("transparent.clean") ? primary : theme.secondaryTextColor;
        v.setImageViewResource(R.id.widget_background,theme.backgroundDrawableRes);
        v.setInt(R.id.widget_background,"setImageAlpha",config.backgroundAlpha);
        v.setInt(R.id.widget_settings,"setColorFilter",primary);
        v.setOnClickPendingIntent(R.id.widget_root,WidgetPendingIntentFactory.create(c,config.appWidgetId,WidgetPendingIntentFactory.tap(config.tapAction)));
        v.setOnClickPendingIntent(R.id.widget_settings,WidgetPendingIntentFactory.create(c,config.appWidgetId,CONFIG));
        long now=System.currentTimeMillis(); TimeZone zone=config.zone();
        Locale locale=c.getResources().getConfiguration().getLocales().get(0);
        boolean compact=size==WidgetSizeClass.COMPACT;
        String date=WidgetTextFormatter.join(config.showDate ? WidgetTextFormatter.formatGregorianDate(now,zone,locale) : "",
                config.showWeekday ? WidgetTextFormatter.formatWeekday(now,zone,locale) : "");
        text(v,R.id.widget_date,date,secondary,12*config.textScale,!date.isEmpty() && (!compact || config.kind==WidgetKind.CALENDAR || config.kind==WidgetKind.DIGITAL));
        String lunar=config.showLunar ? WidgetTextFormatter.formatLunar(now,zone,locale) : "";
        text(v,R.id.widget_lunar,lunar,secondary,12*config.textScale,config.showLunar && !compact && (tier==2 || config.kind==WidgetKind.CALENDAR));
        String holiday=WidgetTextFormatter.formatHolidayAndSolarTerm(now,zone,locale);
        HolidayRepository.HolidayStatus day=holidays(c).statusOn(WidgetTextFormatter.format(now,zone,Locale.ROOT,"yyyy-MM-dd"));
        if(day!=null) holiday=WidgetTextFormatter.join(holiday,day.name+" "+c.getString(day.offDay ? R.string.widget_rest : R.string.widget_work));
        text(v,R.id.widget_holiday,holiday,secondary,12*config.textScale,config.showLunar && tier==2 && !holiday.isEmpty());
        text(v,R.id.widget_zone,zone.getID().replace('_',' '),secondary,11*config.textScale,config.showLocation && tier==2 && config.kind!=WidgetKind.WEATHER);
        for(int view:new int[]{R.id.widget_date,R.id.widget_lunar,R.id.widget_holiday}) v.setOnClickPendingIntent(view,WidgetPendingIntentFactory.create(c,config.appWidgetId,CALENDAR));
        if(config.kind==WidgetKind.DIGITAL || config.kind==WidgetKind.WEATHER) {
            String twelve=WidgetTextFormatter.timePattern(locale,config.useSystemTimeFormat ? false : config.use24Hour,config.showSeconds);
            String twentyFour=WidgetTextFormatter.timePattern(locale,config.useSystemTimeFormat || config.use24Hour,config.showSeconds);
            v.setCharSequence(R.id.widget_time,"setFormat12Hour",twelve);
            v.setCharSequence(R.id.widget_time,"setFormat24Hour",twentyFour);
            v.setString(R.id.widget_time,"setTimeZone",config.useSystemTimeZone ? null : config.timeZoneId);
            v.setTextColor(R.id.widget_time,primary);
            // TextClock autosizes within this scaled box; RemoteViews textSize alone is ignored
            // by an autosizing TextView. The box also bounds long 12-hour/seconds strings.
            float clockHeight=config.kind==WidgetKind.WEATHER ? (compact ? 28 : tier==2 ? 48 : 36) : (compact ? 32 : tier==2 ? 68 : 48);
            // The compact digital card is the only case where the clock keeps its layout weight, so
            // it needs no explicit height. Insets always come from the layout resource now, which
            // keeps every size class clear of the 28dp system corner radius.
            if(!(compact && config.kind==WidgetKind.DIGITAL)) v.setViewLayoutHeight(R.id.widget_time,clockHeight*config.textScale,TypedValue.COMPLEX_UNIT_DIP);
            v.setOnClickPendingIntent(R.id.widget_time,WidgetPendingIntentFactory.create(c,config.appWidgetId,CLOCK));
        } else if(config.kind==WidgetKind.ANALOG) {
            int dialIndex=config.themeId.equals("paper.warm") ? 2 : config.themeId.equals("neon.night") ? 3 : config.themeId.equals("instrument.dark") ? 1 : 0;
            int[] dials={R.drawable.widget_analog_0_dial,R.drawable.widget_analog_1_dial,R.drawable.widget_analog_2_dial,R.drawable.widget_analog_3_dial};
            int[] hours={R.drawable.widget_analog_0_hour,R.drawable.widget_analog_1_hour,R.drawable.widget_analog_2_hour,R.drawable.widget_analog_3_hour};
            int[] minutes={R.drawable.widget_analog_0_minute,R.drawable.widget_analog_1_minute,R.drawable.widget_analog_2_minute,R.drawable.widget_analog_3_minute};
            v.setIcon(R.id.widget_analog,"setDial",Icon.createWithResource(c,dials[dialIndex]));
            v.setIcon(R.id.widget_analog,"setHourHand",Icon.createWithResource(c,hours[dialIndex]));
            v.setIcon(R.id.widget_analog,"setMinuteHand",Icon.createWithResource(c,minutes[dialIndex]));
            v.setIcon(R.id.widget_analog,"setSecondHand",null);
            v.setColorStateList(R.id.widget_analog,"setDialTintList",ColorStateList.valueOf(primary));
            v.setColorStateList(R.id.widget_analog,"setHourHandTintList",ColorStateList.valueOf(primary));
            v.setColorStateList(R.id.widget_analog,"setMinuteHandTintList",ColorStateList.valueOf(config.themeId.equals("transparent.clean") ? primary : theme.accentColor));
            v.setString(R.id.widget_analog,"setTimeZone",config.useSystemTimeZone ? null : config.timeZoneId);
            // No padding override here: the layout already insets the content by one corner radius
            // on both sides, which keeps the dial centred in the panel (AnalogClock centres its
            // drawable on the raw view bounds and ignores padding) and keeps every caption clear of
            // the corner fan.
            v.setOnClickPendingIntent(R.id.widget_analog,WidgetPendingIntentFactory.create(c,config.appWidgetId,CLOCK));
        } else {
            text(v,R.id.widget_day,WidgetTextFormatter.format(now,zone,locale,"d"),primary,(tier==0 ? 46 : 64)*config.textScale,true);
            v.setOnClickPendingIntent(R.id.widget_day,WidgetPendingIntentFactory.create(c,config.appWidgetId,CALENDAR));
        }
        if(config.kind==WidgetKind.WEATHER) {
            ClockPreferences prefs=new ClockPreferences(c);
            WeatherModels.WeatherDisplayData data=new WeatherRepository(c).getCached(prefs.getWeatherLocationMode(),prefs.getWeatherLocationId());
            boolean setup=!QWeatherConfig.isConfigured() || data==null && !WeatherRefreshUseCase.hasLocation(c,prefs);
            String temperature=data==null ? "—" : WeatherTemperatureFormatter.format(data.temperature,prefs.getWeatherTemperatureUnit());
            text(v,R.id.widget_temperature,temperature,primary,24*config.textScale,true);
            String summary=data==null ? "" : WidgetTextFormatter.join(config.showLocation ? WeatherModels.locationText(data.city,data.district) : "",config.showWeatherDescription ? data.text : "");
            text(v,R.id.widget_weather_summary,summary,secondary,12*config.textScale,!compact && !summary.isEmpty());
            String status=data==null ? c.getString(setup ? R.string.widget_weather_setup : R.string.widget_weather_retry)
                : c.getString(R.string.widget_weather_updated,WidgetTextFormatter.format(data.updatedAt,zone,locale,"MM-dd HH:mm"));
            text(v,R.id.widget_weather_status,status,secondary,11*config.textScale,data==null || !compact);
            v.setImageViewBitmap(R.id.widget_weather_icon,WidgetWeatherIconFactory.render(c,data==null ? "999" : data.icon,prefs.isWeatherIconFill(),config.themeId.equals("transparent.clean") ? primary : theme.iconColor,dp(c,40)));
            for(int view:new int[]{R.id.widget_weather_region})
                v.setOnClickPendingIntent(view,WidgetPendingIntentFactory.create(c,config.appWidgetId,setup ? WEATHER : REFRESH));
        }
        if(config.themeId.equals("system.dynamic")) {
            for(int view:new int[]{R.id.widget_date,R.id.widget_lunar,R.id.widget_holiday,R.id.widget_zone}) v.setColor(view,"setTextColor",R.color.widget_secondary);
            v.setColor(R.id.widget_settings,"setColorFilter",R.color.widget_primary);
            if(config.kind==WidgetKind.DIGITAL || config.kind==WidgetKind.WEATHER) v.setColor(R.id.widget_time,"setTextColor",R.color.widget_primary);
            if(config.kind==WidgetKind.CALENDAR) v.setColor(R.id.widget_day,"setTextColor",R.color.widget_primary);
            if(config.kind==WidgetKind.WEATHER) {
                v.setColor(R.id.widget_temperature,"setTextColor",R.color.widget_primary);
                v.setColor(R.id.widget_weather_summary,"setTextColor",R.color.widget_secondary);
                v.setColor(R.id.widget_weather_status,"setTextColor",R.color.widget_secondary);
            }
        }
        return v;
    }
    private static int dp(Context c,int value) { return Math.round(value*c.getResources().getDisplayMetrics().density); }
    private static void text(RemoteViews v,int id,String text,int color,float size,boolean visible) {
        v.setTextViewText(id,text); v.setTextColor(id,color); v.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_SP,size);
        v.setViewVisibility(id,visible ? View.VISIBLE : View.GONE);
    }
}
