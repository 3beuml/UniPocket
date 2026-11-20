# 接入指南：把 UniPocket 变成你学校的课表 App

这份指南假设你会写 Kotlin。整体只有四步：**改名 → 填配置 → 写数据源 → 接进 demo**。
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

## 2. 填学校配置

打开 `demo/src/main/kotlin/.../FakeData.kt`，把 `config` 换成你们学校的节次表与学期：

```kotlin
val config = SchoolConfig(
    schoolName = "××大学",
    periods = listOf(
        Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
        Period(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
        // …按学校实际节次继续，序号必须从 1 开始连续
    ),
    semester = Semester(
        name = "2026-2027 第一学期",
        startDate = LocalDate.of(2026, 9, 1), // 第 1 周内任意一天都行，框架会自动归一到周首
        weekCount = 20,
        weekStartOnSunday = false,            // 学校按周一算周首就填 false
    ),
)
```

**常见差异：**

| 学校情况 | 怎么填 |
|---|---|
| 一周从周日开始 | `weekStartOnSunday = true` |
| 有上午/下午/晚上分段 | 放进 `Period.attributes["dayPart"]`，框架不依赖 |
| 节次名不是"第 N 节" | `Period.name = "第 N 大节"`，仅影响显示 |
| 学期起始日不确定 | 填第 1 周任意一天即可 |

---

## 3. 写数据源

新建一个文件，实现 `ScheduleSource`。核心只有一件事：**把学校的数据转成 `Course` 列表**。

```kotlin
class MyScheduleSource(private val client: MySchoolClient) : ScheduleSource {

    override val id: String = "my-school"

    override suspend fun loadCourses(): List<Course> {
        return try {
            client.fetchTimetable().map { dto ->
                Course(
                    id = dto.stableKey(),          // 同一节课每次必须相同
                    name = dto.courseName,
                    teacher = dto.teacher,
                    room = dto.roomName,
                    weeks = dto.weekNumbers,       // Set<Int>，1 起
                    dayOfWeek = dto.weekday,       // 1=周一 … 7=周日
                    periods = dto.startPeriod..dto.endPeriod,
                    attributes = mapOf(
                        "campus" to dto.campusName,
                        "experiment" to dto.isExperiment.toString(),
                    ),
                )
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

1. **一条 = 一个时间段**。同一门课周二、周四都有，就产生两条 `Course`；非连续节次（1、3 节）也要拆成两条。
2. **`id` 必须稳定**。用"课号 + 星期 + 节次"拼，别用列表下标或时间戳。
3. **学校专有信息放进 `attributes`**，不要新增字段、不要把校区拼进 `room`。

**多来源合并**（例如本科生教务 + 研究生教务 + 手动补录）：

```kotlin
val source = MergedScheduleSource(EasSource(), GmsSource(), ManualSource())
```

内容相同的课会自动去重（保留先出现的来源）。

---

## 4. 接进 demo

`MainActivity` 里把假数据源换成你的数据源即可——加载、缓存、翻周、错误态都由框架的
`ScheduleViewModel` 负责，**这段代码接入方不用写**：

```kotlin
val source = remember { MyScheduleSource(client) }
val viewModel: ScheduleViewModel = viewModel(
    factory = ScheduleViewModel.factory(config, source),
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

state.error?.let { ScheduleErrorBanner(error = it, onRetry = viewModel::retry) }

Box(Modifier.weight(1f)) {
    WeekTimetable(
        config = config,
        courses = state.courses,
        week = state.week,
        onCourseClick = { selectedCourse = it },
    )
    when {
        state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        state.isEmpty -> Text("本周没有课程", Modifier.align(Alignment.Center))
    }
}
```

状态字段的含义：

| 字段 | 含义 |
|---|---|
| `courses` | 当前周已过滤好的课程 |
| `isLoading` | 首次加载中（还没有任何数据） |
| `isRefreshing` | 已有数据、后台刷新中 |
| `error` | 最近一次失败（`UniPocketException` 四类），成功后清空 |
| `isEmpty` | 本周确实没课（不是加载中，也不是出错） |
| `lastUpdated` | 最近一次成功加载的时间 |

**刷新失败不会清空课表**：`courses` 保留旧数据，`error` 有值，界面同时显示课表和错误条。

到这里，课表页已经是你们学校的数据了。剩下的设置页、番茄钟、统计等，按需从上游搬或自己写。

今天页可以直接复用同一份课程数据（`todayItems` 是纯函数，不额外请求）：

```kotlin
TodayScreen(
    date = LocalDate.now(),
    items = state.courses.todayItems(config, LocalDate.now().dayOfWeek.value, LocalTime.now()),
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
val vm: RoomViewModel = viewModel(factory = RoomViewModel.factory(config, MyRoomSource()))
val state by vm.state.collectAsStateWithLifecycle()
RoomScreen(config, state, onDaySelected = vm::setDayOfWeek, onPeriodSelected = vm::setPeriod, onBuildingSelected = vm::setBuilding, onRetry = vm::retry)
```

不想提供这个能力时，把 `AppShell` 的 tabs 里 `"rooms"` 那一项删掉即可，其它页面不受影响。

---

## 4.6 偏好与设置页

偏好接口 `PrefsStore` 在 `core-domain`，框架**不绑定存储**。demo 里有一份
SharedPreferences 实现（`SharedPreferencesPrefsStore.kt`）可以直接抄。

App 级只需要建一个仓储并启动一次：

```kotlin
val repository = remember {
    PrefsRepository(SharedPreferencesPrefsStore(context.getSharedPreferences("app", MODE_PRIVATE)), scope)
}
LaunchedEffect(Unit) { repository.start() }

val prefs by repository.prefs.collectAsStateWithLifecycle()
UniPocketTheme(
    darkTheme = when (prefs.darkMode) { SYSTEM -> isSystemInDarkTheme(); LIGHT -> false; DARK -> true },
    seedColor = Color(prefs.themeSeed),
) { /* AppShell… */ }
```

设置页 UI 在 `feature-settings`（可选模块）。不需要设置页时，删掉这个模块即可；
但**只要你想保留用户偏好**，`PrefsRepository` 与 `PrefsStore` 实现仍要留在 App 里——
它们属于核心层，不在设置模块内。

课表页读取显示项：

```kotlin
WeekTimetable(
    config = config,
    courses = state.courses,
    week = state.week,
    showWeekend = prefs.showWeekend,
    showTeacher = prefs.showTeacher,
    showRoom = prefs.showRoom,
)
```

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
                    val config = MyConfig
                    val courses = MyScheduleSource().loadCourses()
                    val week = config.semester.currentWeek() ?: 1
                    val options = manager.getAppWidgetOptions(id)
                    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toFloat()
                    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toFloat()
                    val views = WeekWidgetRenderer.render(
                        context, config, courses, week, width, height,
                        WidgetStyle(dark = isNightMode(context)),
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

今天组件用 `TodayWidgetRenderer.render(..., week, date, now, ...)`，`date/time` 由 Provider 传当前值。

注意两个真机结论（已写进 `widget` 模块的实现）：
- 组件尺寸上报**虚高约 30dp**，账本会先扣掉；
- 行高按课名实际行数计算，装不下就少画一行——**绝不压缩到裁字**。

## 5. 自检清单

- [ ] `./gradlew :core-domain:test` 与 `./gradlew :core-ui:test` 全绿
- [ ] 第 1 周、中间某周、最后一周各翻一次，周次过滤正确
- [ ] 周末有课/没课两种情况都正常
- [ ] 断网时界面不崩溃，有可读的错误提示
- [ ] **没有把**学号、密码、keystore、校园网凭据、完整房间表提交进仓库
- [ ] 公共仓库里 `grep -ri zzu`（或你学校的标识）没有任何命中

最后一条由 CI 自动检查（见 `.github/workflows/ci.yml`）。

---

## 6. 去掉不需要的模块

框架里**只有课表页是必选的**。其它页面（空教室、将来的设置/番茄钟）都是独立模块，
不需要时删三处即可，代码完全不参与构建、也不进 APK：

1. `settings.gradle.kts`：删掉 `include(":feature-room")`
2. `demo/build.gradle.kts`：删掉 `implementation(project(":feature-room"))`
3. `demo/MainActivity.kt`：删掉 `AppTab(id = "rooms", …)` 与 `when` 里的对应分支，
   并删除 `demo/.../RoomTab.kt`

`feature-settings`、`feature-focus` 同理（各删自己的 include / 依赖 / tab / `*Tab.kt`）。

删完直接 `./gradlew :demo:assembleDebug` 即可，不需要动其它文件。

> 已验证：去掉 `feature-room` 后工程可独立构建（构建任务从 104 个降到 73 个）。
