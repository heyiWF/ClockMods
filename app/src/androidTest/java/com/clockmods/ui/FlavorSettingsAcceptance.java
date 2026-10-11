package com.clockmods.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import com.clockmods.background.ClockPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Opens the installed flavor and its actual settings UI with large and localized settings. */
@android.annotation.TargetApi(18)
public final class FlavorSettingsAcceptance extends Instrumentation {
    private int checks;
    private int orientation;
    private boolean fontScaleOnly;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        fontScaleOnly = args != null && "true".equals(args.getString("fontScaleOnly"));
        start();
    }
    private void check(boolean ok,String message) { checks++; if (!ok) throw new AssertionError(message); }
    @Override public void onStart() {
        Context context=getTargetContext(); SharedPreferences preferences=context.getSharedPreferences("clock_prefs",0);
        Map<String,?> original=new HashMap<>(preferences.getAll()); Bundle report=new Bundle(); int code=Activity.RESULT_OK;
        Activity activity=null; File directory=new File(context.getExternalFilesDir(null),"flavor-settings-audit"); directory.mkdirs();
        try {
            com.clockmods.time.NetworkTimeProvider provider=new com.clockmods.time.NetworkTimeProvider();
            provider.shutdown(); provider.setEnabled(true); check(!provider.isEnabled(),"closed provider stays disabled");
            provider.currentTimeMillis(); provider.shutdown();
            // Verify persisted booleans through the real getters/setters on Android.
            ClockPreferences settings=new ClockPreferences(context);
            for(Method setter:ClockPreferences.class.getMethods()) {
                if(!setter.getName().startsWith("set") || setter.getParameterTypes().length!=1 || setter.getParameterTypes()[0]!=boolean.class) continue;
                String suffix=setter.getName().substring(3); Method getter=null;
                for(String prefix:new String[]{"is","get"}) try { getter=ClockPreferences.class.getMethod(prefix+suffix); break; } catch(NoSuchMethodException ignored) { }
                if(getter==null) continue;
                boolean saved=(Boolean)getter.invoke(settings);
                setter.invoke(settings,!saved); check((Boolean)getter.invoke(settings)!=saved,setter.getName()+" persists");
                setter.invoke(settings,saved);
            }
            String component=context.getPackageName().endsWith(".pro")?"com.clockmods.pro.ProMainActivity":"com.clockmods.MainActivity";
            for(String locale:new String[]{"zh-Hans","zh-Hant","en"}) for(int orientation:new int[]{1,2}) {
                this.orientation=orientation;
                preferences.edit().putString("clock_language",locale).putInt("screen_orientation",orientation)
                        .putBoolean("weather_enabled",false).putBoolean("use_network_time",false)
                        .putBoolean("hourly_visual_chime",false).putBoolean("show_status_icons",true)
                        .putFloat("status_icon_scale",1.5f).putFloat("date_font_scale",1.5f)
                        .putString("custom_message","Long settings verification message: every character must remain readable through the final word.")
                        .commit();
                ActivityMonitor monitor=addMonitor(component,null,false);
                context.startActivity(new Intent().setClassName(context.getPackageName(),component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                activity=monitor.waitForActivityWithTimeout(10000); check(activity!=null,"flavor launches");
                if (fontScaleOnly) {
                    final Activity current = activity;
                    final Throwable[] failure = {null};
                    runOnMainSync(() -> { try { checkFontSizeWarnings(current); }
                        catch (Throwable error) { failure[0] = error; } });
                    if (failure[0] != null) throw new AssertionError("font size controls", failure[0]);
                    removeMonitor(monitor);
                    runOnMainSync(current::finish);
                    activity = null;
                    continue;
                }
                boolean visible=false;
                for(int attempt=0;attempt<40;attempt++) {
                    Bitmap screenshot=getUiAutomation().takeScreenshot();
                    if(screenshot!=null) {
                        java.util.Set<Integer> colors=new java.util.HashSet<>();
                        for(int y=screenshot.getHeight()/4;y<screenshot.getHeight()*3/4;y+=11)
                            for(int x=screenshot.getWidth()/4;x<screenshot.getWidth()*3/4;x+=11) colors.add(screenshot.getPixel(x,y));
                        visible=colors.size()>8; screenshot.recycle();
                    }
                    if(visible) break; SystemClock.sleep(250);
                }
                check(visible,"installed clock content visible");
                save(new File(directory,locale+"-"+orientation+"-clock.png"));
                if(monitor.getLastActivity()!=null) activity=monitor.getLastActivity();
                removeMonitor(monitor);
                final Activity current=activity; final Throwable[] failure={null};
                runOnMainSync(()-> {try {checkFontSizeWarnings(current); Method method=current.getClass().getDeclaredMethod("showSettings"); method.setAccessible(true); method.invoke(current);} catch(Throwable e){failure[0]=e;}});
                if(failure[0]!=null) throw new AssertionError("settings dialog opens",failure[0]);
                SystemClock.sleep(300); save(new File(directory,locale+"-"+orientation+"-settings.png"));
                check(getUiAutomation().getRootInActiveWindow()!=null,"settings accessibility tree");
                runOnMainSync(current::finish); activity=null;
            }
            report.putString("stream","PASS: "+checks+" persisted settings and "
                    +(fontScaleOnly ? "font slider" : "installed UI")
                    +" checks across 3 locales and 2 orientations.\n");
            report.putString("evidence",directory.toString());
        } catch(Throwable error){code=Activity.RESULT_CANCELED;report.putString("stream",android.util.Log.getStackTraceString(error));}
        finally {
            final Activity remaining=activity;if(remaining!=null)runOnMainSync(remaining::finish);
            SharedPreferences.Editor editor=preferences.edit().clear();
            for(Map.Entry<String,?> entry:original.entrySet()) {
                Object value=entry.getValue();String key=entry.getKey();
                if(value instanceof String)editor.putString(key,(String)value);
                else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
                else if(value instanceof Integer)editor.putInt(key,(Integer)value);
                else if(value instanceof Float)editor.putFloat(key,(Float)value);
                else if(value instanceof Long)editor.putLong(key,(Long)value);
            }
            editor.commit();
        }
        finish(code,report);
    }
    private void checkFontSizeWarnings(Activity activity) throws Exception {
        SettingsDialog dialog = new SettingsDialog(activity,
                new com.clockmods.background.BackgroundRepository(activity), null);
        java.lang.reflect.Field warningsField = SettingsDialog.class.getDeclaredField("fontSizeWarnings");
        warningsField.setAccessible(true);
        Map<?, ?> warnings = (Map<?, ?>) warningsField.get(dialog);
        for (String prefix : new String[]{"time", "date"}) {
            java.lang.reflect.Field barField = SettingsDialog.class.getDeclaredField(prefix + "SizeBar");
            java.lang.reflect.Field valueField = SettingsDialog.class.getDeclaredField(prefix + "SizeValue");
            barField.setAccessible(true);
            valueField.setAccessible(true);
            Object bar = barField.get(dialog);
            android.widget.TextView warning = (android.widget.TextView) warnings.get(valueField.get(dialog));
            for (int percent : new int[]{100, 101, Math.round(ClockPreferences.MAX_FONT_SCALE * 100), 100, 50}) {
                if (bar instanceof android.widget.SeekBar) {
                    ((android.widget.SeekBar) bar).setProgress(percent - Math.round(ClockPreferences.MIN_FONT_SCALE * 100));
                } else {
                    bar.getClass().getMethod("setValue", float.class).invoke(bar, (float) percent);
                }
                check(warning.getVisibility() == (percent > 100 ? android.view.View.VISIBLE : android.view.View.GONE),
                        prefix + " warning follows " + percent + "%");
                check(warning.getText().length() > 0, "localized overflow warning");
                android.view.ViewGroup container = (android.view.ViewGroup) warning.getParent();
                check(container.indexOfChild(warning) == 1, "warning follows slider row");
            }
        }
        dialog.dismiss();
    }

    private void save(File file)throws Exception {
        Bitmap bitmap=null; int stable=0;
        for(int attempt=0;attempt<60;attempt++) {
            if(bitmap!=null)bitmap.recycle(); bitmap=getUiAutomation().takeScreenshot();
            android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            java.util.Set<Integer> colors=new java.util.HashSet<>();
            if(bitmap!=null)for(int y=bitmap.getHeight()/4;y<bitmap.getHeight()*3/4;y+=11)
                for(int x=bitmap.getWidth()/4;x<bitmap.getWidth()*3/4;x+=11)colors.add(bitmap.getPixel(x,y));
            boolean ready=bitmap!=null && colors.size()>8 && ((bitmap.getWidth()<bitmap.getHeight())==(orientation==1))
                    && root!=null && getTargetContext().getPackageName().contentEquals(root.getPackageName());
            stable=ready?stable+1:0; if(stable>=4)break;SystemClock.sleep(200);
        }
        check(stable>=4,"stable installed page screenshot: "+file.getName());
        try(FileOutputStream output=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
    }
}
