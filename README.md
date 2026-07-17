# TrackDay · 轻量时间记录

把每一段时间轻轻记下。TrackDay 是一款基于 Jetpack Compose 的 Android 时间记录应用，通过定时提醒引导你随手记录「现在在做什么」，并以清晰的图表帮你复盘时间去向。

## 功能特性

### 记录 · 时间线
- 按日浏览记录，7 天日期条默认展示今天，向左滑查看更早
- 日历回看任意历史日期
- 记录卡片按上午 / 下午 / 晚上分组，可展开修改、删除
- 手动补记：选择小标签、滑动选择时 / 分、填写备注
- 支持自定义标签输入，未归类的自动归入「其他」

### 复盘 · 数据统计
- 日 / 周 / 月三种视图切换
- 环形图 + 占比条形图展示各大标签时长占比（按大标签汇总）
- 近 7 日 / 本周 / 本月趋势柱状图
- 自动生成时段小结，可手动改写
- 支持为小结添加配图

### 体系 · 标签管理
- 大标签 + 小标签两级结构
- 大标签可自定义底色与图形，实时预览
- 标签增删改全流程；重命名会同步更新历史记录与统计

### 设置 · 提醒
- 自定义提醒间隔（预设或任意分钟数）
- 24 小时精确生效时段，支持跨夜
- 夜间免打扰、系统通知、全屏打卡弹窗、短时暂停等开关

### 后台定时提醒（核心）
- 基于 `AlarmManager.setAlarmClock`，熄屏 / 休眠下也能准时触发
- 常驻前台服务保活，App 划掉后仍持续提醒
- 全屏打卡弹窗：借助悬浮窗权限，任何时候都能弹出整页标签选择界面
- 30 秒无操作自动继承上一标签（后台独立调度，不打扰专注中的用户）
- 暂停后在设定时长内彻底静默，到点自动恢复
- 开机 / 更新后自动重新调度

## 技术栈

- **UI**：Jetpack Compose + Material 3
- **导航**：Navigation Compose
- **状态**：ViewModel + Compose State
- **持久化**：DataStore（Preferences）+ Kotlinx Serialization
- **后台**：AlarmManager + 前台 Service + BroadcastReceiver
- **图片**：Coil
- **语言**：Kotlin
- **最低支持**：Android 7.0（API 24）

## 构建运行

```bash
# 构建 debug APK
./gradlew :app:assembleDebug

# 产物路径
app/build/outputs/apk/debug/app-debug.apk
```

或在 Android Studio 中打开项目，直接 Run 到设备 / 模拟器。

### 权限说明

首次启动会依次引导授予：
- **通知权限**（Android 13+）
- **精确闹钟权限**（Android 12+）
- **显示在其他应用上层 / 悬浮窗权限**——保证任何时候都能弹出全屏打卡界面

> 部分国产 ROM 需额外在系统设置中为 TrackDay 开启「自启动」与「后台无限制」，否则后台提醒可能被系统杀死。

## 项目结构

```
app/src/main/java/com/example/trackday/
├── MainActivity.kt            # 入口 + 底部导航 + NavHost
├── CheckInActivity.kt         # 全屏打卡界面（后台提醒拉起）
├── data/                      # 数据模型、仓库、ViewModel
├── reminder/                  # 闹钟调度、通知、前台服务、接收器
└── ui/
    ├── screens/               # 时间线 / 统计 / 标签 / 提醒 / 打卡
    ├── common/                # 通用组件（底部弹窗、滚轮选择器等）
    └── theme/                 # 配色、字体、主题
```

## 说明

本项目为原型迭代阶段，仍在持续完善中。
