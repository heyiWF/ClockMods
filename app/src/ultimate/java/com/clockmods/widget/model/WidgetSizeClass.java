package com.clockmods.widget.model;
/**
 * The layout a widget resolves to.
 *
 * <p>{@code ROW} is the one-row card that is wide enough to hold its lead value and its caption
 * side by side; a narrower one-row card is {@code COMPACT} and stacks them instead. Both are short
 * cards, so they share the module set that fits a single launcher row.
 */
public enum WidgetSizeClass { COMPACT, ROW, SMALL, WIDE, TALL, LARGE }
