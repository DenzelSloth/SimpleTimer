# Simple Timer

A client-side Fabric mod for Minecraft **26.1.2** that provides on-screen countdown timers with hotkey reset, mob detection with automatic spawn tracking, and configurable HUD/waypoint rendering.

## Requirements

- Java **25+**
- Minecraft **26.1.2**
- Fabric Loader **0.19.3+**
- Fabric API **0.155.2+26.1.2**
- Fabric Language Kotlin **1.13.13+**

## Features

- Up to **999** simultaneous timers (hotkeys for slots 1–10, commands for any slot)
- **Multi-spawn tracking**: track multiple spawns of the same mob type at different locations simultaneously (e.g. 2 Key Guardians + 1 Corleone)
- Draggable HUD with aligned name/timer columns and color-coded progress bars
- 3D waypoint labels at timer/mob positions (visible through walls, configurable distance up to 500 blocks, default 200)
- Timers persist across restarts using wall-clock time (including mute state)
- **Mob detection**: watch for named entities (e.g. Hypixel bosses) with partial name matching and field-of-view check
- **Auto spawn tracking**: learns kill-to-spawn and spawn-to-spawn intervals, automatically creates timers
- **LIVE / countdown / UP cycle**: red "LIVE" while mob is alive, blue countdown after kill, green "UP" when respawn is due
- **Full-screen alerts**: large colored text for mob spawns (red) and timer warnings (yellow), displayed simultaneously for different mobs
- **Click interactions**: left-click to mute, double left-click to reset, double right-click to remove
- **View/clear learned intervals**: see and manage respawn timing data via commands
- Configurable warning threshold, alarm durations, and volume
- Two-tone urgent spawn alarm (alternating bell + bit sounds)
- Muted timers shown with gray bar and `[M]` indicator

## Commands

### Timer Commands

```
/STimer set <name> <amount> [unit] <slot> [waypoint]
/STimer remove <slot>
/STimer remove name <name>
```

| Argument | Meaning |
|----------|---------|
| `name` | Label shown on the HUD (quote names with spaces: `"Boss Spawn"`) |
| `amount` | Duration number (with optional `unit`, or alone as seconds) |
| `unit` | Optional: `seconds` / `minutes` / `hours` (also `s` / `m` / `h`). Max **24 hours** |
| `slot` | Timer slot **1–999** (slots 1–10 have hotkeys: Ctrl/Cmd + number, slot 10 = Ctrl+0) |
| `waypoint` | Optional `true`/`false` (default `false`). Saves your position and shows a floating timer label |

`remove name` matches both exact timer names and base names (without coordinate suffixes), so `/STimer remove name "Key Guardian"` will also remove `Key Guardian [100, -200]`.

### Mob Watchlist Commands

```
/STimer watch add <name>
/STimer watch remove <name>
/STimer watch list
/STimer watch clear
/STimer watch alert
/STimer watch intervals
/STimer watch intervals clear
/STimer watch intervals clear <name>
```

Add mob names (partial match supported) — e.g. `/STimer watch add "Key Guardian"` will match `[Lv100] Key Guardian 250k/250k❤`.

Use `intervals` to view all learned respawn times. Use `intervals clear` to reset all, or `intervals clear <name>` to reset a specific mob.

### Config Commands

```
/STimer config
/STimer config showKeybinds <true|false>
/STimer config showClickHints <true|false>
/STimer config hotkeyResets <true|false>
/STimer config draggable <true|false>
/STimer config showBackground <true|false>
/STimer config size <0.5-3.0>
/STimer config textOpacity <10-100>
/STimer config backgroundOpacity <0-100>
/STimer config warningThreshold <1-60>
/STimer config showWarningMessage <true|false>
/STimer config alarmDuration <0-60>
/STimer config spawnAlarmDuration <0-30>
/STimer config volume <0-100>
/STimer config waypoint enabled <true|false>
/STimer config waypoint distance <8-500>
/STimer config waypoint size <0.5-3.0>
/STimer config waypoint showBackground <true|false>
/STimer config waypoint textOpacity <10-100>
/STimer config waypoint backgroundOpacity <0-100>
/STimer config waypoint height <0.0-5.0>
/STimer config waypoint showCoords <true|false>
/STimer config waypoint showDistance <true|false>
/STimer config waypoint spawnGrid <1-64>
```

Also available as `/stimer`.

### Examples

```
/STimer set Spawner 90 1
/STimer set "Key Guardian" 5 minutes 2 true
/STimer remove 1
/STimer remove name "Key Guardian"
/STimer watch add "Key Guardian"
/STimer watch add "Boss Corleone"
/STimer watch intervals
/STimer watch intervals clear "Key Guardian"
/STimer config warningThreshold 15
/STimer config spawnAlarmDuration 5
/STimer config showClickHints true
/STimer config hotkeyResets false
/STimer config showWarningMessage false
/STimer config waypoint showCoords false
/STimer config waypoint showDistance true
/STimer config waypoint spawnGrid 32
```

## Behavior

### Timers
- HUD lists active timers with left-aligned names and right-aligned countdowns
- Color-coded bars: blue (active), yellow (warning), green (expired/UP), red (LIVE), gray (muted)
- **Left-click** a timer in the HUD (chat open) to mute/unmute it
- **Double left-click** a timer to reset it
- **Double right-click** a timer to remove it
- Hotkey (Ctrl/Cmd + slot number) restarts that timer (can be disabled via `hotkeyResets`, only slots 1–10)
- When a timer hits the warning threshold: turns yellow + full-screen alert + sound
- When a timer expires: displays "UP" + bell alarm for configurable duration
- Mute state persists across restarts

### Mob Detection
- Scans nearby entities and matches display names against your watchlist (case-insensitive partial match)
- Uses entity set differencing with retry queue (SkyHanni-style) and periodic full scan for reliable detection
- Field-of-view check: mobs are only detected when in front of you and within 128 blocks (no X-ray through walls or detection behind you)
- On detection: chat message + full-screen red alert + urgent two-tone alarm
- Waypoint placed at the mob's spawn location (doesn't follow the mob)
- Supports **multiple spawns** of the same mob type at different locations (dimension-aware)
- Coordinates shown in timer names (e.g. `Key Guardian [100, -200]`) — togglable via `showCoords`
- Duplicate instances auto-numbered when coords hidden (e.g. `Key Guardian (1)`, `Key Guardian (2)`)

### Spawn Tracking & Timer Lifecycle
- Tracks **kill-to-spawn** (respawn cooldown) and **spawn-to-spawn** (full cycle) intervals
- **LIVE → countdown → UP → LIVE** cycle:
  1. Mob spawns → timer shows **LIVE** (red bar) with waypoint at spawn location
  2. Mob killed → timer switches to respawn **countdown** (blue bar)
  3. Countdown expires → timer shows **UP** (green bar) — mob should be back
  4. Mob respawns → timer returns to **LIVE** (red bar)
- Intervals average over time for better accuracy
- Configurable **spawn grid size** (default 16 blocks) prevents duplicate timers from small coordinate differences
- All learned data persists to disk
- View and manage learned intervals with `/STimer watch intervals`
- Warning when all timer slots are full

## Build / Run

```bat
gradlew.bat runClient
gradlew.bat build
```

The built jar is under `build/libs/`.

## Optional: Mod Menu

Install [Mod Menu](https://modrinth.com/mod/modmenu) and [Cloth Config](https://modrinth.com/mod/cloth-config) to edit settings from the Mods screen with a GUI. Neither is required — `/STimer config` works without them.

## Tech Stack

- **Language**: Kotlin (JVM)
- **Build**: Gradle Kotlin DSL + Fabric Loom
- **Architecture**: Domain-driven package structure following SkyHanni conventions
- **Persistence**: `.properties` files in the config directory
