package com.clockmods.audit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.clockmods.R;
import com.clockmods.background.AutoStartManager;
import com.clockmods.background.BackgroundRepository;
import com.clockmods.background.ClockPreferences;
import com.clockmods.background.FontCatalog;
import com.clockmods.platform.ApiCompat;
import com.clockmods.time.RegionTimeZones;
import com.clockmods.ui.ColorPickerView;
import com.clockmods.ui.DateFormatter;
import com.clockmods.ui.SegmentedSelector;
import com.clockmods.ui.SettingsDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Debug-only audit of the settings panel.
 *
 * <p>Every control is driven through the same call a finger makes (performClick, setProgress,
 * setSelection), then the dialog's "apply" path runs and a freshly constructed repository is asked
 * for the stored value. A PASS therefore means: control reacted, apply ran, the preference was
 * really persisted, and no exception was thrown on Android 1.0.
 *
 * <p>Live results are written to logcat under the {@code SETTINGS_AUDIT} tag.
 *
 * <p>Layout inspection: {@code --ez hold true} leaves the panel open, {@code --ei board 1} opens
 * the function board, {@code --ei scroll N} scrolls to N pixels, {@code --ez dump true} writes the
 * absolute geometry of every visible view, {@code --ez landscape true} rotates first and
 * {@code --ez dropdown true} opens the first spinner so its popup can be inspected.
 *
 * <p>Render-capture helpers: {@code --ei bgcolor AARRGGBB} recolours the background,
 * {@code --ei datecolor AARRGGBB} recolours the time and supporting text, {@code --ez restore true}
 * resets every preference, and {@code --ez layout true} with {@code --ez stacked/animate/marquee}
 * switches the clock into the layouts (and the scrolling message) a plain capture cannot reach.
 */
public class SettingsAuditActivity extends Activity {
    private static final String TAG = "SETTINGS_AUDIT";

    private static final int COLOR_BG = 0xFF20639B;
    private static final int COLOR_SWATCH = 0xFF173F5F;
    private static final int COLOR_TIME = 0xFFF4C430;
    private static final int COLOR_DATE = 0xFF3CB371;
    private static final String MESSAGE = "自检消息 2026";

    private BackgroundRepository repo;
    private SettingsDialog dialog;
    private final Handler handler = new Handler();
    private int passed;
    private int failed;

    // Values observed mid-flight that only make sense before the next interaction.
    private boolean smallSecondsDisabledWhenOff;
    private boolean smallSecondsEnabledWhenOn;
    private boolean statusSizeEnabledWhenOn;
    private boolean syncEnabledWhenNetworkOn;
    private boolean dimControlsEnabledWhenScheduled;
    private boolean dateSectionRebuilt;
    private boolean timePickerOpened;
    private int regionIndex;
    private int colorBgSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        normalizeToDefaults();
        repo = new BackgroundRepository(this);
        regionIndex = pickRegionIndex();
        if (getIntent() != null && getIntent().getBooleanExtra("restore", false)) {
            Log.i(TAG, "RESTORE | preferences reset to defaults");
            finish();
            return;
        }
        // Lets a capture run against a coloured (not black) background, which is what the
        // fill and dirty-rectangle paths need to be checked on.
        if (getIntent() != null && getIntent().hasExtra("bgcolor")) {
            int color = getIntent().getIntExtra("bgcolor", 0);
            repo.setCurrentColor(color);
            Log.i(TAG, "BG | color=" + Integer.toHexString(color));
            finish();
            return;
        }
        // Recolours the supporting text without touching anything else, so a capture can prove
        // that cached layers are rebuilt when a style preference changes.
        if (getIntent() != null && getIntent().hasExtra("datecolor")) {
            int color = getIntent().getIntExtra("datecolor", 0);
            ClockPreferences prefs = new ClockPreferences(this);
            prefs.setDateColor(color);
            prefs.setTimeColor(color);
            Log.i(TAG, "COLOR | " + Integer.toHexString(color));
            finish();
            return;
        }
        // Switches the clock into the layouts that a plain capture cannot reach: the portrait
        // stacked block and the scrolling supporting line.
        if (getIntent() != null && getIntent().getBooleanExtra("layout", false)) {
            ClockPreferences prefs = new ClockPreferences(this);
            prefs.setPortraitStacked(getIntent().getBooleanExtra("stacked", false));
            prefs.setShowSeconds(true);
            prefs.setAnimateTimeChanges(getIntent().getBooleanExtra("animate", false));
            if (getIntent().getBooleanExtra("marquee", false)) {
                prefs.setCustomMessage("自检留言 marquee 2026 ClockMods 设置在时钟下方滚动显示");
            }
            Log.i(TAG, "LAYOUT | stacked=" + prefs.isPortraitStacked()
                    + " animate=" + prefs.isAnimateTimeChanges());
            finish();
            return;
        }
        // Landscape mode recreates the activity once; only open the panel when the requested
        // orientation is already in effect, otherwise the dialog is torn down immediately.
        if (getIntent() != null && getIntent().getBooleanExtra("landscape", false)
                && getResources().getConfiguration().orientation
                        != android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            return;
        }
        final boolean hold = getIntent() != null && getIntent().getBooleanExtra("hold", false);
        if (hold) {
            final int board = getIntent().getIntExtra("board", 0);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    holdPanel(board);
                }
            }, 400);
            return;
        }
        Log.i(TAG, "=== settings audit start (SDK_INT=" + ApiCompat.SDK_INT + ") ===");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                group1Set();
            }
        }, 400);
    }

    // ---------------------------------------------------------------- groups

    /** G1: background colour picker, swatches, mode radios. */
    private void group1Set() {
        dialog = openDialog();
        ColorPickerView picker = (ColorPickerView) field("backgroundPicker");
        driveSwatches(picker);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                group1Verify();
            }
        }, 350);
    }

    private void driveSwatches(ColorPickerView picker) {
        View swatchRow = firstSwatchRow(view("colorControls"));
        expect("背景颜色-预设色块", swatchRow != null, "找到色块行");
        if (swatchRow != null) {
            View swatch = ((ViewGroup) swatchRow).getChildAt(2);
            swatch.performClick();
            expect("背景颜色-色块点击同步取色器",
                    nearColor(picker.getColor(), COLOR_SWATCH),
                    "取色器=" + hex(picker.getColor()) + " 期望=" + hex(COLOR_SWATCH)
                            + "（取色器 HSV 往返有 ±1 量化）");
        }
        picker.setColor(COLOR_BG);
        colorBgSet = picker.getColor();
        expect("背景颜色-取色器设色", nearColor(picker.getColor(), COLOR_BG),
                "取色器=" + hex(picker.getColor()) + " 期望=" + hex(COLOR_BG));
        expect("背景模式-纯色默认选中", ((RadioButton) field("colorMode")).isChecked(),
                "纯色单选被选中");
        expect("背景模式-控件可见性",
                view("colorControls").getVisibility() == View.VISIBLE
                        && view("imageControls").getVisibility() == View.GONE,
                "纯色板可见 / 图片板隐藏");
    }

    private void group1Verify() {
        apply();
        BackgroundRepository stored = fresh();
        expect("背景颜色-应用后落盘", stored.getCurrentColor() == colorBgSet,
                "存储=" + hex(stored.getCurrentColor()) + " 期望=" + hex(colorBgSet));
        expect("背景模式-纯色落盘",
                ClockPreferences.MODE_COLOR.equals(stored.getBackgroundMode()),
                "mode=" + stored.getBackgroundMode());
        next(new Runnable() {
            @Override
            public void run() {
                group2Set();
            }
        });
    }

    /** G2: font family, sizes, colours, every style switch, status bar section. */
    private void group2Set() {
        dialog = openDialog();
        SeekBar timeBar = (SeekBar) field("timeSizeBar");
        SeekBar dateBar = (SeekBar) field("dateSizeBar");
        SeekBar iconBar = (SeekBar) field("statusIconSizeBar");
        Spinner fonts = (Spinner) field("fontFamilySpinner");

        timeBar.setProgress(100);                       // 20 + 100 = 120%
        dateBar.setProgress(25);                        // 20 + 25  = 45%
        java.util.List<View> rows = new java.util.ArrayList<View>();
        collectSwatchRows(dialog.getWindow().getDecorView(), rows);
        expect("颜色-色块行数量", rows.size() >= 6, "找到 " + rows.size() + " 行色块");
        if (rows.size() >= 6) {
            // 每行 4 块：背景 0-1 行，时间 2-3 行，日期 4-5 行。
            ((ViewGroup) rows.get(2)).getChildAt(2).performClick();   // 时间颜色 #F4C430
            ((ViewGroup) rows.get(5)).getChildAt(0).performClick();   // 日期颜色 #3CB371
        }
        fonts.setSelection(1);

        click("blinkColonSwitch");
        click("animateTimeChangesSwitch");
        click("portraitStackedSwitch");
        click("boldTextSwitch");

        click("showSecondsSwitch");                     // on -> off
        smallSecondsDisabledWhenOff = !view("smallSecondsSwitch").isEnabled();
        click("showSecondsSwitch");                     // off -> on
        smallSecondsEnabledWhenOn = view("smallSecondsSwitch").isEnabled();
        click("smallSecondsSwitch");                    // on

        click("showLunarSwitch");                       // on -> off
        click("dateLunarDualLineSwitch");               // off -> on

        click("statusIconsSwitch");                     // off -> on
        statusSizeEnabledWhenOn = view("statusIconSizeControls").isEnabled();
        iconBar.setProgress(25);                        // 60% + 25 = 85%

        expect("字号-时间滑杆提示", ((TextView) field("timeSizeValue")).getText().toString()
                .contains("120"), "标签=" + ((TextView) field("timeSizeValue")).getText());
        expect("字号-日期滑杆提示", ((TextView) field("dateSizeValue")).getText().toString()
                .contains("45"), "标签=" + ((TextView) field("dateSizeValue")).getText());
        expect("小秒开关-关闭秒时禁用", smallSecondsDisabledWhenOff, "showSeconds=off 时不可点");
        expect("小秒开关-开启秒时可用", smallSecondsEnabledWhenOn, "showSeconds=on 时可用");
        expect("状态栏图标尺寸-随开关启用", statusSizeEnabledWhenOn, "开关打开后尺寸滑杆可用");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                group2Verify();
            }
        }, 350);
    }

    private void group2Verify() {
        apply();
        BackgroundRepository r = fresh();
        expect("字号-时间", near(r.getTimeFontScale(), 1.20f), "scale=" + r.getTimeFontScale());
        expect("字号-日期", near(r.getDateFontScale(), 0.45f), "scale=" + r.getDateFontScale());
        expect("颜色-时间", r.getTimeColor() == COLOR_TIME, "color=" + hex(r.getTimeColor()));
        expect("颜色-日期", r.getDateColor() == COLOR_DATE, "color=" + hex(r.getDateColor()));
        expect("字体-切换字体族", FontCatalog.idForIndex(1).equals(r.getFontFamily()),
                "family=" + r.getFontFamily());
        expect("开关-闪烁冒号", r.isBlinkColon(), "blink=" + r.isBlinkColon());
        expect("开关-时间变化动画", !r.isAnimateTimeChanges(), "animate=" + r.isAnimateTimeChanges());
        expect("开关-竖屏堆叠时钟", r.isPortraitStacked(), "stacked=" + r.isPortraitStacked());
        expect("开关-粗体", r.isBoldText(), "bold=" + r.isBoldText());
        expect("开关-显示秒", r.isShowSeconds(), "seconds=" + r.isShowSeconds());
        expect("开关-小号秒数", r.isSmallSeconds(), "small=" + r.isSmallSeconds());
        expect("开关-显示农历", !r.isShowLunar(), "lunar=" + r.isShowLunar());
        expect("开关-农历双行", r.isDateLunarDualLine(), "dual=" + r.isDateLunarDualLine());
        expect("开关-状态栏图标", r.isShowStatusIcons(), "icons=" + r.isShowStatusIcons());
        expect("状态栏图标尺寸", near(r.getStatusIconScale(), 0.85f),
                "scale=" + r.getStatusIconScale());
        next(new Runnable() {
            @Override
            public void run() {
                group3Set();
            }
        });
    }

    /** G3: function board — orientation, auto start, 24h, network time, region, language, message. */
    private void group3Set() {
        dialog = openDialog();
        ((SegmentedSelector) field("orientationSelector")).setSelectedIndex(2);
        click("autoStartSwitch");
        click("use24HourSwitch");
        click("networkTimeSwitch");                      // off -> on
        syncEnabledWhenNetworkOn = view("functionLockedControls").isEnabled();
        ((RadioButton) field("sync30Min")).performClick();

        setIntField("selectedRegionIndex", regionIndex);

        String[] enFormats = DateFormatter.fixedFormats(DateFormatter.Lang.ENGLISH);
        click("clockUseEnglishSwitch");
        Spinner dateSpinner = (Spinner) field("dateFixedSpinner");
        dateSectionRebuilt = dateSpinner != null
                && dateSpinner.getAdapter() != null
                && dateSpinner.getAdapter().getCount() == enFormats.length;
        if (enFormats.length > 1) {
            dateSpinner.setSelection(1);
        }
        ((TextView) field("customMessageInput")).setText(MESSAGE);

        expect("地区-摘要随选择更新",
                invokeString("regionSummary").contains(regionNames()[regionIndex]),
                "摘要=" + invokeString("regionSummary"));
        expect("日期格式-随语言重建", dateSectionRebuilt,
                "英文格式数量=" + enFormats.length);
        expect("网络时间-开启后同步项可用", syncEnabledWhenNetworkOn, "同步间隔可选");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                group3Verify();
            }
        }, 400);
    }

    private void group3Verify() {
        apply();
        BackgroundRepository r = fresh();
        expect("屏幕方向", r.getScreenOrientation() == ClockPreferences.ORIENTATION_LANDSCAPE,
                "orientation=" + r.getScreenOrientation());
        expect("开机自启动-偏好", r.isAutoStart(), "autoStart=" + r.isAutoStart());
        expect("开机自启动-启用HOME别名", aliasEnabled(), "component=" + aliasState());
        expect("24小时制", !r.isUse24Hour(), "use24=" + r.isUse24Hour());
        expect("网络时间", r.isUseNetworkTime(), "network=" + r.isUseNetworkTime());
        expect("同步间隔", r.getSyncIntervalMinutes() == 30,
                "minutes=" + r.getSyncIntervalMinutes());
        expect("地区时区", RegionTimeZones.ZONE_IDS[regionIndex].equals(r.getTimeZoneId()),
                "zone=" + r.getTimeZoneId());
        expect("时钟语言-英文", r.isClockUseEnglish(), "english=" + r.isClockUseEnglish());
        Spinner dateSpinner = (Spinner) field("dateFixedSpinner");
        int pos = dateSpinner == null ? -1 : dateSpinner.getSelectedItemPosition();
        String[] enFormats = DateFormatter.fixedFormats(DateFormatter.Lang.ENGLISH);
        String expectedEn = pos >= 0 && pos < enFormats.length ? enFormats[pos] : null;
        expect("日期格式-选中项保存",
                expectedEn != null && expectedEn.equals(r.getDatePatternEn()),
                "选中=" + pos + " 存储=" + r.getDatePatternEn() + " 期望=" + expectedEn);
        expect("自定义消息", MESSAGE.equals(r.getCustomMessage()),
                "message=" + r.getCustomMessage());
        next(new Runnable() {
            @Override
            public void run() {
                group4Set();
            }
        });
    }

    /** G4: image mode without an image, advanced picker, dim background + schedule. */
    private void group4Set() {
        dialog = openDialog();
        click("imageMode");
        expect("背景模式-切图片板", view("imageControls").getVisibility() == View.VISIBLE
                && view("colorControls").getVisibility() == View.GONE, "图片板显示");
        apply();                                          // no image yet -> must not persist
        expect("背景模式-无图片时不落盘",
                ClockPreferences.MODE_COLOR.equals(fresh().getBackgroundMode()),
                "mode=" + fresh().getBackgroundMode());
        expect("背景模式-无图片时对话框保持", dialog.isShowing(), "对话框未被关闭");

        click("colorMode");
        View advanced = firstButtonWithText(view("colorControls"),
                getString(R.string.advanced));
        expect("高级取色器-开关存在", advanced != null, "找到高级开关");
        if (advanced != null) {
            advanced.performClick();
            expect("高级取色器-展开", view("backgroundPicker").getVisibility() == View.VISIBLE,
                    "取色器已显示");
        }
        click("dimBackgroundSwitch");
        click("scheduleDimBackgroundSwitch");
        dimControlsEnabledWhenScheduled = view("dimScheduleControls").isEnabled();
        expect("定时变暗-开启后时间可用", dimControlsEnabledWhenScheduled, "起止时间可点");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                group4Verify();
            }
        }, 350);
    }

    private void group4Verify() {
        apply();
        BackgroundRepository r = fresh();
        expect("变暗背景", r.isDimBackground(), "dim=" + r.isDimBackground());
        expect("定时变暗", r.isScheduleDimBackground(), "schedule=" + r.isScheduleDimBackground());
        expect("定时变暗-开始时间", r.getDimStartMinutes() == ClockPreferences.DEFAULT_DIM_START_MINUTES,
                "start=" + r.getDimStartMinutes());
        expect("定时变暗-结束时间", r.getDimEndMinutes() == ClockPreferences.DEFAULT_DIM_END_MINUTES,
                "end=" + r.getDimEndMinutes());
        next(new Runnable() {
            @Override
            public void run() {
                group5Set();
            }
        });
    }

    /** G5: restore defaults + time picker entry. */
    private void group5Set() {
        dialog = openDialog();
        Button reset = (Button) firstButtonWithText(dialog.getWindow().getDecorView(),
                getString(R.string.reset_default));
        expect("恢复默认-按钮存在", reset != null, "找到恢复默认按钮");
        if (reset != null) {
            reset.performClick();
        }
        try {
            ((Button) field("dimStartButton")).performClick();
            timePickerOpened = true;
        } catch (Throwable t) {
            timePickerOpened = false;
            Log.e(TAG, "time picker failed: " + t);
        }
        expect("定时变暗-打开时间选择器", timePickerOpened, "未抛异常");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                group5Verify();
            }
        }, 300);
    }

    private void group5Verify() {
        apply();
        BackgroundRepository r = fresh();
        expect("恢复默认-24小时制", r.isUse24Hour() == ClockPreferences.DEFAULT_USE_24_HOUR,
                "use24=" + r.isUse24Hour());
        expect("恢复默认-显示秒", r.isShowSeconds() == ClockPreferences.DEFAULT_SHOW_SECONDS,
                "seconds=" + r.isShowSeconds());
        expect("恢复默认-显示农历", r.isShowLunar() == ClockPreferences.DEFAULT_SHOW_LUNAR,
                "lunar=" + r.isShowLunar());
        expect("恢复默认-粗体", r.isBoldText() == ClockPreferences.DEFAULT_BOLD_TEXT,
                "bold=" + r.isBoldText());
        expect("恢复默认-时钟语言", !r.isClockUseEnglish(), "english=" + r.isClockUseEnglish());
        expect("恢复默认-自定义消息", ClockPreferences.DEFAULT_CUSTOM_MESSAGE
                .equals(r.getCustomMessage()), "message=" + r.getCustomMessage());
        expect("恢复默认-屏幕方向",
                r.getScreenOrientation() == ClockPreferences.ORIENTATION_FOLLOW_SYSTEM,
                "orientation=" + r.getScreenOrientation());
        expect("恢复默认-开机自启动", !r.isAutoStart(), "autoStart=" + r.isAutoStart());
        expect("恢复默认-HOME别名关闭", !aliasEnabled(), "component=" + aliasState());
        expect("恢复默认-字体族", ClockPreferences.DEFAULT_FONT_FAMILY.equals(r.getFontFamily()),
                "family=" + r.getFontFamily());
        expect("恢复默认-时间颜色", r.getTimeColor() == ClockPreferences.DEFAULT_TEXT_COLOR,
                "color=" + hex(r.getTimeColor()));
        expect("恢复默认-背景色", r.getCurrentColor() == ClockPreferences.DEFAULT_BACKGROUND_COLOR,
                "color=" + hex(r.getCurrentColor()));
        expect("恢复默认-变暗背景", !r.isDimBackground(), "dim=" + r.isDimBackground());
        expect("恢复默认-网络时间", !r.isUseNetworkTime(), "network=" + r.isUseNetworkTime());
        expect("恢复默认-同步间隔",
                r.getSyncIntervalMinutes() == ClockPreferences.DEFAULT_SYNC_INTERVAL_MINUTES,
                "minutes=" + r.getSyncIntervalMinutes());
        expect("恢复默认-地区时区",
                ClockPreferences.DEFAULT_TIME_ZONE_ID.equals(r.getTimeZoneId()),
                "zone=" + r.getTimeZoneId());
        finishWithSummary();
    }

    /** Deterministic starting point so every run toggles from the documented defaults. */
    /** Opens the panel and leaves it on screen so the device framebuffer can be inspected. */
    private void holdPanel(int board) {
        dialog = openDialog();
        if (board != 0) {
            SegmentedSelector selector = findSelector(dialog.getWindow().getDecorView());
            if (selector != null) {
                selector.setSelectedIndex(board);
            }
        }
        Log.i(TAG, "HOLD | panel open board=" + board);
        final int scroll = getIntent() == null ? -1 : getIntent().getIntExtra("scroll", -1);
        final boolean dump = getIntent() != null && getIntent().getBooleanExtra("dump", false);
        final boolean dropdown = getIntent() != null
                && getIntent().getBooleanExtra("dropdown", false);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scroll >= 0) {
                    View scroller = findScrollView(dialog.getWindow().getDecorView());
                    if (scroller != null) {
                        scroller.scrollTo(0, scroll);
                        Log.i(TAG, "HOLD | scrollTo " + scroll + " max="
                                + (scroller.getHeight() - ((ViewGroup) scroller).getChildAt(0)
                                        .getHeight()));
                    }
                }
                if (dropdown) {
                    // Opens the spinner popup so its (framework) window can be screenshotted.
                    View spinner = findSpinner(dialog.getWindow().getDecorView());
                    if (spinner != null) {
                        spinner.performClick();
                        Log.i(TAG, "HOLD | dropdown opened");
                    } else {
                        Log.e(TAG, "HOLD | no spinner found");
                    }
                }
                if (getIntent() != null && getIntent().getBooleanExtra("region", false)) {
                    View button = (View) field("regionButton");
                    if (button != null) {
                        button.performClick();
                        Log.i(TAG, "HOLD | region chooser opened");
                    }
                }
                if (dump) {
                    try {
                        dumpLayout(dialog.getWindow().getDecorView(), 0);
                    } catch (RuntimeException ignored) {
                        // The dialog can be gone already (e.g. after an orientation change).
                    }
                    Log.i(TAG, "LAYOUT | dump end");
                }
            }
        }, 700);
    }

    private View findScrollView(View root) {
        if (root instanceof android.widget.ScrollView) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findScrollView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private View findSpinner(View root) {
        if (root instanceof Spinner && root.getVisibility() == View.VISIBLE) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findSpinner(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Logs absolute geometry for every view so spacing can be measured, not guessed. */
    private void dumpLayout(View view, int depth) {
        if (depth > 14 || view.getVisibility() != View.VISIBLE) {
            return;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        String text = "";
        if (view instanceof TextView) {
            CharSequence value = ((TextView) view).getText();
            if (value != null && value.length() > 0) {
                text = " text=\"" + value.toString().replace('\n', '/') + "\"";
            }
        }
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        Log.i(TAG, "LAYOUT | " + indent + view.getClass().getSimpleName()
                + " @" + location[0] + "," + location[1]
                + " " + view.getWidth() + "x" + view.getHeight() + text);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                dumpLayout(group.getChildAt(i), depth + 1);
            }
        }
    }

    private SegmentedSelector findSelector(View root) {
        if (root instanceof SegmentedSelector) {
            return (SegmentedSelector) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                SegmentedSelector found = findSelector(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void normalizeToDefaults() {
        ClockPreferences p = new ClockPreferences(this);
        p.setUseNetworkTime(ClockPreferences.DEFAULT_USE_NETWORK_TIME);
        p.setSyncIntervalMinutes(ClockPreferences.DEFAULT_SYNC_INTERVAL_MINUTES);
        p.setTimeZoneId(ClockPreferences.DEFAULT_TIME_ZONE_ID);
        p.setAutoStart(ClockPreferences.DEFAULT_AUTO_START);
        p.setScreenOrientation(ClockPreferences.DEFAULT_SCREEN_ORIENTATION);
        p.setCustomMessage(ClockPreferences.DEFAULT_CUSTOM_MESSAGE);
        p.setClockUseEnglish(ClockPreferences.DEFAULT_CLOCK_USE_ENGLISH);
        p.setDatePatternCn(ClockPreferences.DEFAULT_DATE_PATTERN_CN);
        p.setDatePatternEn(ClockPreferences.DEFAULT_DATE_PATTERN_EN);
        p.setDimBackground(ClockPreferences.DEFAULT_DIM_BACKGROUND);
        p.setScheduleDimBackground(ClockPreferences.DEFAULT_SCHEDULE_DIM_BACKGROUND);
        p.setDimStartMinutes(ClockPreferences.DEFAULT_DIM_START_MINUTES);
        p.setDimEndMinutes(ClockPreferences.DEFAULT_DIM_END_MINUTES);
        p.setTimeFontScale(ClockPreferences.DEFAULT_TIME_FONT_SCALE);
        p.setDateFontScale(ClockPreferences.DEFAULT_DATE_FONT_SCALE);
        p.setTimeColor(ClockPreferences.DEFAULT_TEXT_COLOR);
        p.setDateColor(ClockPreferences.DEFAULT_TEXT_COLOR);
        p.setShowStatusIcons(ClockPreferences.DEFAULT_SHOW_STATUS_ICONS);
        p.setStatusIconScale(ClockPreferences.DEFAULT_STATUS_ICON_SCALE);
        p.setBlinkColon(ClockPreferences.DEFAULT_BLINK_COLON);
        p.setAnimateTimeChanges(ClockPreferences.DEFAULT_ANIMATE_TIME_CHANGES);
        p.setPortraitStacked(ClockPreferences.DEFAULT_PORTRAIT_STACKED);
        p.setBoldText(ClockPreferences.DEFAULT_BOLD_TEXT);
        p.setShowSeconds(ClockPreferences.DEFAULT_SHOW_SECONDS);
        p.setSmallSeconds(ClockPreferences.DEFAULT_SMALL_SECONDS);
        p.setShowLunar(ClockPreferences.DEFAULT_SHOW_LUNAR);
        p.setDateLunarDualLine(ClockPreferences.DEFAULT_DATE_LUNAR_DUAL_LINE);
        p.setUse24Hour(ClockPreferences.DEFAULT_USE_24_HOUR);
        p.setFontFamily(ClockPreferences.DEFAULT_FONT_FAMILY);
        p.setBackgroundMode(ClockPreferences.MODE_COLOR);
        p.setBackgroundColor(ClockPreferences.DEFAULT_BACKGROUND_COLOR);
        AutoStartManager.setEnabled(this, false);
    }

    private void finishWithSummary() {
        Log.i(TAG, "=== audit finished: PASS=" + passed + " FAIL=" + failed + " ===");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 300);
    }

    // ------------------------------------------------------------- mechanics

    private SettingsDialog openDialog() {
        SettingsDialog d = new SettingsDialog(this, repo, new SettingsDialog.Listener() {
            @Override
            public void onColorApplied(int color) {
                repo.setCurrentColor(color);
            }

            @Override
            public void onImageModeApplied() {
                repo.useImage();
            }

            @Override
            public void onChooseImage() {
                Log.i(TAG, "listener: onChooseImage");
            }

            @Override
            public void onFontSettingsApplied() {
                Log.i(TAG, "listener: onFontSettingsApplied");
            }

            @Override
            public void onDismissed() {
                Log.i(TAG, "listener: onDismissed");
            }

            @Override
            public void onLanguageChanged() {
                Log.i(TAG, "listener: onLanguageChanged");
            }
        });
        d.show();
        return d;
    }

    private void apply() {
        try {
            Method m = SettingsDialog.class.getDeclaredMethod("applySelection");
            m.setAccessible(true);
            m.invoke(dialog);
        } catch (Throwable t) {
            failed++;
            Log.e(TAG, "FAIL | applySelection | " + t);
        }
    }

    private BackgroundRepository fresh() {
        return new BackgroundRepository(this);
    }

    private void next(final Runnable runnable) {
        handler.postDelayed(runnable, 250);
    }

    /** Typed accessor for the many View-typed dialog fields. */
    private View view(String name) {
        return (View) field(name);
    }

    private Object field(String name) {
        return field(dialog, name);
    }

    private Object field(Object target, String name) {
        try {
            Field f = SettingsDialog.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable t) {
            failed++;
            Log.e(TAG, "FAIL | reflect field " + name + " | " + t);
            return null;
        }
    }

    /** Calls a private no-argument String getter on the dialog, e.g. {@code regionSummary}. */
    private String invokeString(String methodName) {
        try {
            Method m = SettingsDialog.class.getDeclaredMethod(methodName);
            m.setAccessible(true);
            Object value = m.invoke(dialog);
            return value == null ? "" : value.toString();
        } catch (Throwable t) {
            Log.e(TAG, "FAIL | invoke " + methodName + " | " + t);
            return "";
        }
    }

    private void setIntField(String name, int value) {        try {
            Field f = SettingsDialog.class.getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(dialog, value);
        } catch (Throwable t) {
            failed++;
            Log.e(TAG, "FAIL | reflect int " + name + " | " + t);
        }
    }

    private void click(String fieldName) {
        View view = (View) field(fieldName);
        if (view == null) {
            return;
        }
        view.performClick();
    }

    /**
     * Swatches wrap to four leaf cells per row (HorizontalScrollView is API 3), so the first row
     * whose four children are plain Views is the swatch row.
     */
    private void collectSwatchRows(View root, java.util.List<View> out) {
        if (!(root instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) root;
        if (group.getChildCount() == 4 && allLeaves(group)) {
            out.add(group);
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            collectSwatchRows(group.getChildAt(i), out);
        }
    }

    private View firstSwatchRow(View root) {
        if (!(root instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) root;
        if (group.getChildCount() == 4 && allLeaves(group)) {
            return group;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = firstSwatchRow(group.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean allLeaves(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof ViewGroup) {
                return false;
            }
        }
        return true;
    }

    private View firstButtonWithText(View root, String text) {
        if (root instanceof Button) {
            CharSequence label = ((Button) root).getText();
            if (label != null && text.contentEquals(label)) {
                return root;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = firstButtonWithText(group.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String[] regionNames() {
        return getResources().getStringArray(R.array.region_names);
    }

    private int pickRegionIndex() {
        for (int i = 0; i < RegionTimeZones.ZONE_IDS.length; i++) {
            if ("Asia/Tokyo".equals(RegionTimeZones.ZONE_IDS[i])) {
                return i;
            }
        }
        return RegionTimeZones.ZONE_IDS.length > 1 ? 1 : 0;
    }

    private ComponentName alias() {
        return new ComponentName(getPackageName(), "com.clockmods.HomeLauncherAlias");
    }

    private boolean aliasEnabled() {
        return getPackageManager().getComponentEnabledSetting(alias())
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private int aliasState() {
        return getPackageManager().getComponentEnabledSetting(alias());
    }

    /** Colour equality with a small tolerance for the picker's HSV round trip. */
    private static boolean nearColor(int actual, int expected) {
        return Math.abs(((actual >> 16) & 0xFF) - ((expected >> 16) & 0xFF)) <= 2
                && Math.abs(((actual >> 8) & 0xFF) - ((expected >> 8) & 0xFF)) <= 2
                && Math.abs((actual & 0xFF) - (expected & 0xFF)) <= 2;
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) < 0.005f;
    }

    private static String hex(int color) {
        return "#" + Integer.toHexString(color).toUpperCase();
    }

    private void expect(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            Log.i(TAG, "PASS | " + name + " | " + detail);
        } else {
            failed++;
            Log.e(TAG, "FAIL | " + name + " | " + detail);
        }
    }
}
