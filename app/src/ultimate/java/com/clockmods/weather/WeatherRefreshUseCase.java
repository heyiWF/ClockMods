package com.clockmods.weather;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import com.clockmods.background.ClockPreferences;
/** Background-only use case. Never asks for permission or starts location listeners. */
public final class WeatherRefreshUseCase {
    public enum RefreshResult { SUCCESS, NOT_CONFIGURED, NO_LOCATION, TRANSIENT_FAILURE }
    private final Context context;
    public WeatherRefreshUseCase(Context c) { context=c.getApplicationContext(); }
    public static boolean hasLocation(Context c,ClockPreferences p) {
        return ClockPreferences.WEATHER_LOCATION_MANUAL.equals(p.getWeatherLocationMode()) || lastLocation(c)!=null;
    }
    private static Location lastLocation(Context c) {
        if(c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED
                && c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) return null;
        LocationManager manager=c.getSystemService(LocationManager.class);
        Location best=null;
        try {
            for(String provider:manager.getProviders(true)) {
                Location value=manager.getLastKnownLocation(provider);
                if(value!=null && (best==null || value.getTime()>best.getTime())) best=value;
            }
        } catch(SecurityException ignored) { return null; }
        return best;
    }
    public RefreshResult refreshForWidget(boolean force) {
        if(!QWeatherConfig.isConfigured()) return RefreshResult.NOT_CONFIGURED;
        ClockPreferences p=new ClockPreferences(context);
        String source=p.getWeatherLocationMode(); String locationId=p.getWeatherLocationId(); String language=p.getClockLanguage();
        WeatherRepository repo=new WeatherRepository(context);
        WeatherModels.WeatherDisplayData cached=repo.getCached(source,locationId);
        if(!force && cached!=null && System.currentTimeMillis()-cached.updatedAt<25*60*1000L) return RefreshResult.SUCCESS;
        try {
            QWeatherClient client=new QWeatherClient(context,QWeatherConfig.apiHost());
            WeatherModels.WeatherDisplayData data;
            if(ClockPreferences.WEATHER_LOCATION_MANUAL.equals(source)) {
                String city=p.getWeatherCity(),district=p.getWeatherDistrict();
                if(p.isClockUseEnglish()) {
                    WeatherLocationCatalog.LocationEntry entry=WeatherLocationCatalog.load(context).findById(locationId);
                    if(entry!=null) { city=entry.displayCity(true); district=entry.displayDistrict(true); }
                }
                data=client.fetchLocation(locationId,city,district);
            } else {
                Location location=lastLocation(context);
                if(location==null) return RefreshResult.NO_LOCATION;
                data=client.fetch(location.getLatitude(),location.getLongitude(),false);
            }
            // Do not let an in-flight request overwrite a newly selected city/language.
            ClockPreferences current=new ClockPreferences(context);
            if(!source.equals(current.getWeatherLocationMode()) || !locationId.equals(current.getWeatherLocationId()) || !language.equals(current.getClockLanguage())) return RefreshResult.NO_LOCATION;
            repo.save(data,source);
            return RefreshResult.SUCCESS;
        } catch(Exception ignored) { return RefreshResult.TRANSIENT_FAILURE; }
    }
}
