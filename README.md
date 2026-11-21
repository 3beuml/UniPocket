# UniPocket

[![CI](https://github.com/3beuml/UniPocket/actions/workflows/ci.yml/badge.svg)](https://github.com/3beuml/UniPocket/actions/workflows/ci.yml)

校园课程表 UI 框架：fork 之后换掉数据源，就是一个学校的课表 App。
课表网格、今天页、空教室、番茄钟、桌面小组件都是现成的，要写的只有本校教务接入层。

> v0.1 还没发版，API 和目录结构都可能变，暂不建议用于生产。

## 先跑起来

```bash
./gradlew :demo:installDebug      # 或 :demo:assembleDebug，产物在 demo/build/outputs/apk/debug/
```

demo 用的是假数据（`demo/src/main/kotlin/com/unipocket/demo/FakeData.kt`）：一个能翻 20 周的
七列课表，点课程看详情，底栏还能切到空教室、专注和设置。界面上看到的东西就是框架提供的东西，
接入自己学校时这些界面一行都不用改。

## 模块

- `core-domain` — 契约与接口：`Session` / `Semester` / `Period`、`ScheduleSource`、`UiPrefs`、统一错误类型。纯 Kotlin
- `core-ui` — 课表网格、今天页、周翻页、`AppShell` 底栏、主题
- `feature-room` — 空教室页（可选）
- `feature-settings` — 外观与显示设置（可选）
- `feature-focus` — 番茄钟（可选）
- `widget` — 桌面小组件：一周课表、今天列表（可选）
- `demo` — 假数据 App，fork 后改它

可选模块不要就删三处：`settings.gradle.kts` 的 `include`、`demo/build.gradle.kts` 的依赖、
底栏对应的 tab。删完不参与构建，也不进 APK。

## 接入自己学校

1. fork 后跑改名脚本：
   ```bash
   ./tools/rename.sh --package com.yourschool.pocket --app-name "××大学课程表"
   ```
2. 在 `FakeData.kt` 里填本校的节次表和学期。
3. 实现 `ScheduleSource`：把教务的课表变成 `Session`（每周一条），失败时抛框架的统一错误类型。
4. 有空教室需求再实现 `RoomSource`。

详细步骤和代码示例在 [docs/INTEGRATION.md](docs/INTEGRATION.md)。

## 不做什么

- 不做多学校切换、远端配置，电费、校园卡、图书馆这类各校不一样的东西也不做。
- 不收录各校适配器，各校自己 fork 维护。
- 没有网络层：HTTP 由接入方自行选型，框架只负责把错误统一展示。

## 许可证

MIT，见 [LICENSE](LICENSE)。
