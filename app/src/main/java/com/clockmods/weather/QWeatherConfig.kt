package com.clockmods.weather

import com.clockmods.BuildConfig

object QWeatherConfig {
    @JvmStatic
    fun isConfigured(): Boolean =
        notBlank(BuildConfig.QWEATHER_API_HOST) &&
            notBlank(BuildConfig.QWEATHER_CREDENTIAL_ID) &&
            notBlank(BuildConfig.QWEATHER_DEVELOPER_ID) &&
            notBlank(BuildConfig.QWEATHER_PROJECT_ID) &&
            notBlank(BuildConfig.QWEATHER_PRIVATE_KEY_BASE64) &&
            !BuildConfig.QWEATHER_CREDENTIAL_ID.startsWith("replace-") &&
            !BuildConfig.QWEATHER_DEVELOPER_ID.startsWith("replace-")

    @JvmStatic fun apiHost(): String = BuildConfig.QWEATHER_API_HOST
    @JvmStatic fun credentialId(): String = BuildConfig.QWEATHER_CREDENTIAL_ID
    @JvmStatic fun developerId(): String = BuildConfig.QWEATHER_DEVELOPER_ID
    @JvmStatic fun projectId(): String = BuildConfig.QWEATHER_PROJECT_ID
    @JvmStatic fun privateKeyBase64(): String = BuildConfig.QWEATHER_PRIVATE_KEY_BASE64

    private fun notBlank(value: String?): Boolean = !value.isNullOrBlank()
}
