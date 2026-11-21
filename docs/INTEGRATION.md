# 接入指南：把 UniPocket 变成你学校的课表 App

这份指南假设你会写 Kotlin。整体只有四步：**改名 → 填学期与节次 → 写数据源 → 接进 demo**。
课表网格、周翻页、课程详情这些界面代码一行都不用改。

---

## 0. 准备

- JDK 17 或更高（推荐 21）
- Android SDK Platform 36
- 一个能拿到本校课表的办法：教务接口、网页抓取、导出的文件都行
- 如果学校没有干净接口，先想清楚**登录与会话**怎么处理——这部分框架不提供

---

## 1. 改名

```bash
# 先看一眼会改哪些文件
./tools/rename.sh --package com.yourschool.pocket --app-name "你的课程表" --dry-run

# 确认后执行
./tools/rename.sh --package com.yourschool.pocket --app-name "你的课程表"

./gradlew :demo:assembleDebug
```

脚本会移动 `com/unipocket` 源码目录、改写包名与应用名。跑完装上手机，看到的还是假课表——
这是正常的，下一步换数据。

> 类名（`UniPocketTheme`、`UniPocketException`）刻意不改，方便日后与上游对比。
> MIT 许可证要求保留 `LICENSE`；建议在 README 里注明"基于 UniPocket"。

---

## 2. 填学期与节次

数据模型没有「SchoolConfig」这种大配置对象——**节次表（`Period` 列表）和学期（`Semester`）**
是两个独立的东西，在各处分别传入。打开 `demo/src/main/kotlin/.../FakeData.kt` 换成你们的：

```kotlin
val periods = listOf(
    Period(1, "第1节", 800, 845, "MORNING", 0),
    Period(2, "第2节", 855, 940, "MORNING", 0),
    Period(3, "第3节", 1000, 1045, "MORNING", 1),
    // …按学校实际节次继续；indexNo 必须从 1 开始连续
)

val semester = Semester(
    id = 1,
    nameZh = "2026-2027-1",                // 教务给的原始学期名
    schoolYear = "2026-2027",              // 学年（展示串会自动拼好）
    startDate = LocalDate.of(2026, 9, 1),  // 第 1 周内任意一天都行，框架自动归一到周首
    endDate = null,
    weekStartOnSunday = false,             // 学校按周一算周首就填 false
    weekIndices = (1..20).toList(),        // 该学期包含哪些教学周
    isCurrent = true,
)
```

**节次时间用 `HHmm` 整数**：`800` 表示 08:00，`845` 表示 08:45。框架负责格式化成
"08:00"（`Period.startText` / `endText`）。

**常见差异：**

| 学校情况 | 怎么填 |
|---|---|
| 一周从周日开始 | `weekStartOnSunday = true` |
| 有上午/下午/晚上分段 | `dayPart`（`"MORNING"` / `"AFTERNOON"` / `"EVENING"`）+ `segmentIndex`（段内序号，从 0 起） |
| 节次名不是"第 N 节" | `nameZh = "第 N 大节"`，仅影响显示 |
| 学期起始日不确定 | 填第 1 周任意一天即可，周次换算自动对齐 |
| 只有 16 周 | `weekIndices = (1..16).toList()`；补考周不排课就不放进去 |

---

## 3. 写数据源

新建一个文件，实现 `ScheduleSource`。核心只有一件事：**把学校的数据转成 `Session` 列表**。
`Session` 是「某一周的一个上课时段」——**每周一条**，没有 `weeks: Set<Int>` 这种多周字段。

```kotlin
class MyScheduleSource(private val client: MySchoolClient) : ScheduleSource {

    override val id: String = "my-school"

    override suspend fun loadSessions(): List<Session> {
        return try {
            client.fetchTimetable().flatMap { dto ->
                dto.weekNumbers.map { week ->          // 多周展开成每周一条
                    Session(
                        id = "${dto.stableKey()}-w$week",  // 同一条记录每次调用必须相同
                        semesterId = semester.id,
                        source = "eas",                    // 来源标识："eas"/"gms"/"custom"，仅用于归因
                        courseCode = dto.courseCode,       // 课程级稳定标识：配色与详情聚合靠它
                        courseName = dto.courseName,
                        weekIndex = week,                  // 1 起
                        weekday = dto.weekday,             // 1=周一 … 7=周日
                        startUnit = dto.startPeriod,
                        endUnit = dto.endPeriod,
                        date = null,                       // 调休/补课填真实日期（ISO "2026-10-08"）
                        startTime = null,                  // 节次表查不到时才需要，"08:00"
                        endTime = null,
                        teacherName = dto.teacher,
                        roomText = dto.roomName,
                        campusText = dto.campusName,       // 校区，可空
                        color = null,
                        experiment = dto.isExperiment,     // 实验/见习课标记
                    )
                }
            }
        } catch (e: IOException) {
            throw UniPocketException.Network("课表获取失败：${e.message}", e)
        } catch (e: SessionExpiredException) {
            throw UniPocketException.AuthExpired(cause = e)
        } catch (e: Exception) {
            throw UniPocketException.DataFormat("课表数据格式异常：${e.message}", e)
        }
    }
}
```

**三条硬约定：**

1. **一条 = 某一周的一个时段**。同一门课覆盖 16 周 → 16 条 `Session`；同一周里周二、周四都有 → 各一条；非连续节次（1、3 节）再各拆一条。
2. **两个稳定标识**：`id` 必须记录级稳定（"课号+周次+星期+节次"拼，别用下标或时间戳）；`courseCode` 必须课程级稳定（同一门课所有周相同）——网格配色、课程详情的周次聚合、组件取色都靠它。
3. **学校专有信息走既有字段**（`campusText`、`experiment`、`color`…），不要新增学校语义字段。

**多来源合并**（例如本科生教务 + 研究生教务 + 手动补录）：

```kotlin
val source = MergedScheduleSource(EasSource(), GmsSource(), ManualSource())
```

「课名 + 星期 + 起止节次 + 周次」都相同的记录视为同一节课，自动去重（保留先出现的来源）。

---

## 4. 接进 demo

把假数据源换成你的即可——加载、缓存、翻周、错误态都由框架的 `ScheduleViewModel` 负责。
demo 的 `TimetableTab` 就是完整范例，核心接线长这样：

```kotlin
val source = remember { MyScheduleSource(client) }
val viewModel: ScheduleViewModel = viewModel(
    key = "timetable",   // ⚠️ 同一个 ViewModel 类被多个 Tab 使用时必须给 key，
                         //    否则拿到的是同一个缓存实例（编辑通道会被静默吞掉）
    factory = ScheduleViewModel.factory(semester, source),
)
val state by viewModel.state.collectAsStateWithLifecycle()

WeekHeader(
    week = state.week,
    weekCount = state.weekCount,
    isCurrentWeek = state.isCurrentWeek,
    onPrevious = viewModel::previousWeek,
    onNext = viewModel::nextWeek,
    onToday = viewModel::goToToday,
)

state.error?.let { UniPocketErrorBanner(error = it, onRetry = viewModel::retry) }

ScheduleGrid(
    slots = state.sessions.expandByWeek(state.week),
    periods = periods,                                   // 你在第 2 步填的节次表
    prefs = prefs,                                       // UiPrefs，见 4.6
    weekMonday = ScheduleText.mondayOf(semester.startDate, state.week),
    conflictChoices = emptyList(),
    week = state.week,
    onBlockClick = { block -> /* 详情：block.displaySlot */ },
)
```

状态字段的含义：

| 字段 | 含义 |
|---|---|
| `sessions` | 当前周已过滤好的上课记录（每周一条 `Session`） |
| `week` / `weekCount` / `isCurrentWeek` | 当前周次、总周数、是否本周（周次由 `Semester.startDate` 推算） |
| `isLoading` | 首次加载中（还没有任何数据） |
| `isRefreshing` | 已有数据、后台刷新中 |
| `error` | 最近一次失败（`UniPocketException` 四类），成功后清空 |
| `isEmpty` | 本周确实没课（不是加载中，也不是出错） |
| `lastUpdated` | 最近一次成功加载的时间 |

**刷新失败不会清空课表**：`sessions` 保留旧数据，`error` 有值，界面同时显示课表和错误条。

今天页复用同一份数据（`todayItems` 是纯函数，不额外请求；先按周过滤再取今天星期几）：

```kotlin
val currentWeek = WeekAlignment.indexOf(
    semester.startDate, semester.weekStartOnSunday, semester.weekIndices, LocalDate.now(),
) ?: 1

TodayScreen(
    date = LocalDate.now(),
    items = state.sessions
        .filter { it.weekIndex == currentWeek }
        .todayItems(periods, LocalDate.now().dayOfWeek.value, LocalTime.now()),
    isLoading = state.isLoading,
)
```

---

## 4.5 可选：空教室页

实现了 `RoomSource` 后，空教室页不需要再写代码——`RoomScreen` + `RoomViewModel` 直接可用：

```kotlin
class MyRoomSource : RoomSource {
    override val id = "my-rooms"

    override suspend fun locations(): LocationNode = /* 校区 → 楼 → 教室 的树 */

    override suspend fun freeRooms(week: Int, dayOfWeek: Int, periods: IntRange): List<LocationNode> =
        /* 返回该时段空闲的叶子节点（教室） */
}
```

```kotlin
val vm: RoomViewModel = viewModel(factory = RoomViewModel.factory(periods, semester, MyRoomSource()))
val state by vm.state.collectAsStateWithLifecycle()
RoomScreen(
    periods = periods,
    state = state,
    onDaySelected = vm::setDayOfWeek,
    onBuildingSelected = vm::setBuilding,
    onRetry = vm::retry,
)
```

`semester` 传 `null` 也可以（周次固定为 1）。不想提供这个能力时，把第 6 节说的三处删掉即可。

---

## 4.6 偏好与设置页

偏好接口 `PrefsStore` 在 `core-domain`，框架**不绑定存储**。demo 里有一份
SharedPreferences 实现（`SharedPreferencesPrefsStore.kt`）可以直接抄。

App 级只需要建一个 `UiPrefsRepository` 并启动一次：

```kotlin
val repository = remember {
    UiPrefsRepository(
        store = SharedPreferencesPrefsStore(context.getSharedPreferences("app", MODE_PRIVATE)),
        scope = scope,
    )
}
LaunchedEffect(repository) { repository.start() }

val prefs by repository.prefs.collectAsStateWithLifecycle()
```

设置页 UI 在 `feature-settings`（可选模块），读写都走这个 repository
（`setDarkMode` / `setShowWeekend` / `setBackground` / …）。不需要设置页时删掉模块即可；
但**只要你想保留用户偏好**，`UiPrefsRepository` 与 `PrefsStore` 实现仍要留在 App 里——
它们属于核心层，不在设置模块内。

课表网格的显示项（周末列数、格子密度、字号、描边、配色模式、正在上课描边…）全部收敛在
`UiPrefs` 里，`ScheduleGrid(prefs = prefs, …)` 一个参数带进去，不需要逐个传开关。

---

## 4.7 桌面组件（可选）

`widget` 模块只提供 **RemoteViews 渲染器**与行高账本，不取数据。App 里需要一个
`AppWidgetProvider` 负责「取课表 → 调渲染器 → updateAppWidget」：

```kotlin
class MyWeekWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val sessions = MyScheduleSource().loadSessions()
                    val week = WeekAlignment.indexOf(
                        semester.startDate, semester.weekStartOnSunday,
                        semester.weekIndices, LocalDate.now(),
                    ) ?: 1
                    val options = manager.getAppWidgetOptions(id)
                    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toFloat()
                    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toFloat()
                    val views = WeekWidgetRenderer.render(
                        context = context,
                        sessions = sessions,
                        periods = periods,
                        week = week,
                        weekMonday = ScheduleText.mondayOf(semester.startDate, week), // null = 列头不显示日期
                        reportedWidthDp = width,
                        reportedHeightDp = height,
                        style = WidgetStyle(dark = isNightMode(context)),
                        options = WidgetOptions(showRoom = true, showTime = true),
                    )
                    manager.updateAppWidget(id, views)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
```

再补 `res/xml/week_widget_info.xml` 与 manifest 里的 `<receiver>`（照抄 demo 的
`WeekWidgetProvider` / `week_widget_info.xml` 即可）。

**改尺寸验证**：`WidgetPreviewActivity` 可以在不装桌面的情况下按任意尺寸预览：

```bash
adb shell am start -n <你的包名>/.WidgetPreviewActivity --ei width 342 --ei height 380
adb shell am start -n <你的包名>/.WidgetPreviewActivity --es type today --ei width 250 --ei height 150
```

今天组件用 `TodayWidgetRenderer.render(context, sessions, periods, week, date, now, …)`，
`date/now` 由 Provider 传当前值。

注意两个真机结论（已写进 `widget` 模块的实现）：
- 组件尺寸上报**虚高约 30dp**，账本会先扣掉；
- 行高按课名实际行数计算，装不下就少画一行——**绝不压缩到裁字**。

## 4.8 背景图（可选）

框架只负责画：`AppBackground(painter, …)` 放在主题之下、`AppShell` 之上。
偏好字段是现成的（`backgroundPath` / `backgroundOpacity` / `backgroundBlur` / `backgroundScrim`）：

```kotlin
val painter = rememberBackgroundPainter(prefs.backgroundPath)   // demo 里有参考实现
AppBackground(painter = painter, backgroundScrim = prefs.backgroundScrim) { AppShell(…) }
```

选图归 App：demo 用 `ActivityResultContracts.OpenDocument` + `takePersistableUriPermission`
（重启后仍可读），`repository.setBackground(uri.toString())` 存进框架偏好。
不想让用户换背景，忽略这个入口就行，核心页面不受影响。

## 4.9 手动补录/编辑（可选）

补录是一个独立的数据源：实现 `EditableScheduleSource`（按**条**增删改：`upsert(session)` /
`delete(id)` / `manages(id)`），持久化完全归你。然后把「合并后的读取来源」和「编辑来源」
分开交给 ViewModel：

```kotlin
val manual = remember { MyManualSource(store) }          // EditableScheduleSource
val source = remember { MergedScheduleSource(EasSource(), manual) }
val vm: ScheduleViewModel = viewModel(
    key = "timetable",                                    // ⚠️ 多 Tab 共用必须给 key
    factory = ScheduleViewModel.factory(semester, source, editableSource = manual),
)
```

- 补录：`vm.upsertSessions(sessions)`——一门课覆盖 N 周 → 传 N 条（同 `courseCode`、
  各自 `weekIndex`），保存后自动刷新；
- 删除：`vm.deleteSession(id)` 逐条调（demo 的删除弹窗按 courseCode 聚合后整门课一起删）；
- 详情页"编辑/删除"按钮会依据 `manages(id)` 显示，只有补录来源的课可编辑；
- 编辑**教务课**（覆盖教室/时间）v0.1 不支持，需要覆盖层概念。

`CourseEditorDialog` 是现成的编辑 UI：表单里选「每周/单周/双周/自定义」周次档，
保存时自动展开成每周一条的 `Session` 列表（编辑已有课会沿用原 id，保住冲突选择记忆），
周次文本的解析（`1-8,10-16`）与回填都在对话框内部完成，接入方不用碰。
demo 的 `ManualCourseSource`（JSON 存 SharedPreferences）可以直接抄。

---

## 5. 自检清单

- [ ] `./gradlew :core-domain:test :core-ui:test` 全绿
- [ ] 第 1 周、中间某周、最后一周各翻一次，周次过滤正确
- [ ] 周末有课/没课两种情况都正常
- [ ] 断网时界面不崩溃，有可读的错误提示
- [ ] **没有把**学号、密码、keystore、校园网凭据、完整房间表提交进仓库
- [ ] 公共仓库里 `grep -ri zzu`（或你学校的标识）没有任何命中

最后一条由 CI 自动检查（见 `.github/workflows/ci.yml`）。

---

## 6. 去掉不需要的模块

框架里**只有课表页是必选的**。其它页面（空教室、设置、番茄钟）都是独立模块，
不需要时删三处即可，代码完全不参与构建、也不进 APK：

1. `settings.gradle.kts`：删掉 `include(":feature-room")`
2. `demo/build.gradle.kts`：删掉 `implementation(project(":feature-room"))`
3. `demo/MainActivity.kt`：删掉 `demoDestinations` 里 `"rooms"` 那项 `DockDestination`
   与 `when` 里的对应分支，并删除 `demo/.../RoomTab.kt`

`feature-settings`、`feature-focus`、`widget` 同理（各删自己的 include / 依赖 / tab / 入口文件）。

删完直接 `./gradlew :demo:assembleDebug` 即可，不需要动其它文件。

> 已验证：去掉 `feature-room` 后工程可独立构建（构建任务从 104 个降到 73 个）。
