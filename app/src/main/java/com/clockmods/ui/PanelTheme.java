package com.clockmods.ui;

/**
 * Single source of truth for the settings sheet's palette.
 *
 * <p>Every control in the sheet is drawn by hand (see {@link WidgetStyles}) because Android 1.0
 * ships light, un-tintable widget assets. Keeping the colours here means a control's fill, its
 * stroke and the card behind it cannot drift apart.</p>
 */
public final class PanelTheme {
    /** Backdrop of the whole sheet. */
    public static final int SURFACE = 0xFF1B1D22;
    /** Rounded card that groups one settings module. */
    public static final int CARD = 0xFF23262D;
    /** Inset control surface: text fields, spinners, secondary buttons. */
    public static final int FIELD = 0xFF2C313A;
    /** Same surface while the finger is down. */
    public static final int FIELD_PRESSED = 0xFF353B46;
    /** Hairline around an inset control. */
    public static final int FIELD_STROKE = 0xFF3A404B;
    /** Empty part of a slider track. */
    public static final int TRACK = 0xFF343A45;
    /** Slider thumb at rest. */
    public static final int THUMB = 0xFFDDE3EC;

    public static final int TITLE = 0xFFF3F4F6;
    public static final int SECTION = 0xFF60A5FA;
    public static final int PRIMARY_TEXT = 0xFFE5E7EB;
    public static final int SECONDARY_TEXT = 0xFF9CA3AF;
    /** Outline and symbol of an unchecked toggle. */
    public static final int TOGGLE_STROKE = 0xFF6B7280;
    public static final int SWATCH_STROKE = 0x33FFFFFF;
    /**
     * Scroll indicator. The framework draws a near-white bar that reads as a bright stripe on the
     * dark sheet, so the sheet draws its own dim thumb instead.
     */
    public static final int SCROLL_THUMB = 0x59FFFFFF;

    /** Accent for text, selected pills and slider fill. */
    public static final int ACCENT = 0xFF60A5FA;
    /** Deeper accent used behind white text so the primary button stays legible. */
    public static final int ACCENT_STRONG = 0xFF3B82F6;
    public static final int ACCENT_STRONG_PRESSED = 0xFF2F6FE0;
    public static final int ON_ACCENT = 0xFFFFFFFF;

    private PanelTheme() {
    }
}
