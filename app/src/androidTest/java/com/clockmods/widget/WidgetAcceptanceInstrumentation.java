package com.clockmods.widget;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.clockmods.R;
import com.clockmods.widget.model.*;
import com.clockmods.widget.store.*;
import com.clockmods.widget.render.*;
import com.clockmods.widget.update.*;
import com.clockmods.widget.config.WidgetConfigActivity;
import java.io.*;
import java.util.*;
/** Device tests deliberately exercise real RemoteViews reflection and launcher binding APIs. */
public final class WidgetAcceptanceInstrumentation extends Instrumentation {
    private Context app;
    private int checks;
    private int rendered;
    private boolean onlineWeather;
    private boolean fontProbe;
    private void check(boolean value,String message) { checks++; if(!value) throw new AssertionError(message); }
    @Override public void onCreate(Bundle args) { super.onCreate(args); onlineWeather=args!=null && "online".equals(args.getString("weather")); fontProbe=args!=null && "true".equals(args.getString("fonts")); start(); }
    @Override public void onStart() {
        if(fontProbe) { runFontProbe(); return; }
        Bundle report=new Bundle();
        AppWidgetHost host=null; int resultCode=Activity.RESULT_CANCELED;
        try {
            app=getTargetContext();
            check(Build.VERSION.SDK_INT>=31,"requires API 31+");
            java.util.List<AppWidgetProviderInfo> providers=new ArrayList<>();
            for(AppWidgetProviderInfo info:AppWidgetManager.getInstance(app).getInstalledProviders())
                if(info.provider.getPackageName().equals(app.getPackageName())) providers.add(info);
            check(providers.size()==4,"four picker entries");
            getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
            host=new AppWidgetHost(app,9917);host.deleteHost();host.startListening();
            int baselineWeather=WidgetUpdateCoordinator.weatherCount(app);
            ArrayList<Integer> ids=new ArrayList<>();
            for(AppWidgetProviderInfo info:providers) {
                check(info.configure!=null && info.previewLayout!=0,"picker configuration and preview");
                int id=host.allocateAppWidgetId();ids.add(id);
                check(AppWidgetManager.getInstance(app).bindAppWidgetIdIfAllowed(id,info.provider),"bind "+info.provider);
                WidgetKind kind=WidgetUpdateCoordinator.kindFor(app,id);check(kind!=null,"provider detection");
                WidgetConfig saved=WidgetConfig.builder(id,kind).themeId("paper.warm").useSystemTimeZone(false).timeZoneId("Asia/Shanghai").build();
                new WidgetConfigStore(app).save(saved);
                WidgetUpdateCoordinator.updateOne(app,id);
                // Render each size class at a realistic box derived from the widget's own declared
                // minimum, so the test exercises the contract the Launcher honours rather than an
                // arbitrary number. AppWidgetProviderInfo dimensions have already been resolved to px.
                float density=app.getResources().getDisplayMetrics().density;
                int minW=Math.round(info.minResizeWidth/density),minH=Math.round(info.minResizeHeight/density);
                // A one-row card is the contract of a one-row widget. A widget that declares a taller
                // minimum - the weather card needs its condition row - never renders this class, so
                // there is no honest box to test it in.
                boolean hasRowClass=minH<100;
                for(WidgetThemeSpec theme:WidgetThemeRegistry.all(app)) {
                    for(WidgetSizeClass size:WidgetSizeClass.values()) {
                      if(size==WidgetSizeClass.ROW && !hasRowClass) continue;
                      for(String font:WidgetConfig.FONT_IDS) {
                        WidgetConfig config=saved.toBuilder().themeId(theme.id).fontId(font).textScale(1.2f).useSystemTimeFormat(false).use24Hour(false).showSeconds(true).backgroundAlpha(theme.defaultBackgroundAlpha).build();
                        RemoteViews rv=WidgetRemoteViewsFactory.createAt(app,config,size,java.time.Instant.parse("2026-10-01T08:00:00Z").toEpochMilli(),weatherFixture());
                        final Throwable[] error={null};
                        runOnMainSync(()-> { try {
                            FrameLayout parent=new FrameLayout(app);View view=rv.apply(app,parent);parent.addView(view);
                            int width,height;
                            int compactW=minW,compactH=minH;
                            if(compactW>=180 && compactH>=100) compactW=179;
                            int smallW=Math.max(minW,180),smallH=Math.max(minH,110);
                            int wideW=260,largeH=180;
                            if(size==WidgetSizeClass.COMPACT) { width=compactW;height=compactH; }
                            else if(size==WidgetSizeClass.SMALL) { width=smallW;height=smallH; }
                            else if(size==WidgetSizeClass.WIDE) { width=wideW;height=100; }
                            else if(size==WidgetSizeClass.TALL) { width=smallW;height=largeH; }
                            else if(size==WidgetSizeClass.ROW) { width=(int)WidgetSizeClassResolver.ROW_MIN_WIDTH;height=Math.min(minH,56); }
                            else { width=wideW;height=largeH; }
                            check(WidgetSizeClassResolver.resolve(width,height)==size,"test box matches requested size class");
                            int w=(int)(width*app.getResources().getDisplayMetrics().density),h=(int)(height*app.getResources().getDisplayMetrics().density);
                            parent.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));parent.layout(0,0,w,h);
                            assertVisibleBounds(view,view);
                            check(view.findViewById(R.id.widget_zone).getVisibility()==View.GONE,"timezone name is never displayed");
                            rendered++;
                            check(view.findViewById(R.id.widget_settings).isClickable(),"settings bound");
                            int mainId=kind==WidgetKind.CALENDAR ? R.id.widget_day : kind==WidgetKind.ANALOG ? R.id.widget_analog : R.id.widget_time;
                            View main=view.findViewById(mainId);
                            check(main.getTop()>=0 && main.getBottom()<=((View)main.getParent()).getHeight(),"main content fits vertically");
                            if(main instanceof TextView) check(((TextView)main).getLayout().getEllipsisCount(0)==0,"main text is not truncated");
                            if(kind==WidgetKind.DIGITAL || kind==WidgetKind.WEATHER) check(view.findViewById(R.id.widget_time) instanceof TextClock,"live text clock");
                            if(kind==WidgetKind.ANALOG) check(view.findViewById(R.id.widget_analog) instanceof AnalogClock,"live analog clock");
                            View caption=view.findViewById(R.id.widget_date);
                            if(caption!=null && caption.getVisibility()==View.VISIBLE) {
                                check(leftInRoot(caption)>=app.getResources().getDimensionPixelSize(R.dimen.widget_inset_start)-1,
                                        "caption clears the corner fan ("+font+", left="+leftInRoot(caption)+")");
                                check(caption.getTop()>=0 && caption.getBottom()<=((View)caption.getParent()).getHeight(),"caption fits vertically");
                            }
                            if(size==WidgetSizeClass.LARGE) {
                                Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);parent.draw(new Canvas(image));
                                File dir=new File(app.getCacheDir(),"widget-acceptance");dir.mkdirs();
                                try(FileOutputStream out=new FileOutputStream(new File(dir,kind+"-"+theme.id+"-"+font+".png"))) { image.compress(Bitmap.CompressFormat.PNG,100,out); }
                            }
                        } catch(Throwable e) {error[0]=e;} });
                        if(error[0]!=null) throw new AssertionError(kind+" "+theme.id+" "+size+" "+font,error[0]);
                      }
                    }
                }
                verifyMinimumSizes(info,saved);
                if(kind==WidgetKind.DIGITAL) verifyAppTimeZone(host,id,info);
                String before=WidgetConfigStore.encode(new WidgetConfigStore(app).getOrDefault(id,kind));
                Activity config=startActivitySync(new Intent(app,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                waitForControl(config,R.id.widget_config_date);
                runOnMainSync(()-> { CompoundButton toggle=config.findViewById(R.id.widget_config_date);check(toggle!=null,"configuration loaded");toggle.toggle();config.finish(); });
                waitForIdleSync();
                check(before.equals(WidgetConfigStore.encode(new WidgetConfigStore(app).getOrDefault(id,kind))),"cancel leaves saved instance untouched");
                Activity save=startActivitySync(new Intent(app,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                waitForControl(save,R.id.widget_config_date);
                runOnMainSync(()-> { ((CompoundButton)save.findViewById(R.id.widget_config_date)).toggle();save.findViewById(R.id.widget_config_done).performClick(); });
                long deadline=SystemClock.uptimeMillis()+5000;
                while(new WidgetConfigStore(app).getOrDefault(id,kind).showDate && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(20);
                check(!new WidgetConfigStore(app).getOrDefault(id,kind).showDate,"Done persists the draft");
                waitForIdleSync();
            }
            if(onlineWeather) {
                com.clockmods.weather.WeatherRefreshUseCase.RefreshResult weather=new com.clockmods.weather.WeatherRefreshUseCase(app).refreshForWidget(true);
                check(weather==com.clockmods.weather.WeatherRefreshUseCase.RefreshResult.SUCCESS,"live weather refresh: "+weather);
            }
            check(WidgetUpdateCoordinator.active(app).size()>=4,"active instance isolation");
            int weatherId=-1;
            for(int id:ids) if(WidgetUpdateCoordinator.kindFor(app,id)==WidgetKind.WEATHER) weatherId=id;
            check(weatherId!=-1,"weather instance present");
            check(WidgetWeatherIconFactory.render(app,"invalid",false,Color.WHITE,999).getWidth()==144,"bounded fallback bitmap");
            for(int id:ids) host.deleteAppWidgetId(id);
            WidgetUpdateCoordinator.reconcile(app);
            check(WidgetUpdateCoordinator.weatherCount(app)==baselineWeather,"existing weather instances preserved");
            if(baselineWeather==0)
            for(androidx.work.WorkInfo work:androidx.work.WorkManager.getInstance(app).getWorkInfosForUniqueWork(WidgetRefreshWorker.PERIODIC).get())
                check(work.getState().isFinished(),"weather periodic work cancelled");
            report.putString("stream","PASS: "+checks+" device checks; "+WidgetConfig.FONT_IDS.length+" fonts; "+rendered+" real RemoteViews layouts across "+WidgetSizeClass.values().length+" size classes and 6 themes, plus provider binding, preview and cancel isolation.\n");
            resultCode=Activity.RESULT_OK;
        } catch(Throwable e) {
            StringWriter trace=new StringWriter();e.printStackTrace(new PrintWriter(trace));report.putString("stream",trace.toString());
        } finally {
            if(host!=null) {host.deleteHost();host.stopListening();}
            if(app!=null) WidgetUpdateCoordinator.reconcile(app);
            getUiAutomation().dropShellPermissionIdentity();
        }
        finish(resultCode,report);
    }

    private static com.clockmods.weather.WeatherModels.WeatherDisplayData weatherFixture() {
        return new com.clockmods.weather.WeatherModels.WeatherDisplayData("101010100","Beijing","Haidian","Light rain","305","23",java.time.Instant.parse("2026-10-01T07:30:00Z").toEpochMilli());
    }

    private void runFontProbe() {
        Bundle report=new Bundle();
        try {
            app=getTargetContext();
            Context foreign=app.createPackageContext("android",Context.CONTEXT_RESTRICTED);
            final Throwable[] error={null};
            runOnMainSync(()-> { try {
                for(WidgetKind kind:WidgetKind.values()) for(WidgetSizeClass size:WidgetSizeClass.values())
                for(WidgetThemeSpec theme:WidgetThemeRegistry.all(app)) for(String font:WidgetConfig.FONT_IDS) {
                    WidgetConfig config=WidgetConfig.builder(1,kind).themeId(theme.id).fontId(font).build();
                    RemoteViews views=WidgetRemoteViewsFactory.create(app,config,size);
                    // Round-trip like the launcher receives over Binder, not an in-process preview.
                    Parcel parcel=Parcel.obtain();
                    try { views.writeToParcel(parcel,0);parcel.setDataPosition(0);views=RemoteViews.CREATOR.createFromParcel(parcel); }
                    finally { parcel.recycle(); }
                    View own=views.apply(app,new FrameLayout(app));
                    View hosted=views.apply(foreign,new FrameLayout(foreign));
                    int main=kind==WidgetKind.CALENDAR ? R.id.widget_day : kind==WidgetKind.ANALOG ? R.id.widget_date : R.id.widget_time;
                    TextView expected=own.findViewById(main),actual=hosted.findViewById(main);
                    check(actual.getContext().isRestricted(),"font check uses a restricted host context");
                    check(expected.getTypeface().equals(actual.getTypeface()),"font differs between preview and restricted host: "+font);
                    int themeColumn=theme.fontLayoutVariant==WidgetThemeSpec.Font.SERIF ? 1 : theme.fontLayoutVariant==WidgetThemeSpec.Font.MONOSPACE ? 2 : 0;
                    String[] families={"sans-serif","serif","monospace","sans-serif-condensed","sans-serif-light"};
                    check(Typeface.create(families[WidgetFontRegistry.columnOf(font,themeColumn)],Typeface.NORMAL).equals(actual.getTypeface()),"selected font is rendered: "+font);
                }
            } catch(Throwable e) { error[0]=e; } });
            if(error[0]!=null) throw new AssertionError(error[0]);
            verifyFontSelection();
            report.putString("stream","PASS: "+checks+" restricted-host font checks, plus preview, save, host update and reopen\n");
            finish(Activity.RESULT_OK,report);
        } catch(Throwable e) {
            StringWriter trace=new StringWriter();e.printStackTrace(new PrintWriter(trace));report.putString("stream",trace.toString());
            finish(Activity.RESULT_CANCELED,report);
        }
    }

    private void verifyFontSelection() throws Exception {
        Context restricted=app.createPackageContext(app.getPackageName(),Context.CONTEXT_RESTRICTED);
        getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");
        AppWidgetHost host=new AppWidgetHost(app,9918);
        host.deleteHost();
        host.startListening();
        Activity activity=null;
        try {
            for(AppWidgetProviderInfo info:AppWidgetManager.getInstance(app).getInstalledProviders()) {
                if(!info.provider.getPackageName().equals(app.getPackageName())) continue;
                int id=host.allocateAppWidgetId();
                check(AppWidgetManager.getInstance(app).bindAppWidgetIdIfAllowed(id,info.provider),"font host bound");
                WidgetKind kind=WidgetUpdateCoordinator.kindFor(app,id);
                WidgetConfig initial=WidgetConfig.builder(id,kind).themeId("paper.warm").build();
                new WidgetConfigStore(app).save(initial);
                final AppWidgetHostView[] hosted={null};
                runOnMainSync(()->hosted[0]=host.createView(restricted,id,info));
                for(String font:new String[]{"monospace","serif","condensed","light","system","theme"}) {
                    Bundle progress=new Bundle();progress.putString("stream","Checking font flow: "+kind+" "+font+"\n");sendStatus(0,progress);
                    activity=startActivitySync(new Intent(app,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    waitForControl(activity,R.id.widget_config_font);
                    final Activity current=activity;
                    runOnMainSync(()-> {
                        android.widget.AutoCompleteTextView field=current.findViewById(R.id.widget_config_font);
                        field.getOnItemClickListener().onItemClick(null,null,WidgetFontRegistry.indexOf(font),0);
                    });
                    final int textId=kind==WidgetKind.CALENDAR ? R.id.widget_day : kind==WidgetKind.ANALOG ? R.id.widget_date : R.id.widget_time;
                    final String[] families={"sans-serif","serif","monospace","sans-serif-condensed","sans-serif-light"};
                    final Typeface expected=Typeface.create(families[WidgetFontRegistry.columnOf(font,1)],Typeface.NORMAL);
                    awaitFont(current.findViewById(R.id.widget_config_preview),textId,expected,"preview "+kind+" "+font);
                    runOnMainSync(()->current.findViewById(R.id.widget_config_done).performClick());
                    awaitFont(hosted[0],textId,expected,"saved host "+kind+" "+font);
                    check(font.equals(new WidgetConfigStore(app).getOrDefault(id,kind).fontId),"selected font persisted");
                    long deadline=SystemClock.uptimeMillis()+5000;
                    while(!current.isDestroyed() && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(25);
                    check(current.isDestroyed(),"configuration closes after saving");
                }
                activity=startActivitySync(new Intent(app,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                waitForControl(activity,R.id.widget_config_font);
                final Activity reopened=activity;
                final String[] label={null};
                runOnMainSync(()->label[0]=((TextView)reopened.findViewById(R.id.widget_config_font)).getText().toString());
                check(WidgetFontRegistry.labels(activity)[0].equals(label[0]),"reopened selection matches saved font");
                runOnMainSync(reopened::finish);
            }
        } finally {
            if(activity!=null) { final Activity last=activity;runOnMainSync(last::finish); }
            host.deleteHost();host.stopListening();WidgetUpdateCoordinator.reconcile(app);
            getUiAutomation().dropShellPermissionIdentity();
        }
    }

    private void awaitFont(View root,int id,Typeface expected,String description) {
        long deadline=SystemClock.uptimeMillis()+7500;
        final boolean[] matches={false};
        do {
            runOnMainSync(()-> { TextView view=root.findViewById(id);matches[0]=view!=null && expected.equals(view.getTypeface()); });
            if(!matches[0]) SystemClock.sleep(25);
        } while(!matches[0] && SystemClock.uptimeMillis()<deadline);
        check(matches[0],description);
    }

    private void verifyAppTimeZone(AppWidgetHost host,int id,AppWidgetProviderInfo info) {
        com.clockmods.background.ClockPreferences prefs=new com.clockmods.background.ClockPreferences(app);
        String original=prefs.getTimeZoneId();
        final AppWidgetHostView[] hosted={null};
        runOnMainSync(()->hosted[0]=host.createView(app,id,info));
        try {
            for(String selected:new String[]{"Pacific/Honolulu","Asia/Tokyo",""}) {
                prefs.setTimeZoneId(selected);
                String expected=WidgetTimeZone.resolve(selected).getID();
                final boolean[] refreshed={false};
                long deadline=SystemClock.uptimeMillis()+10000;
                do {
                    runOnMainSync(()-> {
                        TextClock clock=hosted[0].findViewById(R.id.widget_time);
                        refreshed[0]=clock!=null && expected.equals(clock.getTimeZone());
                    });
                    if(!refreshed[0]) SystemClock.sleep(30);
                } while(!refreshed[0] && SystemClock.uptimeMillis()<deadline);
                check(refreshed[0],"app timezone change updates an existing hosted clock: "+expected);
            }
        } finally { prefs.setTimeZoneId(original); }
    }

    private void waitForControl(Activity activity,int id) {
        long deadline=SystemClock.uptimeMillis()+5000;
        final boolean[] ready={false};
        do {
            runOnMainSync(()->ready[0]=activity.findViewById(id)!=null);
            if(ready[0]) return;
            SystemClock.sleep(20);
        } while(SystemClock.uptimeMillis()<deadline);
        throw new AssertionError("configuration did not load");
    }

    private void verifyMinimumSizes(AppWidgetProviderInfo info,WidgetConfig saved) {
        float density=app.getResources().getDisplayMetrics().density;
        int width=Math.round(info.minResizeWidth/density),height=Math.round(info.minResizeHeight/density);
        for(Locale locale:new Locale[]{Locale.CHINA,Locale.ENGLISH,Locale.TAIWAN}) {
            android.content.res.Configuration resources=new android.content.res.Configuration(app.getResources().getConfiguration());
            resources.setLocale(locale);
            Context localized=app.createConfigurationContext(resources);
            for(float scale:new float[]{.85f,1.2f}) for(String font:WidgetConfig.FONT_IDS) {
              for(int observation=0;observation<(saved.kind==WidgetKind.WEATHER ? 2 : 1);observation++) {
                WidgetConfig config=saved.toBuilder().fontId(font).textScale(scale).useSystemTimeFormat(false).use24Hour(false).showSeconds(true).build();
                RemoteViews views=WidgetRemoteViewsFactory.createAt(localized,config,WidgetSizeClassResolver.resolve(width,height),java.time.Instant.parse("2026-10-01T08:00:00Z").toEpochMilli(),observation==0 ? weatherFixture() : null);
                final Throwable[] error={null};
                runOnMainSync(()-> { try {
                    FrameLayout parent=new FrameLayout(localized);View view=views.apply(localized,parent);parent.addView(view);
                    parent.measure(View.MeasureSpec.makeMeasureSpec(info.minResizeWidth,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(info.minResizeHeight,View.MeasureSpec.EXACTLY));
                    parent.layout(0,0,info.minResizeWidth,info.minResizeHeight);assertVisibleBounds(view,view);
                    int mainId=config.kind==WidgetKind.CALENDAR ? R.id.widget_day : config.kind==WidgetKind.ANALOG ? R.id.widget_analog : R.id.widget_time;
                    View main=view.findViewById(mainId);
                    if(main instanceof TextView) {
                        TextView text=(TextView)main;
                        check(text.getLayout()!=null && text.getLayout().getEllipsisCount(0)==0,"minimum-size time/day must fit");
                        check(text.getLayout().getHeight()<=text.getHeight()+1,"minimum-size glyph height must fit");
                    }
                    rendered++;
                } catch(Throwable e) { error[0]=e; } });
                if(error[0]!=null) throw new AssertionError(config.kind+" minimum "+width+"x"+height+" "+locale+" "+font+" scale "+scale+" observation "+observation,error[0]);
              }
            }
        }
    }

    private void assertVisibleBounds(View view,View root) {
        if(view.getVisibility()!=View.VISIBLE) return;
        if(view instanceof TextView || view.getId()==R.id.widget_analog || view.getId()==R.id.widget_weather_icon) {
            Rect box=new Rect(0,0,view.getWidth(),view.getHeight());
            ((ViewGroup)root).offsetDescendantRectToMyCoords(view,box);
            check(box.left>=0 && box.top>=0 && box.right<=root.getWidth() && box.bottom<=root.getHeight(),
                "visible content must fit: "+view.getResources().getResourceEntryName(view.getId())+" "+box+" in "+root.getWidth()+"x"+root.getHeight());
            if(view instanceof TextView && ((TextView)view).getText().length()>0) {
                TextView text=(TextView)view;
                android.text.Layout lines=text.getLayout();
                int visibleLines=lines==null ? 0 : Math.min(lines.getLineCount(),text.getMaxLines());
                check(visibleLines>0 && lines.getLineBottom(visibleLines-1)<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom()+1,
                    "visible glyphs must fit: "+view.getResources().getResourceEntryName(view.getId())+" height="+text.getHeight()+" layout="+(lines==null ? -1 : lines.getHeight()));
            }
        }
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) assertVisibleBounds(((ViewGroup)view).getChildAt(i),root);
    }

    /** Left edge of {@code view} inside the widget root, i.e. including every ancestor padding. */
    private static int leftInRoot(View view) {
        int left=0;
        while(view!=null) {
            left+=view.getLeft();
            ViewParent parent=view.getParent();
            view=parent instanceof View ? (View)parent : null;
        }
        return left;
    }
}
