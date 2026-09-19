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
    private void check(boolean value,String message) { checks++; if(!value) throw new AssertionError(message); }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
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
            host=new AppWidgetHost(app,9917);host.startListening();
            ArrayList<Integer> ids=new ArrayList<>();
            for(AppWidgetProviderInfo info:providers) {
                check(info.configure!=null && info.previewLayout!=0,"picker configuration and preview");
                int id=host.allocateAppWidgetId();ids.add(id);
                check(AppWidgetManager.getInstance(app).bindAppWidgetIdIfAllowed(id,info.provider),"bind "+info.provider);
                WidgetKind kind=WidgetUpdateCoordinator.kindFor(app,id);check(kind!=null,"provider detection");
                WidgetConfig saved=WidgetConfig.builder(id,kind).themeId("paper.warm").useSystemTimeZone(false).timeZoneId("Asia/Shanghai").build();
                new WidgetConfigStore(app).save(saved);
                WidgetUpdateCoordinator.updateOne(app,id);
                for(WidgetThemeSpec theme:WidgetThemeRegistry.all(app)) {
                    for(WidgetSizeClass size:WidgetSizeClass.values()) {
                      for(String font:WidgetConfig.FONT_IDS) {
                        WidgetConfig config=saved.toBuilder().themeId(theme.id).fontId(font).backgroundAlpha(theme.defaultBackgroundAlpha).build();
                        RemoteViews rv=WidgetRemoteViewsFactory.create(app,config,size);
                        final Throwable[] error={null};
                        runOnMainSync(()-> { try {
                            FrameLayout parent=new FrameLayout(app);View view=rv.apply(app,parent);parent.addView(view);
                            int width,height;
                            // Render each size class at a realistic box derived from the widget's own
                            // declared minimum, so the test exercises the contract the Launcher honours
                            // rather than an arbitrary number.
                            int minW=info.minResizeWidth,minH=info.minResizeHeight;
                            int compactW=Math.min(minW,170),compactH=Math.max(minH,56);
                            int smallW=Math.max(minW,180),smallH=Math.max(minH,100);
                            int wideW=smallW+160,largeH=smallH+180;
                            if(size==WidgetSizeClass.COMPACT) { width=compactW;height=compactH; }
                            else if(size==WidgetSizeClass.SMALL) { width=smallW;height=smallH; }
                            else if(size==WidgetSizeClass.WIDE) { width=wideW;height=smallH; }
                            else { width=wideW;height=largeH; }
                            int w=(int)(width*app.getResources().getDisplayMetrics().density),h=(int)(height*app.getResources().getDisplayMetrics().density);
                            parent.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));parent.layout(0,0,w,h);
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
                                if(size!=WidgetSizeClass.COMPACT && caption instanceof TextView)
                                    check(((TextView)caption).getLayout().getEllipsisCount(0)==0,"caption is not truncated");
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
                String before=WidgetConfigStore.encode(new WidgetConfigStore(app).getOrDefault(id,kind));
                Activity config=startActivitySync(new Intent(app,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                waitForIdleSync();SystemClock.sleep(600);waitForIdleSync();
                runOnMainSync(()-> { CompoundButton toggle=config.findViewById(R.id.widget_config_date);check(toggle!=null,"configuration loaded");toggle.toggle();config.finish(); });
                waitForIdleSync();
                check(before.equals(WidgetConfigStore.encode(new WidgetConfigStore(app).getOrDefault(id,kind))),"cancel leaves saved instance untouched");
            }
            if(com.clockmods.weather.QWeatherConfig.isConfigured()) {
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
            check(WidgetUpdateCoordinator.weatherCount(app)==0,"no remaining test weather work");
            for(androidx.work.WorkInfo work:androidx.work.WorkManager.getInstance(app).getWorkInfosForUniqueWork(WidgetRefreshWorker.PERIODIC).get())
                check(work.getState().isFinished(),"weather periodic work cancelled");
            report.putString("stream","PASS: "+checks+" device checks; "+WidgetConfig.FONT_IDS.length+" fonts x 4 sizes x 6 themes per widget on real RemoteViews, plus provider binding, preview and cancel isolation.\n");
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
