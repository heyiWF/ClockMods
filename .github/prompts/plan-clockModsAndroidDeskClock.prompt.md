## Plan: ClockMods Android 桌面时钟

在空工作区中初始化一个 Java/经典 View 的单 Activity Android 应用，使用 `minSdk 14`（Android 4.0）、`compileSdk/targetSdk 36`（当前稳定 Android 16）。主界面全屏常亮、跟随设备方向重建并自适应排版；点击画面唤起底部设置面板，可用 HSV 连续调色板/常用色或相册图片设置背景。时间每秒与系统时钟边界对齐更新，日期包含公历、星期与离线计算的农历信息。

**Steps**
1. 初始化 Gradle Android 工程骨架：配置 Gradle Wrapper、AGP 9.2.x、JDK 17、单一 `app` 模块、Java 源集、`minSdk 14`、`targetSdk 36`、`compileSdk 36`，设置应用名 `ClockMods` 和包名 `com.clockmods`；仅引入确有必要且仍支持 API 14 的依赖，避免 Compose/Material/新 AndroidX 抬高最低版本。
2. 建立应用清单、主题和基础资源：声明可横竖屏自动旋转的 `MainActivity`，启用硬件加速、无 ActionBar、常亮和全屏主题，提供 API 分级资源以兼容 Android 4.0 与 Android 15/16 强制 edge-to-edge 行为，并准备默认深色背景、中文字符串、尺寸和应用图标资源。*depends on 1*
3. 实现 `ClockPreferences` 配置存储：用 `SharedPreferences` 保存背景模式、ARGB 纯色值和内部图片文件状态；图片模式不长期依赖外部 URI 权限，而是在选择后复制到应用私有文件并原子替换，纯色切换时保留图片文件供用户再次选择或按明确策略清理。*depends on 1*
4. 实现离线 `LunarCalendar`：用覆盖目标日期范围的农历年表数据和公历转农历算法输出干支年、生肖、农历月名、农历日名，组合成如 `丙午[马]年六月初一`；明确算法支持范围并在超出范围时只显示公历和星期，避免崩溃或错误日期。为闰月、春节边界、大小月和示例 `2026-07-14` 添加纯 JVM 单元测试。*parallel with 3 after 1*
5. 实现 `ClockView` 自绘主界面：背景先绘制纯色，再将图片按 center-crop 比例矩阵居中铺满；使用 `HH:mm:ss` 的固定 24 小时格式和设备当前时区/Locale 实时生成时间、公历与中文星期；时间使用等宽数字字形或稳定测量，避免秒数变化引起布局跳动。日期位于时间上方且字号更小，横屏优先将“公历+星期”和农历放在同一行，竖屏或实测宽度不足时拆为两行；字体大小根据可用宽高、有界最小/最大值和文本测量计算，不按视口宽度直接线性缩放。*depends on 2, 4*
6. 在 `MainActivity` 实现生命周期与系统 UI：窗口保持常亮；API 30+ 使用 `WindowInsetsController`，旧版本使用兼容的 system UI flags 进入 sticky immersive；处理显示缺口和 Android 15/16 edge-to-edge 安全区域；在 `onResume` 启动按下一秒边界校准的 `Handler` 更新，在 `onPause` 停止回调，并监听时间、时区、日期变化广播以立即纠正。方向变化交给系统重建，重新测量并加载对应屏幕尺寸的背景。*depends on 5*
7. 实现点击唤起的底部设置面板：点击时暂时显示系统栏并弹出面板，关闭后恢复沉浸模式；面板提供“纯色/图片”分段选择、常用色色块、自绘 HSV 饱和度/明度区域与色相滑条、当前颜色预览、选择相册图片和确认/取消操作。控件尺寸和触摸目标适配手机横竖屏，横屏高度不足时允许设置内容滚动。*depends on 2, 3*
8. 实现相册选择和图片管线：API 19+ 使用 `ACTION_OPEN_DOCUMENT` 单选 `image/*`，API 14-18 使用 `ACTION_GET_CONTENT`；结果通过 `ContentResolver` 流复制到临时私有文件后原子提交。先读取尺寸，再按当前显示尺寸计算 `inSampleSize` 解码，应用 EXIF 方向（若不引入兼容库则通过平台可用信息与安全回退处理），后台线程完成 I/O/解码，主线程更新视图；捕获取消、无权限、损坏图片、超大图片和内存不足并保留原背景。*depends on 3, 7*
9. 补齐可测试边界与工程文档：为日期文本组合、单行/双行判定、center-crop 目标矩形/矩阵和颜色持久化提取纯 Java 可测逻辑并添加测试；README 记录 JDK 17、Android SDK 36、构建安装命令、API 14 兼容说明、农历范围和图片隐私策略。*depends on 4-8*
10. 执行完整验证并仅修复本功能范围内的问题：先跑单元测试与 debug 构建，再跑 lint；随后在至少一个现代 API 36 模拟器/设备和一个 API 14 或可获得的最老模拟器上做手工验收，验证旋转、跨秒/跨日、时区变化、设置持久化、相册取消/损坏/超大图片、沉浸手势和文字不重叠。*depends on all previous steps*

**Relevant files**
- `d:\ClockMods\settings.gradle` — 单模块工程与仓库配置。
- `d:\ClockMods\build.gradle` — AGP 版本和根工程插件配置。
- `d:\ClockMods\gradle\wrapper\gradle-wrapper.properties` — 与 AGP 9.2.x 匹配的 Gradle 9.4.1 Wrapper。
- `d:\ClockMods\app\build.gradle` — API 14/36、Java、测试和打包配置。
- `d:\ClockMods\app\src\main\AndroidManifest.xml` — Activity、方向、主题与系统兼容声明。
- `d:\ClockMods\app\src\main\java\com\clockmods\MainActivity.java` — 生命周期、秒级调度、沉浸模式、设置入口和图片选择协调。
- `d:\ClockMods\app\src\main\java\com\clockmods\ui\ClockView.java` — 背景、时间、日期和自适应绘制。
- `d:\ClockMods\app\src\main\java\com\clockmods\ui\ClockLayoutCalculator.java` — 可测试的字号、日期断行与布局几何计算。
- `d:\ClockMods\app\src\main\java\com\clockmods\ui\ColorPickerView.java` — HSV 连续调色交互与绘制。
- `d:\ClockMods\app\src\main\java\com\clockmods\ui\SettingsDialog.java` — 底部设置面板及确认/取消状态管理。
- `d:\ClockMods\app\src\main\java\com\clockmods\calendar\LunarCalendar.java` — 离线农历转换和中文格式化。
- `d:\ClockMods\app\src\main\java\com\clockmods\background\BackgroundRepository.java` — 偏好持久化、图片私有复制和采样解码。
- `d:\ClockMods\app\src\main\res\values\`、`values-v21\`、`values-v35\` — 字符串、颜色、尺寸和分版本主题/窗口行为。
- `d:\ClockMods\app\src\test\java\com\clockmods\` — 农历、格式化、布局和裁剪算法单元测试。
- `d:\ClockMods\README.md` — 环境、构建、运行和兼容范围说明。

**Verification**
1. 使用 JDK 17 和已安装的 Android SDK 36 执行 `gradlew.bat testDebugUnitTest`，重点断言 `2026-07-14` 输出 `2026年7月14日星期二` 与 `丙午[马]年六月初一`，并覆盖春节、闰月、月末和算法范围外回退。
2. 执行 `gradlew.bat assembleDebug`，确认 API 14 最低版本下无缺失类或高版本 API 的无保护调用，并产出可安装 APK。
3. 执行 `gradlew.bat lintDebug`，处理 NewApi、权限、资源和国际化问题；高版本 API 调用必须由 SDK 检查隔离。
4. 在 API 36 上验收：启动即常亮沉浸；横竖屏切换后时间/日期居中且不重叠；横屏足够宽时日期同一行，窄屏/竖屏农历换行；系统栏可边缘滑出并自动隐藏。
5. 在 API 14（若本机 SDK/模拟器镜像可用）或能获得的最老设备上验收：应用可启动、每秒更新、`ACTION_GET_CONTENT` 可选图、无运行时权限请求、调色板可操作、旋转无崩溃。若无法取得 API 14 镜像，在结果中明确记录此未验证风险。
6. 选择超宽、超高、带 EXIF 旋转和超大分辨率图片，确认居中裁剪铺满、方向正确、无明显卡顿或 OOM；重启和旋转后仍可显示，取消或损坏输入不覆盖原背景。
7. 修改系统时间、时区并跨越午夜，确认下一次 tick/广播后时间、公历、星期和农历同步刷新；后台时无持续每秒回调。

**Decisions**
- 使用 Java + 平台经典 View/Canvas，而非 Compose；这是覆盖 API 14 且保持依赖轻量的保守方案。
- 当前稳定目标平台按 Android 16/API 36 配置；Android 17 尚非稳定 target，不纳入本次目标。
- 设置入口为点击时钟画面；设置面板关闭后恢复沉浸式。
- 图片采用 center-crop 居中裁剪铺满，不拉伸、不留边。
- 默认保持屏幕常亮并隐藏状态栏/导航栏，系统边缘手势仍可临时唤出系统栏。
- 纯色设置提供 HSV 任意颜色、常用色块和实时预览。
- 相册只选择单张图片，不申请全图库权限；API 14-18 走系统内容选择器，选中内容复制进应用私有存储。
- 时间强制 24 小时制并显示秒；日期使用中文固定格式，星期不随系统语言改成英文。
- 本次不包含桌面小部件、闹钟、网络天气、字体/文字颜色自定义、图片编辑或云同步。

**Further Considerations**
1. 农历算法建议支持 1900-2100 年；范围外优雅降级为仅公历。若未来要求更长范围，应改用经过验证的数据集/库并重新评估 API 14 兼容与包体积。
2. 默认文字使用白色并加轻微阴影以适配图片背景；本次不加入自动取色或手动文字颜色设置，避免扩展设置复杂度。