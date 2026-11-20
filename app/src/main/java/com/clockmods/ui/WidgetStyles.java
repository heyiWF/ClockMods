package com.clockmods.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;

/**
 * Draws the settings sheet's stock controls by hand.
 *
 * <p>Android 1.0 has no themeable widgets. CheckBox and RadioButton ship a white gradient glyph
 * with a green dot, SeekBar a yellow track with a bevelled grey thumb, and Button a grey 9-patch.
 * None of them can be tinted before API 21 ({@code setButtonTintList}, {@code setProgressTintList}),
 * so the previous code multiplied an accent colour over the original bitmap: the hue changed but
 * the bevels, the white tick and the 3D thumb stayed, which is exactly what made the sheet look
 * like two different designs stitched together.</p>
 *
 * <p>Everything here is drawn from primitives ({@link Canvas#drawRoundRect}, {@link Canvas#drawCircle},
 * {@link Path}) so the sheet is one palette on every API level. All members used below exist in
 * Android 1.0; {@code api1_check.py} enforces that.</p>
 */
public final class WidgetStyles {
    private static final int CORNER_DP = 8;
    private static final int TOGGLE_DP = 20;
    private static final int TOGGLE_LABEL_GAP_DP = 10;
    private static final int TRACK_DP = 6;
    private static final int THUMB_DP = 18;
    /** Touch height of a text field or a spinner row. */
    public static final int FIELD_HEIGHT_DP = 44;

    private WidgetStyles() {
    }

    // ------------------------------------------------------------------ buttons

    /**
     * Replaces a button's 9-patch with a flat rounded plate. {@code primary} is the single
     * affirmative action of the sheet (Apply); everything else is a secondary outlined button, so
     * two buttons no longer compete for attention.
     */
    public static void styleButton(Button button, boolean primary) {
        Context context = button.getContext();
        int radius = dp(context, CORNER_DP);
        int stroke = dp(context, 1);
        GradientDrawable rest = rounded(radius,
                primary ? PanelTheme.ACCENT_STRONG : PanelTheme.FIELD, stroke,
                primary ? PanelTheme.ACCENT_STRONG : PanelTheme.FIELD_STROKE);
        GradientDrawable pressed = rounded(radius,
                primary ? PanelTheme.ACCENT_STRONG_PRESSED : PanelTheme.FIELD_PRESSED, stroke,
                primary ? PanelTheme.ACCENT_STRONG_PRESSED : PanelTheme.FIELD_STROKE);
        button.setBackgroundDrawable(stateList(rest, pressed));
        button.setTextColor(primary ? PanelTheme.ON_ACCENT : PanelTheme.PRIMARY_TEXT);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        // Matches styleSpinner/styleTextInput: a left-aligned button label then sits on the same
        // x as the text inside a field, instead of 2dp to its left.
        int horizontal = dp(context, 12);
        button.setPadding(horizontal, 0, horizontal, 0);
    }

    // ------------------------------------------------------------------ toggles

    /**
     * Swaps the framework's white glyph for an outlined box that fills with the accent colour and
     * shows a tick when checked. Radio buttons get the same treatment as a ring plus a dot.
     */
    public static void styleToggle(CompoundButton control) {
        Context context = control.getContext();
        control.setButtonDrawable(new ToggleDrawable(control instanceof RadioButton,
                dp(context, TOGGLE_DP), dp(context, 1.5f), PanelTheme.ACCENT,
                PanelTheme.TOGGLE_STROKE, PanelTheme.ON_ACCENT));
        int labelGap = dp(context, TOGGLE_LABEL_GAP_DP);
        control.setPadding(labelGap, control.getPaddingTop(), labelGap, control.getPaddingBottom());
    }

    // ------------------------------------------------------------------ sliders

    /**
     * Gives a SeekBar an accent track, a dim remainder and a round thumb. The framework track is
     * yellow on most Android 1.0 builds, which clashes with every other control in the sheet.
     */
    public static void styleSeekBar(SeekBar bar) {
        Context context = bar.getContext();
        int thumb = dp(context, THUMB_DP);
        bar.setThumb(thumb(context, thumb));
        bar.setProgressDrawable(new SeekTrackDrawable(dp(context, TRACK_DP), thumb,
                PanelTheme.TRACK, PanelTheme.ACCENT));
    }

    // ------------------------------------------------------------------ fields

    /** Rounded inset field used by the custom-message input. */
    public static void styleTextInput(EditText input) {
        Context context = input.getContext();
        input.setBackgroundDrawable(rounded(dp(context, CORNER_DP), PanelTheme.FIELD,
                dp(context, 1), PanelTheme.FIELD_STROKE));
        input.setTextColor(PanelTheme.PRIMARY_TEXT);
        input.setHintTextColor(PanelTheme.SECONDARY_TEXT);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        input.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        input.setMinHeight(dp(context, FIELD_HEIGHT_DP));
        input.setGravity(android.view.Gravity.CENTER_VERTICAL);
    }

    /** Rounded inset field with a chevron on the trailing edge, used by spinners. */
    public static void styleSpinner(Spinner spinner) {
        Context context = spinner.getContext();
        spinner.setBackgroundDrawable(new SpinnerFieldDrawable(dp(context, CORNER_DP),
                dp(context, 1), dp(context, 32), dp(context, 5),
                PanelTheme.FIELD, PanelTheme.FIELD_PRESSED, PanelTheme.FIELD_STROKE,
                PanelTheme.SECONDARY_TEXT));
        spinner.setPadding(dp(context, 12), 0, dp(context, 36), 0);
        spinner.setMinimumHeight(dp(context, FIELD_HEIGHT_DP));
    }

    // ------------------------------------------------------------------ pieces

    private static GradientDrawable rounded(int radius, int fill, int strokeWidth, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, stroke);
        }
        return drawable;
    }

    private static StateListDrawable stateList(Drawable rest, Drawable pressed) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, pressed);
        states.addState(new int[] { android.R.attr.state_enabled }, rest);
        // Anything left over is disabled: keep the same plate so the layout is unchanged.
        states.addState(new int[0], rest);
        return states;
    }

    private static Drawable thumb(Context context, int size) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(PanelTheme.THUMB);
        circle.setSize(size, size);
        GradientDrawable pressed = new GradientDrawable();
        pressed.setShape(GradientDrawable.OVAL);
        pressed.setColor(PanelTheme.ACCENT);
        pressed.setSize(size, size);
        return stateList(circle, pressed);
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    static boolean hasState(int[] state, int attribute) {
        if (state == null) {
            return false;
        }
        for (int index = 0; index < state.length; index++) {
            if (state[index] == attribute) {
                return true;
            }
        }
        return false;
    }

    /**
     * Outlined box (or ring) that fills with the accent colour when checked. A Drawable is used
     * rather than two static bitmaps so the same class serves CheckBox and RadioButton and reacts
     * to the checked/enabled state that {@code CompoundButton} pushes into it.
     */
    static final class ToggleDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path tick = new Path();
        private final RectF box = new RectF();
        private final boolean radio;
        private final int size;
        private final float stroke;
        private final int checkedColor;
        private final int uncheckedColor;
        private final int markColor;
        private boolean checked;
        private boolean enabled = true;
        private int alpha = 255;

        ToggleDrawable(boolean radio, int size, float stroke, int checkedColor,
                int uncheckedColor, int markColor) {
            this.radio = radio;
            this.size = size;
            this.stroke = stroke;
            this.checkedColor = checkedColor;
            this.uncheckedColor = uncheckedColor;
            this.markColor = markColor;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float inset = stroke / 2f;
            box.set(bounds.left + inset, bounds.top + inset,
                    bounds.right - inset, bounds.bottom - inset);
            float centerX = (box.left + box.right) / 2f;
            float centerY = (box.top + box.bottom) / 2f;
            float radius = Math.min(box.width(), box.height()) / 2f;

            if (checked) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(modulate(checkedColor));
                if (radio) {
                    canvas.drawCircle(centerX, centerY, radius, paint);
                    paint.setColor(modulate(markColor));
                    canvas.drawCircle(centerX, centerY, radius * 0.42f, paint);
                } else {
                    canvas.drawRoundRect(box, radius * 0.5f, radius * 0.5f, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(stroke, size * 0.11f));
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    paint.setStrokeJoin(Paint.Join.ROUND);
                    paint.setColor(modulate(markColor));
                    float width = box.width();
                    float height = box.height();
                    tick.reset();
                    tick.moveTo(box.left + width * 0.26f, box.top + height * 0.52f);
                    tick.lineTo(box.left + width * 0.44f, box.top + height * 0.70f);
                    tick.lineTo(box.left + width * 0.75f, box.top + height * 0.32f);
                    canvas.drawPath(tick, paint);
                }
            } else {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
                paint.setColor(modulate(enabled ? uncheckedColor : dim(uncheckedColor)));
                if (radio) {
                    canvas.drawCircle(centerX, centerY, radius, paint);
                } else {
                    canvas.drawRoundRect(box, radius * 0.5f, radius * 0.5f, paint);
                }
            }
        }

        private int modulate(int color) {
            if (alpha >= 255) {
                return color;
            }
            int value = (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
            // A translucent fill has to be blended by the canvas, not by the source alpha alone.
            return value;
        }

        private static int dim(int color) {
            int alpha = (color >>> 24) & 0xFF;
            int dimmed = alpha * 45 / 100;
            return (color & 0x00FFFFFF) | (dimmed << 24);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean nowChecked = hasState(state, android.R.attr.state_checked);
            boolean nowEnabled = hasState(state, android.R.attr.state_enabled);
            if (nowChecked == checked && nowEnabled == enabled) {
                return false;
            }
            checked = nowChecked;
            enabled = nowEnabled;
            invalidateSelf();
            return true;
        }

        @Override
        public void setAlpha(int alpha) {
            if (this.alpha == alpha) {
                return;
            }
            this.alpha = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }
    }

    /**
     * Slider track: a dim rounded rail with the accent part drawn up to the current level.
     * {@code ProgressBar} pushes 0..10000 into the progress drawable, so the fraction is read
     * from {@link #onLevelChange(int)}; the rail is inset by half a thumb so the fill starts
     * exactly under the thumb instead of poking out at the far left when the value is 0.
     */
    static final class SeekTrackDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rail = new RectF();
        private final int height;
        private final int inset;
        private final int trackColor;
        private final int fillColor;
        private int level;

        SeekTrackDrawable(int height, int thumbSize, int trackColor, int fillColor) {
            this.height = height;
            this.inset = thumbSize / 2;
            this.trackColor = trackColor;
            this.fillColor = fillColor;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float centerY = (bounds.top + bounds.bottom) / 2f;
            float left = bounds.left + inset;
            float right = bounds.right - inset;
            if (right <= left) {
                left = bounds.left;
                right = bounds.right;
            }
            float half = height / 2f;
            float radius = half;
            rail.set(left, centerY - half, right, centerY + half);
            paint.setColor(trackColor);
            canvas.drawRoundRect(rail, radius, radius, paint);
            if (level <= 0) {
                return;
            }
            float end = left + (right - left) * Math.min(level, 10000) / 10000f;
            if (end - left < 1f) {
                return;
            }
            rail.set(left, centerY - half, end, centerY + half);
            paint.setColor(fillColor);
            canvas.drawRoundRect(rail, Math.min(radius, (end - left) / 2f), radius, paint);
        }

        @Override
        protected boolean onLevelChange(int level) {
            this.level = level;
            invalidateSelf();
            return true;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return inset * 8;
        }

        @Override
        public int getIntrinsicHeight() {
            return height;
        }
    }

    /**
     * Spinner plate: a rounded field with a chevron on the trailing edge. The framework spinner
     * background is a light 9-patch whose arrow only exists as pixels, so it cannot be recoloured
     * for the dark sheet.
     */
    static final class SpinnerFieldDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final float radius;
        private final float stroke;
        private final float chevronArea;
        private final float chevronSize;
        private final int fill;
        private final int pressedFill;
        private final int strokeColor;
        private final int chevronColor;
        private boolean pressed;

        SpinnerFieldDrawable(float radius, float stroke, float chevronArea, float chevronSize,
                int fill, int pressedFill, int strokeColor, int chevronColor) {
            this.radius = radius;
            this.stroke = stroke;
            this.chevronArea = chevronArea;
            this.chevronSize = chevronSize;
            this.fill = fill;
            this.pressedFill = pressedFill;
            this.strokeColor = strokeColor;
            this.chevronColor = chevronColor;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float inset = stroke / 2f;
            box.set(bounds.left + inset, bounds.top + inset,
                    bounds.right - inset, bounds.bottom - inset);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(pressed ? pressedFill : fill);
            canvas.drawRoundRect(box, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(strokeColor);
            canvas.drawRoundRect(box, radius, radius, paint);

            float centerX = bounds.right - chevronArea / 2f;
            float centerY = (bounds.top + bounds.bottom) / 2f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(stroke, 2f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(chevronColor);
            float half = chevronSize;
            canvas.drawLine(centerX - half, centerY - half * 0.55f,
                    centerX, centerY + half * 0.45f, paint);
            canvas.drawLine(centerX, centerY + half * 0.45f,
                    centerX + half, centerY - half * 0.55f, paint);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean nowPressed = hasState(state, android.R.attr.state_pressed);
            if (nowPressed == pressed) {
                return false;
            }
            pressed = nowPressed;
            invalidateSelf();
            return true;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
