package com.clockmods.ui;
import android.content.Context;
import android.graphics.*;
/** Bounded bitmap facade; the parser and icon assets stay shared with WeatherIconView. */
public final class WidgetWeatherBitmap {
    private WidgetWeatherBitmap() { }
    public static Bitmap render(Context c,String code,boolean fill,int color,int size) {
        WeatherIcon icon=WeatherIcon.load(c,code,fill);
        if(icon==null) icon=WeatherIcon.load(c,"999",fill);
        Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(color);
        Canvas canvas=new Canvas(bitmap);
        if(icon!=null) icon.draw(canvas,0,0,size,p);
        else { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(size/12f); canvas.drawCircle(size/2f,size/2f,size/3f,p); }
        return bitmap;
    }
}
