# ClockMods Clock Style SDK

Clock Style SDK 是 ClockMods Ultimate 当前使用的进程内时钟样式扩展契约。开发者提供稳定的样式元数据、主题 token 和一个 Canvas renderer；宿主负责时间源、生命周期、刷新频率、时区、无障碍、天气与背景数据。

当前 SDK 位于 `app/src/main/java/com/clockmods/sdk/clock/`，使用 Java 8 语法并兼容 Android API 14。Ultimate 应用本身的最低版本是 API 31。SDK 目前不是可动态下载或独立安装的插件系统：第三方样式需要作为应用源码或依赖一起编译，然后由应用代码注册。

## 核心模型

一个标准 SDK 样式由三个互相独立的部分组成：

- `ClockStyleMetadata`：稳定 ID、显示名称、说明、种类、能力、样式版本和最低 API。
- `ClockThemeTokens`：颜色、字体族和描边比例等可复用视觉 token。
- `ClockRenderer`：决定布局、几何、刻度、表针、数字和信息层级的 Canvas renderer。

`ClockStyle` 只负责把这三部分组合起来：

```java
public interface ClockStyle {
    ClockStyleMetadata getMetadata();
    ClockThemeTokens getThemeTokens();
    ClockRenderer getRenderer();
}
```

时间和外部信息由不可变的 `ClockState` 传入。renderer 不应自行读取系统时间、SharedPreferences、天气仓库或网络时间服务。

## 最小样式示例

下面的样式只演示完整接入面。正式样式应缓存 `Paint` 等绘制对象，并针对横竖屏分别检查布局。

```java
package com.example.clockstyles;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.clockmods.sdk.clock.ClockRenderContext;
import com.clockmods.sdk.clock.ClockRenderer;
import com.clockmods.sdk.clock.ClockState;
import com.clockmods.sdk.clock.ClockStyle;
import com.clockmods.sdk.clock.ClockStyleCapabilities;
import com.clockmods.sdk.clock.ClockStyleMetadata;
import com.clockmods.sdk.clock.ClockThemeTokens;

import java.util.Calendar;
import java.util.Locale;

public final class MeridianStyle implements ClockStyle {
    private final ClockStyleMetadata metadata = new ClockStyleMetadata(
            "example.meridian",
            "Meridian",
            "A restrained digital clock with a radial seconds marker.",
            ClockStyleMetadata.Kind.HYBRID,
            ClockStyleCapabilities.of(
                    ClockStyleCapabilities.Capability.SECONDS,
                    ClockStyleCapabilities.Capability.DATE,
                    ClockStyleCapabilities.Capability.TIME_ZONE,
                    ClockStyleCapabilities.Capability.TWENTY_FOUR_HOUR,
                    ClockStyleCapabilities.Capability.REDUCED_MOTION),
            1,
            14);

    private final ClockThemeTokens tokens = ClockThemeTokens.builder()
            .background(0xFF111418, 0xFF050607)
            .surfaceColor(0xFF20252B)
            .primaryTextColor(0xFFF4F6F8)
            .secondaryTextColor(0xFF9AA4AD)
            .accentColor(0xFFFFC857)
            .lineColor(0xFF56616A)
            .fonts("sans-serif", "sans-serif")
            .strokeScale(1f)
            .build();

    private final ClockRenderer renderer = new MeridianRenderer();

    @Override public ClockStyleMetadata getMetadata() { return metadata; }
    @Override public ClockThemeTokens getThemeTokens() { return tokens; }
    @Override public ClockRenderer getRenderer() { return renderer; }

    private static final class MeridianRenderer implements ClockRenderer {
        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);

        @Override public void render(Canvas canvas, ClockRenderContext context,
                ClockState state, ClockThemeTokens theme) {
            int saveCount = canvas.save();
            try {
                canvas.drawColor(theme.getBackgroundStartColor());

                Calendar now = state.newCalendar();
                int hour = now.get(Calendar.HOUR_OF_DAY);
                if (!state.isUse24Hour()) {
                    hour %= 12;
                    if (hour == 0) hour = 12;
                }
                String time = String.format(Locale.US, "%02d:%02d",
                        hour, now.get(Calendar.MINUTE));

                paint.setColor(theme.getPrimaryTextColor());
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create(
                        theme.getDisplayFontFamily(), Typeface.NORMAL));
                paint.setTextSize(Math.min(context.getWidth(), context.getHeight()) * 0.20f);
                canvas.drawText(time, context.getCenterX(), context.getCenterY(), paint);

                paint.setColor(theme.getSecondaryTextColor());
                paint.setTextSize(14f * context.getScaledDensity());
                canvas.drawText(state.getDateText(), context.getCenterX(),
                        context.getCenterY() + 32f * context.getDensity(), paint);
            } finally {
                canvas.restoreToCount(saveCount);
            }
        }
    }
}
```

## 注册与发现

`ClockStyleRegistry` 保持注册顺序并拒绝重复 ID。第一个注册的样式自动成为 fallback，也可以在目标样式注册后调用 `setFallback(id)` 显式修改。

Ultimate 的内置 registry 每次通过 `UltimateClockStyles.createRegistry()` 新建。应用集成代码可在把 registry 交给 View 前追加第三方样式：

```java
ClockStyleRegistry registry = UltimateClockStyles.createRegistry();
registry.register(new MeridianStyle());

UltimateClockView clockView = findViewById(R.id.ultimate_clock_view);
clockView.setStyleRegistry(registry);
clockView.setStyleId("example.meridian");
```

设置页或其他样式选择器应从 `registry.getStyles()` 枚举样式，并读取 metadata，不应维护另一份样式 ID 列表。`getStyles()` 返回按注册顺序排列的不可修改快照。

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

- `pro.classic`（应用内部 view-backed 特例；registry renderer 只用于图库预览）
- `glass.atelier`
- `noir.instrument`
- `paper.station`
- `orbit.neon`
- `digital.grid`
- `typographic.poster`

## Capabilities

`ClockStyleCapabilities` 是不可变能力集合。可声明：

- `SECONDS`：样式能显示秒。
- `SMOOTH_SECONDS`：样式支持平滑秒针或连续秒进度。
- `DATE`：使用预格式化日期文本。
- `TIME_ZONE`：使用时区文本或时区信息。
- `WEATHER`：使用天气摘要。
- `STATUS`：使用状态摘要。
- `TWENTY_FOUR_HOUR`：尊重 12/24 小时设置。
- `REDUCED_MOTION`：能根据宿主的减少动画策略降级动态效果。

Ultimate 宿主会根据 `SECONDS` 和 `SMOOTH_SECONDS` 调整有效秒针模式与刷新节奏。其他能力同时用于发现和描述；声明能力不会自动绘制相应内容，renderer 仍需读取 `ClockState` 并设计对应布局。只声明真正实现的能力。

## Theme Tokens

`ClockThemeTokens` 通过 builder 创建，提供：

- 背景起止色、surface、主文字、次文字、accent 和线条 ARGB 色值。
- display 与 supporting Android 字体族名称。
- 正数 `strokeScale`。

token 用于统一预览和实时渲染的视觉参数，但不是完整主题。布局结构、刻度体系、表针形状、数字构成和信息层级都属于 renderer。仅替换颜色不应注册成新的完整样式。

## 每帧输入

`ClockRenderContext` 提供当前帧的像素边界、中心点、density、scaled density、帧时间、减少动画状态和可选 `ClockBackground`。

`ClockState` 提供：

- 当前毫秒时间、时区与 locale；`newCalendar()` 会生成匹配它们的 `Calendar`。
- 12/24 小时设置、是否显示秒、`OFF/TICK/SWEEP` 秒针模式。
- 已由宿主格式化的日期、时区、天气和状态文本。

renderer 应以 `ClockState.getTimeMillis()` 或 `newCalendar()` 为唯一时间依据。这样网络校时、系统时间和预览固定时间都可使用同一个 renderer。

### 用户背景

`ClockBackground` 有 `THEME`、`COLOR`、`IMAGE` 三种模式。`THEME` 表示使用样式自己的设计背景；`COLOR` 和 `IMAGE` 表示宿主提供了用户背景。bitmap 生命周期归宿主所有，renderer 不得回收或长期持有它。`isDimmed()` 为真时，renderer 应在背景层应用暗化，但不要暗化前景时钟内容。

调用旧的、不带背景参数的 `ClockRenderContext` 构造函数时，`getBackground()` 为 `null`，renderer 应把它视为使用自身主题背景。

## 生命周期与线程

标准 SDK renderer 没有生命周期回调。`UltimateClockView` 是生命周期所有者：

```java
@Override public void onResume() {
    super.onResume();
    clockView.start();
}

@Override public void onPause() {
    clockView.stop();
    super.onPause();
}
```

宿主还会在 View detach、窗口不可见或失去焦点时停止帧调度，并根据秒针模式、能力和减少动画设置选择分钟、秒或逐帧刷新。renderer 不应创建 Handler、线程、定时器或自行调用 `invalidate()`。

约束：

- `render()` 在 Android UI 线程调用；不要阻塞、访问网络或执行磁盘 I/O。
- 不要保存 `Canvas`、`ClockRenderContext`、`ClockState` 或宿主 bitmap 的跨帧引用。
- 缓存可复用的 `Paint`、`Path` 和字体，但不要把当前时间或布局结果作为共享全局状态。
- 对 Canvas 的 rotate、translate、clip、layer 等修改必须成对 save/restore。Ultimate 当前会隔离 renderer 的 Canvas 状态，但 renderer 自身仍应保持这个约束，便于预览和其他宿主复用。
- registry 的公开方法已同步，但注册通常应在 View 开始渲染前完成。一个 renderer 实例可能被实时 View 和预览复用，因此不要依赖可变的帧状态。

## Android 兼容约束

SDK 源码按 Java 8 和 API 14 编译；样式必须把实际需要的最低 API 写入 metadata。使用高于 `minApi` 的 Android API 时，应提高 `minApi`，或使用 `Build.VERSION.SDK_INT` 分支保护调用。

Ultimate 会使用 `resolveForApi()` 过滤不兼容样式。不要仅依赖设置页隐藏不兼容项，因为已持久化 ID、深链或自定义宿主仍可能请求该样式。

## Pro Classic 特例

`Pro Classic` 是 ClockMods 应用内部的 view-backed built-in 特例：它以 `pro.classic` metadata 出现在 registry 中，并提供一个只供样式图库使用的 preview renderer；实时界面复用已有 Pro `ClockView` 及其完整设置行为，由 Ultimate 宿主在 View 层路由。这一特例用于兼容成熟的旧界面，不代表 SDK 存在第二套公开 renderer 协议。

第三方开发者仍应只使用本文的标准路径：实现 `ClockStyle`、`ClockRenderer`、metadata 和 tokens，然后注册进 `ClockStyleRegistry`。不要继承应用内部 View、Fragment 或 Pro 类，也不要依赖 `Pro Classic` 的宿主分支。

## 接入检查表

1. 使用不会变化的小写点号 ID，并填写准确的 `kind`、`version` 和 `minApi`。
2. 只声明 renderer 确实实现的 capabilities。
3. 将状态读取和视觉 token 与 Canvas 几何分离。
4. 同时验证横屏、竖屏、小尺寸、12/24 小时、长日期、天气缺失和减少动画。
5. 对 `TICK`、`SWEEP`、`OFF` 分别验证秒显示；没有 `SMOOTH_SECONDS` 时正确降级。
6. 正确处理 `null/THEME`、`COLOR`、`IMAGE` 和 dimmed 背景。
7. 通过 `ClockStyleRegistry` 驱动选择器和 fallback，不复制 ID 清单。
8. 在目标最低 API 上运行，并确认 renderer 不产生跨帧对象泄漏或后台任务。
