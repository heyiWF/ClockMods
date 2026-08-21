package com.clockmods.pro;

/** Page contract used by the host when settings change without recreating the activity. */
public interface SettingsRefreshable {
    void refreshSettings();
}
