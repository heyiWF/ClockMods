package com.clockmods.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import com.clockmods.background.ClockPreferences

/** Background-only use case. Never asks for permission or starts location listeners. */
class WeatherRefreshUseCase(context: Context) {
    enum class RefreshResult {
        SUCCESS,
        NOT_CONFIGURED,
        NO_LOCATION,
        TRANSIENT_FAILURE,
    }

    private val context = context.applicationContext

    fun refreshForWidget(force: Boolean): RefreshResult {
        if (!QWeatherConfig.isConfigured()) return RefreshResult.NOT_CONFIGURED

        val preferences = ClockPreferences(context)
        val source = preferences.getWeatherLocationMode()
        val locationId = preferences.getWeatherLocationId()
        val language = preferences.getClockLanguage()
        val repository = WeatherRepository(context)
        val cached = repository.getCached(source, locationId)
        if (!force && cached != null && System.currentTimeMillis() - cached.updatedAt < CACHE_FRESHNESS_MILLIS) {
            return RefreshResult.SUCCESS
        }

        return try {
            val client = QWeatherClient(context, QWeatherConfig.apiHost())
            val data = if (source == ClockPreferences.WEATHER_LOCATION_MANUAL) {
                var city = preferences.getWeatherCity()
                var district = preferences.getWeatherDistrict()
                if (preferences.isClockUseEnglish()) {
                    WeatherLocationCatalog.load(context).findById(locationId)?.let { entry ->
                        city = entry.displayCity(true)
                        district = entry.displayDistrict(true)
                    }
                }
                client.fetchLocation(locationId, city, district)
            } else {
                val location = lastLocation(context) ?: return RefreshResult.NO_LOCATION
                client.fetch(location.latitude, location.longitude, false)
            }

            // Do not let an in-flight request overwrite a newly selected city/language.
            val current = ClockPreferences(context)
            if (
                source != current.getWeatherLocationMode() ||
                locationId != current.getWeatherLocationId() ||
                language != current.getClockLanguage()
            ) {
                return RefreshResult.NO_LOCATION
            }
            repository.save(data, source)
            RefreshResult.SUCCESS
        } catch (_: Exception) {
            RefreshResult.TRANSIENT_FAILURE
        }
    }

    companion object {
        private const val CACHE_FRESHNESS_MILLIS = 25 * 60 * 1000L

        @JvmStatic
        fun hasLocation(context: Context, preferences: ClockPreferences): Boolean =
            preferences.getWeatherLocationMode() == ClockPreferences.WEATHER_LOCATION_MANUAL ||
                lastLocation(context) != null

        private fun lastLocation(context: Context): Location? {
            if (
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            val manager = context.getSystemService(LocationManager::class.java)
            return try {
                manager.getProviders(true)
                    .mapNotNull(manager::getLastKnownLocation)
                    .maxByOrNull(Location::getTime)
            } catch (_: SecurityException) {
                null
            }
        }
    }
}
