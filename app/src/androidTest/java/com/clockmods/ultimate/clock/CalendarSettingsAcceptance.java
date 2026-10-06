package com.clockmods.ultimate.clock;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import com.clockmods.R;
import com.clockmods.background.ClockPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/** Installed calendar themes, maximum typography, week preferences and day selection. */
final class CalendarSettingsAcceptance {
    static void run(Instrumentation test,Bundle arguments) {
        Context context=test.getTargetContext(); SharedPreferences prefs=context.getSharedPreferences("clock_prefs",0);
        SharedPreferences onboarding=context.getSharedPreferences("clockmods_onboarding",0);
        boolean hadOnboarding=onboarding.contains("completed"), completed=onboarding.getBoolean("completed",false);
        Map<String,?> original=new HashMap<>(prefs.getAll()); Bundle report=new Bundle();int code=Activity.RESULT_OK,checks=0;
        Activity activity=null;File directory=new File(context.getExternalFilesDir(null),"calendar-settings-audit");directory.mkdirs();
        try {
            onboarding.edit().putBoolean("completed",true).commit();
            ClockPreferences settings=new ClockPreferences(context);
            for(int orientation:new int[]{1,2})
                for(String id:new String[]{"calendar.graphite","calendar.carbon","calendar.paper","calendar.poster","calendar.agenda"}) {
                    if(arguments!=null && arguments.containsKey("theme") && !id.equals(arguments.getString("theme")))continue;
                    if(arguments!=null && arguments.containsKey("orientation") && orientation!=Integer.parseInt(arguments.getString("orientation")))continue;
                    settings.setCalendarTheme(id);settings.setCalendarWeekStart(java.util.Calendar.MONDAY);
                    settings.setCalendarHighlightWeekends(true);settings.setShowStatusIcons(true);settings.setStatusIconScale(1.6f);
                    String scope=ClockPreferences.calendarScope(id);
                    settings.setDateFontScale(scope,2f);settings.setSupportingFontScale(scope,2f);
                    settings.setTimeFontScale(scope,1.5f);settings.setFontWeight(scope,700);
                    prefs.edit().putInt("screen_orientation",orientation).putBoolean("weather_enabled",false).commit();
                    test.getUiAutomation().setRotation(orientation-1);SystemClock.sleep(700);
                    Instrumentation.ActivityMonitor monitor=test.addMonitor("com.clockmods.ultimate.UltimateMainActivity",null,false);
                    context.startActivity(new Intent().setClassName(context.getPackageName(),"com.clockmods.ultimate.UltimateMainActivity")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    activity=monitor.waitForActivityWithTimeout(10000);
                    if(activity==null)throw new AssertionError("calendar Activity opens");
                    int stable=0;
                    for(int attempt=0;attempt<60;attempt++) {
                        if(monitor.getLastActivity()!=null)activity=monitor.getLastActivity();
                        final Activity shown=activity;
                        test.runOnMainSync(()-> { androidx.viewpager2.widget.ViewPager2 pager=shown.findViewById(R.id.pro_pager);pager.setCurrentItem(1,false); });
                        Bitmap frame=test.getUiAutomation().takeScreenshot();
                        boolean ready=frame!=null && ((frame.getWidth()<frame.getHeight())==(orientation==1));
                        if(frame!=null)frame.recycle();stable=ready?stable+1:0;
                        if(stable>=4)break;SystemClock.sleep(250);
                    }
                    if(stable<4)throw new AssertionError(id+" stable orientation "+orientation+" requested="+activity.getRequestedOrientation()+" preference="+settings.getScreenOrientation());
                    test.removeMonitor(monitor);
                    final Activity current=activity;
                    test.runOnMainSync(()-> {
                        androidx.viewpager2.widget.ViewPager2 pager=current.findViewById(R.id.pro_pager);pager.setCurrentItem(1,false);
                    });
                    SystemClock.sleep(1500);
                    final ViewGroup[] days={null};
                    test.runOnMainSync(()-> days[0]=current.findViewById(id.endsWith("agenda")?R.id.calendar_agenda_strip:R.id.calendar_grid));
                    if(days[0]==null || days[0].getChildCount()!=(id.endsWith("agenda")?7:42))throw new AssertionError(id+" day inventory");checks++;
                    test.runOnMainSync(()-> {
                        View day=days[0].getChildAt(2);if(!day.performClick())throw new AssertionError(id+" day selection");
                    });checks++;
                    SystemClock.sleep(400);
                    Bitmap bitmap=test.getUiAutomation().takeScreenshot();
                    if(bitmap==null || ((bitmap.getWidth()<bitmap.getHeight())!=(orientation==1)))throw new AssertionError(id+" orientation "+orientation);checks++;
                    try(FileOutputStream output=new FileOutputStream(new File(directory,id+"-"+orientation+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
                    test.runOnMainSync(current::finish);activity=null;SystemClock.sleep(400);
                }
            report.putString("stream","PASS: "+checks+" installed calendar checks with maximum typography.\n");
        }catch(Throwable error){code=Activity.RESULT_CANCELED;report.putString("stream",android.util.Log.getStackTraceString(error));}
        finally {
            test.getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);
            if(hadOnboarding)onboarding.edit().putBoolean("completed",completed).commit();else onboarding.edit().remove("completed").commit();
            final Activity remaining=activity;if(remaining!=null)test.runOnMainSync(remaining::finish);
            SharedPreferences.Editor editor=prefs.edit().clear();
            for(Map.Entry<String,?> entry:original.entrySet()) {
                Object value=entry.getValue();String key=entry.getKey();
                if(value instanceof String)editor.putString(key,(String)value);else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
                else if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof Long)editor.putLong(key,(Long)value);
                else if(value instanceof Float)editor.putFloat(key,(Float)value);else if(value instanceof java.util.Set)editor.putStringSet(key,(java.util.Set<String>)value);
            }editor.commit();
        }
        test.finish(code,report);
    }
}
