# Bug修复建议 (修订版 v1.1)

## 1. 核心崩溃修复 (Critical Crash Fix)
**问题**: `RecordingService` 和 `FloatingWindowService` 在启动或交互时崩溃，错误为 `ViewTreeLifecycleOwner not found`。
**根源**: Service 中使用 Compose View 时，缺少 Lifecycle 环境。
**解决方案**:
创建一个抽象基类 `OverlayService` (或类似名称)，统一管理生命周期。

```kotlin
// 示例代码结构
abstract class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
    
    // 关键：在添加 ComposeView 之前调用
    fun attachLifecycle(view: ComposeView) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }
}
```
所有悬浮窗 Service 均需继承此基类，并在创建 View 时调用 `attachLifecycle(view)`。

## 2. 交互失效修复 (Interaction Fix)
**问题**: 悬浮窗可拖动但点击回调不触发。
**解决方案**:
除了上述 Lifecycle 修复外，还需检查 `WindowManager.LayoutParams`。
*   **必须移除** `FLAG_NOT_TOUCHABLE`。
*   **建议保留** `FLAG_NOT_FOCUSABLE` (防止抢占输入法焦点)，但在某些特殊交互下可能需要动态移除。

## 3. 功能缺失修复 (Feature Gap)
**问题**: 脚本快捷��无法显示。
**根源**: `ScriptEditorScreen` 缺少修改 `isEnabled` 属性的入口。
**解决方案**:
在 `ScriptEditorScreen.kt` 的 `TopAppBar` 的 `actions` 中添加一个 `Switch`。

```kotlin
// 伪代码
TopAppBar(
    title = { Text("编辑脚本") },
    actions = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("显示快捷键", style = MaterialTheme.typography.bodySmall)
            Switch(
                checked = script.isEnabled,
                onCheckedChange = { isEnabled -> 
                    viewModel.updateScript(script.copy(isEnabled = isEnabled)) 
                }
            )
        }
    }
)
```

## 4. 逻辑确认
**确认项**: 
*   `FloatingWindowService` 必须使用 `scriptRepository.getEnabledScripts().collect { ... }` (即 Flow 收集) 来实时响应数据库变化。
*   当前的 Service 实现中，如果只在 `onCreate` 获取一次数据，那么用户在主界面开关脚本后，悬浮窗不会自动更新。必须确保是一个持续的观察者 (Observer)。