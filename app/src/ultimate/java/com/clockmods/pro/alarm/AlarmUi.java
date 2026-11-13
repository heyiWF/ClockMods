package com.clockmods.pro.alarm;

import android.content.*;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.clockmods.LocaleManager;
import com.clockmods.R;
import com.clockmods.ui.ClockTimeText;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.*;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.*;
import java.time.*;
import java.time.format.*;
import java.util.Locale;

/** Material 3 surfaces. Android owns predictive Back at each activity root. */
public abstract class AlarmUi extends AppCompatActivity {
    public static final String DRAFT = "alarm_draft", MODE = "alarm_mode";
    protected AlarmSpec draft;
    protected LinearLayout body;
    private final java.util.Map<androidx.fragment.app.Fragment, AutoCloseable> dialogBack = new java.util.HashMap<>();
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleManager.wrap(base)); }
    @Override public void applyOverrideConfiguration(android.content.res.Configuration config) {
        if (config != null) config.setLocale(LocaleManager.resolveLocale(this)); super.applyOverrideConfiguration(config);
    }
    @Override public void onCreate(Bundle state) {
        DynamicColors.applyToActivityIfAvailable(this); super.onCreate(state);
        try { String value = state == null ? getIntent().getStringExtra(DRAFT) : state.getString(DRAFT);
            draft = value == null ? new AlarmSpec() : AlarmSpec.fromJson(new org.json.JSONObject(value));
        } catch (Exception e) { draft = new AlarmSpec(); }
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override public void onFragmentStarted(androidx.fragment.app.FragmentManager manager, androidx.fragment.app.Fragment fragment) {
                if (fragment instanceof androidx.fragment.app.DialogFragment) {
                    android.app.Dialog dialog = ((androidx.fragment.app.DialogFragment) fragment).getDialog();
                    closePreview(fragment);
                    if (dialog != null && dialog.getWindow() != null) dialogBack.put(fragment,
                        com.clockmods.ultimate.navigation.DialogBackAnimation.install(dialog.getWindow().getDecorView(), dialog::cancel));
                    if (fragment instanceof MaterialTimePicker) {
                        MaterialTimePicker picker = (MaterialTimePicker) fragment;
                        picker.clearOnPositiveButtonClickListeners();
                        picker.addOnPositiveButtonClickListener(v -> timeSelected(picker.getHour() * 60 + picker.getMinute()));
                    } else if (fragment instanceof MaterialDatePicker) {
                        @SuppressWarnings("unchecked") MaterialDatePicker<Long> picker = (MaterialDatePicker<Long>) fragment;
                        picker.clearOnPositiveButtonClickListeners();
                        picker.addOnPositiveButtonClickListener(value -> dateSelected(Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()));
                    }
                }
            }
            @Override public void onFragmentStopped(androidx.fragment.app.FragmentManager manager, androidx.fragment.app.Fragment fragment) { closePreview(fragment); }
        }, false);
    }
    private void closePreview(androidx.fragment.app.Fragment fragment) {
        AutoCloseable handle = dialogBack.remove(fragment);
        if (handle != null) try { handle.close(); } catch (Exception ignored) { }
    }
    protected void timeSelected(int minutes) { }
    protected void dateSelected(LocalDate date) { }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString(DRAFT, json()); super.onSaveInstanceState(state); }
    protected String json() { try { return draft.toJson().toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
    protected int color(int attr) { return MaterialColors.getColor(this, attr, "AlarmUi"); }
    protected int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    protected void screen(int title, Runnable save) {
        int y = body != null && body.getParent() instanceof ScrollView ? ((ScrollView) body.getParent()).getScrollY() : 0;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(color(com.google.android.material.R.attr.colorSurface));
        root.setOnApplyWindowInsetsListener((v, insets) -> { android.graphics.Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()); v.setPadding(b.left,b.top,b.right,b.bottom); return insets; });
        MaterialToolbar toolbar = new MaterialToolbar(this); toolbar.setTitle(title); toolbar.setNavigationIcon(R.drawable.import_ic_back);
        toolbar.setNavigationIconTint(color(com.google.android.material.R.attr.colorOnSurface)); toolbar.setNavigationContentDescription(R.string.font_import_back); toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(-1,dp(64)));
        LinearLayout center = new LinearLayout(this); center.setGravity(Gravity.CENTER_HORIZONTAL);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(24),dp(8),dp(24),dp(24)); scroll.addView(body);
        center.addView(scroll,new LinearLayout.LayoutParams(dp(Math.min(640,getResources().getConfiguration().screenWidthDp)),-1)); root.addView(center,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.END); actions.setPadding(dp(24),dp(8),dp(24),dp(16));
        MaterialButton done = new MaterialButton(this); done.setText(R.string.apply); done.setMinWidth(dp(112)); done.setOnClickListener(v -> save.run());
        actions.addView(done,new LinearLayout.LayoutParams(-2,dp(56))); root.addView(actions); setContentView(root); root.requestApplyInsets(); scroll.post(() -> scroll.scrollTo(0,y));
        root.post(() -> com.clockmods.pro.ProFontApplier.apply(root));
    }
    protected TextView text(String value,int size) {
        TextView view = new TextView(this); view.setText(ClockTimeText.align(value)); view.setTextSize(size); view.setTextColor(color(com.google.android.material.R.attr.colorOnSurface)); view.setPadding(0,dp(8),0,dp(8)); return view;
    }
    protected void space(int size) { body.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size))); }
    protected MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(24)); card.setStrokeWidth(0); card.setCardElevation(0);
        card.setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceContainer));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(4); body.addView(card,p); return card;
    }
    protected void timeHeader(int minutes,Runnable click) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(8),dp(8),0,dp(16));
        TextView value = text(time(minutes),64); value.setFontFeatureSettings("tnum"); value.setOnClickListener(v -> click.run());
        value.setContentDescription(getString(R.string.alarm_choose_time)+" "+time(minutes)); row.addView(value,new LinearLayout.LayoutParams(0,-2,1));
        MaterialButton edit = new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonTonalStyle); edit.setIconResource(R.drawable.alarm_ic_edit); edit.setContentDescription(getString(R.string.alarm_choose_time));
        edit.setIconPadding(0); edit.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START); edit.setMinWidth(0); edit.setInsetTop(0);edit.setInsetBottom(0); edit.setOnClickListener(v -> click.run());
        row.addView(edit,new LinearLayout.LayoutParams(dp(56),dp(56))); body.addView(row);
    }
    private LinearLayout rowContent(int label) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(68));row.setPadding(dp(18),dp(6),dp(18),dp(6));
        ImageView icon=new ImageView(this);icon.setImageResource(icon(label));icon.setImageTintList(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorOnSurfaceVariant)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(24),dp(24));p.rightMargin=dp(16);row.addView(icon,p);
        TextView title=text(getString(label),16);title.setMaxLines(2);p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=dp(12);row.addView(title,p);return row;
    }
    protected MaterialCardView row(int label,String value,Runnable click) {
        MaterialCardView card=card();LinearLayout row=rowContent(label);card.addView(row);
        if(!value.isEmpty()){TextView summary=text(value,14);summary.setGravity(Gravity.END);summary.setMaxLines(2);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);summary.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));row.addView(summary,new LinearLayout.LayoutParams(dp(Math.min(160,getResources().getConfiguration().screenWidthDp/3)),-2));}
        card.setClickable(true);card.setFocusable(true);card.setOnClickListener(v->click.run());return card;
    }
    protected void toggle(int label,boolean checked,java.util.function.Consumer<Boolean> change) {
        MaterialCardView card=card();LinearLayout row=rowContent(label);MaterialSwitch control=new MaterialSwitch(this);control.setChecked(checked);control.setContentDescription(getString(label));control.setOnCheckedChangeListener((v,value)->change.accept(value));row.addView(control);card.addView(row);card.setOnClickListener(v->control.setChecked(!control.isChecked()));
    }
    protected int icon(int label){
        if(label==R.string.alarm_label)return R.drawable.alarm_ic_label;
        if(label==R.string.alarm_ringtone)return R.drawable.alarm_ic_sound;
        if(label==R.string.alarm_vibrate)return R.drawable.alarm_ic_vibrate;
        if(label==R.string.alarm_repeat||label==R.string.alarm_cycle_length)return R.drawable.alarm_ic_repeat;
        if(label==R.string.alarm_snooze)return R.drawable.alarm_ic_snooze;
        if(label==R.string.alarm_delete)return R.drawable.alarm_ic_delete;
        return R.drawable.alarm_ic_calendar;
    }
    protected MaterialButton dayButton(String label,boolean checked){
        MaterialButton b=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);b.setText(label);b.setTextSize(16);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setInsetTop(0);b.setInsetBottom(0);b.setPadding(0,0,0,0);b.setCornerRadius(dp(28));b.setStrokeWidth(0);b.setSelected(checked);
        b.setBackgroundTintList(ColorStateList.valueOf(checked?color(androidx.appcompat.R.attr.colorPrimary):android.graphics.Color.TRANSPARENT));b.setTextColor(color(checked?com.google.android.material.R.attr.colorOnPrimary:com.google.android.material.R.attr.colorOnSurface));return b;
    }
    protected String time(int minutes){return String.format(Locale.getDefault(),"%02d:%02d",minutes/60,minutes%60);}
    protected String date(LocalDate date){return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocaleManager.resolveLocale(this)));}
    protected void datePicker(LocalDate current){
        MaterialDatePicker<Long> picker=MaterialDatePicker.Builder.datePicker().setSelection(current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()).build();
        picker.show(getSupportFragmentManager(),"alarm-date");
    }
    protected void timePicker(int minutes){
        MaterialTimePicker picker=new MaterialTimePicker.Builder().setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK).setHour(minutes/60).setMinute(minutes%60).setTimeFormat(new com.clockmods.background.ClockPreferences(this).isUse24Hour()?TimeFormat.CLOCK_24H:TimeFormat.CLOCK_12H).setTitleText(R.string.alarm_choose_time).build();
        picker.show(getSupportFragmentManager(),"alarm-time");
    }
    protected void show(android.app.Dialog dialog){dialog.show();if(dialog.getWindow()!=null)com.clockmods.ultimate.navigation.DialogBackAnimation.install(dialog.getWindow().getDecorView(),dialog::cancel);}
    protected void returnDraft(){setResult(RESULT_OK,new Intent().putExtra(DRAFT,json()));finish();}
}
