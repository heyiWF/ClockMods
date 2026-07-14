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

        List<String> provinces = catalog.provinces();
        setItems(context, provinceSpinner, provinces);
        String province = contains(provinces, initialProvince) ? initialProvince : provinces.get(0);
        List<String> cities = catalog.cities(province);
        setItems(context, citySpinner, cities);
        String city = contains(cities, initialCity) ? initialCity : cities.get(0);
        List<WeatherLocationCatalog.LocationEntry> districts = catalog.districts(province, city);
        setDistricts(context, districtSpinner, districts);
        provinceSpinner.setSelection(provinces.indexOf(province));
        citySpinner.setSelection(cities.indexOf(city));
        districtSpinner.setSelection(indexOfDistrict(districts, initialDistrict));

        provinceSpinner.setOnItemSelectedListener(new SimpleSelectionListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view,
                    int position, long id) {
                List<String> filteredCities = catalog.cities((String) provinceSpinner.getSelectedItem());
                setItems(context, citySpinner, filteredCities);
                updateDistricts(context, catalog, provinceSpinner, citySpinner, districtSpinner);
            }
        });
        citySpinner.setOnItemSelectedListener(new SimpleSelectionListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view,
                    int position, long id) {
                updateDistricts(context, catalog, provinceSpinner, citySpinner, districtSpinner);
            }
        });

        new AlertDialog.Builder(context)
                .setTitle(R.string.weather_choose_location)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    List<WeatherLocationCatalog.LocationEntry> selected = catalog.districts(
                            (String) provinceSpinner.getSelectedItem(),
                            (String) citySpinner.getSelectedItem());
                    int position = districtSpinner.getSelectedItemPosition();
                    if (position >= 0 && position < selected.size()) listener.onLocationSelected(selected.get(position));
                })
                .show();
    }

    private static void updateDistricts(Context context, WeatherLocationCatalog catalog,
            Spinner province, Spinner city, Spinner district) {
        if (province.getSelectedItem() == null || city.getSelectedItem() == null) return;
        setDistricts(context, district, catalog.districts(
                (String) province.getSelectedItem(), (String) city.getSelectedItem()));
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

    private static boolean contains(List<String> values, String value) {
        return value != null && values.contains(value);
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