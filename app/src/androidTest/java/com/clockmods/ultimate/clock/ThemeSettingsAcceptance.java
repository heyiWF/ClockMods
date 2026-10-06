package com.clockmods.ultimate.clock;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.SystemClock;
import com.clockmods.sdk.clock.*;
import com.clockmods.background.ClockPreferences;
import com.clockmods.time.NetworkTimeProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/** Real device render matrix plus installed screens. Records ignored controls explicitly. */
public final class ThemeSettingsAcceptance extends Instrumentation {
    private int checks, changes;
    private final StringBuilder observations = new StringBuilder();
    private final Map<String, Map<String, ?>> saved = new HashMap<>();
    private boolean calendar;
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { super.onCreate(args); arguments=args; calendar=args!=null && "true".equals(args.getString("calendar"));start(); }
    private void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private ClockState state(int variant, Locale locale) {
        ClockState.Builder b = ClockState.builder(1791292500123L).locale(locale)
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai")).secondHandMotion(ClockState.SecondHandMotion.SWEEP).dateText("2026 / 10 / 06 Tuesday")
                .weatherText("Beijing Sunny 26℃").statusText("Wi-Fi 80%").timeZoneText("UTC+8");
        switch (variant) {
            case 1: b.showSeconds(false); break;
            case 2: b.use24Hour(false); break;
            case 3: b.timeZone(TimeZone.getTimeZone("America/New_York")); break;
            case 4: b.dateText(""); break;
            case 5: b.weatherText(""); break;
            case 6: b.timeScale(.5f); break;
            case 7: b.dateScale(2f); break;
            case 8: b.supportingScale(2f); break;
            case 9: b.statusText(""); break;
            case 10: b.secondHandMotion(ClockState.SecondHandMotion.TICK); break;
            case 11: b.weatherText("Long custom message keeps its configured size and must scroll to the final words.")
                    .messageActive(true).messageScrollElapsedMillis(5000); break;
        }
        return b.build();
    }
    private Bitmap render(ClockStyle style, ClockState state, int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); int count = canvas.getSaveCount();
        style.getRenderer().render(canvas, new ClockRenderContext(0, 0, w, h, 2, 2, state.getTimeMillis()), state, style.getThemeTokens());
        check(canvas.getSaveCount() == count, style.getMetadata().getId() + " Canvas state balanced");
        return bitmap;
    }
    private boolean hasContent(Bitmap bitmap) {
        if (bitmap==null) return false;
        Set<Integer> colors=new HashSet<>();
        for (int y=bitmap.getHeight()/4; y<bitmap.getHeight()*3/4; y+=11)
            for (int x=bitmap.getWidth()/4; x<bitmap.getWidth()*3/4; x+=11) {
                colors.add(bitmap.getPixel(x,y)); if(colors.size()>8) return true;
            }
        return false;
    }
    @Override public void onStart() {
        if(calendar) { CalendarSettingsAcceptance.run(this,arguments);return; }
        Context context = getTargetContext(); Bundle result = new Bundle(); Activity activity = null;
        File directory = new File(context.getExternalFilesDir(null), "theme-settings-audit"); directory.mkdirs();
        int code = Activity.RESULT_OK;
        try {
            for (String name : new String[] {"clock_prefs", "clockmods_ultimate_style", "clockmods_onboarding"})
                saved.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
            NetworkTimeProvider provider=new NetworkTimeProvider(); provider.shutdown(); provider.setEnabled(true);
            check(!provider.isEnabled(),"closed time provider stays disabled"); provider.currentTimeMillis(); provider.shutdown();
            ClockPreferences settings=new ClockPreferences(context);
            UltimateClockPreferences faceSettings=new UltimateClockPreferences(context);
            for(java.lang.reflect.Method setter:ClockPreferences.class.getMethods()) {
                if(!setter.getName().startsWith("set") || setter.getParameterTypes().length!=1 || setter.getParameterTypes()[0]!=boolean.class) continue;
                for(String prefix:new String[]{"is","get"}) try {
                    java.lang.reflect.Method getter=ClockPreferences.class.getMethod(prefix+setter.getName().substring(3));
                    boolean value=(Boolean)getter.invoke(settings); setter.invoke(settings,!value);
                    check((Boolean)getter.invoke(settings)!=value,setter.getName()+" persists"); setter.invoke(settings,value); break;
                } catch(NoSuchMethodException ignored) { }
            }
            for (ClockStyle style : UltimateClockStyles.builtIns()) {
                String id=style.getMetadata().getId(), scope=id;
                for(String font:new String[]{ClockPreferences.FONT_SYSTEM,ClockPreferences.FONT_ROBOTO,ClockPreferences.FONT_LORA}) {
                    settings.setFontFamily(scope,font); check(settings.getFontFamily(scope).equals(font),id+" font persists");
                }
                settings.setFontFamily(scope,"system"); settings.setFontWeight(scope,700);
                check(settings.getFontWeight(scope)==700,id+" font weight persists");
                settings.setTimeFontScale(scope,1.5f); settings.setDateFontScale(scope,2f); settings.setSupportingFontScale(scope,2f);
                check(settings.getTimeFontScale(scope)==1.5f && settings.getDateFontScale(scope)==2f && settings.getSupportingFontScale(scope)==2f,id+" maximum font sizes persist");
                for(String motion:new String[]{"fade","slide_up","slide_down","scale","flip","slide_right","scan"}) {
                    faceSettings.setDigitTransition(id,motion); faceSettings.setWeatherTransition(id,motion);
                    check(faceSettings.getDigitTransition(id).equals(motion) && faceSettings.getWeatherTransition(id).equals(motion),id+" independent transitions persist");
                }
                faceSettings.setDigitTransition(id,"fade");faceSettings.setWeatherTransition(id,"fade");
                for (Locale locale : new Locale[] {Locale.US, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE})
                    for (int[] size : new int[][] {{720,1280},{1280,720}}) {
                        Bitmap baseline = render(style, state(0,locale), size[0], size[1]);
                        for (int option = 1; option <= 11; option++) {
                            Bitmap changed = render(style,state(option,locale),size[0],size[1]);
                            boolean different = !baseline.sameAs(changed);
                            if (different) changes++;
                            observations.append(style.getMetadata().getId()).append(',').append(locale)
                                    .append(',').append(size[0]).append(',').append(option).append(',').append(different).append('\n');
                            changed.recycle();
                        }
                        if (locale == Locale.US) try (FileOutputStream out = new FileOutputStream(new File(directory, style.getMetadata().getId()+"-"+size[0]+".png"))) {
                            baseline.compress(Bitmap.CompressFormat.PNG,100,out);
                        }
                        baseline.recycle();
                    }
                // Start each actual theme from persisted preferences, not just an offscreen renderer.
                context.getSharedPreferences("clockmods_onboarding",0).edit().putBoolean("completed",true).commit();
                context.getSharedPreferences("clock_prefs",0).edit().putBoolean("weather_enabled",false)
                        .putBoolean("use_network_time",false).putBoolean("hourly_visual_chime",false)
                        .putBoolean("half_hour_visual_chime",false).putBoolean("show_status_icons",true)
                        .putString("custom_message","Settings audit — long message tail stays visible")
                        .putInt("screen_orientation",2).commit();
                context.getSharedPreferences("clockmods_ultimate_style",0).edit().putString("style_id",style.getMetadata().getId()).commit();
                ActivityMonitor monitor = addMonitor("com.clockmods.ultimate.UltimateMainActivity",null,false);
                context.startActivity(new Intent().setClassName(context.getPackageName(),"com.clockmods.ultimate.UltimateMainActivity")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                activity = monitor.waitForActivityWithTimeout(10000);
                check(activity != null,"installed theme opened"); SystemClock.sleep(700);
                Bitmap screenshot = null;
                for (int attempt=0; attempt<40; attempt++) {
                    if (screenshot != null) screenshot.recycle();
                    screenshot=getUiAutomation().takeScreenshot();
                    if (hasContent(screenshot)) break;
                    SystemClock.sleep(250);
                }
                check(hasContent(screenshot),"live theme content visible: "+style.getMetadata().getId());
                if (monitor.getLastActivity()!=null) activity=monitor.getLastActivity();
                removeMonitor(monitor);
                try (FileOutputStream out = new FileOutputStream(new File(directory,"live-"+style.getMetadata().getId()+".png"))) { screenshot.compress(Bitmap.CompressFormat.PNG,100,out); }
                screenshot.recycle(); final Activity current = activity;
                Intent settingsIntent=(Intent)Class.forName("com.clockmods.ultimate.settings.UltimateSettingsActivity")
                        .getMethod("createSubpageIntent",Context.class,String.class).invoke(null,context,"time_date");
                ActivityMonitor settingsMonitor=addMonitor(settingsIntent.getComponent().getClassName(),null,false);
                runOnMainSync(()->current.startActivity(settingsIntent));
                activity=settingsMonitor.waitForActivityWithTimeout(10000); check(activity!=null,id+" settings opens");
                Bitmap settingsImage=null;
                for(int attempt=0;attempt<40;attempt++) {
                    if(settingsImage!=null)settingsImage.recycle();
                    settingsImage=getUiAutomation().takeScreenshot();
                    if(hasContent(settingsImage))break;SystemClock.sleep(250);
                }
                check(hasContent(settingsImage),id+" settings visible");
                if(settingsMonitor.getLastActivity()!=null)activity=settingsMonitor.getLastActivity();
                removeMonitor(settingsMonitor);
                try(FileOutputStream out=new FileOutputStream(new File(directory,"settings-"+id+".png"))){settingsImage.compress(Bitmap.CompressFormat.PNG,100,out);}
                settingsImage.recycle();final Activity settingsActivity=activity;runOnMainSync(settingsActivity::finish);activity=null;
                runOnMainSync(current::finish);
            }
            try (FileOutputStream out = new FileOutputStream(new File(directory,"matrix.csv"))) { out.write(observations.toString().getBytes("UTF-8")); }
            check(changes > 0,"settings change rendered pixels");
            result.putString("stream", "PASS: " + checks + " device checks; " + changes + " setting/render changes. Matrix records unsupported or ignored controls for review.\n");
            result.putString("evidence",directory.toString());
        } catch (Throwable error) { code = Activity.RESULT_CANCELED; result.putString("stream",android.util.Log.getStackTraceString(error)); }
        finally {
            final Activity remaining = activity; if (remaining != null) runOnMainSync(remaining::finish);
            for (Map.Entry<String, Map<String, ?>> entry : saved.entrySet()) {
                SharedPreferences.Editor editor = context.getSharedPreferences(entry.getKey(),0).edit().clear();
                for (Map.Entry<String, ?> value : entry.getValue().entrySet()) {
                    Object item=value.getValue(); String key=value.getKey();
                    if(item instanceof String) editor.putString(key,(String)item);
                    else if(item instanceof Boolean) editor.putBoolean(key,(Boolean)item);
                    else if(item instanceof Integer) editor.putInt(key,(Integer)item);
                    else if(item instanceof Long) editor.putLong(key,(Long)item);
                    else if(item instanceof Float) editor.putFloat(key,(Float)item);
                    else if(item instanceof Set) editor.putStringSet(key,(Set<String>)item);
                }
                editor.commit();
            }
        }
        finish(code,result);
    }
}
