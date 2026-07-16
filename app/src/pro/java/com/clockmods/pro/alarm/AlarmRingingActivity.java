package com.clockmods.pro.alarm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.clockmods.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AlarmRingingActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_alarm_ringing);
        ((TextView) findViewById(R.id.ringing_time)).setText(
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        findViewById(R.id.alarm_dismiss).setOnClickListener(view -> {
            stopService(new Intent(this, AlarmRingingService.class));
            AlarmNotifications.cancel(this);
            finishAndRemoveTask();
        });
    }
}