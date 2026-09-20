# ClockMods Android 桌面小组件实现规格

> 本文是面向 AI 编码代理的实施说明，不是工期估算。执行者应按本文顺序完成代码、测试和 Android 15 模拟器验收，不应只生成界面草图或不可运行的占位代码。

> 2026-09-20 需求更新：小组件不显示时区名称，也不单独选择时区；统一跟随 App 的时区设置。本文后续关于“每实例独立时区”的原始规格以此更新为准。当前实现约定与实测范围分别见 `docs/WIDGET_CONTRACT.md`、`docs/WIDGET_VALIDATION.md`。

> 2026-09-21 字体验证修正：桌面宿主不能可靠加载应用内置字体资源。小组件采用跟随主题、系统、衬线、等宽、窄体、细体六个选项；预览与测试必须覆盖受限宿主上下文。App 全屏字体库不受影响，旧小组件字体配置自动迁移。

## 1. 目标与边界

### 1.1 目标

为 ClockMods Ultimate 增加一组美观、实用、可配置、可缩放的 Android 桌面小组件，并尽量复用项目已有的时间、主题、天气、农历、节假日和世界时钟能力。

第一版必须做到：

1. 在系统小组件选择器中提供 4 个独立入口：数字时钟、模拟时钟、时钟天气、日期日历。
2. 每个入口都能在添加后重新配置。
3. 每个小组件实例独立保存主题、时区、时间制、显示模块和背景透明度。
4. 支持 Android 12+ 的响应式尺寸；本项目只在 Android 15 模拟器上验收。
5. 时间显示由 `TextClock` 或 `AnalogClock` 实时驱动，不能靠应用每分钟唤醒并推送整张图片。
6. 日期、农历、天气等非实时内容可由应用生成并按事件或后台任务刷新。
7. 离线、无天气配置、无定位权限、无缓存时都必须有明确的降级界面，不能留下空白 Widget。
8. 风格至少提供 6 套：系统动态色、玻璃、深色仪表、纸张、霓虹、透明。
9. 多个实例不能串配置、串点击事件或互相覆盖。

### 1.2 第一版不做

以下内容不要混入第一版主实现：

- 不在 Widget 内运行 `UltimateClockView`、`ClockRenderer` 或其他自定义 View。
- 不逐帧重现全屏时钟的 Canvas 动画、高斯模糊或平滑扫秒。
- 不做桌面内任意坐标拖拽；内容排列由配置页中的模块选择和预定义模板决定。
- 不做番茄钟、倒计时、闹钟控制、完整日程列表。这些放在扩展阶段。
- 不申请后台定位权限。
- 不为了 Widget 引入 Compose 或 Jetpack Glance。
- 不要求 Android 12、13、14、16 或实体机验收。

## 2. 项目现状与关键约束

当前工程的实现条件：

- 单 `app` 模块，Java 代码，Android Gradle Plugin 9.2.1。
- `minSdk 31`、`targetSdk 36`、`compileSdk 36.1`。
- 只有 `ultimate` product flavor，最终 application ID 为 `com.clockmods.ultimate`。
- 现有时钟样式通过 `ClockStyleRegistry`、`ClockThemeTokens`、`ClockRenderer` 和 `ClockState` 组织。
- 天气使用 `WeatherRepository`、`DailyForecastRepository` 和 QWeather 客户端。
- 农历与节假日已有离线数据和算法。
- 配置主要使用 `SharedPreferences`，日程等复合数据使用 JSON。
- 项目目前没有 App Widget、WorkManager、Room 或 Compose 基础设施。

### 2.1 RemoteViews 约束

桌面小组件运行在 Launcher/Widget Host 中，只能使用 `RemoteViews` 支持的控件。可使用的核心控件包括：

- `FrameLayout`
- `LinearLayout`
- `RelativeLayout`
- `GridLayout`
- `TextView`
- `TextClock`
- `AnalogClock`
- `ImageView`
- `ProgressBar`

不能使用：

- `UltimateClockView`
- `WeatherIconView`
- `MaterialCardView`
- 任意自定义 View 或上述控件的自定义子类
- 现有全屏布局中的复杂 Fragment/View 树

因此，现有代码的复用边界必须明确：

| 能直接复用 | 不能直接复用 |
| --- | --- |
| `ClockThemeTokens` 的颜色语义 | `ClockRenderer.render(Canvas, ...)` 作为实时 Widget |
| 时间制、时区、语言偏好 | `UltimateClockView` |
| 农历、节气、节假日计算 | `WeatherIconView` |
| `WeatherRepository` 缓存模型 | 高斯玻璃实时采样 |
| `WorldClockRepository` 数据 | 每帧动画和扫秒 |
| QWeather 图标资产和 SVG 路径解析 | 设置页里的 Material 自定义 View |

参考：

- [RemoteViews 支持范围](https://developer.android.com/reference/android/widget/RemoteViews)
- [响应式 Widget 布局](https://developer.android.com/develop/ui/views/appwidgets/layouts)
- [Widget 更新策略](https://developer.android.com/develop/ui/views/appwidgets/advanced)

## 3. 第一版产品定义

### 3.1 Picker 中的 4 个入口

#### A. 数字时钟

- Provider：`DigitalClockWidgetProvider`
- 默认尺寸：4×1
- 最小尺寸：2×1
- 支持尺寸档：compact、wide、tall
- 内容：时间、日期、星期；宽/高尺寸可增加农历和时区。
- 时间必须使用 `TextClock`。
- 默认不显示秒；配置页可选择显示秒，并显示耗电提示。

#### B. 模拟时钟

- Provider：`AnalogClockWidgetProvider`
- 默认尺寸：2×2
- 最小尺寸：1×1
- 支持尺寸档：compact、medium、large
- 内容：表盘；medium 以上可增加日期或城市名。
- 时间必须使用 `AnalogClock`。
- 表盘、时针、分针通过资源 drawable 定义。
- 第一版不启用秒针。

#### C. 时钟天气

- Provider：`WeatherClockWidgetProvider`
- 默认尺寸：4×2
- 最小尺寸：2×2
- 支持尺寸档：compact、wide、large
- 内容：时间、日期、天气图标、温度、天气描述、城市。
- compact 只显示时间、图标、温度。
- 无缓存时显示“打开应用完成天气设置”，并让该区域可点击。

#### D. 日期日历

- Provider：`CalendarWidgetProvider`
- 默认尺寸：2×2
- 最小尺寸：2×2
- 支持尺寸档：compact、wide、large
- 内容：日号、月份、星期、农历、节气/节假日。
- large 尺寸可展示当月简化月历；如果完整月历在第一轮实现中过于复杂，必须先完成“大日期卡”而不是留下空实现。

### 3.2 主题

定义以下稳定主题 ID：

```text
system.dynamic
glass.light
instrument.dark
paper.warm
neon.night
transparent.clean
```

主题至少包含：

```text
id
displayNameRes
backgroundDrawableRes
primaryTextColor
secondaryTextColor
accentColor
iconColor
fontLayoutVariant
defaultBackgroundAlpha
supportsAnalog
supportsDigital
supportsWeather
supportsCalendar
```

主题实现要求：

- `system.dynamic` 使用 Android 12+ 动态色资源/主题属性。
- `transparent.clean` 的背景透明度默认为 0，但仍保留足够的文字阴影或高对比颜色。
- `glass.light` 只能做半透明填充、描边和高光，不做实时背景模糊。
- `neon.night` 不使用会产生明显锯齿或超大 Bitmap 的发光效果。
- 所有背景使用系统 Widget 圆角：`@android:dimen/system_app_widget_background_radius`。

### 3.3 布局尺寸档

不要直接依赖“几列几行”，因为不同 Launcher 的网格实际 dp 不同。代码以 dp 尺寸分类：

```text
COMPACT: width < 180dp 或 height < 100dp
SMALL:   width < 260dp 且 height < 180dp
WIDE:    width >= 260dp 且 height < 180dp
TALL:    width < 260dp 且 height >= 180dp
LARGE:   width >= 260dp 且 height >= 180dp
```

实现 `WidgetSizeClassResolver.resolve(widthDp, heightDp)`，并为边界值写单元测试。实际 `appwidget-provider` XML 同时设置：

- `targetCellWidth`
- `targetCellHeight`
- `minResizeWidth`
- `minResizeHeight`
- `maxResizeWidth`
- `maxResizeHeight`
- `resizeMode="horizontal|vertical"`
- `widgetCategory="home_screen"`
- `widgetFeatures="reconfigurable|configuration_optional"`
- `updatePeriodMillis="0"`

使用 API 31 的 `RemoteViews(Map<SizeF, RemoteViews>)` 提供响应式映射；仍需实现 `onAppWidgetOptionsChanged()`，在 Launcher 没有正确使用映射时主动刷新。

## 4. 目标代码结构

新增目录：

```text
app/src/ultimate/java/com/clockmods/widget/
├─ model/
│  ├─ WidgetConfig.java
│  ├─ WidgetKind.java
│  ├─ WidgetModule.java
│  ├─ WidgetSizeClass.java
│  └─ WidgetThemeSpec.java
├─ store/
│  └─ WidgetConfigStore.java
├─ provider/
│  ├─ BaseClockModsWidgetProvider.java
│  ├─ DigitalClockWidgetProvider.java
│  ├─ AnalogClockWidgetProvider.java
│  ├─ WeatherClockWidgetProvider.java
│  └─ CalendarWidgetProvider.java
├─ render/
│  ├─ WidgetRemoteViewsFactory.java
│  ├─ WidgetSizeClassResolver.java
│  ├─ WidgetThemeRegistry.java
│  ├─ WidgetTextFormatter.java
│  ├─ WidgetWeatherIconFactory.java
│  └─ WidgetPendingIntentFactory.java
├─ config/
│  ├─ WidgetConfigActivity.java
│  └─ WidgetPreviewView.java
└─ update/
   ├─ WidgetUpdateCoordinator.java
   ├─ WidgetRefreshWorker.java
   ├─ WidgetMidnightScheduler.java
   └─ WidgetDataChangedReceiver.java
```

新增资源：

```text
app/src/ultimate/res/xml/
├─ widget_digital_clock_info.xml
├─ widget_analog_clock_info.xml
├─ widget_weather_clock_info.xml
└─ widget_calendar_info.xml

app/src/ultimate/res/layout/
├─ widget_digital_compact.xml
├─ widget_digital_wide.xml
├─ widget_digital_tall.xml
├─ widget_analog_compact.xml
├─ widget_analog_medium.xml
├─ widget_analog_large.xml
├─ widget_weather_compact.xml
├─ widget_weather_wide.xml
├─ widget_weather_large.xml
├─ widget_calendar_compact.xml
├─ widget_calendar_wide.xml
├─ widget_calendar_large.xml
└─ activity_widget_config.xml

app/src/ultimate/res/drawable/
├─ widget_bg_dynamic.xml
├─ widget_bg_glass.xml
├─ widget_bg_instrument.xml
├─ widget_bg_paper.xml
├─ widget_bg_neon.xml
├─ widget_bg_transparent.xml
├─ widget_analog_*_dial.xml
├─ widget_analog_*_hour.xml
└─ widget_analog_*_minute.xml
```

所有 Widget XML 只能使用 RemoteViews 支持的系统控件。不要在 layout 中使用 Material Components。

## 5. 数据模型与持久化

### 5.1 WidgetConfig

`WidgetConfig` 是不可变对象，使用 Builder 创建。至少包含：

```java
int schemaVersion;
int appWidgetId;
WidgetKind kind;
String themeId;
String timeZoneId;
boolean useSystemTimeZone;
boolean use24Hour;
boolean showSeconds;
boolean showDate;
boolean showWeekday;
boolean showLunar;
boolean showWeatherDescription;
boolean showLocation;
int backgroundAlpha;       // 0..255
float textScale;           // 限制在 0.85f..1.20f
String tapAction;          // open_clock/open_calendar/open_weather/open_config
long updatedAt;
```

约束：

- 构造时修正非法值，不让 renderer 再做防御性判断。
- `themeId` 不存在时回退 `system.dynamic`。
- `timeZoneId` 无效时回退系统时区。
- `showSeconds` 只对数字时钟和时钟天气生效。
- 模拟时钟强制 `showSeconds=false`。
- `WidgetKind` 与 Provider 不一致时，以 Provider 类型为准并写回修正后的配置。

### 5.2 WidgetConfigStore

继续使用项目现有风格：`SharedPreferences + JSON`。

```text
preferences name: clockmods_widgets
key: widget_<appWidgetId>
value: JSON
```

公共方法：

```java
WidgetConfig getOrDefault(int appWidgetId, WidgetKind providerKind)
void save(WidgetConfig config)
void delete(int appWidgetId)
List<WidgetConfig> getAll()
List<Integer> getIdsUsingWeather()
boolean contains(int appWidgetId)
```

JSON 必须写入 `schemaVersion`。第一版设为 `1`，并预留：

```java
private JSONObject migrate(JSONObject source, int fromVersion)
```

即使当前没有旧数据，也要有迁移入口和损坏 JSON 回退测试。

## 6. RemoteViews 渲染实现

### 6.1 WidgetRemoteViewsFactory

这是唯一负责构造最终 `RemoteViews` 的类。Provider 和 Worker 不直接操作具体 view ID。

公共入口：

```java
RemoteViews createResponsive(Context context, int appWidgetId, WidgetKind kind)
RemoteViews createForSize(Context context, int appWidgetId, WidgetKind kind,
        WidgetSizeClass sizeClass)
```

`createResponsive()` 流程：

1. 从 `WidgetConfigStore` 读取实例配置。
2. 从 `WidgetThemeRegistry` 解析主题。
3. 读取当前日期、农历、节气、节假日和天气缓存。
4. 分别构造需要的尺寸档 `RemoteViews`。
5. 创建 `Map<SizeF, RemoteViews>`，返回 `new RemoteViews(map)`。
6. 所有尺寸档使用相同实例配置，但可以隐藏次要模块。

渲染规则：

- 不把时间绘制成 Bitmap。
- 数字时间使用 `TextClock`，通过 `setCharSequence()` 设置 `setFormat12Hour` 和 `setFormat24Hour`。
- 通过 `setString(timeViewId, "setTimeZone", zoneId)` 设置固定时区；跟随系统时区时传 `null` 或不覆盖。
- 日期、农历、天气使用普通 `TextView`，由应用格式化。
- 主题颜色使用 `RemoteViews.setTextColor()`、`setColorStateList()` 或资源变体设置。
- 背景使用铺满根布局的 `ImageView`，通过 `setImageViewResource()` 切换 drawable，通过 `setInt(id, "setImageAlpha", alpha)` 调节透明度。
- 每个可能隐藏的区域必须设置 `VISIBLE/GONE`，不能依赖上一次 RemoteViews 状态。
- 所有点击区域每次完整构建都重新绑定 `PendingIntent`。

### 6.2 字体处理

`RemoteViews` 不能像普通 View 一样任意注入 `Typeface`。不要尝试反射设置资产字体。

第一版使用三类预定义字体布局：

```text
SANS       -> 系统 sans-serif
SERIF      -> 系统 serif
MONOSPACE  -> 系统 monospace
```

`WidgetThemeSpec.fontLayoutVariant` 决定选择哪个布局资源。如果为了主题字体新增 `res/font`，必须：

1. 确认许可证允许重新打包。
2. 把字体复制到 `res/font`，不能引用 `assets/fonts`。
3. 在布局 XML 中静态引用。
4. 不为每种颜色复制整套布局，只按字体结构区分。

### 6.3 天气图标

现有 `WeatherIconView` 是自定义 View，不能放进 RemoteViews。新增一个 Bitmap 生成器：

```java
Bitmap WidgetWeatherIconFactory.render(
        Context context,
        String qWeatherCode,
        boolean fill,
        int color,
        int sizePx)
```

实现要求：

- 复用现有 `qweather-icons` SVG 资产和 `SvgPath` 解析能力。
- 生成小尺寸 ARGB Bitmap，建议 48dp 对应像素，不生成 Widget 全尺寸图。
- 使用 `LruCache`，key 至少包含 `code/fill/color/sizePx`。
- 无效天气代码返回通用天气图标，而不是 `null`。
- 传给 `RemoteViews.setImageViewBitmap()` 前保证尺寸受控，避免 Binder 负载过大。

如果复用包私有 `WeatherIcon` 不方便，可以把它安全地提升为公共只读 API，或在 `com.clockmods.ui` 包中新增公共 Bitmap 工厂；不要复制一套 SVG parser。

### 6.4 PendingIntent

`WidgetPendingIntentFactory` 统一生成点击行为。request code 必须包含 `appWidgetId` 和 action，避免多个实例复用同一个 PendingIntent：

```java
int requestCode = 31 * appWidgetId + action.ordinal();
```

所有 PendingIntent 使用：

```text
FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE
```

第一版跳转规则：

- 点击时间：打开 `UltimateMainActivity`。
- 点击日期/农历：打开应用主界面；若实现稳定的页面路由，再跳到日历页。
- 点击天气：配置不完整时打开天气设置；否则先执行手动刷新广播。
- 点击设置图标：打开 `WidgetConfigActivity` 并带上 `EXTRA_APPWIDGET_ID`。

当前 `ProMainActivity` 没有稳定的外部页面 deep link 协议，因此不要伪造不存在的 extra。若需要直达日历，先为主 Activity 增加一个公开、测试覆盖的页面路由契约。

## 7. Provider 实现

### 7.1 BaseClockModsWidgetProvider

抽取公共行为：

```java
protected abstract WidgetKind getWidgetKind();

onUpdate(...)
onAppWidgetOptionsChanged(...)
onDeleted(...)
onEnabled(...)
onDisabled(...)
onReceive(...)
```

行为：

- `onUpdate`：对每个 ID 调用 `WidgetUpdateCoordinator.updateOne()`。
- `onAppWidgetOptionsChanged`：立即完整刷新目标 ID。
- `onDeleted`：删除对应配置；如果没有任何天气 Widget，取消不再需要的天气刷新任务。
- `onEnabled`：确保后台刷新和跨日更新已调度。
- `onDisabled`：只有所有 4 类 Widget 都不存在时才取消全局任务。
- `onReceive`：只处理明确列出的系统事件和应用内部 action。

需要处理的系统变化：

```text
ACTION_DATE_CHANGED
ACTION_TIME_CHANGED
ACTION_TIMEZONE_CHANGED
ACTION_LOCALE_CHANGED
ACTION_MY_PACKAGE_REPLACED
ACTION_BOOT_COMPLETED
```

接收器中不得做网络请求、SVG 大批量解析或背景图片解码。

### 7.2 Manifest

在 `app/src/ultimate/AndroidManifest.xml` 中：

1. 注册 `WidgetConfigActivity`，`exported=true`，因为 Launcher 需要启动它。
2. 注册 4 个 Provider receiver，`exported=true`，权限设为 `android.permission.BIND_APPWIDGET`。
3. 每个 receiver 添加 `APPWIDGET_UPDATE` intent filter。
4. 每个 receiver 通过 `meta-data` 指向对应 `res/xml/*_info.xml`。
5. 注册内部数据变化 receiver 时使用显式包内广播；不对外暴露修改 Widget 的任意接口。

配置 Activity 完成时必须：

```java
setResult(RESULT_CANCELED); // onCreate 最先设置
// 用户确认后保存配置并刷新
setResult(RESULT_OK, new Intent().putExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
finish();
```

如果用户直接退出而没有保存，系统应删除本次未完成添加的 Widget。

## 8. 配置 Activity

### 8.1 启动模式

同一个 `WidgetConfigActivity` 服务全部 Provider。通过：

```java
int appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID);
```

取得 ID，再使用 `AppWidgetManager.getAppWidgetInfo(appWidgetId).provider` 判断 Provider 类型。不要依赖自定义 extra，因为 Launcher 初次添加时不会传它。

### 8.2 页面结构

配置页至少包含：

1. 顶部预览。
2. 主题横向选择。
3. 内容开关。
4. 时间制和时区。
5. 背景透明度。
6. 文字比例。
7. 点击行为。
8. “恢复默认”和“完成”按钮。

不同 Provider 隐藏不适用的选项。例如模拟时钟不显示“秒数”，日期日历不显示“12/24 小时”。

### 8.3 预览

配置页预览运行在应用进程，可以使用普通 View，但必须与 RemoteViews 共享同一个：

- `WidgetConfig`
- `WidgetThemeSpec`
- 文本格式化器
- 尺寸分类器

预览不能直接拿现有全屏 `ClockRenderer` 代替，因为它会造成预览与桌面实际结果明显不一致。优先实现一个只使用普通 `TextView/ImageView/TextClock/AnalogClock` 的镜像预览布局。

每次设置变化：

1. 更新内存中的 draft config。
2. 刷新预览。
3. 不立即写 SharedPreferences。
4. 用户点“完成”才保存并更新桌面 Widget。

重新配置已有 Widget 时，取消返回不能覆盖原配置。

## 9. 日期、农历和日历内容

新增 `WidgetTextFormatter`，避免把格式化逻辑散落在 Provider 中。

公共方法建议：

```java
String formatGregorianDate(long now, TimeZone zone, Locale locale)
String formatWeekday(long now, TimeZone zone, Locale locale)
String formatLunar(long now, TimeZone zone, Locale locale)
String formatHolidayAndSolarTerm(long now, TimeZone zone, Locale locale)
String formatWeatherSummary(WeatherDisplayData data, boolean includeLocation)
```

要求：

- 时间必须以 Widget 配置的时区计算，而不是无条件使用系统时区。
- 农历和节假日复用现有实现，不复制算法。
- 空节气/节假日不显示多余分隔符。
- 中文、英文、繁体中文使用现有 `values`、`values-en`、`values-zh-rTW`。
- 文字过长时使用合理的 `maxLines` 和 `ellipsize=end`。

大尺寸月历如果实现：

- 只显示 6×7 日期网格和星期标题。
- 当天使用 accent 背景。
- 非本月日期降低透明度。
- 每个日期不绑定独立点击，整个月历点击进入应用日历。
- 不使用 `GridView + RemoteViewsService`；42 个固定日期格可以使用静态布局和一次 RemoteViews 填充，减少复杂度。

## 10. 天气缓存与后台刷新

### 10.1 不直接复用 WeatherController 生命周期

当前 `WeatherController` 使用主线程 `Handler`、动态位置监听和进程内定时，适合前台页面，不适合 Widget Worker。应抽取或新增一个无 UI 生命周期的天气刷新用例。

建议接口：

```java
public final class WeatherRefreshUseCase {
    public RefreshResult refreshForWidget(boolean force) throws Exception;
}
```

后台刷新策略：

1. 手动城市：直接使用保存的 location ID 调 QWeather。
2. 自动定位：只尝试有权限的最近位置，不在后台启动持续定位监听。
3. 没有可用位置：保留旧缓存，返回可重试/无需重试状态。
4. QWeather 未配置：不重试，显示配置提示。
5. 请求成功：写入现有 `WeatherRepository`，然后刷新所有使用天气的 Widget。
6. 请求失败：保留缓存；只有完全无缓存时才显示错误状态。

### 10.2 WorkManager

在 `app/build.gradle` 添加 AndroidX WorkManager Java runtime 依赖。实现时应从 AndroidX 官方 release notes 选择与当前 AGP/compileSdk 兼容的稳定版本并固定版本号，不使用 `+` 动态版本。

`WidgetRefreshWorker`：

- 唯一周期任务名：`clockmods_widget_weather_refresh`。
- 只在至少存在一个天气 Widget 时启用。
- 网络约束：`NetworkType.CONNECTED`。
- 建议周期：30 分钟。
- 使用 `ExistingPeriodicWorkPolicy.UPDATE` 或等效策略，避免重复任务。
- 手动刷新使用独立 one-time work，并做唯一任务去重。
- `doWork()` 成功保存天气后调用 `WidgetUpdateCoordinator.updateWeatherWidgets()`。

不要把 `updatePeriodMillis` 和 WorkManager 同时都设为 30 分钟，否则会重复唤醒。

### 10.3 跨日刷新

日期/农历必须在跨日后更新。可组合使用：

- `ACTION_DATE_CHANGED`
- `ACTION_TIMEZONE_CHANGED`
- 一次性 WorkManager/Alarm 在下一个本地午夜后执行

`WidgetMidnightScheduler` 每次执行后重新计算下一个午夜，不能简单固定 24 小时，否则夏令时和时区变化会漂移。

## 11. 动态色与可读性

### 11.1 动态色

`system.dynamic` 通过 Widget 专用 theme 获取系统动态色。不要依赖配置 Activity 当前 Activity theme 的解析结果，因为 RemoteViews 在 Launcher 中显示。

应定义 Widget 专用资源主题，例如：

```xml
<style name="Theme.ClockMods.Widget.Dynamic"
    parent="@android:style/Theme.DeviceDefault.DayNight">
    <!-- 使用系统动态颜色属性 -->
</style>
```

根布局或背景 drawable 使用系统颜色属性。若某 Launcher 不能正确解析动态属性，回退到明确的浅/深色资源。

### 11.2 可读性规则

- 主时间和背景对比度目标至少 4.5:1。
- 次要文字目标至少 3:1；达不到时提高不透明度。
- 透明主题必须同时提供浅色和深色文字选择，或者自动选择带阴影的高对比预设。
- 所有可点击图标的可点击区域至少 48dp，即使图标本身只有 20～24dp。
- 所有 ImageView 设置 content description；纯装饰背景明确标记为非重要无障碍内容。
- 不把关键信息只用颜色表达。

## 12. Picker 预览

每个 Provider 必须提供：

- `previewLayout`：静态 XML 回退预览。
- `description`：说明内容和推荐尺寸。
- 清晰的 label，不要四个都叫“ClockMods”。

建议名称：

```text
ClockMods 数字时钟
ClockMods 模拟时钟
ClockMods 时钟天气
ClockMods 日期日历
```

项目 compileSdk 已高于 35，可在基础功能稳定后增加 Android 15 generated widget preview。该功能是增强项，不得阻塞第一版可添加、可配置、可刷新。

## 13. 具体实施顺序

AI 编码代理必须按以下批次执行，每个批次完成后先运行相关测试，再继续下一批。

### 批次 1：基础模型和存储

1. 创建 `WidgetKind`、`WidgetSizeClass`、`WidgetConfig`。
2. 创建 `WidgetConfigStore` 和 schema v1 JSON。
3. 创建 `WidgetSizeClassResolver`。
4. 创建单元测试：
   - 默认值
   - JSON round trip
   - 损坏 JSON
   - 非法时区/主题/透明度
   - 尺寸边界
5. 运行 `testUltimateDebugUnitTest`。

完成标准：纯模型层测试全部通过，没有 Manifest 或 UI 依赖。

### 批次 2：数字时钟最小闭环

1. 新增数字时钟 provider info XML。
2. 新增 compact/wide/tall 布局。
3. 实现主题注册表的 6 个主题基本颜色。
4. 实现 `WidgetRemoteViewsFactory` 的数字时钟分支。
5. 实现 `DigitalClockWidgetProvider`。
6. Manifest 注册。
7. 先使用默认配置，不等待完整配置页。
8. 构建并在模拟器手动添加。

完成标准：Widget 可添加、时间实时变化、缩放能切换内容，点击可打开应用。

### 批次 3：配置页和多实例

1. 实现共享 `WidgetConfigActivity`。
2. 实现 Provider 类型识别。
3. 实现草稿配置和取消不保存。
4. 实现主题、时区、时间制、秒、日期、农历、透明度和文字比例。
5. 添加重新配置入口。
6. 检查 `PendingIntent` 唯一性。
7. 在桌面同时添加至少两个配置不同的数字时钟。

完成标准：两个实例互不影响，重启 Launcher/应用后配置仍存在。

### 批次 4：模拟时钟

1. 创建各尺寸布局。
2. 创建至少 4 套可辨认的表盘/指针组合；不支持模拟的主题从选择器隐藏。
3. 设置固定时区。
4. 添加日期和城市辅助文字。
5. 注册 Provider 和 picker metadata。

完成标准：1×1 到较大尺寸都不裁切，时针/分针显示正确。

### 批次 5：日期、农历和日历

1. 实现 `WidgetTextFormatter`。
2. 复用现有农历、节气、节假日能力。
3. 创建日期日历布局和 Provider。
4. 实现跨日、时区、语言变化刷新。
5. 根据实现稳定性决定 large 是完整月历还是大日期信息卡；不能提交半成品月历。

完成标准：修改模拟器日期/时区后内容正确刷新，没有重启应用要求。

### 批次 6：天气

1. 实现小尺寸天气图标 Bitmap 工厂及缓存。
2. 实现天气缓存读取和所有错误/空状态。
3. 创建天气 Widget 布局和 Provider。
4. 抽取后台天气刷新用例。
5. 接入 WorkManager 周期和手动刷新。
6. 天气成功保存后只刷新使用天气的实例。

完成标准：有网络时能刷新；断网显示缓存；无缓存时显示可操作提示；没有 ANR。

### 批次 7：选择器预览、文案和清理

1. 完成中、英、繁中字符串。
2. 添加 picker label、description、preview layout。
3. 删除所有临时 TODO、假数据和调试按钮。
4. 更新 README 的 Widget 说明。
5. 核对现有 12 个时钟样式 ID 与 README 的主题数量差异，但不要借此重构无关时钟代码。

### 批次 8：全量验证

依次运行：

```powershell
.\gradlew.bat testUltimateDebugUnitTest
.\gradlew.bat lintUltimateDebug
.\gradlew.bat assembleUltimateDebug
```

任何失败都必须解决，不能通过关闭 lint、删除测试或放宽断言绕过。

## 14. 单元测试清单

新增测试建议位于：

```text
app/src/testUltimate/java/com/clockmods/widget/
```

至少包含：

```text
WidgetConfigTest
WidgetConfigStoreTest
WidgetSizeClassResolverTest
WidgetThemeRegistryTest
WidgetTextFormatterTest
WidgetPendingIntentFactoryTest
WidgetWeatherIconFactoryTest
WidgetRefreshPolicyTest
```

必须覆盖：

- 每种 Provider 的默认配置。
- 配置 JSON 保存/读取一致。
- schema 缺失、字段缺失、损坏 JSON 的回退。
- alpha、textScale、无效 timezone 的归一化。
- 所有布局尺寸边界。
- 12/24 小时格式。
- 固定时区与系统时区。
- 中文/英文/繁体日期。
- 有/无农历、节气、节假日时分隔符正确。
- 天气位置和描述缺失时的文案。
- 多实例 PendingIntent request code 不重复。
- 没有天气 Widget 时不调度天气刷新。
- QWeather 未配置时不进入无限 retry。

## 15. Android 15 模拟器验收

### 15.1 验收环境

唯一强制环境：

```text
Android 15 / API 35 模拟器
```

不要求其他 API、OEM Launcher 或实体机结果。代码仍须保持项目声明的 Android 12+ 兼容写法。

### 15.2 安装前检查

```powershell
.\gradlew.bat testUltimateDebugUnitTest
.\gradlew.bat lintUltimateDebug
.\gradlew.bat assembleUltimateDebug
adb install -r app\build\outputs\apk\ultimate\debug\app-ultimate-debug.apk
```

如果实际 APK 文件名不同，以构建目录中的真实产物为准，不要硬编码不存在的路径到脚本中。

### 15.3 手动验收步骤

#### A. Picker 与首次添加

1. 长按桌面进入小组件选择器。
2. 搜索 ClockMods。
3. 确认出现 4 个入口，名称和预览可区分。
4. 分别添加 4 种 Widget。
5. 确认配置页能完成、取消和恢复默认。

通过条件：没有崩溃、空白卡片、错误尺寸预览或重复同名入口。

#### B. 响应式布局

对每种 Widget：

1. 缩到允许的最小尺寸。
2. 拉宽到整行。
3. 拉高到至少 3 行。
4. 再缩回默认尺寸。

通过条件：

- 时间和主日期从不被裁切。
- 次要内容按尺寸隐藏/出现。
- 不发生文字重叠、负 margin、图标拉伸。
- 设置按钮仍可点击。

#### C. 多实例隔离

1. 添加两个数字时钟。
2. 一个设置 24 小时制、上海时区、纸张主题。
3. 另一个设置 12 小时制、纽约时区、霓虹主题。
4. 分别重新配置两者。

通过条件：时间、主题、时区、点击行为互不影响。

#### D. 系统变化

在模拟器设置中依次修改：

- 12/24 小时格式。
- 系统时区。
- 系统语言：简体中文与英文至少各一次。
- 浅色/深色模式。
- 壁纸或动态色主题。
- 日期跨日测试，可临时手动调到 23:59 后等待跨日。

通过条件：使用系统设置的实例正确跟随；使用固定设置的实例不被错误覆盖；日期和农历在跨日后更新。

#### E. 天气

1. 使用手动城市完成一次成功刷新。
2. 关闭网络，点击刷新。
3. 确认仍显示最后缓存，并提示更新时间或状态。
4. 清除应用数据后，在未完成天气配置时添加天气 Widget。
5. 点击错误/提示区域，确认进入可完成设置的位置。

通过条件：失败不清空缓存、不崩溃、不无限转圈、不在桌面弹权限对话框。

#### F. 生命周期

1. 强制停止应用后返回桌面。
2. 点击 Widget，确认应用能正常打开。
3. 重启模拟器。
4. 解锁后确认 Widget 恢复。
5. 删除一个实例，确认其他实例仍正常。
6. 删除全部 Widget，确认后台周期任务被取消。

#### G. 可读性

对 6 套主题分别截图检查：

- 时间与背景对比是否足够。
- 透明主题在浅色和深色壁纸下是否可读。
- 天气图标是否清晰。
- 圆角和 Launcher 留白是否协调。
- 中文、英文长文本是否省略合理。

### 15.4 最终通过标准

只有同时满足以下条件才算完成：

- 3 条 Gradle 命令全部成功。
- Android 15 模拟器可找到并添加 4 种 Widget。
- 4 种 Widget 都有实际内容，不是占位 UI。
- 所有 Widget 可缩放且布局会响应变化。
- 至少 6 套主题可选，且没有明显不可读组合。
- 多实例独立。
- 数字/模拟时间无需应用前台运行即可变化。
- 日期、农历、时区、语言变化能刷新。
- 天气成功、断网、有缓存、无缓存、未配置五种状态都可用。
- 重启模拟器后 Widget 仍存在并恢复内容。
- 删除全部 Widget 后不保留无意义周期工作。
- 没有 ANR、崩溃、持续日志异常或明显高频后台唤醒。

## 16. 后续扩展顺序

第一版通过后，再按以下顺序扩展，不要在基础架构未稳定时并行加入：

1. 世界时钟 Widget：本地 + 2～4 个城市。
2. 今日日程 Widget：复用 `ScheduleStore`，显示当天前三项。
3. 完整月历 Widget：42 日格、节假日和日程标记。
4. 下一闹钟 Widget。
5. 番茄钟/倒计时控制 Widget。
6. Android 15 generated widget preview。
7. 可导入/导出 Widget 配置预设。

每个扩展都必须继续使用 `WidgetConfigStore`、`WidgetUpdateCoordinator`、`WidgetRemoteViewsFactory` 和统一主题注册表，禁止为新 Widget 复制一套更新与持久化框架。

## 17. 对实现代理的最终要求

- 修改前先阅读相关现有类，不根据类名猜测接口。
- 建议在项目中补充易于后期扩展、第三方接入的 SDK/规范/契约。
- 保留用户现有改动，不清理无关文件。
- 所有新增稳定 ID 一经写入持久化后不得随意改名。
- 所有网络和磁盘工作离开 BroadcastReceiver 主线程。
- 不在 Provider 中持有 Activity、View 或 Bitmap 的长生命周期引用。
- 不把整个 Widget 渲染为每分钟更新的大 Bitmap。
- 不提交仅能编译、无法在 Launcher 添加的“框架代码”。
- 每完成一个批次都运行测试，并在最终交付中报告真实执行结果、APK 路径和 Android 15 验收结果。
