# AutoAction v2.1 - 录制增强与视觉优化设计文档

**版本**: v2.1
**日期**: 2023年12月12日
**目标**: 提升录制体验的流畅性与完整性，增强界面自定义能力。

---

## 1. 录制功能增强 (Recording Enhancement)

### 1.1 需求概述
*   **穿透操作**: 用户在录制脚本时，点击屏幕不仅要被记录，还要能实际触发底层应用的操作（如点击游戏按钮），实现“所见即所得”的录制体验。
*   **复杂手势**: 支持录制滑动手势 (Swipe) 和长按操作 (Long Press)。

### 1.2 技术方案：同步分发 (Synchronous Dispatch)
由于 Android 安全机制限制，悬浮窗拦截触摸事件后，事件会被消费，无法自动传递给底层应用。
**解决方案**: 在 `RecordingService` 拦截到触摸事件并记录的同时，通过 `AutoActionService` (AccessibilityService) 立即执行一个相同的手势。

*   **流程**:
    1.  用户手指按下 (`ACTION_DOWN`) 录制层。
    2.  `RecordingService` 记录坐标 `(x, y)` 和时间 `t1`。
    3.  **立即调用** `dispatchGesture` 执行一个极短的 `Click` 或开始构建 `Path`。
    4.  用户移动手指 (`ACTION_MOVE`)。
    5.  `RecordingService` 记录轨迹。
    6.  (可选) `dispatchGesture` 实时模拟滑动 (即时性要求高，可能只需录制完成后回放，或者分段模拟)。
    7.  用户抬起 (`ACTION_UP`)。
    8.  判断手势类型 (Click/Swipe/LongPress) 并保存为 `Action`。

*   **手势识别逻辑**:
    *   **Click**: `DOWN` -> 短暂停留 -> `UP` (位移 < 阈值)。
    *   **Long Press**: `DOWN` -> 停留 > 500ms -> `UP` (位移 < 阈值)。
    *   **Swipe**: `DOWN` -> `MOVE` (位移 > 阈值) -> `UP`。
        *   记录起点 `(downX, downY)` 和终点 `(upX, upY)`。
        *   记录时长 `duration = upTime - downTime`。

### 1.3 交互设计
*   **录制状态栏**:
    *   增加一个“同步执行”开关。开启后，录制时的操作会生效；关闭后，仅录制不执行���防止误触）。

---

## 2. 视觉自定义 (Visual Customization)

### 2.1 需求概述
允许用户调节悬浮窗的透明度，以适应不同背景的游戏或应用，避免遮挡视线。

### 2.2 设置项扩展
在 `SettingsScreen` (全局设置) 中新增“外观设置”区块。

| Key | Type | Default | Range | Description |
| :--- | :--- | :--- | :--- | :--- |
| `control_bar_alpha` | Float | `0.9` | `0.1` - `1.0` | 全局控制条的不透明度 |
| `shortcut_alpha` | Float | `0.8` | `0.1` - `1.0` | 脚本快捷键的不透明度 |

### 2.3 UI 实现
*   **设置界面**: 新增两个 `Slider`，分别控制控制条和快捷键的透明度。实时预览效果（可选）。
*   **应用逻辑**:
    *   `FloatingWindowService` 需监听这两个 Preference 的变化。
    *   当值变化时，实时更新 `ControlBarContent` 和 `ScriptShortcutContent` 的 `Modifier.alpha()` 或背景色 alpha 值。

---

## 3. 开发任务清单 (Checklist for Claude)

1.  **录制服务 (`RecordingService.kt`)**:
    *   [ ] 重构触摸监听器 (`OnTouchListener`)。
    *   [ ] 实现手势识别器 (Gesture Detector)，区分 Click, Long Press, Swipe。
    *   [ ] 集成 `AutoActionService.dispatchGesture`，在捕获事件时同步分发手势（需注意避免无限递归：录制层应设置 `FLAG_NOT_FOCUSABLE` 的反面? 不，录制层必须吃掉事件才能记录。分发的事件是系统层的，不会被应用层悬浮窗再次拦截，除非是 AccessibilityEvent，这点需验证）。
    *   [ ] 保存 Action 时，根据识别结果存为 `ActionType.CLICK`, `SWIPE`, `DELAY` 等。

2.  **设置与存储 (`SettingsRepository.kt` & `SettingsScreen.kt`)**:
    *   [ ] 增加 `control_bar_alpha` 和 `shortcut_alpha` 的 DataStore 键值。
    *   [ ] 在设置页添加对应的 Slider。

3.  **悬浮窗服务 (`FloatingWindowService.kt`)**:
    *   [ ] 订阅新的 Alpha 设置值。
    *   [ ] 将 Alpha 值传递给 `ControlBarContent` 和 `ScriptShortcutContent`。

4.  **UI 组件 (`ControlBarContent.kt` & `ScriptShortcutContent.kt`)**:
    *   [ ] 接收 `alpha` 参数，并应用到根布局的 `background` 或 `alpha` 修饰符上。

