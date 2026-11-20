# UniPocket

[![CI](https://github.com/3beuml/UniPocket/actions/workflows/ci.yml/badge.svg)](https://github.com/3beuml/UniPocket/actions/workflows/ci.yml)

校园课程表 UI 框架——给「想做自己学校校园助手」的人一个**能跑起来的起点**。

> 状态：**公开开发中，v0.1 尚未发布**（API 与目录结构仍可能变动，请勿直接用于生产）。
> 当前已完成：Gradle 骨架、契约与偏好模型、今天/课表/空教室/专注/设置五个页面、
> 一周课表与今天两个桌面组件（含行高账本与小组件设置）、背景图、手动补录/编辑、
> 可配置底栏、改名脚本与 CI（共 65 个单测）。
> v0.1 收尾清单：今天页卡片槽、番茄钟记录统计、冲突警示视觉。

## 快速体验

```bash
./gradlew :demo:assembleDebug      # 产出 demo/build/outputs/apk/debug/demo-debug.apk
./gradlew :demo:installDebug       # 或直接装到已连接的设备
```

打开后是「示例大学」的假课表：7 列恒显、可翻 20 周、点课程弹出详情；底栏可切到「空教室」页
（按星期/节次/楼栋筛选，结果按楼栋分组）。假数据集中在
`demo/src/main/kotlin/com/unipocket/demo/FakeData.kt`，换掉它就是接入自己学校的第一步。


## 它提供什么

- `core-domain` — 契约模型与接口（`Course`、`Semester`、`Period`、`LocationNode`、`SchoolConfig`、`ScheduleSource`、`RoomSource`、偏好读写接口、统一错误类型）。**不含数据库、不含网络实现。**
- `core-ui` — **必选基础**：无状态 Compose 组件 + 官方 ViewModel（今天时间轴、课表网格、周翻页、课程详情、错误/加载/空态）+ 可配置 `AppShell(tabs = …)` + 主题与背景层。
- `feature-room` — 空教室页（**可选模块**）。
- `feature-settings` — 设置页：深色模式、主题色、显示项开关（**可选模块**）。
- `feature-focus` — 番茄钟：25 分钟专注 / 5 分钟短休息 / 每 4 个长休息（**可选模块**）。
- `widget` — 桌面组件（**可选模块**）：一周课表（7 列两行列头、周末明细条）与今天列表；行高账本换行感知、宁可少画不裁字；只做 RemoteViews 布局，取数由 App 的 Provider 负责。
- `demo` — 假数据可运行 App。**外校 fork 的起点。**

> **可选模块怎么用**：不是每所学校都需要的页面，都是独立的 Gradle 模块。不要时删掉
> `settings.gradle.kts` 里的 `include(":feature-xxx")`、`demo/build.gradle.kts` 里的依赖、
> 以及底栏里对应的 tab 即可——这个模块**完全不参与构建，也不进 APK**。
> 后续的设置页、番茄钟也会按这个规则拆成独立模块。

## 接入你学校需要做什么

1. fork 本仓库，跑改名脚本把包名与应用名换成自己的：
   ```bash
   ./tools/rename.sh --package com.yourschool.pocket --app-name "你的课程表"
   ```
2. 填 `SchoolConfig`（节次时间表、学期起始日、总周数）。
3. 实现 `ScheduleSource`（把自家教务的课表转成 `Course`），可选实现 `RoomSource`。
4. 用你的 Source 替换 demo 里的假数据源。

界面代码一行都不用改。完整步骤、代码示例与常见坑见 **[docs/INTEGRATION.md](docs/INTEGRATION.md)**。

## 边界

- **不做**多学校切换、远端配置、电费/校园卡/图书馆/统一身份认证等本校专属功能。
- **不收录**各校适配器：各校各自 fork 维护。
- 没有网络层：HTTP 由接入方自己选型，框架只定义统一错误类型供 UI 展示。

## 许可证

MIT，见 [LICENSE](LICENSE)。
