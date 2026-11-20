package com.clockmods.pro.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = complete(context, intent.getStringExtra("mode"))

    companion object {
        @JvmStatic fun complete(context: Context, mode: String?) = TimerCompletion.complete(context, mode)
    }
}
