package com.clockmods.sdk.style;

/** Renderer-independent identity shared by clock and calendar style descriptors. */
public interface StyleIdentity {
    String getId();
    int getVersion();
    int getMinApi();
    boolean supportsApi(int apiLevel);
}
