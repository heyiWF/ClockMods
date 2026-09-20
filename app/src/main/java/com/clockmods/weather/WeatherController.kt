package com.clockmods.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import com.clockmods.LocaleManager
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import java.util.concurrent.Executors

class WeatherController @JvmOverloads constructor(context: Context, private val listener: Listener, private val detailedOverride: Boolean? = null) {
    interface Listener { fun onWeatherState(state: WeatherModels.WeatherState) }
    private val context = context.applicationContext
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val repository = WeatherRepository(context)
    private val preferences = ClockPreferences(context)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var running = false; private var generation = 0; private var intervalMinutes = 30; private var locationListener: LocationListener? = null
    fun start(intervalMinutes: Int) { handler.removeCallbacksAndMessages(null); removeLocationListener(); generation++; running = true; this.intervalMinutes = intervalMinutes; val cached = getCached(); if (cached != null) listener.onWeatherState(WeatherModels.WeatherState(WeatherModels.Status.SUCCESS, cached, null)); refreshIfNeeded(cached, cached != null && !cached.satisfies(isDetailedRequired())) }
    fun refreshNow() = refreshIfNeeded(getCached(), true)
    fun stop() { running = false; generation++; handler.removeCallbacksAndMessages(null); removeLocationListener() }
    fun shutdown() { stop(); executor.shutdownNow() }
    private fun refreshIfNeeded(cached: WeatherModels.WeatherDisplayData?, force: Boolean) { if (!running) return; val maxAge = intervalMinutes * 60L * 1000L; if (!force && cached != null && System.currentTimeMillis() - cached.updatedAt < maxAge) { schedule(maxAge - (System.currentTimeMillis() - cached.updatedAt)); return }; if (isManualLocation()) { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.LOADING, msg(R.string.weather_fetching))); fetchManual(++generation); return }; if (!hasLocationPermission()) { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.PERMISSION_DENIED, msg(R.string.weather_permission_denied))); return }; listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.LOADING, msg(R.string.weather_fetching))); requestLocation() }
    @Suppress("MissingPermission") private fun requestLocation() { val requestGeneration = ++generation; val best = newest(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)); if (best != null && System.currentTimeMillis() - best.time < 10 * 60 * 1000L) { fetch(best, requestGeneration); return }; val fallback = best; locationListener = object : LocationListener { override fun onLocationChanged(location: Location) { removeLocationListener(); fetch(location, requestGeneration) } }; var requested = false; if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener!!); requested = true }; if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener!!); requested = true }; if (!requested) { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.LOCATION_UNAVAILABLE, msg(R.string.weather_location_unavailable))); schedule(intervalMinutes * 60L * 1000L); return }; handler.postDelayed({ if (!running || requestGeneration != generation) return@postDelayed; removeLocationListener(); if (fallback != null) fetch(fallback, requestGeneration) else { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.LOCATION_UNAVAILABLE, msg(R.string.weather_location_unavailable))); schedule(intervalMinutes * 60L * 1000L) } }, LOCATION_TIMEOUT_MS) }
    private fun fetch(location: Location, requestGeneration: Int) { if (!QWeatherConfig.isConfigured()) { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.CONFIG_ERROR, msg(R.string.weather_not_configured))); return }; executor.execute { try { val data = QWeatherClient(context, QWeatherConfig.apiHost()).fetch(location.latitude, location.longitude, isDetailedRequired()); repository.save(data, ClockPreferences.WEATHER_LOCATION_AUTOMATIC); handler.post { if (running && requestGeneration == generation) { listener.onWeatherState(WeatherModels.WeatherState(WeatherModels.Status.SUCCESS, data, null)); schedule(intervalMinutes * 60L * 1000L) } } } catch (error: Exception) { handler.post { if (running && requestGeneration == generation) { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.NETWORK_ERROR, msg(R.string.weather_fetch_failed, describeError(error)))); schedule(intervalMinutes * 60L * 1000L) } } } } }
    private fun fetchManual(requestGeneration: Int) { if (!QWeatherConfig.isConfigured()) { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.CONFIG_ERROR, msg(R.string.weather_not_configured))); return }; val locationId = preferences.getWeatherLocationId(); val city = preferences.getWeatherCity(); val district = preferences.getWeatherDistrict(); val detailed = isDetailedRequired(); val english = preferences.isClockUseEnglish(); val latitude = preferences.getWeatherLatitude(); val longitude = preferences.getWeatherLongitude(); executor.execute { try { var lat = latitude; var lon = longitude; var displayCity = city; var displayDistrict = district; val needCoordinates = detailed && (lat.isNaN() || lon.isNaN()); if (english || needCoordinates) WeatherLocationCatalog.load(context).findById(locationId)?.let { entry -> if (needCoordinates) { lat = entry.latitude; lon = entry.longitude }; if (english) { displayCity = entry.displayCity(true); displayDistrict = entry.displayDistrict(true) } }; val data = QWeatherClient(context, QWeatherConfig.apiHost()).fetchLocation(locationId, displayCity, displayDistrict, lat, lon, detailed); repository.save(data, ClockPreferences.WEATHER_LOCATION_MANUAL); handler.post { if (running && requestGeneration == generation) { listener.onWeatherState(WeatherModels.WeatherState(WeatherModels.Status.SUCCESS, data, null)); schedule(intervalMinutes * 60L * 1000L) } } } catch (error: Exception) { handler.post { if (running && requestGeneration == generation) { listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.NETWORK_ERROR, msg(R.string.weather_fetch_failed, describeError(error)))); schedule(intervalMinutes * 60L * 1000L) } } } } }
    private fun getCached() = repository.getCached(if (isManualLocation()) ClockPreferences.WEATHER_LOCATION_MANUAL else ClockPreferences.WEATHER_LOCATION_AUTOMATIC, preferences.getWeatherLocationId())
    private fun isManualLocation() = preferences.getWeatherLocationMode() == ClockPreferences.WEATHER_LOCATION_MANUAL
    private fun isDetailedRequired() = detailedOverride ?: preferences.isWeatherDetailed()
    private fun schedule(delay: Long) { handler.removeCallbacksAndMessages(null); handler.postDelayed({ refreshNow() }, maxOf(1000L, delay)) }
    private fun hasLocationPermission() = android.os.Build.VERSION.SDK_INT < 23 || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun removeLocationListener() { locationListener?.let { try { locationManager.removeUpdates(it) } catch (_: SecurityException) {} }; locationListener = null }
    private fun msg(resId: Int): String = LocaleManager.wrap(context)!!.getString(resId)
    private fun msg(resId: Int, vararg args: Any): String = LocaleManager.wrap(context)!!.getString(resId, *args)
    private fun newest(first: Location?, second: Location?): Location? = when { first == null -> second; second == null -> first; first.time >= second.time -> first; else -> second }
    private fun describeError(error: Throwable) = error.message?.trim().takeUnless { it.isNullOrEmpty() } ?: error.javaClass.simpleName
    companion object { private const val LOCATION_TIMEOUT_MS = 15000L }
}
