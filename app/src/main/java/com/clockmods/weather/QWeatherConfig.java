package com.clockmods.weather;

import com.clockmods.BuildConfig;

public final class QWeatherConfig {
    private QWeatherConfig() { }
    public static boolean isConfigured() {
        return notBlank(BuildConfig.QWEATHER_API_HOST)
                && notBlank(BuildConfig.QWEATHER_CREDENTIAL_ID)
                && notBlank(BuildConfig.QWEATHER_PROJECT_ID)
                && notBlank(BuildConfig.QWEATHER_PRIVATE_KEY_BASE64)
                && !BuildConfig.QWEATHER_CREDENTIAL_ID.startsWith("replace-");
    }
    public static String apiHost() { return BuildConfig.QWEATHER_API_HOST; }
    public static String credentialId() { return BuildConfig.QWEATHER_CREDENTIAL_ID; }
    public static String projectId() { return BuildConfig.QWEATHER_PROJECT_ID; }
    public static String privateKeyBase64() { return BuildConfig.QWEATHER_PRIVATE_KEY_BASE64; }
    private static boolean notBlank(String value) { return value != null && value.trim().length() > 0; }
}