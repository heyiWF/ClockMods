package com.clockmods.pro;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clockmods.R;
import com.clockmods.pro.alarm.AlarmScheduler;
import com.clockmods.pro.alarm.AlarmStore;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class ProAlarmFragment extends Fragment {
    private AlarmStore store;
    private TextView time;
    private TextView next;
    private MaterialSwitch enabled;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pro_alarm, container, false);
        store = new AlarmStore(requireContext());
        time = root.findViewById(R.id.alarm_time);
        next = root.findViewById(R.id.alarm_next);
        enabled = root.findViewById(R.id.alarm_enabled);
        enabled.setChecked(store.enabled());
        enabled.setOnCheckedChangeListener((button, checked) -> {
            store.setEnabled(checked);
            if (checked) AlarmScheduler.schedule(requireContext(), store.hour(), store.minute());
            else AlarmScheduler.cancel(requireContext());
            render();
        });
        root.findViewById(R.id.alarm_edit).setOnClickListener(view -> new TimePickerDialog(
                requireContext(), (picker, hour, minute) -> {
                    store.save(hour, minute, true);
                    enabled.setChecked(true);
                    AlarmScheduler.schedule(requireContext(), hour, minute);
                    render();
                }, store.hour(), store.minute(), true).show());
        render();
        return root;
    }

    private void render() {
        time.setText(String.format(Locale.getDefault(), "%02d:%02d", store.hour(), store.minute()));
        if (!store.enabled()) {
            next.setText(R.string.alarm_disabled);
            return;
        }
        long trigger = AlarmScheduler.nextTrigger(store.hour(), store.minute(),
                System.currentTimeMillis());
        next.setText(getString(R.string.alarm_next_trigger,
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(trigger))));
    }
}