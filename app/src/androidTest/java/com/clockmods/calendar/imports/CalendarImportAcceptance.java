package com.clockmods.calendar.imports;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import com.clockmods.R;
import com.clockmods.LocaleManager;
import com.clockmods.background.ClockPreferences;
import com.clockmods.pro.schedule.*;
import com.clockmods.ultimate.settings.CalendarManagerActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Real document-result, storage and installed-calendar acceptance in an isolated audit app. */
public final class CalendarImportAcceptance extends Instrumentation {
    private int checks, orientation = 1;
    private Context context;
    private File output;
    private UserCalendarStore store;
    private ActivityMonitor activeMonitor;
    private Activity active;
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { super.onCreate(args); arguments=args; start(); }
    @Override public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        if(orientation!=0) activity.setRequestedOrientation(orientation==1
                ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
    private void check(boolean condition,String message) {
        checks++; if(!condition)throw new AssertionError(message);
        Bundle progress=new Bundle();progress.putString("check",checks+": "+message);sendStatus(0,progress);
    }
    @Override public void onStart() {
        context=getTargetContext();Bundle result=new Bundle();int code=Activity.RESULT_OK;
        Map<String,Map<String,?>> preferences=new HashMap<>();
        for(String name:new String[]{"clock_prefs","clockmods_onboarding","clockmods_ultimate_style","pro_schedule"})
            preferences.put(name,new HashMap<>(context.getSharedPreferences(name,0).getAll()));
        store=UserCalendarStore.get(context);Set<String> prior=new HashSet<>();
        for(UserCalendarStore.Source source:store.files())prior.add(source.id);
        output=new File(context.getExternalFilesDir(null),"calendar-import-acceptance");output.mkdirs();
        try {
            check((context.getPackageName().contains("fontaudit") || context.getPackageName().startsWith("com.clockmods.permission.")),"isolated audit package");
            ClockPreferences settings=new ClockPreferences(context);settings.setClockLanguage("en");
            settings.setChineseCalendarEnabled(false);
            context.getSharedPreferences("clock_prefs",0).edit().putBoolean("weather_enabled",false)
                    .putBoolean("use_network_time",false).putString("time_zone_id","UTC").putInt("screen_orientation",1).commit();
            context.getSharedPreferences("clockmods_onboarding",0).edit().putBoolean("completed",true).commit();
            LocalDate today=LocalDate.now(ZoneOffset.UTC);
            byte[] fixture=fixture(today);
            if ("motion".equals(arguments.getString("mode"))) {
                checkMenuMotion();
            } else {
            checkNativeScheduleFonts();
            checkDocumentImport(fixture);
            UserCalendarStore.Source source=store.importStream(new ByteArrayInputStream(fixture),"Agenda.ics");
            check(source.eventCount==3,"three event definitions retained");
            List<UserCalendarStore.Occurrence> day=awaitEvents(today,3);
            check(day.size()==3,"all-day, timed and overnight expanded together");
            check(day.get(0).event.allDay,"all-day sorts first");
            List<ScheduleItem> imported=ImportedSchedules.on(LocaleManager.wrap(context),store,
                    today.getYear(),today.getMonthValue()-1,today.getDayOfMonth(),TimeZone.getTimeZone("UTC"));
            check(imported.size()==3 && imported.stream().anyMatch(it->it.timeLabel().equals("09:00–10:30")),"presentation keeps timed range");
            check(imported.stream().anyMatch(it->!it.hasTime()),"presentation preserves all-day flag");
            check(imported.stream().allMatch(it->!it.importSource.isEmpty() && it.details.contains("Audit calendar")),"imported rows carry source details");
            check(awaitEvents(today.plusDays(1),2).size()==2,"overnight and repeated all-day appear tomorrow");
            check(awaitEvents(today.plusDays(3),0).isEmpty(),"exclusive end and recurrence count respected");
            java.lang.reflect.Constructor<UserCalendarStore> constructor=UserCalendarStore.class.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);UserCalendarStore reopened=constructor.newInstance(context);
            check(reopened.files().stream().anyMatch(it->it.id.equals(source.id) && it.eventCount==3),"catalog and source survive store recreation");
            File retained=new File(context.getFilesDir(),"imported-calendars/"+source.id+".ics");
            File tombstone=new File(context.getFilesDir(),"imported-calendars/"+source.id+".deleted");
            check(retained.renameTo(tombstone),"simulate interrupted removal before catalog commit");
            UserCalendarStore recovered=constructor.newInstance(context);
            check(retained.isFile() && !tombstone.exists() && recovered.files().size()==reopened.files().size(),"interrupted removal recovers its retained source");
            checkSubscriptions(fixture);
            int count=store.files().size();
            try{store.importStream(new ByteArrayInputStream(fixture),"Renamed.ics");throw new AssertionError("duplicate accepted");}
            catch(CalendarImportException expected){check(expected.reason.equals("duplicate"),"renamed duplicate rejected");}
            try{store.importStream(new ByteArrayInputStream("not a calendar".getBytes(StandardCharsets.UTF_8)),"Invalid.ics");throw new AssertionError("invalid accepted");}
            catch(CalendarImportException expected){check(expected.reason.equals("invalid"),"invalid calendar rejected");}
            check(store.files().size()==count,"rejected imports do not change catalog");
            ScheduleStore manual=new ScheduleStore(context);
            manual.save(today.getYear(),today.getMonthValue()-1,today.getDayOfMonth(),"calendar-audit-manual","Manual event",11,0);
            check(ImportedSchedules.merge(manual.itemsFor(today.getYear(),today.getMonthValue()-1,today.getDayOfMonth()),imported).size()==4,"manual and imported schedules coexist");
            for(String language:new String[]{"ja","ko"}) {
                settings.setClockLanguage(language);orientation=language.equals("ja")?1:2;
                launch(new Intent(context,CalendarManagerActivity.class));
                check(waitText(active.getString(R.string.calendar_import_title),true),"localized manager "+language);
                check(waitText(active.getString(R.string.calendar_import_add),true),"accessible import menu "+language);
                screenshot("manager-"+language);closeScreen();
            }
            settings.setClockLanguage("en");settings.setChineseCalendarEnabled(false);
            for(int rotation:new int[]{1,2}) for(String theme:new String[]{"graphite","carbon","paper","poster","agenda"}) {
                orientation=rotation;
                context.getSharedPreferences("clock_prefs",0).edit().putString("calendar_theme","calendar."+theme).putInt("screen_orientation",rotation).commit();
                launch(new Intent().setClassName(context.getPackageName(),"com.clockmods.ultimate.UltimateMainActivity")
                        .putExtra("com.clockmods.ultimate.extra.DESTINATION","calendar"));
                selectCalendarPage();
                if (!theme.equals("poster")) {
                    check(waitText("Imported all-day",true),"imported event visible without Chinese calendar "+theme+rotation);
                    checkNativeCarousels(false);
                }
                checkDateAlignment(theme + rotation);
                if(theme.equals("agenda")) {
                    check(waitText("Team sync",true) && waitText("Manual event",true),"Agenda includes both sources "+rotation);
                    check(waitText("09:00",true),"Agenda displays time range "+rotation);
                    screenshot("agenda-"+rotation);
                    check(click("09:00–10:30"),"imported event details open");
                    check(waitText("Meeting room",true) && waitText("Audit calendar",true),"read-only detail includes location and source");
                    screenshot("event-details-"+rotation);
                    check(click(active.getString(android.R.string.ok)),"details dismissed");
                } else {
                    screenshot(theme+"-"+rotation);
                    int emptyDay = today.getDayOfMonth()==14 || today.getDayOfMonth()==15 ? 20 : 15;
                    check(click(active.getString(R.string.calendar_day_number_accessibility, emptyDay)),
                            "empty date selected " + theme + rotation);
                    checkDateAlignment(theme + rotation + " empty selection");
                    screenshot(theme+"-empty-"+rotation);
                }
                closeScreen();
            }
            settings.setChineseCalendarEnabled(true);orientation=1;
            context.getSharedPreferences("clock_prefs",0).edit().putString("calendar_theme","calendar.graphite").putInt("screen_orientation",1).commit();
            launch(new Intent().setClassName(context.getPackageName(),"com.clockmods.ultimate.UltimateMainActivity")
                    .putExtra("com.clockmods.ultimate.extra.DESTINATION","calendar"));selectCalendarPage();
            check(waitText("Imported all-day",true),"imported labels coexist with Chinese calendar");
            checkNativeCarousels(true);screenshot("month-lunar-and-events");closeScreen();
            settings.setChineseCalendarEnabled(false);
            // Delete while the live Agenda is visible; the observer must remove only imported rows.
            orientation=1;
            context.getSharedPreferences("clock_prefs",0).edit().putString("calendar_theme","calendar.agenda").putInt("screen_orientation",1).commit();
            launch(new Intent().setClassName(context.getPackageName(),"com.clockmods.ultimate.UltimateMainActivity")
                    .putExtra("com.clockmods.ultimate.extra.DESTINATION","calendar"));selectCalendarPage();
            check(waitText("Team sync",true),"Agenda ready before deletion");
            check(store.delete(source.id),"source deleted");
            check(waitText("Team sync",false) && waitText("Manual event",true),"live deletion removes imported rows and retains manual row");
            check(store.eventsOn(today.getYear(),today.getMonthValue()-1,today.getDayOfMonth(),TimeZone.getTimeZone("UTC")).isEmpty(),"deleted occurrences leave cache");
            check(!new File(context.getFilesDir(),"imported-calendars/"+source.id+".ics").exists(),"private source file removed");
            closeScreen();
            File privateFixture=new File(context.getExternalFilesDir(null),"basic.ics");
            if(privateFixture.isFile()) {
                UserCalendarStore.Source supplied;
                try(InputStream input=new FileInputStream(privateFixture)){supplied=store.importStream(input,"basic.ics");}
                check(supplied.eventCount==317,"supplied private file imports 317 events on Android");
                check(store.delete(supplied.id),"supplied private file can be deleted");
            } else check(!"true".equals(arguments.getString("requirePrivateFixture")),"private fixture optional outside local acceptance");
            }
            result.putString("result","PASS "+checks+" checks");
        } catch(Throwable failure) {
            code=Activity.RESULT_CANCELED;
            try{screenshot("failure");result.putString("visible",spoken(root()));}catch(Throwable ignored){}
            result.putString("error",android.util.Log.getStackTraceString(failure));
        } finally {
            try{closeScreen();}catch(Throwable ignored){}
            for(UserCalendarStore.Source source:new ArrayList<>(store.files()))if(!prior.contains(source.id))try{store.delete(source.id);}catch(Exception ignored){}
            for(Map.Entry<String,Map<String,?>> entry:preferences.entrySet())restore(context.getSharedPreferences(entry.getKey(),0),entry.getValue());
            orientation=0;try{getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);}catch(Throwable ignored){}
        }
        finish(code,result);
    }
    private void checkMenuMotion() throws Exception {
        launch(new Intent(context,CalendarManagerActivity.class));
        SystemClock.sleep(1200);
        for (int cycle=0;cycle<2;cycle++) {
            check(click(active.getString(R.string.calendar_import_add)), "menu opens for frame capture");
            SystemClock.sleep(700);
            check(waitText(active.getString(R.string.calendar_import_link),true), "expanded menu exposes link action");
            check(click(active.getString(R.string.calendar_import_close)), "menu closes for frame capture");
            SystemClock.sleep(700);
        }
        check(click(active.getString(R.string.calendar_import_add)), "menu reopens after animation reversal");
        check(click(active.getString(R.string.calendar_import_link)), "link action opens dialog");
        check(waitText(active.getString(R.string.calendar_sync_enabled),true), "link dialog offers optional subscription sync");
        screenshot("subscription-dialog");
        check(click(active.getString(android.R.string.cancel)), "link dialog cancels without import");
    }
    private void checkSubscriptions(byte[] fixture) throws Exception {
        byte[] initial=new String(fixture,StandardCharsets.UTF_8).replace("Imported all-day","Subscribed initial").getBytes(StandardCharsets.UTF_8);
        byte[] updated=new String(fixture,StandardCharsets.UTF_8).replace("Imported all-day","Subscribed updated").getBytes(StandardCharsets.UTF_8);
        int before=store.files().size();
        UserCalendarStore.Source source=store.importStream(new ByteArrayInputStream(initial),"subscription.ics","https://example.invalid/calendar.ics",true);
        boolean scheduled=false;
        long deadline=SystemClock.uptimeMillis()+5000;
        do {
            scheduled=androidx.work.WorkManager.getInstance(context).getWorkInfosForUniqueWork("calendar-subscriptions")
                    .get().stream().anyMatch(work -> work.getState()==androidx.work.WorkInfo.State.ENQUEUED);
            if (!scheduled) SystemClock.sleep(100);
        } while (!scheduled && SystemClock.uptimeMillis()<deadline);
        check(scheduled,"enabled subscription schedules durable periodic work");
        File retained=new File(context.getFilesDir(),"imported-calendars/"+source.id+".ics");
        check(store.replaceSubscription(source,updated),"subscription replacement accepted");
        UserCalendarStore.Source current=store.files().stream().filter(it->it.id.equals(source.id)).findFirst().get();
        check(store.files().size()==before+1 && current.id.equals(source.id),"sync retains one stable subscription");
        check(new String(java.nio.file.Files.readAllBytes(retained.toPath()),StandardCharsets.UTF_8).contains("Subscribed updated"),"sync replaces old local events");
        try {store.replaceSubscription(current,"invalid".getBytes(StandardCharsets.UTF_8));throw new AssertionError("invalid sync accepted");}
        catch(java.io.IOException expected) {check(true,"invalid subscription rejected before replacement");}
        check(Arrays.equals(updated,java.nio.file.Files.readAllBytes(retained.toPath())),"failed sync preserves the last valid local copy");
        java.lang.reflect.Constructor<UserCalendarStore> constructor=UserCalendarStore.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);UserCalendarStore reopened=constructor.newInstance(context);
        UserCalendarStore.Source restored=reopened.files().stream().filter(it->it.id.equals(source.id)).findFirst().get();
        check(restored.syncEnabled && restored.sourceUrl.equals(source.sourceUrl) && restored.lastSyncedAt>0,"subscription metadata survives recreation");
        store.setSyncEnabled(source.id,false);
        check(!store.replaceSubscription(current,initial),"disabled subscription ignores an in-flight download");
        check(retained.isFile(),"disabling sync retains events");
        store.delete(source.id);
        check(!store.replaceSubscription(current,initial) && !retained.exists(),"in-flight download cannot resurrect deleted subscription");
        check(store.files().size()==before,"subscription cleanup restores source count");
    }
    private void checkDocumentImport(byte[] data)throws Exception {
        ContentValues values=new ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"ClockMods-calendar-audit.ics");
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"text/calendar");values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/ClockModsCalendarAudit");
        Uri uri=context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        check(uri!=null,"document fixture created");ActivityMonitor picker=null;int count=store.files().size();
        try {
            try(OutputStream out=context.getContentResolver().openOutputStream(uri)){out.write(data);}
            launch(new Intent(context,CalendarManagerActivity.class));
            IntentFilter filter=new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);filter.addCategory(Intent.CATEGORY_OPENABLE);filter.addDataType("*/*");
            picker=addMonitor(filter,new ActivityResult(Activity.RESULT_OK,new Intent().setData(uri)),true);
            check(click(active.getString(R.string.calendar_import_add)),"plus expands import options");
            check(waitText(active.getString(R.string.calendar_import_link),true),"link action expands above plus");
            screenshot("import-menu-expanded");
            check(click(active.getString(R.string.calendar_import_file)),"file action opens document picker");
            long until=SystemClock.uptimeMillis()+15000;
            while(store.files().size()==count && SystemClock.uptimeMillis()<until)SystemClock.sleep(100);
            check(picker.getHits()==1 && store.files().size()==count+1,"document result imports and publishes calendar");
            check(waitText("Audit calendar",true),"manager shows calendar name");screenshot("document-import");
            check(click(active.getString(R.string.calendar_import_add)),"repeat import menu opened");
            check(click(active.getString(R.string.calendar_import_file)),"repeat file import clicked");
            check(waitText(active.getString(R.string.calendar_import_duplicate),true),"duplicate upload feedback");
            check(store.files().size()==count+1,"duplicate retains single file");
            check(click(active.getString(R.string.calendar_import_delete)),"delete action opens confirmation");
            check(click(active.getString(android.R.string.cancel)),"deletion can be cancelled");
            check(store.files().size()==count+1,"cancel keeps source");
            check(click(active.getString(R.string.calendar_import_delete)),"delete action reopened");
            check(click(active.getString(R.string.calendar_import_delete)),"file deletion confirmed");
            until=SystemClock.uptimeMillis()+10000;
            while(store.files().size()!=count && SystemClock.uptimeMillis()<until)SystemClock.sleep(100);
            check(store.files().size()==count,"manager deletes file");
        } finally {if(picker!=null)removeMonitor(picker);closeScreen();context.getContentResolver().delete(uri,null,null);}
    }
    private List<UserCalendarStore.Occurrence> awaitEvents(LocalDate date,int expected) {
        long until=SystemClock.uptimeMillis()+12000;List<UserCalendarStore.Occurrence> events;
        do{events=store.eventsOn(date.getYear(),date.getMonthValue()-1,date.getDayOfMonth(),TimeZone.getTimeZone("UTC"));
            if(events.size()==expected)return events;SystemClock.sleep(100);
        }while(SystemClock.uptimeMillis()<until);return events;
    }
    private void launch(Intent intent)throws Exception {
        getUiAutomation().setRotation(orientation==1?android.app.UiAutomation.ROTATION_FREEZE_0:android.app.UiAutomation.ROTATION_FREEZE_90);
        SystemClock.sleep(700);
        activeMonitor=addMonitor(intent.getComponent().getClassName(),null,false);
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        active=activeMonitor.waitForActivityWithTimeout(12000);check(active!=null,"screen launched");SystemClock.sleep(1200);
        android.graphics.Rect screen = new android.graphics.Rect();
        long rotationDeadline=SystemClock.uptimeMillis()+10000;
        do {
            AccessibilityNodeInfo visible=root();
            if(visible!=null) visible.getBoundsInScreen(screen);
            if(orientation==1 ? screen.height()>screen.width() : screen.width()>screen.height()) break;
            SystemClock.sleep(150);
        } while(SystemClock.uptimeMillis()<rotationDeadline);
        check(orientation==1 ? screen.height()>screen.width() : screen.width()>screen.height(),
                "actual screen orientation " + orientation);
        if(activeMonitor.getLastActivity()!=null)active=activeMonitor.getLastActivity();
    }
    private void onUi(Runnable action) {
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> {
            try { action.run(); } catch (Throwable error) { failure[0] = error; }
        });
        if (failure[0] != null) throw new AssertionError("UI probe failed", failure[0]);
    }
    private void selectCalendarPage() {
        if(activeMonitor!=null && activeMonitor.getLastActivity()!=null)active=activeMonitor.getLastActivity();
        Activity screen=active;
        onUi(()->{
            int id=screen.getResources().getIdentifier("pro_pager","id",context.getPackageName());
            android.view.View pager=id==0?null:screen.findViewById(id);
            if(pager!=null)try{pager.getClass().getMethod("setCurrentItem",int.class,boolean.class).invoke(pager,1,false);}
            catch(Exception failure){throw new RuntimeException(failure);}
        });SystemClock.sleep(1000);
    }
    private void checkNativeScheduleFonts() throws Exception {
        Class<?> type;
        try{type=Class.forName("com.clockmods.pro.schedule.AgendaScheduleView");}
        catch(ClassNotFoundException compose){return;}
        Class<?> viewClass=type;
        onUi(()->{
            try {
                android.view.View view=(android.view.View)viewClass.getConstructor(Context.class).newInstance(context);
                android.graphics.Typeface font=android.graphics.Typeface.MONOSPACE;
                viewClass.getMethod("setTypefaces",android.graphics.Typeface.class,android.graphics.Typeface.class).invoke(view,font,font);
                for(int revision=0;revision<2;revision++){
                    viewClass.getMethod("setItems",List.class).invoke(view,Arrays.asList(
                            new ScheduleItem("manual","Manual",9,0),new ScheduleItem("import","Imported",10,0,"source","10:00–11:00","Details")));
                    check(allTextUses(view,font),"new and rebound Agenda rows retain selected font "+revision);
                }
            } catch(Exception failure){throw new AssertionError(failure);}
        });
    }
    private static boolean allTextUses(android.view.View view,android.graphics.Typeface font) {
        if(view instanceof android.widget.TextView && !font.equals(((android.widget.TextView)view).getTypeface()))return false;
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)
            if(!allTextUses(((android.view.ViewGroup)view).getChildAt(i),font))return false;
        return true;
    }
    private void checkNativeCarousels(boolean lunar) {
        if(active.getClass().getSuperclass().getName().contains("ComponentActivity"))return;
        Activity screen=active;
        onUi(()->{
            try {
                List<android.view.View> views=new ArrayList<>();collectCarousels(screen.getWindow().getDecorView(),views);
                if(views.isEmpty())return; // Compose paints its own labels.
                int imported=0;
                for(android.view.View view:views){
                    java.lang.reflect.Field items=view.getClass().getDeclaredField("items");items.setAccessible(true);
                    List<?> labels=(List<?>)items.get(view);
                    if(!labels.contains("Imported all-day"))continue;
                    imported++;
                    java.lang.reflect.Field running=view.getClass().getDeclaredField("active");running.setAccessible(true);
                    check(running.getBoolean(view),"rebuilt imported carousel is running");
                    check(!labels.contains(""),"imported carousel has no blank page");
                    if(lunar)check(labels.stream().anyMatch(label -> !Arrays.asList("Imported all-day", "Team sync", "Overnight").contains(label)),"lunar and event labels share the carousel");
                }
                check(imported>0,"live native carousel includes imported event");
            } catch(Exception failure){throw new AssertionError(failure);}
        });
    }
    private void checkDateAlignment(String label) {
        List<float[]> centers = new ArrayList<>();
        onUi(() -> collectNativeCenters(active.getWindow().getDecorView(), centers));
        if (centers.isEmpty()) {
            Map<String, android.graphics.Rect> cells = new HashMap<>(), numbers = new HashMap<>();
            collectComposeBounds(root(), cells, numbers);
            for (Map.Entry<String, android.graphics.Rect> entry : numbers.entrySet()) {
                android.graphics.Rect cell = cells.get(entry.getKey()), number = entry.getValue();
                if (cell != null && cell.width() > 0 && cell.height() > 0)
                    centers.add(new float[]{(number.exactCenterX()-cell.left)/cell.width(),
                            (number.exactCenterY()-cell.top)/cell.height(), cell.width(), cell.height()});
            }
        }
        check(centers.size() >= 7, "date geometry available " + label);
        float[] first = centers.get(0);
        for (float[] center : centers) {
            check(Math.abs(first[0]-center[0])*Math.min(first[2],center[2]) <= 2.1f
                    && Math.abs(first[1]-center[1])*Math.min(first[3],center[3]) <= 2.1f,
                    "event and empty date numbers align " + label);
        }
    }
    private static void collectNativeCenters(android.view.View view, List<float[]> centers) {
        if (!view.isShown()) return;
        if (view.getClass().getName().equals("com.clockmods.pro.CalendarDayNumberView")) {
            android.view.View parent = (android.view.View) view.getParent();
            if (parent.getWidth()>0 && parent.getHeight()>0)
                centers.add(new float[]{(view.getLeft()+view.getWidth()/2f)/parent.getWidth(),
                        (view.getTop()+view.getHeight()/2f)/parent.getHeight(), parent.getWidth(), parent.getHeight()});
        }
        if (view instanceof android.view.ViewGroup) for (int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)
            collectNativeCenters(((android.view.ViewGroup)view).getChildAt(i), centers);
    }
    private static void collectComposeBounds(AccessibilityNodeInfo node,
            Map<String, android.graphics.Rect> cells, Map<String, android.graphics.Rect> numbers) {
        if (node == null) return;
        String tag = node.getViewIdResourceName();
        if (tag != null && node.isVisibleToUser()) {
            android.graphics.Rect bounds = new android.graphics.Rect(); node.getBoundsInScreen(bounds);
            if (tag.startsWith("day:")) cells.put(tag.substring(4), bounds);
            if (tag.startsWith("week-day:")) cells.put(tag.substring(9), bounds);
            if (tag.startsWith("calendar-number:")) numbers.put(tag.substring(16), bounds);
        }
        for (int i=0;i<node.getChildCount();i++) collectComposeBounds(node.getChild(i),cells,numbers);
    }
    private static void collectCarousels(android.view.View view,List<android.view.View> result) {
        if(!view.isShown())return;
        if(view.getClass().getName().equals("com.clockmods.ui.CalendarLabelCarouselView"))result.add(view);
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)
            collectCarousels(((android.view.ViewGroup)view).getChildAt(i),result);
    }
    private void closeScreen() {
        if(activeMonitor!=null && activeMonitor.getLastActivity()!=null)active=activeMonitor.getLastActivity();
        if(active!=null){Activity close=active;onUi(close::finish);active=null;}
        if(activeMonitor!=null){removeMonitor(activeMonitor);activeMonitor=null;}
    }
    private AccessibilityNodeInfo root(){if(Build.VERSION.SDK_INT>=34)getUiAutomation().clearCache();return getUiAutomation().getRootInActiveWindow();}
    private static String spoken(AccessibilityNodeInfo node) {
        if(node==null)return "";StringBuilder text=new StringBuilder();if(node.getText()!=null)text.append(node.getText()).append('\n');
        if(node.getContentDescription()!=null)text.append(node.getContentDescription()).append('\n');
        for(int i=0;i<node.getChildCount();i++)text.append(spoken(node.getChild(i)));return text.toString();
    }
    private boolean waitText(String value,boolean present) {
        long until=SystemClock.uptimeMillis()+12000;
        do{if(spoken(root()).contains(value)==present)return true;SystemClock.sleep(150);}while(SystemClock.uptimeMillis()<until);return false;
    }
    private static AccessibilityNodeInfo actionFor(AccessibilityNodeInfo node, String label, boolean exact) {
        if (node == null) return null;
        String text = node.getText() == null ? "" : node.getText().toString();
        String description = node.getContentDescription() == null ? "" : node.getContentDescription().toString();
        if (exact ? text.equals(label) || description.equals(label)
                : text.contains(label) || description.startsWith(label + " ")) {
            for (AccessibilityNodeInfo action = node; action != null; action = action.getParent())
                if (action.isClickable() && action.isEnabled()) return action;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo action = actionFor(node.getChild(i), label, exact);
            if (action != null) return action;
        }
        return null;
    }
    private boolean click(String label) {
        long until = SystemClock.uptimeMillis() + 10000;
        do {
            // Compose virtual nodes do not implement the platform find-by-text lookup.
            AccessibilityNodeInfo root = root();
            AccessibilityNodeInfo action = actionFor(root, label, true);
            if (action == null) action = actionFor(root, label, false);
            if (action != null && action.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                SystemClock.sleep(350); return true;
            }
            SystemClock.sleep(150);
        } while (SystemClock.uptimeMillis() < until);
        Bundle diagnostic = new Bundle();
        diagnostic.putString("missingAction", label + " in " + spoken(root()));
        sendStatus(0, diagnostic);
        return false;
    }
    private void screenshot(String name)throws Exception {
        Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap==null)throw new AssertionError("screenshot unavailable");
        try(FileOutputStream out=new FileOutputStream(new File(output,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
    }
    private static byte[] fixture(LocalDate today) {
        DateTimeFormatter f=DateTimeFormatter.BASIC_ISO_DATE;String date=today.format(f),next=today.plusDays(1).format(f);
        return ("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ClockMods//Acceptance//EN\r\nX-WR-CALNAME:Audit calendar\r\n"
                +"BEGIN:VEVENT\r\nUID:all-day\r\nDTSTAMP:20260101T000000Z\r\nDTSTART;VALUE=DATE:"+date+"\r\nRRULE:FREQ=DAILY;COUNT=2\r\nSUMMARY:Imported all-day\r\nEND:VEVENT\r\n"
                +"BEGIN:VEVENT\r\nUID:meeting\r\nDTSTAMP:20260101T000000Z\r\nDTSTART:"+date+"T090000Z\r\nDTEND:"+date+"T103000Z\r\nSUMMARY:Team sync\r\nLOCATION:Meeting room\r\nDESCRIPTION:Imported meeting details\r\nEND:VEVENT\r\n"
                +"BEGIN:VEVENT\r\nUID:overnight\r\nDTSTAMP:20260101T000000Z\r\nDTSTART:"+date+"T233000Z\r\nDTEND:"+next+"T003000Z\r\nSUMMARY:Overnight\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n").getBytes(StandardCharsets.UTF_8);
    }
    private static void restore(SharedPreferences prefs,Map<String,?> values) {
        SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> entry:values.entrySet()){
            String key=entry.getKey();Object value=entry.getValue();if(value instanceof String)edit.putString(key,(String)value);
            else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);
            else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);
            else if(value instanceof Set)edit.putStringSet(key,(Set<String>)value);
        }edit.commit();
    }
}
