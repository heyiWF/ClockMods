package com.clockmods.pro.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.clockmods.LocaleManager
import org.json.JSONObject

/** Activity boundaries keep system predictive Back and independent, unsaved page drafts. */
abstract class AlarmUi : ComponentActivity() {
    @JvmField protected var draft = AlarmSpec()
    protected var revision by mutableIntStateOf(0)
    override fun attachBaseContext(base: Context) = super.attachBaseContext(LocaleManager.wrap(base) ?: base)
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge()
        val json = if (state == null) intent.getStringExtra(DRAFT) else state.getString(DRAFT)
        draft = runCatching { json?.let { AlarmSpec.fromJson(JSONObject(it)) } ?: AlarmSpec() }.getOrDefault(AlarmSpec())
    }
    override fun onSaveInstanceState(state: Bundle) { state.putString(DRAFT, json()); super.onSaveInstanceState(state) }
    protected fun json(): String = draft.toJson().toString()
    protected fun edit(change: (AlarmSpec) -> Unit) { draft = AlarmSpec(draft).also(change); revision++ }
    protected fun returnDraft() { setResult(RESULT_OK, Intent().putExtra(DRAFT, json())); finish() }
    companion object { const val DRAFT = "alarm_draft"; const val MODE = "alarm_mode" }
}
