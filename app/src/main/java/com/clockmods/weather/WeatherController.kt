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
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val repository = WeatherRepository(context)
    private val preferences = ClockPreferences(context)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var running = false; private var generation = 0; private var intervalMinutes = 30; private var locationListener: LocationListener? = null
    // Whether the listener already holds weather data; background refreshes then stay silent.
    private var hasData = false
    private var locationTimeout: Runnable? = null
    fun start(intervalMinutes: Int) { handler.removeCallbacksAndMessages(null); removeLocationListener(); generation++; running = true; this.intervalMinutes = intervalMinutes; val cached = getCached(); if (cached != null) { hasData = true; listener.onWeatherState(WeatherModels.WeatherState(WeatherModels.Status.SUCCESS, cached, null)) }; refreshIfNeeded(cached, cached != null && !cached.satisfies(isDetailedRequired())) }
    fun refreshNow() = refreshIfNeeded(getCached(), true)
    fun stop() { running = false; generation++; handler.removeCallbacksAndMessages(null); removeLocationListener() }
    fun shutdown() { stop(); executor.shutdownNow() }
    private fun refreshIfNeeded(cached: WeatherModels.WeatherDisplayData?, force: Boolean) { if (!running) return; val maxAge = intervalMinutes * 60L * 1000L; if (!force && cached != null && System.currentTimeMillis() - cached.updatedAt < maxAge) { schedule(maxAge - (System.currentTimeMillis() - cached.updatedAt)); return }; if (isManualLocation()) { if (!hasData) listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.LOADING, msg(R.string.weather_fetching))); fetchManual(++generation); return }; if (!hasLocationPermission()) { if (!hasData) listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.IDLE, msg(R.string.weather_waiting_permission))); return }; if (!hasData) listener.onWeatherState(WeatherModels.WeatherState.of(WeatherModels.Status.LOADING, msg(R.string.weather_fetching))); requestLocation() }
    @Suppress("MissingPermission")
    private fun requestLocation() {
        removeLocationListener()
        val requestGeneration = ++generation
        val best = newest(lastKnownLocation(LocationManager.NETWORK_PROVIDER),
            lastKnownLocation(LocationManager.GPS_PROVIDER))
        val age = best?.let { System.currentTimeMillis() - it.time }
        if (best != null && age != null && age >= 0 && age < 10 * 60 * 1000L) {
            fetch(best, requestGeneration)
            return
        }
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!running || requestGeneration != generation || locationListener !== this) return
                removeLocationListener()
                fetch(location, requestGeneration)
            }
        }
        val networkRequested = requestProvider(LocationManager.NETWORK_PROVIDER)
        val gpsRequested = requestProvider(LocationManager.GPS_PROVIDER)
        if (!networkRequested && !gpsRequested) {
            removeLocationListener()
            reportFailure(WeatherModels.Status.LOCATION_UNAVAILABLE, msg(R.string.weather_location_unavailable))
            schedule(intervalMinutes * 60L * 1000L)
            return
        }
        val timeout = Runnable {
            if (!running || requestGeneration != generation || locationListener == null) return@Runnable
            removeLocationListener()
            if (best != null) fetch(best, requestGeneration) else {
                reportFailure(WeatherModels.Status.LOCATION_UNAVAILABLE, msg(R.string.weather_location_unavailable))
                schedule(intervalMinutes * 60L * 1000L)
            }
        }
        locationTimeout = timeout
        handler.postDelayed(timeout, LOCATION_TIMEOUT_MS)
    }

    private fun lastKnownLocation(provider: String): Location? = try {
        locationManager?.getLastKnownLocation(provider)
    } catch (_: SecurityException) { null } catch (_: IllegalArgumentException) { null }

    private fun requestProvider(provider: String): Boolean = try {
        val manager = locationManager
        if (manager == null || !manager.isProviderEnabled(provider)) false else {
            manager.requestLocationUpdates(provider, 0L, 0f, locationListener!!)
            true
        }
    } catch (_: SecurityException) { false } catch (_: IllegalArgumentException) { false }

    private fun fetch(location: Location, requestGeneration: Int) { if (!QWeatherConfig.isConfigured()) { reportFailure(WeatherModels.Status.CONFIG_ERROR, msg(R.string.weather_not_configured)); return }; executor.execute { try { val data = QWeatherClient(context, QWeatherConfig.apiHost()).fetch(location.latitude, location.longitude, isDetailedRequired()); handler.post { if (running && requestGeneration == generation) { repository.save(data, ClockPreferences.WEATHER_LOCATION_AUTOMATIC); hasData = true; listener.onWeatherState(WeatherModels.WeatherState(WeatherModels.Status.SUCCESS, data, null)); schedule(intervalMinutes * 60L * 1000L) } } } catch (error: Exception) { handler.post { if (running && requestGeneration == generation) { reportFailure(WeatherModels.Status.NETWORK_ERROR, msg(R.string.weather_fetch_failed, describeError(error))); schedule(intervalMinutes * 60L * 1000L) } } } } }
    private fun fetchManual(requestGeneration: Int) { if (!QWeatherConfig.isConfigured()) { reportFailure(WeatherModels.Status.CONFIG_ERROR, msg(R.string.weather_not_configured)); return }; val locationId = preferences.getWeatherLocationId(); val city = preferences.getWeatherCity(); val district = preferences.getWeatherDistrict(); val detailed = isDetailedRequired(); val english = preferences.isClockUseEnglish(); val latitude = preferences.getWeatherLatitude(); val longitude = preferences.getWeatherLongitude(); executor.execute { try { var lat = latitude; var lon = longitude; var displayCity = city; var displayDistrict = district; val needCoordinates = detailed && (lat.isNaN() || lon.isNaN()); if (english || needCoordinates) WeatherLocationCatalog.load(context).findById(locationId)?.let { entry -> if (needCoordinates) { lat = entry.latitude; lon = entry.longitude }; if (english) { displayCity = entry.displayCity(true); displayDistrict = entry.displayDistrict(true) } }; val data = QWeatherClient(context, QWeatherConfig.apiHost()).fetchLocation(locationId, displayCity, displayDistrict, lat, lon, detailed); handler.post { if (running && requestGeneration == generation) { repository.save(data, ClockPreferences.WEATHER_LOCATION_MANUAL); hasData = true; listener.onWeatherState(WeatherModels.WeatherState(WeatherModels.Status.SUCCESS, data, null)); schedule(intervalMinutes * 60L * 1000L) } } } catch (error: Exception) { handler.post { if (running && requestGeneration == generation) { reportFailure(WeatherModels.Status.NETWORK_ERROR, msg(R.string.weather_fetch_failed, describeError(error))); schedule(intervalMinutes * 60L * 1000L) } } } } }
    private fun reportFailure(status: WeatherModels.Status, detail: String) {
        // Keep the data already on screen; only a load without data surfaces the failure.
        if (hasData) return
        WeatherFailureFeedback.showOnce(context, detail)
        listener.onWeatherState(WeatherModels.WeatherState.of(status, WeatherFailureFeedback.placeholder(context)))
    }

    private fun getCached() = repository.getCached(if (isManualLocation()) ClockPreferences.WEATHER_LOCATION_MANUAL else ClockPreferences.WEATHER_LOCATION_AUTOMATIC, preferences.getWeatherLocationId())
    private fun isManualLocation() = preferences.getWeatherLocationMode() == ClockPreferences.WEATHER_LOCATION_MANUAL
    private fun isDetailedRequired() = detailedOverride ?: preferences.isWeatherDetailed()
    private fun schedule(delay: Long) { handler.removeCallbacksAndMessages(null); handler.postDelayed({ refreshNow() }, maxOf(1000L, delay)) }
    private fun hasLocationPermission() = android.os.Build.VERSION.SDK_INT < 23 || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun removeLocationListener() { locationTimeout?.let { handler.removeCallbacks(it) }; locationTimeout = null; locationListener?.let { try { locationManager?.removeUpdates(it) } catch (_: SecurityException) {} }; locationListener = null }
    private fun msg(resId: Int): String = LocaleManager.wrap(context)!!.getString(resId)
    private fun msg(resId: Int, vararg args: Any): String = LocaleManager.wrap(context)!!.getString(resId, *args)
    private fun newest(first: Location?, second: Location?): Location? = when { first == null -> second; second == null -> first; first.time >= second.time -> first; else -> second }
    private fun describeError(error: Throwable) = error.message?.trim().takeUnless { it.isNullOrEmpty() } ?: error.javaClass.simpleName
    companion object { private const val LOCATION_TIMEOUT_MS = 15000L }
}
