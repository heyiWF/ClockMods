package com.clockmods.ultimate.clock;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.content.Context;
import android.graphics.Typeface;
import com.clockmods.background.ClockPreferences;
import com.clockmods.sdk.clock.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/** Real font metrics verify the date/time gaps in the six edited compositions. */
public final class ClockLayoutRefinementProbe {
    private static final class Text {
        final String value; final float top, bottom, size; final Typeface face; final int color;
        Text(String value, float y, Paint paint) {
            this.value=value; size=paint.getTextSize(); face=paint.getTypeface(); color=paint.getColor(); Rect ink=new Rect();
            paint.getTextBounds(value,0,value.length(),ink); top=y+ink.top; bottom=y+ink.bottom;
        }
    }
    private static final class RecordingCanvas extends Canvas {
        final List<Text> text=new ArrayList<>(); int circles;
        RecordingCanvas(Bitmap bitmap){super(bitmap);}
        @Override public void drawText(String value,float x,float y,Paint paint){text.add(new Text(value,y,paint));super.drawText(value,x,y,paint);}
        @Override public void drawText(String value,int start,int end,float x,float y,Paint paint){text.add(new Text(value.substring(start,end),y,paint));super.drawText(value,start,end,x,y,paint);}
        @Override public void drawCircle(float x,float y,float radius,Paint paint){circles++;super.drawCircle(x,y,radius,paint);}
    }
    public static int run(Context appContext, File directory) throws Exception {
        int checks=0;
        Calendar date=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        date.clear();date.set(2026,9,21,17,2,30);
        String[] ids={"glass.atelier","noir.instrument","paper.station","orbit.neon","digital.grid","typographic.poster"};
        for(String id:ids) for(int[] viewport:new int[][]{{1280,720},{720,1280}}) for(float scale:new float[]{1f,1.5f}) {
            ClockStyle style=UltimateClockStyles.createRegistry().find(id);
            ClockState state=ClockState.builder(date.getTimeInMillis()).locale(Locale.SIMPLIFIED_CHINESE)
                    .timeZone(date.getTimeZone()).dateText("2026年10月21日 星期三 | 丙午[马]年九月十二")
                    .showSeconds(true).timeScale(1f).dateScale(scale).supportingScale(1f).build();
            Bitmap bitmap=Bitmap.createBitmap(viewport[0],viewport[1],Bitmap.Config.ARGB_8888);
            RecordingCanvas canvas=new RecordingCanvas(bitmap);
            ClockRenderContext context=new ClockRenderContext(0,0,viewport[0],viewport[1],1f,1f,date.getTimeInMillis());
            ClockThemeTokens typography = new ClockTypography().apply(appContext, style.getThemeTokens(), id,
                    ClockPreferences.FONT_SYSTEM, 400);
            style.getRenderer().render(canvas,context,state,typography);
            for(Text row:canvas.text) if(row.value.matches(".*(ATELIER /|INSTRUMENT /|PAPER /|ORBIT /|DIGITAL GRID|TYPE /).*"))
                throw new AssertionError("Decorative label remains: "+id);
            checks++;
            float dateBottom=0f,timeTop=Float.MAX_VALUE;
            Text solar=null,weekday=null,lunar=null;
            for(Text row:canvas.text) {
                if(row.value.contains("2026"))solar=row;
                if(row.value.equals("星期三"))weekday=row;
                if(row.value.contains("丙午"))lunar=row;
                if(row.value.contains("2026")||row.value.contains("丙午"))dateBottom=Math.max(dateBottom,row.bottom);
                if(row.size>Math.min(viewport[0],viewport[1])*.08f)timeTop=Math.min(timeTop,row.top);
            }
            if(id.equals("glass.atelier")) {
                if(solar==null||lunar==null||timeTop-dateBottom<8f)throw new AssertionError("Atelier date/time gap "+scale);
                checks++;
            }
            if(id.equals("paper.station")) {
                float divider=viewport[1]*(viewport[0]>viewport[1]?.35f:.72f);
                if(solar==null||lunar==null||divider-dateBottom<8f)throw new AssertionError("Paper divider gap "+scale);
                checks++;
            }
            if(id.equals("orbit.neon")) {
                if(solar==null||weekday==null||lunar==null||weekday.top-solar.bottom<4f||lunar.top-weekday.bottom<4f)
                    throw new AssertionError("Orbit date rows "+scale);
                checks++;
            }
            if(id.equals("typographic.poster")) {
                if(canvas.circles!=0)throw new AssertionError("Type colon remains");checks++;
            }
            try(FileOutputStream out=new FileOutputStream(new File(directory,"refinement-"+id+"-"+viewport[0]+"x"+viewport[1]+"-"+scale+".png"))) {
                bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
            } finally {bitmap.recycle();}
        }
        return checks;
    }

    /** Cover every drawn text run, including secondary seconds, dial numerals and legends. */
    public static int fonts(Context context, File directory, String western, String script) throws Exception {
        int checks=0;
        ClockPreferences preferences=new ClockPreferences(context);
        preferences.setClockLanguage("zh-Hans");
        for(String family:new String[]{ClockPreferences.FONT_SYSTEM,western}) {
            for(ClockStyle style:UltimateClockStyles.createRegistry().getStyles()) {
                String id=style.getMetadata().getId();
                preferences.setFontFamily(id,family);preferences.setFontWeight(id,400);
                preferences.setScriptFontFamily(id,script);preferences.setScriptFontWeight(id,400);
                ClockThemeTokens tokens=new ClockTypography().apply(context,style.getThemeTokens(),id);
                Typeface expected=tokens.getDisplayTypeface();
                if(expected==null||!expected.equals(tokens.getSupportingTypeface()))
                    throw new AssertionError("Typography override missing: "+id);
                Calendar date=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                date.clear();date.set(2026,9,21,17,2,30);
                ClockState state=ClockState.builder(date.getTimeInMillis()).locale(Locale.SIMPLIFIED_CHINESE)
                        .timeZone(date.getTimeZone()).dateText("2026年10月21日 星期三 | 丙午[马]年九月十二")
                        .showSeconds(true).timeScale(1f).dateScale(1f).supportingScale(1f).build();
                Bitmap bitmap=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
                RecordingCanvas canvas=new RecordingCanvas(bitmap);
                style.getRenderer().render(canvas,new ClockRenderContext(0,0,1280,720,1f,1f,date.getTimeInMillis()),state,tokens);
                if(canvas.text.isEmpty())throw new AssertionError("No text checked: "+id);
                for(Text text:canvas.text)if(!expected.equals(text.face))
                    throw new AssertionError("Font bypass in "+id+": "+text.value);
                checks++;
                if(id.equals("orbit.neon")) {
                    StringBuilder seconds=new StringBuilder();boolean legend=false;
                    for(Text text:canvas.text){if(text.color==0xFFFF725E)seconds.append(text.value);legend|=text.value.startsWith("H  ");}
                    if(!seconds.toString().equals("30")||!legend)throw new AssertionError("Orbit secondary font coverage missing: "+seconds);
                    checks++;
                    try(FileOutputStream out=new FileOutputStream(new File(directory,
                            "orbit-font-"+(family.equals(ClockPreferences.FONT_SYSTEM)?"system":"imported")+".png"))) {
                        bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
                    }
                }
                bitmap.recycle();
            }
        }
        return checks;
    }
}
