package com.clockmods.ultimate;

import android.app.Application;

import com.clockmods.ultimate.settings.UltimateEmbeddingRules;

/** Registers adaptive settings behavior before the first Activity is created. */
public final class UltimateApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        UltimateEmbeddingRules.install(this);
    }
}
