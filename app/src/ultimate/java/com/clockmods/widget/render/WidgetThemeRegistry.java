package com.clockmods.widget.render;
import android.content.Context;
import android.content.res.Configuration;
import com.clockmods.R;
import com.clockmods.widget.model.*;
import java.util.*;
import static com.clockmods.widget.model.WidgetThemeSpec.Font.*;
public final class WidgetThemeRegistry {
    private WidgetThemeRegistry() { }
    public static List<WidgetThemeSpec> all(Context c) {
        boolean night=(c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        int primary=c.getColor(night ? android.R.color.system_neutral1_50 : android.R.color.system_neutral1_900);
        int secondary=c.getColor(night ? android.R.color.system_neutral2_200 : android.R.color.system_neutral2_700);
        int accent=c.getColor(night ? android.R.color.system_accent1_200 : android.R.color.system_accent1_700);
        return Collections.unmodifiableList(Arrays.asList(
            new WidgetThemeSpec("system.dynamic",R.string.widget_theme_dynamic,R.drawable.widget_bg_dynamic,primary,secondary,accent,SANS,255),
            new WidgetThemeSpec("glass.light",R.string.widget_theme_glass,R.drawable.widget_bg_glass,0xff152a38,0xff304657,0xff125e70,SANS,235),
            new WidgetThemeSpec("instrument.dark",R.string.widget_theme_instrument,R.drawable.widget_bg_instrument,0xfff4f6f9,0xffacbac7,0xffa8dcfa,MONOSPACE,255),
            new WidgetThemeSpec("paper.warm",R.string.widget_theme_paper,R.drawable.widget_bg_paper,0xff30281f,0xff675541,0xff9e362c,SERIF,255),
            new WidgetThemeSpec("neon.night",R.string.widget_theme_neon,R.drawable.widget_bg_neon,0xffecfaff,0xffacbde0,0xff5fffe0,MONOSPACE,255),
            new WidgetThemeSpec("transparent.clean",R.string.widget_theme_transparent,R.drawable.widget_bg_transparent,0xfffafafa,0xfffafafa,0xfffafafa,SANS,0)));
    }
    public static WidgetThemeSpec resolve(Context c,String id) {
        for(WidgetThemeSpec t:all(c)) if(t.id.equals(id)) return t;
        return all(c).get(0);
    }
}
