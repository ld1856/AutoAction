# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AutoAction** is an Android automation app that uses AccessibilityService to enable script-based screen interactions (clicks, swipes, multi-touch) without requiring root access. The app targets gaming scenarios (particularly MOBA games) where users need rapid, repeatable macro operations.

### Core Capabilities
- Visual script recording via transparent overlay capture
- Script library management with independent floating shortcuts
- Atomic action model (CLICK, SWIPE, DELAY) with randomization support
- Multi-instance floating windows for quick macro execution

## Build Commands

Navigate to the `app/` directory for all Gradle commands:

```bash
cd app/

# Build the app
./gradlew assembleDebug

# Install debug build to connected device
./gradlew installDebug

# Run tests
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests

# Clean build
./gradlew clean

# Lint check
./gradlew lint

# Build release APK
./gradlew assembleRelease
```

## Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (modern screens) + XML ViewBinding (legacy fragments)
- **Architecture**: MVVM pattern
- **Database**: Room (local SQLite)
- **DI**: Manual dependency injection via service singletons
- **Async**: Coroutines with Flow for reactive data streams

### Key Components

#### 1. AccessibilityService Layer
**`AutoActionService.kt`** - Core accessibility service
- Implements `AccessibilityService` to dispatch gestures
- Maintains singleton instance accessible via `getInstance()`
- Hosts `ScriptExecutor` for running automation scripts
- Communicates service status via `StateFlow`

**`ScriptExecutor.kt`** - Script orchestration
- Manages script lifecycle (start, loop, stop)
- Delegates gesture execution to `GestureExecutor`
- Applies global randomization settings per-script
- Handles loop modes: infinite (count=0) or finite (count=N)

**`GestureExecutor.kt`** - Low-level gesture dispatch
- Translates `Action` data classes into Android `AccessibilityNodeInfo.GestureDescription`
- Applies coordinate offset and duration variance for anti-detection

#### 2. Floating Window System
**`FloatingWindowService.kt`** - Overlay manager
- Creates two types of floating UI:
  - **Control Bar**: Global controls (recording, settings, shortcut toggle)
  - **Script Shortcuts**: Individual floating buttons per enabled script
- Uses `WindowManager` with `TYPE_APPLICATION_OVERLAY`
- Implements touch disambiguation: <40px movement = click, ≥40px = drag
- Persists shortcut positions to database on drag completion

**`RecordingService.kt`** - Gesture capture
- Overlays full-screen transparent layer to intercept `MotionEvent`
- Converts touch sequences into `Action` objects with calculated delays
- Blocks underlying app interaction during recording (standard Android limitation)

#### 3. Data Layer
**Room Database** (`AppDatabase.kt`)
- Single entity: `ScriptEntity` (JSON-serialized fields for `actions`, `shortcutConfig`)
- DAO operations exposed as `Flow` for reactive UI updates

**Data Models** (in `data/model/`)
- `Script`: Domain model containing action list, loop config, randomization params
- `Action`: Atomic instruction with type (CLICK/SWIPE/DELAY), coordinates, duration
- `ShortcutConfig`: Floating button appearance (icon, position, alpha, scale)
- `ActionType`: Enum defining supported gesture primitives

**Settings Layer** (`data/settings/`)
- `GlobalSettings`: Data class for app-wide randomization parameters
- `SettingsRepository`: DataStore-backed persistence for global settings

**Repository** (`ScriptRepository.kt`)
- Abstracts database access
- Provides `enabledScripts` Flow for observing active shortcuts
- Handles entity ↔ domain model conversion

#### 4. UI Layer
**Compose Screens** (`ui/screen/`)
- `ScriptListScreen`: Main list with accessibility service status indicator
- `ScriptEditorScreen`: Visual action sequence editor with drag-to-reorder
- `SettingsScreen`: Global randomization and anti-detection settings

**Navigation** (`ui/navigation/NavGraph.kt`)
- Simple Compose Navigation with three routes:
  - `script_list` (home)
  - `script_editor/{scriptId}` (create/edit)
  - `settings` (global settings)

**ViewBinding Fragments** (legacy, in `ui/home/`, `ui/dashboard/`, etc.)
- Older XML-based fragments for settings/notifications
- Coexist with Compose screens during migration

### Critical Implementation Details

#### Action Model v2.0 (Atomic Design)
The original design coupled clicks with delays (`baseDelay` field). **v2.0 refactored** to atomic actions:
- Old: `CLICK(x=100, y=200, baseDelay=1000)`
- New: `CLICK(x=100, y=200, duration=50)` + `DELAY(duration=1000)`

This enables advanced patterns like "continuous taps without pause" or "hold button indefinitely."

#### Randomization System (Anti-Detection) - v2.0 Implementation
**Global settings** (stored in DataStore via `SettingsRepository`):
- `randomization_enabled`: Master toggle (default: false)
- `click_offset_radius`: ±N pixels from target coordinates (default: 10px, range: 0-50)
- `click_duration_variance`: ±N milliseconds for click/swipe duration (default: 50ms, range: 0-200)
- `delay_variance`: ±N milliseconds for delay actions (default: 100ms, range: 0-1000)
- `haptic_feedback_enabled`: Vibration feedback toggle (default: true)

**Three-tier override hierarchy**:
1. **Global settings**: App-wide defaults from `SettingsRepository`
2. **Script-level settings**: `Script.globalRandomOffset` and `Script.globalRandomDelay` override global values
3. **Action-level overrides**: `Action.overrideRandomOffset` and `Action.overrideRandomDelay` override both

**Variance is bidirectional (±)**: A setting of 78ms means random value in `[-78, +78]`.

**Execution flow**: `GestureExecutor` receives `GlobalSettings` from `ScriptExecutor`, applies randomization only when `randomization_enabled == true`, using `coerceAtLeast()` to ensure script/action overrides respect global minimums.

#### Floating Window Touch Logic
**Shortcut buttons** (`ScriptShortcutContent`):
- `ACTION_DOWN`: Record initial touch position
- `ACTION_MOVE`: If movement exceeds 40px threshold → enter drag mode
- `ACTION_UP`:
  - If not dragging AND movement <40px → execute script
  - If dragging → save new position to database

**Position memory**: Each script's `shortcutConfig.screenX/Y` persists across app restarts.

## Development Workflow

### Adding a New Action Type
1. Add enum value to `ActionType.kt`
2. Update `Action` data class with required parameters
3. Modify `GestureExecutor.kt` to handle new gesture dispatch:
   - Add case in `executeAction()` when block
   - Implement execution function (e.g., `executeCustomAction()`)
   - Apply randomization if needed
4. Update `ScriptEditorScreen.kt` UI:
   - Add menu item in FAB dropdown with icon
   - Add editing UI in `ActionCard` when block
   - Update action preview text in card summary
5. Update `RecordingService.kt` if action should be recordable
6. Add database migration if changing `Action` schema

### Modifying Script Execution Logic
- **Entry point**: `AutoActionService.executeScript(scriptId)`
- **Loop control**: Edit `ScriptExecutor.executeScript()`
- **Gesture customization**: Modify `GestureExecutor.executeAction()`

### Testing Accessibility Features
- Accessibility services cannot be easily mocked
- Use **instrumented tests** on real devices (not emulators) for full gesture testing
- Manual testing requires:
  1. Enabling "AutoAction" in Settings → Accessibility
  2. Granting "Display over other apps" permission
  3. Monitoring logcat for gesture dispatch failures

### Permission Requirements
Essential runtime permissions (must guide users to grant):
- `SYSTEM_ALERT_WINDOW` - Floating windows
- `BIND_ACCESSIBILITY_SERVICE` - Gesture dispatch
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Background stability
- `FOREGROUND_SERVICE` - Recording/execution persistence

## Known Constraints

1. **Recording blocks interaction**: Transparent overlay intercepts all touch events during recording - cannot operate target app simultaneously
2. **Gesture dispatch limits**: AccessibilityService has ~100ms minimum delay between gestures on some devices
3. **View hierarchy dependency**: Some advanced features (like "tap element by text") require parsing accessibility node tree, not yet implemented
4. **No root features**: Cannot simulate hardware keys (volume, power) or bypass secure screens

## Recent Major Changes (v2.0 Upgrade)

**Completed on 2023-12-12** - Full v2.0 atomic action refactoring:

### What Changed
1. **Global Settings Module**: Added DataStore-backed `SettingsRepository` with UI in `SettingsScreen`
2. **Atomic Action Model**: Deprecated `Action.baseDelay` in favor of separate `DELAY` actions
3. **Script Editor Enhancements**:
   - FAB now opens dropdown menu for CLICK/SWIPE/DELAY
   - Removed baseDelay from CLICK editor
   - Added complete SWIPE parameter editor (startX/Y, endX/Y, duration)
   - Renamed DELAY label to "Wait Duration" for clarity
4. **Randomization Improvements**: Three-tier override system (global → script → action)

### Migration Notes
- Old scripts with `baseDelay != 0` still work but show deprecation warnings
- To convert: manually split into `Action(type=CLICK, duration=X)` + `Action(type=DELAY, duration=Y)`
- Recording service automatically creates DELAY actions between gestures

## Design Documents Reference

See `docs/` directory for detailed specifications:
- `requirements.md`: Original PRD with macro mode requirements
- `design_v2_refinement.md`: Atomic action model and global settings design
- `ui_design.md`: Screen wireframes and interaction flows

## Development Guidelines

### When Working on This Codebase
1. **Respect the atomic action model**: Never add `baseDelay` logic to new code
2. **Test randomization**: Always test with `randomization_enabled = true` to verify variance application
3. **Preserve floating window positions**: Ensure any changes to `FloatingWindowService` maintain the 40px drag threshold
4. **Use descriptive action labels**: When creating actions programmatically, set meaningful `desc` values for debugging
