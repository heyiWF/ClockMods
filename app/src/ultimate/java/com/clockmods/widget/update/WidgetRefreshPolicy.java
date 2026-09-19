package com.clockmods.widget.update;
public final class WidgetRefreshPolicy {
    private WidgetRefreshPolicy() { }
    public static boolean shouldSchedule(int weatherCount) { return weatherCount>0; }
    public static boolean shouldRetry(boolean configured,boolean transientFailure,int attempt) { return configured && transientFailure && attempt<2; }
}
