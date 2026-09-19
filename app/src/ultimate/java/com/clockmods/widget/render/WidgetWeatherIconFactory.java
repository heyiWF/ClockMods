package com.clockmods.widget.render;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import com.clockmods.ui.WidgetWeatherBitmap;
public final class WidgetWeatherIconFactory {
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(1024*1024) {
        @Override protected int sizeOf(String key,Bitmap value) { return value.getAllocationByteCount(); }
    };
    private WidgetWeatherIconFactory() { }
    public static int boundedSize(int size) { return Math.max(16,Math.min(144,size)); }
    public static String normalizedCode(String code) { return code!=null && code.matches("[0-9]{3,4}") ? code : "999"; }
    public static synchronized Bitmap render(Context c,String code,boolean fill,int color,int sizePx) {
        int size=boundedSize(sizePx); code=normalizedCode(code);
        String key=code+"/"+fill+"/"+color+"/"+size;
        Bitmap out=CACHE.get(key);
        if(out==null) { out=WidgetWeatherBitmap.render(c,code,fill,color,size); CACHE.put(key,out); }
        return out;
    }
}
