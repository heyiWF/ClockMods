package com.clockmods.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.clockmods.R;
import com.clockmods.weather.WeatherLocationCatalog;

import java.util.ArrayList;
import java.util.List;

public final class WeatherLocationChooser {
    public interface Listener {
        void onLocationSelected(WeatherLocationCatalog.LocationEntry location);
    }

    private WeatherLocationChooser() { }

    public static void show(Context context, String initialProvince, String initialCity,
            String initialDistrict, Listener listener) {
        final WeatherLocationCatalog catalog;
        try {
            catalog = WeatherLocationCatalog.load(context);
        } catch (Exception error) {
            Toast.makeText(context, R.string.weather_location_list_error, Toast.LENGTH_SHORT).show();
            return;
        }
        int padding = Math.round(20f * context.getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding / 2, padding, 0);
        Spinner provinceSpinner = new Spinner(context);
        Spinner citySpinner = new Spinner(context);
        Spinner districtSpinner = new Spinner(context);
        content.addView(provinceSpinner, spinnerParams());
        content.addView(citySpinner, spinnerParams());
        content.addView(districtSpinner, spinnerParams());

        final List<String> provinces = catalog.provinces();
        setItems(context, provinceSpinner, provinces);
        int provinceIndex = indexOfKey(provinces, initialProvince);
        String provinceKey = provinces.get(provinceIndex);
        List<String> cities = catalog.cities(provinceKey);
        setItems(context, citySpinner, cities);
        int cityIndex = indexOfKey(cities, initialCity);
        List<WeatherLocationCatalog.LocationEntry> districts =
                catalog.districts(provinceKey, cities.get(cityIndex));
        setDistricts(context, districtSpinner, districts);
        provinceSpinner.setSelection(provinceIndex);
        citySpinner.setSelection(cityIndex);
        districtSpinner.setSelection(indexOfDistrict(districts, initialDistrict));

        provinceSpinner.setOnItemSelectedListener(new SimpleSelectionListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view,
                    int position, long id) {
                String key = provinces.get(clampIndex(position, provinces.size()));
                setItems(context, citySpinner, catalog.cities(key));
                updateDistricts(context, catalog, provinces, provinceSpinner, citySpinner,
                        districtSpinner);
            }
        });
        citySpinner.setOnItemSelectedListener(new SimpleSelectionListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view,
                    int position, long id) {
                updateDistricts(context, catalog, provinces, provinceSpinner, citySpinner,
                        districtSpinner);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.weather_choose_location)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, (which, whichButton) -> {
                    List<WeatherLocationCatalog.LocationEntry> selected =
                            currentDistricts(catalog, provinces, provinceSpinner, citySpinner);
                    int position = districtSpinner.getSelectedItemPosition();
                    if (position >= 0 && position < selected.size()) listener.onLocationSelected(selected.get(position));
                })
                .create();
        dialog.show();
        TypefaceApplier.applyToTree(dialog.getWindow().getDecorView());
    }

    private static void updateDistricts(Context context, WeatherLocationCatalog catalog,
            List<String> provinces, Spinner province, Spinner city, Spinner district) {
        setDistricts(context, district, currentDistricts(catalog, provinces, province, city));
    }

    /** Resolves the districts for the current province/city selection via canonical Chinese keys. */
    private static List<WeatherLocationCatalog.LocationEntry> currentDistricts(
            WeatherLocationCatalog catalog, List<String> provinces, Spinner province, Spinner city) {
        int provincePos = province.getSelectedItemPosition();
        int cityPos = city.getSelectedItemPosition();
        if (provincePos < 0 || provincePos >= provinces.size() || cityPos < 0) {
            return new ArrayList<>();
        }
        String provinceKey = provinces.get(provincePos);
        List<String> cities = catalog.cities(provinceKey);
        if (cityPos >= cities.size()) return new ArrayList<>();
        return catalog.districts(provinceKey, cities.get(cityPos));
    }

    private static void setItems(Context context, Spinner spinner, List<String> values) {
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, values));
    }

    private static void setDistricts(Context context, Spinner spinner,
            List<WeatherLocationCatalog.LocationEntry> values) {
        List<String> names = new ArrayList<>();
        for (WeatherLocationCatalog.LocationEntry value : values) names.add(value.district);
        setItems(context, spinner, names);
    }

    private static int clampIndex(int index, int size) {
        return index < 0 || index >= size ? 0 : index;
    }

    /** @return the index of {@code key} in {@code keys}, or 0 when absent, so a selection always exists. */
    private static int indexOfKey(List<String> keys, String key) {
        int index = key == null ? -1 : keys.indexOf(key);
        return index < 0 ? 0 : index;
    }

    private static int indexOfDistrict(List<WeatherLocationCatalog.LocationEntry> values, String district) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).district.equals(district)) return index;
        }
        return 0;
    }

    private static LinearLayout.LayoutParams spinnerParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private abstract static class SimpleSelectionListener implements AdapterView.OnItemSelectedListener {
        @Override public void onNothingSelected(AdapterView<?> parent) { }
    }
}