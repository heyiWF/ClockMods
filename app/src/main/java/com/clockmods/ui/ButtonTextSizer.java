package com.clockmods.ui;

import android.graphics.Paint;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TransformationMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/** Keeps single-line button labels readable when a narrow layout or locale needs more room. */
public final class ButtonTextSizer {
    private static final float DEFAULT_MIN_SP = 10f;
    private static final float FALLBACK_BASE_SP = 14f;
    private static final float EPSILON_PX = 0.25f;
    private static final Map<TextView, Float> BASE_TEXT_SIZES = new WeakHashMap<>();
    private static final Map<TextView, Boolean> LISTENERS_INSTALLED = new WeakHashMap<>();

    private ButtonTextSizer() {
    }

    /** Applies fitting to one button-like text view and re-fits after text or size changes. */
    public static void apply(TextView view) {
        if (view == null) return;
        synchronized (BASE_TEXT_SIZES) {
            if (!BASE_TEXT_SIZES.containsKey(view)) {
                float size = view.getTextSize();
                if (size <= 0f) {
                    size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                            FALLBACK_BASE_SP, view.getResources().getDisplayMetrics());
                }
                BASE_TEXT_SIZES.put(view, size);
            }
            if (!LISTENERS_INSTALLED.containsKey(view)) {
                view.addOnLayoutChangeListener((changed, left, top, right, bottom,
                        oldLeft, oldTop, oldRight, oldBottom) -> fit(changed));
                view.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start,
                            int count, int after) {
                    }

                    @Override public void onTextChanged(CharSequence s, int start,
                            int before, int count) {
                        view.post(() -> fit(view));
                    }

                    @Override public void afterTextChanged(Editable editable) {
                    }
                });
                LISTENERS_INSTALLED.put(view, Boolean.TRUE);
            }
        }
        view.setSingleLine(true);
        view.setEllipsize(null);
        fit(view);
    }

    /** Applies fitting to every button-like control in a view tree. */
    public static void applyToTree(View root) {
        if (root == null) return;
        if (root instanceof TextView && isButtonLike((TextView) root)) {
            apply((TextView) root);
        }
        if (!(root instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) root;
        for (int index = 0; index < group.getChildCount(); index++) {
            applyToTree(group.getChildAt(index));
        }
    }

    /** Applies fitting to all text labels inside a control such as a bottom navigation bar. */
    public static void applyAllTextToTree(View root) {
        if (root == null) return;
        if (root instanceof TextView && !(root instanceof EditText)) {
            apply((TextView) root);
        }
        if (!(root instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) root;
        for (int index = 0; index < group.getChildCount(); index++) {
            applyAllTextToTree(group.getChildAt(index));
        }
    }

    /** Marks a clickable text label such as a custom segmented-control segment for fitting. */
    public static void applyClickableLabel(TextView view) {
        apply(view);
    }

    private static boolean isButtonLike(TextView view) {
        if (view instanceof EditText) return false;
        View parent = view.getParent() instanceof View ? (View) view.getParent() : null;
        return view instanceof Button || view instanceof CompoundButton || view.isClickable()
                || (parent != null && parent.isClickable());
    }

    private static void fit(View view) {
        if (!(view instanceof TextView)) return;
        TextView textView = (TextView) view;
        Float baseSize;
        synchronized (BASE_TEXT_SIZES) {
            baseSize = BASE_TEXT_SIZES.get(textView);
        }
        if (baseSize == null || textView.getWidth() <= 0 || textView.getHeight() <= 0) return;

        CharSequence text = textView.getText();
        if (TextUtils.isEmpty(text)) {
            setTextSizePx(textView, baseSize);
            return;
        }
        TransformationMethod transformation = textView.getTransformationMethod();
        if (transformation != null) {
            CharSequence transformed = transformation.getTransformation(text, textView);
            if (transformed != null) text = transformed;
        }
        String value = text.toString().replace('\n', ' ');
        if (value.length() == 0) {
            setTextSizePx(textView, baseSize);
            return;
        }

        int availableWidth = textView.getWidth()
                - textView.getCompoundPaddingLeft() - textView.getCompoundPaddingRight();
        if (availableWidth <= 0) {
            availableWidth = textView.getWidth()
                    - textView.getPaddingLeft() - textView.getPaddingRight();
        }
        int availableHeight = textView.getHeight()
                - textView.getPaddingTop() - textView.getPaddingBottom();
        if (availableWidth <= 0 || availableHeight <= 0) return;

        Paint paint = textView.getPaint();
        float originalPaintSize = paint.getTextSize();
        paint.setTextSize(baseSize);
        float textWidth = paint.measureText(value);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        float widthScale = textWidth <= 0f ? 1f : availableWidth / textWidth;
        float heightScale = textHeight <= 0f ? 1f : availableHeight / textHeight;
        float targetSize = Math.min(baseSize, baseSize * Math.min(widthScale, heightScale));
        float minSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                DEFAULT_MIN_SP, textView.getResources().getDisplayMetrics());
        targetSize = Math.max(Math.min(baseSize, minSize), targetSize);
        paint.setTextSize(originalPaintSize);
        setTextSizePx(textView, targetSize);
    }

    private static void setTextSizePx(TextView view, float size) {
        if (Math.abs(view.getTextSize() - size) > EPSILON_PX) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }
}
