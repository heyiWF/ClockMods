package com.clockmods.pro.style;

/**
 * The two-layer paging geometry of a layout that declares
 * {@link CalendarLayoutCapabilities.Capability#PAGE_SWIPE}: the page at rest and the neighbour
 * waiting off-screen beside it.
 *
 * <p>The split with the host is deliberate. The layout owns the geometry — how wide a page is, how
 * the two layers translate — because only it knows its own view tree. The host owns the
 * <em>policy</em>: whether animations are enabled at all, and the monotonic epoch that stops a
 * settled animation from advancing the date after a direct jump has already re-rendered.</p>
 */
public interface CalendarPager {
    /** How far one page moves the date. */
    enum PageUnit { DAY, WEEK, MONTH }

    PageUnit unit();

    /** Width of one page in pixels; 0 before the layout has been measured. */
    float pageWidth();

    /**
     * Which neighbour is currently built, or 0 for none. The host checks this before building an
     * adjacent {@link CalendarPageState}, so a drag does not re-resolve a whole page every frame.
     */
    int preparedDirection();

    /** Builds the neighbour in {@code direction} and parks it off-screen. */
    void bindPreview(int direction, CalendarPageState adjacent);

    /** Moves both layers to follow a drag. */
    void setPageOffset(float offsetPx);

    /**
     * Slides the neighbour into place. {@code onSettled} runs when the animation ends — the host
     * advances its own date there, so it must not be assumed to run on a cancelled animation.
     */
    void animateCommit(int direction, Runnable onSettled);

    /** Returns both layers to rest without changing the date. */
    void animateSnapBack(Runnable onSettled);

    /** Drops the neighbour and puts the page back at rest, cancelling any running animation. */
    void resetPages();
}
