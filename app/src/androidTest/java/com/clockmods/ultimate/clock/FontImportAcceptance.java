package com.clockmods.ultimate.clock;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.background.*;
import com.clockmods.ui.ClockTypefaceResolver;
import com.clockmods.ui.DateFormatter;
import com.clockmods.ultimate.settings.FontManagerActivity;
import com.clockmods.ultimate.settings.UltimateSettingsActivity;
import java.io.*;
import java.util.*;

/** Device contract checks exercise Android's real font decoder, fallback and installed settings. */
public final class FontImportAcceptance extends Instrumentation {
    private int checks;
    private Bundle args;
    private volatile int captureOrientation;
    @Override public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        // Orientation changes recreate the native host. Apply the test request to each new instance.
        if(captureOrientation!=0) activity.setRequestedOrientation(captureOrientation==1
                ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); this.args = args; start(); }
    private void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
        Bundle progress=new Bundle();progress.putString("check",checks+": "+message);sendStatus(0,progress);
    }
    private void error(Context context, InputStream input, String expected) throws Exception {
        try { UserFontStore.importStream(context, input, "test.font"); throw new AssertionError("Import should fail: " + expected); }
        catch (UserFontStore.FontException e) { check(e.reason.equals(expected), "reason " + e.reason + " expected " + expected); }
        finally { input.close(); }
    }
    @Override public void onStart() {
        Context context = getTargetContext(); Bundle result = new Bundle();
        SharedPreferences prefs = context.getSharedPreferences("clock_prefs", 0);
        Map<String, ?> saved = new HashMap<>(prefs.getAll());
        Map<String, Map<String, ?>> auxiliary = new HashMap<>();
        for (String name : new String[]{"clockmods_onboarding", "clockmods_ultimate_style"})
            auxiliary.put(name, new HashMap<>(context.getSharedPreferences(name, 0).getAll()));
        Set<String> prior = new HashSet<>(); UserFontStore.initialize(context);
        for (UserFontStore.ImportedFile file : UserFontStore.files()) prior.add(file.id);
        File output = new File(context.getExternalFilesDir(null), "font-import-acceptance"); output.mkdirs();
        Activity activity = null; int code = Activity.RESULT_OK;
        try {
            ClockPreferences settings = new ClockPreferences(context);
            checkLocalizedDates(context);
            settings.setClockLanguage("en");
            checkDocumentImport(context, output);
            settings.setClockLanguage("zh-Hans");
            UserFontStore.ImportedFile regular;
            try (InputStream input = context.getAssets().open("fonts/GoogleSansDisplay-Regular.ttf")) { regular = UserFontStore.importStream(context, input, "Regular.ttf"); }
            String western = UserFontStore.familyId(regular.faces.get(0));
            error(context, context.getAssets().open("fonts/GoogleSansDisplay-Regular.ttf"), "duplicate");
            try (InputStream input = context.getAssets().open("fonts/GoogleSansDisplay-Medium.ttf")) { UserFontStore.importStream(context, input, "Medium.ttf"); }
            UserFontStore.ImportedFile bold;
            try (InputStream input = context.getAssets().open("fonts/GoogleSansDisplay-Bold.ttf")) { bold = UserFontStore.importStream(context, input, "Bold.ttf"); }
            check(Arrays.equals(UserFontStore.family(western).weights, new int[]{400,500,700}), "weights merged by family");
            error(context, new ByteArrayInputStream(new byte[]{0,1,2,3,4}), "invalid");
            byte[] raw;
            try (InputStream input = context.getAssets().open("fonts/GoogleSansDisplay-Regular.ttf")) { raw = read(input); }
            error(context, new ByteArrayInputStream(FontScriptSubset.build(raw, 0, true)), "language");
            for (String widgetFont : com.clockmods.widget.model.WidgetConfig.FONT_IDS)
                check(!UserFontStore.isImported(widgetFont), "widget fonts stay unchanged");
            int count = UserFontStore.files().size();
            check(count == prior.size() + 3, "invalid imports publish no files");
            check(ScriptFontPreferences.supports(western, "en"), "Latin filter includes Latin family");
            check(!ScriptFontPreferences.supports(western, "zh-Hans"), "Chinese filter excludes Latin family");
            UserFontStore.ImportedFile collection;
            try (InputStream input = new FileInputStream("/system/fonts/NotoSerifCJK-Regular.ttc")) { collection = UserFontStore.importStream(context, input, "CJK collection.ttc"); }
            check(collection.faces.size() > 1, "collection faces discovered");
            String script = null;
            for (OpenTypeFont font : collection.faces) if (ScriptFontPreferences.supportsFace(font, "zh-Hans")) { script = UserFontStore.familyId(font); break; }
            check(script != null, "Chinese face selectable");
            for (String language : new String[]{"ja", "ko"}) {
                boolean found = false;
                for (OpenTypeFont font : collection.faces) if (ScriptFontPreferences.supportsFace(font, language)) found = true;
                check(found, language + " coverage detected");
            }
            settings.setFontFamily("font.audit.a", western);
            settings.setFontWeight("font.audit.a", 500);
            settings.setScriptFontFamily("font.audit.a", script);
            settings.setScriptFontWeight("font.audit.a", 400);
            check(settings.getScriptFontFamily("font.audit.a").equals(script), "script stored independently");
            check(settings.getFontFamily("font.audit.a").equals(western), "Western stored independently");
            check(settings.getFontFamily("font.audit.b").equals(ClockPreferences.FONT_SYSTEM), "other theme Western unaffected");
            check(settings.getScriptFontFamily("font.audit.b").equals(ClockPreferences.FONT_SYSTEM), "other theme script unaffected");
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setTextSize(64);
            paint.setTypeface(ClockTypefaceResolver.resolve(context, settings.getRenderFontFamily("font.audit.a"), 500));
            check(paint.hasGlyph("中") && paint.hasGlyph("A") && paint.hasGlyph("한"), "mixed native fallback supplies scripts");
            Bitmap mixed = draw(paint, "中文 日本語 한국어 Aa 12:34，℃");
            save(mixed, new File(output, "mixed-fonts.png"));
            Paint reference = new Paint(paint); reference.setTypeface(UserFontStore.resolve(western, 500));
            check(draw(paint,"Aa 12:34").sameAs(draw(reference,"Aa 12:34")), "Western glyph pixels use selected Western file");
            reference.setTypeface(UserFontStore.resolve(script,400));
            check(draw(paint,"中文").sameAs(draw(reference,"中文")), "Chinese glyph pixels use selected Chinese file");
            settings.setFontFamily("font.audit.a", script); // A CJK file also carries Latin; it must not take the other script.
            settings.setScriptFontFamily("font.audit.a", ClockPreferences.FONT_SYSTEM);
            Paint mixedSystem = new Paint(paint); mixedSystem.setTypeface(ClockTypefaceResolver.resolve(context, settings.getRenderFontFamily("font.audit.a"),400));
            reference.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
            check(draw(mixedSystem,"中文").sameAs(draw(reference,"中文")), "CJK-bearing Western file cannot override system Chinese");
            settings.setFontFamily("font.audit.a", ClockPreferences.FONT_SYSTEM);
            settings.setScriptFontWeight("font.audit.a", 900);
            Paint weightedSystem = new Paint(paint);
            weightedSystem.setTypeface(ClockTypefaceResolver.resolve(context, settings.getRenderFontFamily("font.audit.a"), 400));
            reference.setTypeface(android.graphics.Typeface.create("sans-serif",0));
            check(!draw(weightedSystem,"中文").sameAs(draw(reference,"中文")), "system Chinese uses independent physical weight");
            reference.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.create("sans-serif",0),900,false));
            check(draw(weightedSystem,"中文").sameAs(draw(reference,"中文")), "system Chinese matches native selected weight");
            reference.setTypeface(android.graphics.Typeface.create("sans-serif",0));
            check(draw(weightedSystem,"Aa 12:34").sameAs(draw(reference,"Aa 12:34")), "script weight does not change Western weight");
            settings.setFontFamily("font.audit.a", western);
            check(UserFontStore.delete(bold.id), "individual weight file deleted");
            check(Arrays.equals(UserFontStore.family(western).weights,new int[]{400,500}), "slider stops shrink after delete");
            settings.setFontWeight("font.audit.a",700);
            check(FontCatalog.optionFor(settings.getFontFamily("font.audit.a")).nearestWeight(700)==500, "deleted weight snaps to remaining weight");
            prefs.edit().putFloat("calendar_marquee__calendar:calendar.graphite_speed",80f).putLong("calendar_marquee__calendar:calendar.graphite_pause",0).apply();
            check(settings.getCalendarMarquee("calendar.graphite").equals(com.clockmods.sdk.style.MarqueeSpec.DEFAULT), "old marquee settings ignored");
            for (String lang : new String[]{"ja","ko"}) {
                settings.setClockLanguage(lang);
                check(settings.getClockLanguage().equals(lang), "language round trip " + lang);
                check(LocaleManager.resolveLocale(context).getLanguage().equals(lang), "locale " + lang);
                String weekday=DateFormatter.preview("EEEE",LocaleManager.dateLang(lang));
                check(weekday.equals(lang.equals("ja") ? "金曜日" : "금요일"), "localized weekday " + weekday);
                Context localized=LocaleManager.wrap(context);
                check(localized.getString(R.string.ultimate_category_system).equals(lang.equals("ja") ? "システムと言語" : "시스템 및 언어"), "translated settings " + lang);
                for (int orientation : new int[]{1,2}) {
                    capture(context, new Intent(context,FontManagerActivity.class), output,
                            "manager-"+lang+"-"+orientation, orientation, false, null);
                    capture(context, UltimateSettingsActivity.createSubpageIntent(context,"system"), output,
                            "settings-"+lang+"-"+orientation, orientation, false, localized.getString(R.string.font_import_title));
                    capture(context, UltimateSettingsActivity.createSubpageIntent(context,"time_date"), output,
                            "date-editor-"+lang+"-"+orientation, orientation, false, localized.getString(R.string.ultimate_date_format));
                    context.getSharedPreferences("clockmods_onboarding",0).edit().putBoolean("completed",true).commit();
                    context.getSharedPreferences("clockmods_ultimate_style",0).edit().putString("style_id","typographic.poster").commit();
                    prefs.edit().putBoolean("weather_enabled",false).putBoolean("use_network_time",false)
                            .putBoolean("hourly_visual_chime",false).putBoolean("half_hour_visual_chime",false)
                            .putInt("screen_orientation",orientation).putString("calendar_theme","calendar.graphite").commit();
                    for (String scope : new String[]{"typographic.poster",ClockPreferences.calendarScope("calendar.graphite")}) {
                        settings.setFontFamily(scope,western);settings.setFontWeight(scope,500);
                        settings.setScriptFontFamily(scope,script);settings.setScriptFontWeight(scope,400);
                    }
                    Intent live = new Intent().setClassName(context.getPackageName(),"com.clockmods.ultimate.UltimateMainActivity");
                    capture(context,live,output,"clock-"+lang+"-"+orientation,orientation,false,null);
                    capture(context,new Intent(live).putExtra("com.clockmods.ultimate.extra.DESTINATION","calendar"),output,
                            "calendar-"+lang+"-"+orientation,orientation,true,null);
                }
            }
            checkChineseCalendar(context, output);
            checks += ClockLayoutRefinementProbe.run(context, output);
            checks += ClockLayoutRefinementProbe.fonts(context, output, western, script);
            for (UserFontStore.ImportedFile file : new ArrayList<>(UserFontStore.files())) if (file.faces.get(0).identity.equals(regular.faces.get(0).identity) && !prior.contains(file.id)) UserFontStore.delete(file.id);
            check(settings.getFontFamily("font.audit.a").equals(ClockPreferences.FONT_SYSTEM), "deleted family falls back to system");
            result.putString("result", "PASS " + checks + " checks");
        } catch (Throwable failure) {
            code=Activity.RESULT_CANCELED; StringWriter text=new StringWriter();failure.printStackTrace(new PrintWriter(text));result.putString("result","FAIL after "+checks+" checks\n"+text);
        } finally {
            if(activity!=null){Activity closing=activity;runOnMainSync(closing::finish);}
            getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);
            if (args == null || !"true".equals(args.getString("keep"))) {
                for(UserFontStore.ImportedFile file:new ArrayList<>(UserFontStore.files()))if(!prior.contains(file.id))UserFontStore.delete(file.id);
                SharedPreferences.Editor edit=prefs.edit().clear();
                for(Map.Entry<String,?> entry:saved.entrySet()) {
                    Object v=entry.getValue();String k=entry.getKey();
                    if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);
                    else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);
                }
                edit.commit();
                for (Map.Entry<String,Map<String,?>> backup : auxiliary.entrySet()) {
                    SharedPreferences.Editor restore=context.getSharedPreferences(backup.getKey(),0).edit().clear();
                    for(Map.Entry<String,?> entry:backup.getValue().entrySet()) {
                        Object v=entry.getValue();String k=entry.getKey();
                        if(v instanceof String)restore.putString(k,(String)v);else if(v instanceof Boolean)restore.putBoolean(k,(Boolean)v);
                        else if(v instanceof Integer)restore.putInt(k,(Integer)v);else if(v instanceof Long)restore.putLong(k,(Long)v);else if(v instanceof Float)restore.putFloat(k,(Float)v);
                    }
                    restore.commit();
                }
            }
        }
        finish(code,result);
    }
    /** Exercise the real document URI, activity-result callback, worker and management buttons.
     * The OS picker choice is supplied by an ActivityMonitor so the test needs no external files. */
    private void checkChineseCalendar(Context context, File output) throws Exception {
        ClockPreferences settings=new ClockPreferences(context);
        SharedPreferences prefs=context.getSharedPreferences("clock_prefs",0);
        settings.setClockLanguage("en");
        prefs.edit().remove("show_chinese_calendar").commit();
        check(settings.isChineseCalendarEnabled(),"Chinese calendar defaults on");
        settings.setShowLunar(true);
        for(String language:new String[]{"en","ja","ko"}) {
            settings.setClockLanguage(language);settings.setChineseCalendarEnabled(false);
            check(settings.canToggleChineseCalendar(),"toggle available "+language);
            check(!settings.isChineseCalendarEnabled() && !settings.isShowLunarInApp(),"clock lunar suppressed "+language);
            check(settings.isShowLunar(),"original lunar preference preserved "+language);
            prefs.edit().putBoolean("weather_enabled",false).putBoolean("use_network_time",false)
                    .putBoolean("hourly_visual_chime",false).putBoolean("half_hour_visual_chime",false).commit();
            context.getSharedPreferences("clockmods_onboarding",0).edit().putBoolean("completed",true).commit();
            Context localized=LocaleManager.wrap(context);
            capture(context,UltimateSettingsActivity.createSubpageIntent(context,"system"),output,
                    "chinese-calendar-switch-"+language,1,false,localized.getString(R.string.chinese_calendar_title));
            for(int orientation:new int[]{1,2}) {
                prefs.edit().putInt("screen_orientation",orientation).commit();
                Intent live=new Intent().setClassName(context.getPackageName(),"com.clockmods.ultimate.UltimateMainActivity");
                capture(context,live,output,"clock-hidden-"+language+"-"+orientation,orientation,false,null);
                for(String theme:new String[]{"graphite","carbon","paper","poster","agenda"}) {
                    prefs.edit().putString("calendar_theme","calendar."+theme).commit();
                    capture(context,new Intent(live).putExtra("com.clockmods.ultimate.extra.DESTINATION","calendar"),output,
                            "calendar-hidden-"+language+"-"+theme+"-"+orientation,orientation,true,null);
                }
            }
            settings.setChineseCalendarEnabled(true);
            check(settings.isShowLunarInApp(),"clock lunar restored "+language);
        }
        settings.setChineseCalendarEnabled(false);
        for(String language:new String[]{"zh-Hans","zh-Hant"}) {
            settings.setClockLanguage(language);
            check(!settings.canToggleChineseCalendar() && settings.isShowLunarInApp(),"Chinese locale unaffected "+language);
        }
        settings.setClockLanguage("en");
        check(!settings.isChineseCalendarEnabled(),"foreign-language choice survives language changes");
        settings.setChineseCalendarEnabled(true);settings.setShowLunar(false);
        check(!settings.isShowLunarInApp(),"master switch preserves separately disabled lunar text");
    }

    private void checkLocalizedDates(Context context) {
        ClockPreferences settings=new ClockPreferences(context);
        SharedPreferences prefs=context.getSharedPreferences("clock_prefs",0);
        prefs.edit().remove("date_pattern_ja").remove("date_pattern_ko").commit();
        for(String language:new String[]{"ja","ko"}) {
            settings.setClockLanguage(language);
            check(settings.getDatePattern().equals(DateFormatter.defaultPattern(LocaleManager.dateLang(language))),"localized default "+language);
        }
        String[] languages={"zh-Hans","en","ja","ko"};
        String[] patterns={"yyyy年M月d日","MMMM d, yyyy","yyyy/MM/dd（E）","yyyy. M. d. (E)"};
        for(int i=0;i<languages.length;i++)check(settings.saveDatePattern(languages[i],patterns[i]),"save language pattern "+languages[i]);
        for(int i=0;i<languages.length;i++) {
            settings.setClockLanguage(languages[i]);
            check(settings.getDatePattern().equals(patterns[i]),"independent language pattern "+languages[i]);
            check(!settings.saveDatePattern(languages[i],"'unfinished") && settings.getDatePattern().equals(patterns[i]),"invalid draft preserves pattern "+languages[i]);
        }
        for(String language:new String[]{"zh-Hans","zh-Hant","en","ja","ko"}) {
            settings.setClockLanguage(language);Context localized=LocaleManager.wrap(context);
            check(localized.getString(R.string.ultimate_language_simplified).equals("简体中文")
                    && localized.getString(R.string.ultimate_language_traditional).equals("繁體中文")
                    && localized.getString(R.string.ultimate_language_english).equals("English")
                    && localized.getString(R.string.ultimate_language_japanese).equals("日本語")
                    && localized.getString(R.string.ultimate_language_korean).equals("한국어"),"language autonyms "+language);
        }
    }
    private android.view.accessibility.AccessibilityNodeInfo actionFor(String label) {
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
        if(root==null)return null;
        for(android.view.accessibility.AccessibilityNodeInfo found:root.findAccessibilityNodeInfosByText(label)) {
            android.view.accessibility.AccessibilityNodeInfo node=found;
            for(int depth=0;node!=null && depth<5;depth++,node=node.getParent())
                if(node.isClickable() && node.isEnabled())return node;
        }
        return null;
    }
    private boolean clickLabel(String label) {
        long until=SystemClock.uptimeMillis()+8000;
        while(SystemClock.uptimeMillis()<until) {
            android.view.accessibility.AccessibilityNodeInfo node=actionFor(label);
            if(node!=null && node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))return true;
            android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            if(root!=null)scroll(root);SystemClock.sleep(200);
        }
        return false;
    }
    private android.view.accessibility.AccessibilityNodeInfo editable(android.view.accessibility.AccessibilityNodeInfo node) {
        if(node==null)return null;if(node.isEditable())return node;
        for(int i=0;i<node.getChildCount();i++) {
            android.view.accessibility.AccessibilityNodeInfo result=editable(node.getChild(i));if(result!=null)return result;
        }
        return null;
    }
    private void editPattern(String pattern) {
        android.view.accessibility.AccessibilityNodeInfo field=editable(getUiAutomation().getRootInActiveWindow());
        Bundle value=new Bundle();value.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,pattern);
        check(field!=null && field.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,value),"date pattern field accepts input");
        SystemClock.sleep(350);
    }
    private void inspectDateEditor(Context context,File output,String name) throws Exception {
        ClockPreferences settings=new ClockPreferences(context);Context localized=LocaleManager.wrap(context);
        String previous=settings.getDatePattern();
        check(clickLabel(localized.getString(R.string.ultimate_date_format)),"date editor opens "+name);
        SystemClock.sleep(500);
        editPattern("'unfinished");
        check(actionFor(localized.getString(R.string.ultimate_apply))==null,"invalid date disables Apply "+name);
        check(previous.equals(settings.getDatePattern()),"invalid editor preserves saved date "+name);
        String pattern=settings.getClockLanguage().equals("ja")?"yyyy年M月d日（E）":"yyyy. M. d. (E)";
        editPattern(pattern);
        check(actionFor(localized.getString(R.string.ultimate_apply))!=null,"valid date enables Apply "+name);
        save(getUiAutomation().takeScreenshot(),new File(output,name+".png"));
        check(clickLabel(localized.getString(R.string.ultimate_apply)),"date Apply clicked "+name);
        SystemClock.sleep(400);
        check(pattern.equals(settings.getDatePattern()),"date editor saves current language "+name);
    }

    private void checkDocumentImport(Context context, File output) throws Exception {
        android.accessibilityservice.AccessibilityServiceInfo service=getUiAutomation().getServiceInfo();
        service.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        getUiAutomation().setServiceInfo(service);
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"ClockMods-font-audit.ttf");
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"font/ttf");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/ClockModsFontAudit");
        android.net.Uri uri=context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        check(uri!=null,"document fixture created");
        Set<String> before=new HashSet<>();for(UserFontStore.ImportedFile file:UserFontStore.files())before.add(file.id);
        Activity manager=null;ActivityMonitor picker=null;
        try {
            try(InputStream input=context.getAssets().open("fonts/GoogleSansDisplay-Regular.ttf");
                OutputStream stream=context.getContentResolver().openOutputStream(uri)) { stream.write(read(input)); }
            manager=startActivitySync(new Intent(context,FontManagerActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            waitForIdleSync();
            android.content.IntentFilter filter=new android.content.IntentFilter(Intent.ACTION_OPEN_DOCUMENT);
            filter.addCategory(Intent.CATEGORY_OPENABLE); filter.addDataType("*/*");
            picker=addMonitor(filter,
                    new ActivityResult(Activity.RESULT_OK,new Intent().setData(uri)),true);
            Activity active=manager;
            check(clickLabel(active.getString(R.string.font_import_add)),"import button clicked");
            SystemClock.sleep(300);
            check(picker.getHits()==1,"document picker launched");
            long end=SystemClock.uptimeMillis()+10000;
            while(UserFontStore.files().size()==before.size() && SystemClock.uptimeMillis()<end)SystemClock.sleep(100);
            check(UserFontStore.files().size()==before.size()+1,"document callback imports font");
            SystemClock.sleep(300);
            save(getUiAutomation().takeScreenshot(),new File(output,"document-import.png"));
            clickLabel(active.getString(R.string.font_import_add));
            end=SystemClock.uptimeMillis()+10000;
            boolean duplicate=false;
            while(SystemClock.uptimeMillis()<end) {
                boolean[] found={getUiAutomation().getRootInActiveWindow()!=null && spokenText(getUiAutomation().getRootInActiveWindow()).contains(active.getString(R.string.font_import_duplicate))};
                if(found[0]){duplicate=true;break;}SystemClock.sleep(100);
            }
            check(duplicate && UserFontStore.files().size()==before.size()+1,"repeat picker upload shows duplicate feedback");
            check(clickLabel(active.getString(R.string.font_import_delete)),"delete action clicked");
            SystemClock.sleep(300);
            check(clickLabel(active.getString(R.string.font_import_delete)),"file deletion confirmed");
            end=SystemClock.uptimeMillis()+10000;
            while(UserFontStore.files().size()!=before.size() && SystemClock.uptimeMillis()<end)SystemClock.sleep(100);
            check(UserFontStore.files().size()==before.size(),"manager deletes individual file");
        } finally {
            if(picker!=null)removeMonitor(picker);
            if(manager!=null){Activity close=manager;runOnMainSync(close::finish);}
            for(UserFontStore.ImportedFile file:new ArrayList<>(UserFontStore.files()))if(!before.contains(file.id))UserFontStore.delete(file.id);
            context.getContentResolver().delete(uri,null,null);
        }
    }
    private static boolean clickButton(android.view.View view,String text) {
        if(view instanceof android.widget.Button && text.contentEquals(((android.widget.Button)view).getText()))return view.performClick();
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)
            if(clickButton(((android.view.ViewGroup)view).getChildAt(i),text))return true;
        return false;
    }
    private static boolean containsText(android.view.View view,String text) {
        if(view instanceof android.widget.TextView && text.contentEquals(((android.widget.TextView)view).getText()))return true;
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)
            if(containsText(((android.view.ViewGroup)view).getChildAt(i),text))return true;
        return false;
    }
    private void capture(Context context, Intent intent, File output, String name, int orientation,
            boolean calendar, String scrollTarget) throws Exception {
        ActivityMonitor monitor=null;
        Activity shown=null;
        try {
            captureOrientation=orientation;
            SystemClock.sleep(300);
            // Activity embedding can ignore per-activity orientation requests on large displays.
            // Rotate the test display instead; UiAutomation restores its prior state on disconnect.
            check(getUiAutomation().setRotation(orientation==1?android.app.UiAutomation.ROTATION_FREEZE_0:
                    android.app.UiAutomation.ROTATION_FREEZE_90),"display rotation "+name);
            SystemClock.sleep(700);
            monitor=addMonitor(intent.getComponent().getClassName(),null,false);
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            shown=monitor.waitForActivityWithTimeout(10000);
            check(shown!=null,"screen opened "+name);
            // The native clock renders continuously, so its main queue need not become idle.
            SystemClock.sleep(600);
            long rotateUntil=SystemClock.uptimeMillis()+10000;
            int stableFrames=0;
            while(SystemClock.uptimeMillis()<rotateUntil) {
                getUiAutomation().setRotation(orientation==1?android.app.UiAutomation.ROTATION_FREEZE_0:
                        android.app.UiAutomation.ROTATION_FREEZE_90);
                Bitmap frame=getUiAutomation().takeScreenshot();
                boolean ready=frame!=null && ((frame.getWidth()<frame.getHeight())==(orientation==1));
                if(frame!=null)frame.recycle();
                stableFrames=ready?stableFrames+1:0;
                if(stableFrames>=3)break;
                SystemClock.sleep(200);
            }
            SystemClock.sleep(500);
            if(monitor.getLastActivity()!=null)shown=monitor.getLastActivity();
            if(calendar) {
                Activity active=shown;
                runOnMainSync(()->{
                    int id=active.getResources().getIdentifier("pro_pager","id",context.getPackageName());
                    android.view.View pager=id==0?null:active.findViewById(id);
                    if(pager!=null)try {pager.getClass().getMethod("setCurrentItem",int.class,boolean.class).invoke(pager,1,false);}
                    catch(Exception failure){throw new RuntimeException(failure);}
                });
                SystemClock.sleep(800);
                if(!new ClockPreferences(context).isChineseCalendarEnabled()) {
                    Activity inspected=shown;
                    runOnMainSync(()->checkHiddenCalendarState(inspected));
                }
            }
            if(scrollTarget!=null)for(int attempt=0;attempt<8;attempt++) {
                android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
                if(root==null)break;
                java.util.List<android.view.accessibility.AccessibilityNodeInfo> targets=root.findAccessibilityNodeInfosByText(scrollTarget);
                if(!targets.isEmpty()) {targets.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());SystemClock.sleep(250);break;}
                if(!scroll(root))break;SystemClock.sleep(250);
            }
            if(name.startsWith("date-editor-"))inspectDateEditor(context,output,name);
            if(name.contains("-hidden-")) {
                android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
                check(root!=null && context.getPackageName().contentEquals(root.getPackageName()),"hidden-content screen visible "+name);
                String spoken=spokenText(root);
                check(!spoken.contains("宜") && !spoken.contains("忌") && !spoken.contains("农历") && !spoken.contains("農曆"),"almanac absent from accessibility "+name);
                if(name.contains("-hidden-en-")) check(spoken.codePoints().noneMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN),"English calendar contains no Chinese annotations "+name);
            }
            Bitmap screen=getUiAutomation().takeScreenshot();
            boolean oriented=screen!=null && ((screen.getWidth()<screen.getHeight())==(orientation==1));
            if(!oriented) {
                Bundle diagnostic=new Bundle();
                diagnostic.putString("orientation", "expected="+orientation+" pref="+new ClockPreferences(context).getScreenOrientation()
                        +" requested="+shown.getRequestedOrientation()+" config="+shown.getResources().getConfiguration().orientation
                        +" display="+(screen==null?"null":screen.getWidth()+"x"+screen.getHeight())
                        +" visible="+spokenText(getUiAutomation().getRootInActiveWindow()));
                sendStatus(0,diagnostic);
                if(screen!=null)save(screen,new File(output,"failure-"+name+".png"));
            }
            check(oriented,"screen orientation "+name);
            save(screen,new File(output,name+(name.startsWith("date-editor-")?"-saved":"")+".png"));
        } finally {
            if(monitor!=null && monitor.getLastActivity()!=null)shown=monitor.getLastActivity();
            if(shown!=null){Activity close=shown;runOnMainSync(close::finish);}
            if(monitor!=null)removeMonitor(monitor);
            captureOrientation=0;
        }
    }
    private boolean scroll(android.view.accessibility.AccessibilityNodeInfo node) {
        if(node.isScrollable() && node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true;
        for(int i=0;i<node.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo child=node.getChild(i);if(child!=null&&scroll(child))return true;}
        return false;
    }
    private void checkHiddenCalendarState(Activity activity) {
        com.clockmods.pro.style.CalendarPageState state=null;
        try {
            for(androidx.fragment.app.Fragment fragment:((androidx.fragment.app.FragmentActivity)activity).getSupportFragmentManager().getFragments()) {
                if(fragment instanceof com.clockmods.pro.ProCalendarFragment) {
                    java.lang.reflect.Field field=fragment.getClass().getDeclaredField("pageState");field.setAccessible(true);
                    state=(com.clockmods.pro.style.CalendarPageState)field.get(fragment);
                }
            }
        } catch(Exception e) { throw new AssertionError(e); }
        check(state!=null,"live calendar state attached");
        boolean clean=true;
        for(com.clockmods.pro.style.CalendarPageState.DayInfo day:state.days)
            clean &= day.lunarShort.isEmpty() && day.festivals.isEmpty() && day.holidayBadge.isEmpty() && day.holidayName.isEmpty();
        check(clean,"all visible calendar cells omit Chinese calendar data");
        check(state.selection.suitableLine.isEmpty() && state.selection.avoidLine.isEmpty()
                && state.selection.lunarLine.isEmpty() && state.selection.festivalsLine.isEmpty(),"calendar detail omits lunar and almanac");
    }
    private static String spokenText(android.view.accessibility.AccessibilityNodeInfo node) {
        StringBuilder text=new StringBuilder();
        if(node.getText()!=null)text.append(node.getText());
        if(node.getContentDescription()!=null)text.append(node.getContentDescription());
        for(int i=0;i<node.getChildCount();i++) {
            android.view.accessibility.AccessibilityNodeInfo child=node.getChild(i);
            if(child!=null)text.append(spokenText(child));
        }
        return text.toString();
    }
    private static byte[] read(InputStream input)throws IOException { ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;while((n=input.read(b))!=-1)out.write(b,0,n);return out.toByteArray(); }
    private static Bitmap draw(Paint paint,String text) { Bitmap b=Bitmap.createBitmap(1400,130,Bitmap.Config.ARGB_8888);new Canvas(b).drawText(text,20,90,paint);return b; }
    private static void save(Bitmap bitmap,File file)throws IOException {try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);} bitmap.recycle();}
}
