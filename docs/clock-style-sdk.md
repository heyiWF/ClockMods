# ClockMods Clock Style SDK

Clock Style SDK 是 ClockMods Ultimate 当前使用的进程内时钟样式扩展契约。开发者提供稳定的样式元数据、主题 token 和一个 Canvas renderer；宿主负责时间源、生命周期、刷新频率、时区、无障碍、天气与背景数据。

当前 SDK 位于 `app/src/main/java/com/clockmods/sdk/clock/`。目录沿用 Android 的 `java` source set 命名，但契约和内置实现均为 Kotlin，并随项目以 JVM 17 编译。Ultimate 应用的最低版本是 API 31；每个样式仍必须通过 metadata 的 `minApi` 准确声明 renderer 自身的最低 API。SDK 目前不是可动态下载或独立安装的插件系统：第三方样式需要作为应用源码或依赖一起编译，然后由应用代码注册。

## 核心模型

一个标准 SDK 样式由三个互相独立的部分组成：

- `ClockStyleMetadata`：稳定 ID、显示名称、说明、种类、能力、样式版本和最低 API。
- `ClockThemeTokens`：颜色、字体族和描边比例等可复用视觉 token。
- `ClockRenderer`：决定布局、几何、刻度、表针、数字和信息层级的 Canvas renderer。

`ClockStyle` 只负责把这三部分组合起来：

```kotlin
interface ClockStyle {
    fun getMetadata(): ClockStyleMetadata
    fun getThemeTokens(): ClockThemeTokens
    fun getRenderer(): ClockRenderer
}
```

时间和外部信息由不可变的 `ClockState` 传入。renderer 不应自行读取系统时间、SharedPreferences、天气仓库或网络时间服务。

## 最小样式示例

下面的样式只演示完整接入面。正式样式应缓存 `Paint` 等绘制对象，并针对横竖屏分别检查布局。

```kotlin
package com.example.clockstyles

import android.graphics.Paint
import android.graphics.Typeface
import com.clockmods.sdk.clock.ClockRenderContext
import com.clockmods.sdk.clock.ClockRenderer
import com.clockmods.sdk.clock.ClockState
import com.clockmods.sdk.clock.ClockStyle
import com.clockmods.sdk.clock.ClockStyleCapabilities
import com.clockmods.sdk.clock.ClockStyleCapabilities.Capability
import com.clockmods.sdk.clock.ClockStyleMetadata
import com.clockmods.sdk.clock.ClockThemeTokens
import com.clockmods.ui.ClockTimeText
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class MeridianStyle : ClockStyle {
    private val metadata = ClockStyleMetadata(
        "example.meridian",
        "Meridian",
        "A restrained digital clock with a radial seconds marker.",
        ClockStyleMetadata.Kind.HYBRID,
        ClockStyleCapabilities.of(
            Capability.SECONDS,
            Capability.DATE,
            Capability.TIME_ZONE,
            Capability.TWENTY_FOUR_HOUR,
        ),
        1,
        31,
    )

    private val tokens = ClockThemeTokens.builder()
        .background(0xFF111418.toInt(), 0xFF050607.toInt())
        .surfaceColor(0xFF20252B.toInt())
        .primaryTextColor(0xFFF4F6F8.toInt())
        .secondaryTextColor(0xFF9AA4AD.toInt())
        .accentColor(0xFFFFC857.toInt())
        .lineColor(0xFF56616A.toInt())
        .fonts("sans-serif", "sans-serif")
        .strokeScale(1f)
        .build()

    private val renderer = MeridianRenderer()

    override fun getMetadata() = metadata
    override fun getThemeTokens() = tokens
    override fun getRenderer(): ClockRenderer = renderer

    private class MeridianRenderer : ClockRenderer {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)

        override fun render(
            canvas: android.graphics.Canvas,
            context: ClockRenderContext,
            state: ClockState,
            theme: ClockThemeTokens,
        ) {
            val saveCount = canvas.save()
            try {
                canvas.drawColor(theme.getBackgroundStartColor())

                val now = state.newCalendar()
                var hour = now.get(Calendar.HOUR_OF_DAY)
                if (!state.isUse24Hour()) hour = (hour % 12).takeUnless { it == 0 } ?: 12
                val time = String.format(Locale.US, "%02d:%02d", hour, now.get(Calendar.MINUTE))

                paint.color = theme.getPrimaryTextColor()
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = theme.getDisplayTypeface()
                    ?: Typeface.create(theme.getDisplayFontFamily(), Typeface.NORMAL)
                paint.textSize = min(context.getWidth(), context.getHeight()) * 0.20f
                ClockTimeText.draw(canvas, time, context.getCenterX(), context.getCenterY(), paint)

                paint.color = theme.getSecondaryTextColor()
                paint.textSize = 14f * context.getScaledDensity()
                canvas.drawText(
                    state.getDateText(),
                    context.getCenterX(),
                    context.getCenterY() + 32f * context.getDensity(),
                    paint,
                )
            } finally {
                canvas.restoreToCount(saveCount)
            }
        }
    }
}
```

## 注册与发现

`ClockStyleRegistry` 保持注册顺序并拒绝重复 ID。第一个注册的样式自动成为 fallback，也可以在目标样式注册后调用 `setFallback(id)` 显式修改。

Ultimate 的内置 registry 每次通过 `UltimateClockStyles.createRegistry()` 新建。当前实时界面由 Compose `Canvas` 承载：宿主解析样式、组装不可变 `ClockState` 与 `ClockRenderContext`，再把 Compose canvas 的原生 `android.graphics.Canvas` 交给 renderer。自定义宿主可使用同一条路径：

```kotlin
val registry = UltimateClockStyles.createRegistry()
    .register(MeridianStyle())

@Composable
fun ClockStyleFrame(
    styleId: String,
    state: ClockState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val style = remember(registry, styleId) {
        registry.resolveForApi(styleId, Build.VERSION.SDK_INT)
    }

    Canvas(modifier) {
        val context = ClockRenderContext(
            0f,
            0f,
            size.width,
            size.height,
            density.density,
            density.fontScale * density.density,
            state.getTimeMillis(),
        )
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val saveCount = canvas.save()
            try {
                style.getRenderer().render(
                    canvas,
                    context,
                    state,
                    style.getThemeTokens(),
                )
            } finally {
                canvas.restoreToCount(saveCount)
            }
        }
    }
}
```

实际 Ultimate 宿主还会在调用 renderer 前应用用户 palette、字体、背景与秒针模式；上例只展示 SDK 的最小 Compose 接入边界。registry 应在组合外创建或用 `remember` 保持稳定，不能在每次重组时重新注册。

自定义设置页或其他样式选择器应从它实际使用的 `registry.getStyles()` 枚举样式，并读取 metadata，不应维护另一份样式 ID 列表。`getStyles()` 返回按注册顺序排列的不可修改快照。Ultimate 自带图库目前有意只展示 `UltimateClockStyles.builtIns()` 的固定首方目录；若要让第三方样式出现在该图库中，还需要把图库的数据源改为包含扩展样式的同一个 registry。

选择和回退规则如下：

- `find(id)` 只查找精确 ID，不做回退。
- `resolve(id)` 在 ID 不存在时返回 fallback。
- `resolveForApi(id, apiLevel)` 还会检查 metadata 的 `minApi`；请求样式和 fallback 都不兼容时，按注册顺序选择第一个兼容样式。
- registry 为空，或没有任何样式支持目标 API 时，解析会抛出 `IllegalStateException`。

## Metadata 与稳定 ID

`ClockStyleMetadata` 包含：

| 字段 | 含义 |
| --- | --- |
| `id` | 持久化和注册使用的稳定 ID |
| `name` | 默认显示名称 |
| `description` | 默认样式说明 |
| `kind` | `ANALOG`、`DIGITAL` 或 `HYBRID` |
| `capabilities` | 样式实际支持的状态和动画能力 |
| `version` | 样式自身的正整数版本 |
| `minApi` | renderer 可运行的最低 Android API |

ID 必须是小写、带命名空间的标识符。当前校验允许以字母开头的段，并用点、下划线或连字符分隔；推荐使用点号，例如 `example.meridian`。发布后不要因为显示名称或视觉改版而更换 ID，否则用户已保存的选择会失效。需要表达不兼容的样式演进时递增 `version`，但目前宿主不会自动迁移样式私有数据。

当前内置 registry 使用这些 ID：

- `pro.classic`
- `glass.atelier`
- `noir.instrument`
- `paper.station`
- `orbit.neon`
- `digital.grid`
- `typographic.poster`
- `ultimate.dual_blocks`
- `ultimate.orbit`
- `ultimate.bubbles`
- `ultimate.blend`
- `ultimate.ribbon`

这 12 个内置样式都通过相同的 `ClockStyle` / `ClockRenderer` 契约在 Compose `Canvas` 中实时绘制；图库预览和实时界面不再使用单独的 View renderer 分支。

## Capabilities

`ClockStyleCapabilities` 是不可变能力集合。可声明：

- `SECONDS`：样式能显示秒。
- `SMOOTH_SECONDS`：样式支持平滑秒针或连续秒进度。
- `DATE`：使用预格式化日期文本。
- `TIME_ZONE`：使用时区文本或时区信息。
- `WEATHER`：使用天气摘要。
- `STATUS`：使用状态摘要。
- `TWENTY_FOUR_HOUR`：尊重 12/24 小时设置。
- `WORLD_CLOCK`：使用 `ClockState` 中的世界时钟列表。

Ultimate 宿主会根据 `SECONDS` 和 `SMOOTH_SECONDS` 解析有效秒针模式。其他能力同时用于发现和描述；声明能力不会自动绘制相应内容，renderer 仍需读取 `ClockState` 并设计对应布局。只声明真正实现的能力。

## Theme Tokens

`ClockThemeTokens` 通过 builder 创建，提供：

- 背景起止色、surface、主文字、次文字、accent 和线条 ARGB 色值。
- display 与 supporting Android 字体族名称，以及宿主解析后的可选 `Typeface`。
- 正数 `strokeScale`。
- 高斯模糊开关、强度、亮度和卡片阴影策略。

token 用于统一预览和实时渲染的视觉参数，但不是完整主题。布局结构、刻度体系、表针形状、数字构成和信息层级都属于 renderer。仅替换颜色不应注册成新的完整样式。

## 每帧输入

`ClockRenderContext` 提供当前帧的像素边界、中心点、density、scaled density、帧时间、可选 `ClockBackground`、底部遮罩预留高度、世界时钟滚动量、世界时钟条是否由宿主承载，以及宿主状态胶囊所占的 `ClockOverlayBounds`。

`ClockState` 提供：

- 当前毫秒时间、时区与 locale；`newCalendar()` 会生成匹配它们的 `Calendar`。
- 12/24 小时设置、是否显示秒、`OFF/TICK/SWEEP` 秒针模式。
- 已由宿主格式化的日期、时区、天气和状态文本。
- 不可变的世界时钟列表。
- 时间、日期和辅助文字的用户缩放比例。

renderer 应以 `ClockState.getTimeMillis()` 或 `newCalendar()` 为唯一时间依据。这样网络校时、系统时间和预览固定时间都可使用同一个 renderer。

### 世界时钟

声明 `WORLD_CLOCK` 的 renderer 从 `ClockState.getWorldClocks()` 读取不可变的城市、国家、时区和旗帜数据。`ClockRenderContext.getWorldClockScroll()` 是宿主提供的非负水平滚动偏移；renderer 应把它限制在实际内容宽度内。`isWorldClockStripHosted()` 为 `true` 时，表示宿主已在画布外绘制世界时钟条，renderer 不应重复绘制；为 `false` 时，renderer 可在自己的画布中完成卡片布局。省略这些构造参数时，默认偏移为 `0f` 且由 renderer 承载。

### 用户背景

`ClockBackground` 有 `THEME`、`COLOR`、`IMAGE` 三种模式。`THEME` 表示使用样式自己的设计背景；`COLOR` 和 `IMAGE` 表示宿主提供了用户背景。bitmap 生命周期归宿主所有，renderer 不得回收或长期持有它。`isDimmed()` 为真时，renderer 应在背景层应用暗化，但不要暗化前景时钟内容。

调用旧的、不带背景参数的 `ClockRenderContext` 构造函数时，`getBackground()` 为 `null`，renderer 应把它视为使用自身主题背景。

### 底部遮罩预留

`getBottomInset()` 返回宿主在画面底部叠加内容（如天气服务来源标注）所占的像素高度。画布边界不会因此缩小，背景层仍应铺满整个画面；只有会被遮住的前景元素——时区、日期、天气这类底部元数据——需要按这个值上移。不带 inset 参数的构造函数返回 `0f`，renderer 按无遮罩处理即可。

### 顶部状态胶囊

`getStatusOverlay()` 返回宿主自己叠加的状态胶囊所占的 `ClockOverlayBounds`，未显示时为 `null`。它是一个方框而不是一条横带：胶囊是停在一角的小控件，同一条边上的其余部分仍归样式使用。

判定规则是两轴同时相交——只有当一行文字横向够到胶囊所在的列、纵向也够到它所在的行时，这一行才需要让位。让位方式是下移到方框底部之下，而不是重新排版整块内容：

```kotlin
context.getStatusOverlay()?.let { overlay ->
    if (overlay.spansHorizontally(rowLeft, rowRight) &&
        overlay.spansVertically(baseline - ascent, baseline + descent)
    ) {
        baseline = overlay.getBottom() + gap + ascent
    }
}
```

因此「轨道」这类左边是 context 行、右边是 date 行的样式，可以让左侧那行下移，而右侧那行保持原位。行的横向范围要用该行可用的最大宽度估算，宁可保守也不要漏判。

胶囊的位置、底板与前景由宿主决定，renderer 只在 `getStatusOverlay()` 非空时避让对应矩形。未把状态控件叠加到 renderer 画布上的宿主应传 `null`；第三方样式不得假定该矩形一定存在或固定在某个角。

## 生命周期与线程

标准 SDK renderer 没有生命周期回调。Compose 宿主拥有时间源和帧调度：`ClockScreen` 在进入 composition 后更新不可变时间状态，离开 composition 时相应 coroutine 自动取消；`ClockCanvas` 每次重绘都用最新状态调用 renderer。renderer 不应创建 `Handler`、线程、定时器，也不应尝试调用 View 的 `invalidate()`。

自定义 Compose 宿主应把时间采样放在可取消的 effect 中，并按实际能力选择刷新粒度。例如需要连续秒针时可逐帧或高频更新，不显示秒时可只在分钟边界更新：

```kotlin
@Composable
fun rememberClockTimeMillis(updateIntervalMillis: Long): State<Long> = produceState(
    initialValue = System.currentTimeMillis(),
    key1 = updateIntervalMillis,
) {
    while (true) {
        value = System.currentTimeMillis()
        delay(updateIntervalMillis.coerceAtLeast(16L))
    }
}
```

Ultimate 当前每 250 毫秒统一采样网络或系统时间，再通过 Compose 状态驱动 `Canvas`。renderer 只处理单帧；当 composable 离开 composition 或 Activity 被销毁时，采样 effect 会被取消。需要在 Activity 仅暂停时停止采样的自定义宿主，应另外使用 lifecycle-aware effect。

约束：

- `render()` 在 Android UI 线程调用；不要阻塞、访问网络或执行磁盘 I/O。
- 不要保存 `Canvas`、`ClockRenderContext`、`ClockState` 或宿主 bitmap 的跨帧引用。
- 缓存可复用的 `Paint`、`Path` 和字体，但不要把当前时间或布局结果作为共享全局状态。
- 对 Canvas 的 rotate、translate、clip、layer 等修改必须成对 save/restore。Ultimate 当前会隔离 renderer 的 Canvas 状态，但 renderer 自身仍应保持这个约束，便于预览和其他宿主复用。
- 绘制含冒号的时间时用 `ClockTimeText.draw()` 代替 `Canvas.drawText()`。多数字体把 `:` 对齐到 x 高度，而数字画到数字高度，直接绘制会让冒号偏低；该工具按字形边界把冒号抬到数字的视觉中心，对任意字体和字号都成立。`ClockTimeText.colonBaselineOffset(Paint)` 供逐字符排版的 renderer 取同一偏移量。
- registry 的公开方法是同步的，但注册通常应在 Compose 宿主开始渲染前完成。一个 renderer 实例可能被实时 Canvas 和预览复用，因此不要依赖可变的帧状态。

## Android 兼容约束

SDK 与应用源码均以 Kotlin/JVM 17 编译。样式必须把实际需要的最低 Android API 写入 metadata；使用高于 `minApi` 的 Android API 时，应提高 `minApi`，或使用 `Build.VERSION.SDK_INT` 分支保护调用。当前 Ultimate 宿主最低支持 API 31，但 registry 契约仍会严格执行每个样式声明的 `minApi`。

Ultimate 会使用 `resolveForApi()` 过滤不兼容样式。不要仅依赖设置页隐藏不兼容项，因为已持久化 ID、深链或自定义宿主仍可能请求该样式。

## Pro Classic

`Pro Classic` 现在与其他 11 个内置样式完全一致：`pro.classic` 对应标准 `ClockStyleMetadata`、tokens 和完整实时 `ClockRenderer`，由 Compose `Canvas` 直接调用。它不再复用旧 `ClockView`，也不存在预览专用 renderer 或宿主路由特例。

第三方开发者应使用同一条标准路径：实现 `ClockStyle`、`ClockRenderer`、metadata 和 tokens，再注册进 `ClockStyleRegistry`。不要继承应用内部 Activity、Fragment 或 composable；SDK 的稳定边界是 registry、状态、context 与 renderer 契约。

## 接入检查表

1. 使用不会变化的小写点号 ID，并填写准确的 `kind`、`version` 和 `minApi`。
2. 只声明 renderer 确实实现的 capabilities。
3. 将状态读取和视觉 token 与 Canvas 几何分离。
4. 同时验证横屏、竖屏、小尺寸、12/24 小时、长日期、天气缺失和各秒针模式。
5. 对 `TICK`、`SWEEP`、`OFF` 分别验证秒显示；没有 `SMOOTH_SECONDS` 时正确降级。
6. 正确处理 `null/THEME`、`COLOR`、`IMAGE` 和 dimmed 背景。
7. 通过 `ClockStyleRegistry` 驱动选择器和 fallback，不复制 ID 清单。
8. 在目标最低 API 上运行，并确认 renderer 不产生跨帧对象泄漏或后台任务。
