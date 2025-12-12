# AutoAction v2.0 - 基础功能夯实设计文档

**版本**: v2.0
**日期**: 2023年12月12日
**目标**: 完善应用的配置灵活性，重构脚本动作模型以实现更细粒度的控制。

---

## 1. 全局设置模块 (Global Settings Module)

### 1.1 需求概述
用户需要能够全局控制点击的随机性和防检测参数，而不是在每个脚本里重复设置。这些设置将作为所有脚本执行时的“基准规则”。

### 1.2 数据持久化 (Preferences)
使用 `DataStore` (Protobuf/Preferences) 或 `SharedPreferences` 存储以下键值对：

| Key | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `randomization_enabled` | Boolean | `false` | 总开关，是否启用随机化 |
| `click_offset_radius` | Int | `10` | 点击坐标随机偏移半径 (px) |
| `click_duration_variance` | Long | `50` | 点击持续时长波动范围 (ms) |
| `delay_variance` | Long | `100` | 动作间等待时长波动范围 (ms) |
| `haptic_feedback_enabled` | Boolean | `true` | 点击时是否震动反馈 (录制/执行时) |

### 1.3 UI 设计 (Settings Screen)
*   **入口**: 主页顶部导航栏的 ⚙️ 图标。
*   **布局**: 
    *   **开关组**: "开启随机化防检测" (Switch)。
    *   **参数滑块组** (仅当开关开启时可用/可见):
        *   "坐标偏移范围": Slider (0-50px), 实时显示当前值。
        *   "按压时长波动": Slider (0-200ms)。
        *   "间隔延迟波动": Slider (0-1000ms)。
    *   **其他**:
        *   "触觉反馈": Switch。
        *   "关于": 版本号 v1.0。
    *   **关于波动的特别声明**:
        *   所有设置项中的“波动范围”数值均代表**正负双向偏差 (±)**。
        *   例如：若设置“间隔延迟波动”为 **78ms**，则实际执行时，会在基准时间的基础上增加一个 **-78ms 到 +78ms** 之间的随机值。

---

## 2. 动作模型重构 (Action Model Refactoring)

### 2.1 核心变更
将原本耦合了“点击+等待”的混合模型，拆分为**原子化指令**。这将允许用户构建如“连续点击不等待”或“长按不放”等复杂逻辑。

### 2.2 新数据结构 (Kotlin Data Class)

```kotlin
enum class ActionType {
    CLICK,      // 单次点击 (按下 + 抬起)
    SWIPE,      // 滑动
    DELAY,      // 纯等待
    // --- v2.1 预留 ---
    // TOUCH_DOWN, // 按下不放
    // TOUCH_UP    // 抬起
}

data class Action(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    
    // 坐标参数 (CLICK, SWIPE)
    val x: Int = 0,
    val y: Int = 0,
    
    // 终点参数 (SWIPE 专用)
    val endX: Int = 0,
    val endY: Int = 0,
    
    // 时长参数 (单位: ms)
    // 对于 CLICK: 表示按住多久后抬起 (Tap Duration)
    // 对于 SWIPE: 表示滑动过程持续多久
    // 对于 DELAY: 表示等待多久
    val duration: Long = 0,
    
    // 描述 (UI显示用)
    val label: String? = null
)
```

### 2.3 数据库迁移 (Migration Strategy)
由于结构变化较大，建议执行破坏性更新（清除旧数据）或编写 Migration：
1.  旧 `CLICK (baseDelay=1000)` -> 转换为两个动作:
    *   `Action(type=CLICK, duration=50)`
    *   `Action(type=DELAY, duration=1000)`

---

## 3. 编辑器升级 (Editor Enhancement)

### 3.1 动作列表展示
*   **CLICK**: 显示为圆点图标 `(x, y)`。
*   **SWIPE**: 显示为箭头图标 `(x1,y1) -> (x2,y2)`。
*   **DELAY**: 显示��沙漏/时钟图标 `Time: 500ms`。

### 3.2 添加动作逻辑
底部栏或悬浮按钮提供三个明确选项：
1.  **添加点击**: 默认添加一个中心坐标点击。
2.  **添加滑动**: 默认添加一个短距离滑动。
3.  **添加延迟**: 默认添加 500ms 延迟。

### 3.3 参数编辑面板 (Bottom Sheet)
根据 `type` 动态显示不同的输入框：
*   **CLICK**: X坐标, Y坐标, 按压时长(Duration)。
*   **SWIPE**: 起点X/Y, 终点X/Y, 滑动时长(Duration)。
*   **DELAY**: 等待时长(Duration)。

---

## 4. 执行器逻辑适配 (Executor Logic)

### 4.1 随机化应用
执行器 (`GestureExecutor`) 在读取到 Action 时，需注入全局设置中的随机参数。

```kotlin
// 伪代码逻辑
fun execute(action: Action, settings: GlobalSettings) {
    val random = Random()
    
    when (action.type) {
        CLICK -> {
            var finalX = action.x
            var finalY = action.y
            var finalDur = action.duration
            
            if (settings.randomizationEnabled) {
                // 应用坐标偏移
                finalX += random.nextInt(-settings.offset, settings.offset)
                finalY += random.nextInt(-settings.offset, settings.offset)
                // 应用时长波动
                finalDur += random.nextInt(-settings.durVar, settings.durVar)
            }
            // 执行 dispatchGesture (Path click)
        }
        
        DELAY -> {
            var finalWait = action.duration
            if (settings.randomizationEnabled) {
                finalWait += random.nextInt(-settings.delayVar, settings.delayVar)
            }
            Thread.sleep(max(0, finalWait))
        }
    }
}
```

### 4.2 连续性处理
执行器循环遍历动作列表时，不再自动插入间隔。严格按照用户设定的 `DELAY` 动作执行。如果没有 DELAY 动作，操作将以系统允许的最快速度连续执行。

---

## 5. 开发任务清单 (Checklist for Claude)

1.  **数据层**:
    *   [ ] 创建 `SettingsRepository` 和 `DataStore` 用于存储全局配置。
    *   [ ] 修改 `Action` 实体类，增加 `ActionType.DELAY`，移除旧的 `baseDelay` 字段。
    *   [ ] 更新 `AppDatabase` 版本。
2.  **UI层**:
    *   [ ] 实现 `SettingsScreen` 界面。
    *   [ ] 更新 `ScriptEditorScreen`，适配新的动作类型（拆分点击和等待）。
    *   [ ] 更新 `RecordingService`，录制时自动在操作之间插入 `DELAY` 动作（计算时间差）。
3.  **服务层**:
    *   [ ] 重写 `ScriptExecutor` 和 `GestureExecutor`，适配新的原子化执行逻辑。
    *   [ ] 在执行时读取并应用全局随机化参数。

