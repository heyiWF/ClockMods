package com.clockmods.widget

import android.app.Activity
import android.app.Instrumentation
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.AnalogClock
import android.widget.FrameLayout
import android.widget.TextClock
import android.widget.TextView
import com.clockmods.R
import com.clockmods.background.ClockPreferences
import com.clockmods.weather.QWeatherConfig
import com.clockmods.weather.WeatherRefreshUseCase
import com.clockmods.widget.config.WidgetConfigActivity
import com.clockmods.widget.model.WidgetConfig
import com.clockmods.widget.model.WidgetKind
import com.clockmods.widget.model.WidgetSizeClass
import com.clockmods.widget.render.WidgetRemoteViewsFactory
import com.clockmods.widget.render.WidgetThemeRegistry
import com.clockmods.widget.render.WidgetWeatherIconFactory
import com.clockmods.widget.store.WidgetConfigStore
import com.clockmods.widget.update.WidgetRefreshWorker
import com.clockmods.widget.update.WidgetUpdateCoordinator
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

/** Device tests deliberately exercise real RemoteViews reflection and launcher binding APIs. */
class WidgetAcceptanceInstrumentation : Instrumentation() {
    private var calendarAcceptance = false
    private var calendarWeather = false
    private lateinit var app: Context
    private var checks = 0

    private fun check(value: Boolean, message: String) {
        checks++
        if (!value) throw AssertionError(message)
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        calendarAcceptance = arguments?.getString("calendar") == "true"
        calendarWeather = arguments?.getString("weather") == "true"
        start()
    }

    override fun onStart() {
        if (calendarAcceptance) {
            com.clockmods.calendar.CalendarAcceptance.run(this, calendarWeather)
            return
        }
        val report = Bundle()
        var host: AppWidgetHost? = null
        var resultCode = Activity.RESULT_CANCELED
        try {
            app = targetContext
            check(Build.VERSION.SDK_INT >= 31, "requires API 31+")
            val providers = ArrayList<AppWidgetProviderInfo>()
            for (info in AppWidgetManager.getInstance(app).installedProviders) {
                if (info.provider.packageName == app.packageName) providers += info
            }
            check(providers.size == 4, "four picker entries")
            uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            val activeHost = AppWidgetHost(app, 9917).also { it.startListening() }
            host = activeHost
            val ids = ArrayList<Int>()
            for (info in providers) {
                check(info.configure != null && info.previewLayout != 0, "picker configuration and preview")
                val id = activeHost.allocateAppWidgetId()
                ids += id
                check(
                    AppWidgetManager.getInstance(app).bindAppWidgetIdIfAllowed(id, info.provider),
                    "bind ${info.provider}",
                )
                val detectedKind = WidgetUpdateCoordinator.kindFor(app, id)
                check(detectedKind != null, "provider detection")
                val kind = detectedKind!!
                val saved = WidgetConfig.builder(id, kind)
                    .themeId("paper.warm")
                    .useSystemTimeZone(false)
                    .timeZoneId("Asia/Shanghai")
                    .build()
                WidgetConfigStore(app).save(saved)
                WidgetUpdateCoordinator.updateOne(app, id)
                for (theme in WidgetThemeRegistry.all(app)) {
                    for (size in WidgetSizeClass.values()) {
                        for (font in WidgetConfig.FONT_IDS) {
                            val config = saved.toBuilder()
                                .themeId(theme.id)
                                .fontId(font)
                                .backgroundAlpha(theme.defaultBackgroundAlpha)
                                .build()
                            val remoteViews = WidgetRemoteViewsFactory.create(app, config, size)
                            val error = arrayOfNulls<Throwable>(1)
                            runOnMainSync {
                                try {
                                    val parent = FrameLayout(app)
                                    val view = remoteViews.apply(app, parent)
                                    parent.addView(view)
                                    val minWidth = info.minResizeWidth
                                    val minHeight = info.minResizeHeight
                                    val compactWidth = minOf(minWidth, 170)
                                    val compactHeight = maxOf(minHeight, 56)
                                    val smallWidth = maxOf(minWidth, 180)
                                    val smallHeight = maxOf(minHeight, 100)
                                    val wideWidth = smallWidth + 160
                                    val largeHeight = smallHeight + 180
                                    val (width, height) = when (size) {
                                        WidgetSizeClass.COMPACT -> compactWidth to compactHeight
                                        WidgetSizeClass.SMALL -> smallWidth to smallHeight
                                        WidgetSizeClass.WIDE -> wideWidth to smallHeight
                                        else -> wideWidth to largeHeight
                                    }
                                    val density = app.resources.displayMetrics.density
                                    val pixelWidth = (width * density).toInt()
                                    val pixelHeight = (height * density).toInt()
                                    parent.measure(
                                        View.MeasureSpec.makeMeasureSpec(pixelWidth, View.MeasureSpec.EXACTLY),
                                        View.MeasureSpec.makeMeasureSpec(pixelHeight, View.MeasureSpec.EXACTLY),
                                    )
                                    parent.layout(0, 0, pixelWidth, pixelHeight)
                                    check(view.findViewById<View>(R.id.widget_settings).isClickable, "settings bound")
                                    val mainId = when (kind) {
                                        WidgetKind.CALENDAR -> R.id.widget_day
                                        WidgetKind.ANALOG -> R.id.widget_analog
                                        else -> R.id.widget_time
                                    }
                                    val main = view.findViewById<View>(mainId)
                                    val mainParent = main.parent as View
                                    check(
                                        main.top >= 0 && main.bottom <= mainParent.height,
                                        "main content fits vertically",
                                    )
                                    if (main is TextView) {
                                        check(main.layout!!.getEllipsisCount(0) == 0, "main text is not truncated")
                                    }
                                    if (kind == WidgetKind.DIGITAL || kind == WidgetKind.WEATHER) {
                                        check(view.findViewById<View>(R.id.widget_time) is TextClock, "live text clock")
                                    }
                                    if (kind == WidgetKind.ANALOG) {
                                        check(view.findViewById<View>(R.id.widget_analog) is AnalogClock, "live analog clock")
                                    }
                                    val caption: View? = view.findViewById(R.id.widget_date)
                                    if (caption != null && caption.visibility == View.VISIBLE) {
                                        check(
                                            leftInRoot(caption) >= app.resources.getDimensionPixelSize(R.dimen.widget_inset_start) - 1,
                                            "caption clears the corner fan ($font, left=${leftInRoot(caption)})",
                                        )
                                        if (size != WidgetSizeClass.COMPACT && caption is TextView) {
                                            check(caption.layout!!.getEllipsisCount(0) == 0, "caption is not truncated")
                                        }
                                    }
                                    if (size == WidgetSizeClass.LARGE) {
                                        val image = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888)
                                        parent.draw(Canvas(image))
                                        val directory = File(app.cacheDir, "widget-acceptance")
                                        directory.mkdirs()
                                        FileOutputStream(File(directory, "$kind-${theme.id}-$font.png")).use { output ->
                                            image.compress(Bitmap.CompressFormat.PNG, 100, output)
                                        }
                                    }
                                } catch (throwable: Throwable) {
                                    error[0] = throwable
                                }
                            }
                            error[0]?.let { throw AssertionError("$kind ${theme.id} $size $font", it) }
                        }
                    }
                }
                val before = WidgetConfigStore.encode(WidgetConfigStore(app).getOrDefault(id, kind))
                val configActivity = startActivitySync(
                    Intent(app, WidgetConfigActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                waitForIdleSync()
                SystemClock.sleep(600)
                waitForIdleSync()
                runOnMainSync {
                    check(
                        (configActivity as WidgetConfigActivity).toggleDateForInstrumentation(),
                        "configuration loaded",
                    )
                    configActivity.finish()
                }
                waitForIdleSync()
                check(
                    before == WidgetConfigStore.encode(WidgetConfigStore(app).getOrDefault(id, kind)),
                    "cancel leaves saved instance untouched",
                )
            }
            if (QWeatherConfig.isConfigured()) {
                val weather = WeatherRefreshUseCase(app).refreshForWidget(true)
                val expected = if (WeatherRefreshUseCase.hasLocation(app, ClockPreferences(app))) {
                    WeatherRefreshUseCase.RefreshResult.SUCCESS
                } else {
                    WeatherRefreshUseCase.RefreshResult.NO_LOCATION
                }
                check(weather == expected, "weather refresh: expected $expected, got $weather")
            }
            check(WidgetUpdateCoordinator.active(app).size >= 4, "active instance isolation")
            var weatherId = -1
            for (id in ids) {
                if (WidgetUpdateCoordinator.kindFor(app, id) == WidgetKind.WEATHER) weatherId = id
            }
            check(weatherId != -1, "weather instance present")
            check(
                WidgetWeatherIconFactory.render(app, "invalid", false, Color.WHITE, 999).width == 144,
                "bounded fallback bitmap",
            )
            for (id in ids) activeHost.deleteAppWidgetId(id)
            WidgetUpdateCoordinator.reconcile(app)
            check(WidgetUpdateCoordinator.weatherCount(app) == 0, "no remaining test weather work")
            val workManager = androidx.work.WorkManager.getInstance(app)
            for (work in workManager.getWorkInfosForUniqueWork(WidgetRefreshWorker.PERIODIC).get()) {
                check(work.state.isFinished, "weather periodic work cancelled")
            }
            report.putString(
                "stream",
                "PASS: $checks device checks; ${WidgetConfig.FONT_IDS.size} fonts x 4 sizes x 6 themes per widget on real RemoteViews, plus provider binding, preview and cancel isolation.\n",
            )
            resultCode = Activity.RESULT_OK
        } catch (throwable: Throwable) {
            val trace = StringWriter()
            throwable.printStackTrace(PrintWriter(trace))
            report.putString("stream", trace.toString())
        } finally {
            host?.let {
                it.deleteHost()
                it.stopListening()
            }
            if (::app.isInitialized) WidgetUpdateCoordinator.reconcile(app)
            uiAutomation.dropShellPermissionIdentity()
        }
        finish(resultCode, report)
    }

    private fun leftInRoot(view: View): Int {
        var left = 0
        var current: View? = view
        while (current != null) {
            left += current.left
            current = current.parent as? View
        }
        return left
    }
}
