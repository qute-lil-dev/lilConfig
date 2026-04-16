# lilConfig — Developer Documentation

This document covers the full public API of lilConfig. For a quick overview see [README.md](README.md).

---

## Table of Contents

1. [Registration](#1-registration)
2. [Annotations](#2-annotations)
3. [Config Types](#3-config-types)
4. [Hotkeys](#4-hotkeys)
5. [Persistence](#5-persistence)
6. [i18n](#6-i18n)
7. [Vanilla Keybind Override](#7-vanilla-keybind-override)
8. [Advanced: Manual Provider](#8-advanced-manual-provider)

---

## 1. Registration

The entry point for all mods using lilConfig is `ConfigManager`. Call `register()` once from your `ClientModInitializer`.

```java
public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.getInstance().register(MyModConfig.class);
    }
}
```

`MyModConfig` must be annotated with `@Config`. The scanner reads all `public static` config fields, derives names from field names (UPPER_SNAKE_CASE → lowerCamelCase), and builds tabs and sections from the field annotations. Config values are loaded from disk immediately on `register()`.

---

## 2. Annotations

All annotations live in `net.lilfox.annotation`.

### `@Config`

Applied to the config **class**.

| Attribute     | Required | Description |
|---------------|----------|-------------|
| `modId`       | yes      | Used as the JSON file name (`config/<modId>.json`) and i18n key prefix |
| `displayName` | no       | Title shown in the config screen. Defaults to `modId`. |

```java
import net.lilfox.annotation.Config;

@Config(modId = "mymod", displayName = "My Mod")
public final class MyModConfig { ... }
```

### `@Tab`

Applied to a **field**. Assigns the field to a named GUI tab.

- Tab order follows the first occurrence of each id in declaration order.
- Fields without `@Tab` land in a `"misc"` tab appended at the end.

```java
@Tab("general")
public static ConfigBoolean showHud = ConfigBoolean.of(true);

@Tab("advanced")
public static ConfigInteger maxItems = ConfigInteger.of(64, 1, 256);
```

### `@Section`

Applied to a **field**. Inserts a bold section header above the annotated field (once per section name per tab).

```java
@Tab("general")
@Section("display")
public static ConfigBoolean showHud = ConfigBoolean.of(true);

@Tab("general")
@Section("network")
public static ConfigString serverUrl = ConfigString.of("localhost");
```

### `@MenuKey`

Applied to exactly one `ConfigHotkey` field. When this hotkey fires in-game, the config screen opens.

```java
@Tab("settings")
@MenuKey
public static ConfigHotkey menuKey = ConfigHotkey.of("LEFT_CONTROL, M");
```

If no field carries `@MenuKey`, the config screen is accessible only via Mod Menu.

### `@Hotkeyed`

Applied to a `ConfigBoolean` field. The scanner replaces the value with a `ConfigHotkeyedBoolean` at registration time, adding a rebind button in the GUI alongside the toggle. The field type stays `ConfigBoolean`, so only the boolean value is accessible in code.

```java
@Tab("hotkeys")
@Hotkeyed(defaultKey = "LEFT_CONTROL, H")
public static ConfigBoolean toggleHud = ConfigBoolean.of(false);

// Elsewhere in your code:
if (MyModConfig.toggleHud.getValue()) { ... }
```

`defaultKey` is a comma-separated list of GLFW key names. Leave it empty (`""`) for unbound by default.

### `@DevTab`

Applied to a **field**. The tab this field belongs to is hidden in production environments (i.e. when the game is not launched from a dev environment). Useful for exercising all widget types during development without exposing them to end users.

---

## 3. Config Types

All types live in `net.lilfox.config`. Every type is created via a static factory `Type.of(...)` and assigned to a `public static` field on the config class.

### Shared API (`ConfigBase<T>`)

Every config type inherits these methods:

```java
T getValue()              // current value
void setValue(T value)    // set value (marks dirty, fires onChange)
T getDefaultValue()       // default set at construction time
void resetToDefault()     // restore default value
boolean isModified()      // true if current value differs from default
boolean isDirty()         // true if value changed since last save

ConfigType withOnChange(Runnable listener)         // called when value changes
ConfigType withEffectButton(String labelKey, Runnable action) // extra action button in GUI
```

---

### `ConfigBoolean`

Toggle button. Serialized as a JSON `boolean`.

```java
public static ConfigBoolean showHud = ConfigBoolean.of(true);

// With a change listener:
public static ConfigBoolean showHud = ConfigBoolean.of(true)
        .withOnChange(() -> HudRenderer.setVisible(MyModConfig.showHud.getValue()));

// With an effect button (small button next to the toggle):
public static ConfigBoolean showHud = ConfigBoolean.of(true)
        .withEffectButton("mymod.config.reloadHud", () -> HudRenderer.reload());
```

The effect button label key is looked up in your mod's lang file.

---

### `ConfigHotkeyedBoolean`

Created automatically by `@Hotkeyed`. Not instantiated directly. Exposes the same `getValue()` / `setValue()` API as `ConfigBoolean`. The hotkey binding is managed by the player in the GUI.

---

### `ConfigHotkey`

A standalone hotkey — a rebind button in the GUI with no associated boolean. Use it for actions that are not toggle-based.

```java
public static ConfigHotkey openWaypoints = ConfigHotkey.of("LEFT_CONTROL, W");
```

Attach a callback to react when the hotkey fires:

```java
public static ConfigHotkey openWaypoints = ConfigHotkey.of("LEFT_CONTROL, W")
        .withCallback(() -> WaypointScreen.open());
```

Control when the hotkey is active:

```java
import net.lilfox.hotkey.HotkeyContext;

public static ConfigHotkey screenshot = ConfigHotkey.of("F2")
        .withHotkeyContext(HotkeyContext.IN_GAME);
        // HotkeyContext.GUI_OPEN — fires only when a screen is open
        // HotkeyContext.ALWAYS   — fires in both states
```

Default context is `IN_GAME` (fires only when no screen is open).

---

### `ConfigInteger`

Clamped integer text field. Arguments: `(defaultValue, min, max)`.

```java
public static ConfigInteger maxItems = ConfigInteger.of(64, 1, 256);

int limit = MyModConfig.maxItems.getValue();
```

---

### `ConfigDouble`

Clamped double text field. Arguments: `(defaultValue, min, max)`.

```java
public static ConfigDouble renderScale = ConfigDouble.of(1.0, 0.1, 4.0);

double scale = MyModConfig.renderScale.getValue();
```

---

### `ConfigString`

Free-form text field.

```java
public static ConfigString serverUrl = ConfigString.of("localhost");

String url = MyModConfig.serverUrl.getValue();
```

---

### `ConfigOptionList<E>`

Cycling button over a Java enum. The available options are all constants of the enum type.

```java
public enum Quality { LOW, MEDIUM, HIGH, ULTRA }

public static ConfigOptionList<Quality> quality = ConfigOptionList.of(Quality.MEDIUM);

Quality q = MyModConfig.quality.getValue();
```

---

## 4. Hotkeys

### Key syntax

`ConfigHotkey.of(String)` and `@Hotkeyed(defaultKey = "...")` accept a comma-separated list of GLFW key names (without the `GLFW_KEY_` prefix, case-insensitive):

```java
ConfigHotkey.of("LEFT_CONTROL, LEFT_SHIFT, L")  // Ctrl+Shift+L
ConfigHotkey.of("F2")                            // single key
ConfigHotkey.of("")                              // unbound
```

Up to 4 keys per binding. Key names are standard GLFW constants, e.g. `A`–`Z`, `F1`–`F25`, `LEFT_CONTROL`, `LEFT_SHIFT`, `LEFT_ALT`, `SPACE`, `ENTER`, etc. Mouse buttons are assigned in the GUI by clicking them during rebind mode.

### Firing semantics

- **Fire-on-press (leading edge):** the hotkey fires on the tick when all its keys first become simultaneously held. Holding the keys does not repeat-fire.
- **Order-aware:** keys must be pressed in the order they appear in the binding (i.e. the order they were recorded in the GUI). Pressing them out of order does not fire.
- **Superset-inhibited:** if another binding that shares at least one key with binding A is currently fully held and is not a subset of A, then A is suppressed. Example: `Ctrl+Shift+L` will not accidentally trigger a separate `Ctrl+L` binding.

### Conflict detection

`ConfigManager` can report bindings that overlap with a given `KeyBind`:

```java
KeyBind kb = myHotkey.getKeyBind();
List<ConfigManager.ConflictEntry> conflicts =
        ConfigManager.getInstance().getConflicts(kb, myHotkey);

for (var c : conflicts) {
    System.out.printf("Conflict with %s / %s: %s%n",
            c.modName(), c.groupName(), c.keyDisplay());
}
```

The GUI uses this API to highlight conflicting entries in red automatically.

---

## 5. Persistence

Config values are loaded from `config/<modId>.json` when `register()` is called and saved when the config screen is closed or the client shuts down. Manual calls to `ConfigManager.getInstance().saveAll()` are also possible but rarely needed.

The JSON structure groups values by tab name. Field names are stored in lowerCamelCase (UPPER_SNAKE_CASE field names are converted automatically by the scanner):

```json
{
  "general": {
    "showHud": true,
    "maxItems": 64
  },
  "hotkeys": {
    "toggleHud": {
      "value": false,
      "key": "KEYSYM:341+KEYSYM:72"
    },
    "openWaypoints": "KEYSYM:341+KEYSYM:87"
  }
}
```

- `ConfigHotkeyedBoolean` serializes as `{"value": boolean, "key": "..."}`.
- `ConfigHotkey` serializes as a plain string in `TYPE:code+TYPE:code` format.
- Unknown keys on load are silently ignored.

---

## 6. i18n

lilConfig resolves display names via `Component.translatable()`. Add keys to your mod's lang file at `assets/<modid>/lang/en_us.json`.

### Key patterns

| What | Key pattern | Example |
|------|-------------|---------|
| Config entry label | `<modId>.settings.<fieldCamelCase>` | `mymod.settings.showHud` |
| Tooltip body | `<modId>.settings.<fieldCamelCase>.desc` | `mymod.settings.showHud.desc` |
| In-game notification name | `<modId>.settings.<fieldCamelCase>.nice` | `mymod.settings.showHud.nice` |
| Input hint (int/double/string) | `<modId>.settings.<fieldCamelCase>.inputDesc` | `mymod.settings.maxItems.inputDesc` |
| Hotkey button tooltip | `<modId>.settings.<fieldCamelCase>.hotkeyDesc` | `mymod.settings.toggleHud.hotkeyDesc` |
| Tab label | `<modId>.tabs.<tabId>` | `mymod.tabs.general` |
| Section header | `<modId>.sections.<sectionId>` | `mymod.sections.display` |

Only the entry label is required. All other keys are optional — if absent, the tooltip or notification is simply not shown.

### Example

For this config class:

```java
@Config(modId = "mymod")
public final class MyModConfig {

    @Tab("general")
    @Section("display")
    public static ConfigBoolean showHud = ConfigBoolean.of(true);

    @Tab("general")
    public static ConfigInteger maxItems = ConfigInteger.of(64, 1, 256);
}
```

Add to `assets/mymod/lang/en_us.json`:

```json
{
  "mymod.tabs.general":          "General",
  "mymod.sections.display":      "Display",
  "mymod.settings.showHud":      "Show HUD",
  "mymod.settings.showHud.desc": "Renders the overlay on screen.",
  "mymod.settings.maxItems":     "Max Items",
  "mymod.settings.maxItems.inputDesc": "Number of items to display at once."
}
```

If a label key is missing, lilConfig falls back to displaying the raw field name.

---

## 7. Vanilla Keybind Override

lilConfig can extend or replace Minecraft's Controls screen so that vanilla keybinds also support multi-key combos. This feature is opt-in and off by default.

Enable it in the lilConfig settings screen (Ctrl+Shift+L by default) by toggling **Vanilla Key Override** on.

Two UI variants are available:

| Variant | Setting | Behaviour |
|---------|---------|-----------|
| B | `vanillaUiEmbed = false` (default) | Controls screen is replaced by a flat `ConfigScreen` showing all vanilla bindings. Full multi-key recording. |
| C | `vanillaUiEmbed = true` | Stock Controls screen opens normally. Key input is intercepted to accumulate multi-key combos; button labels update to show the full combo. |

Vanilla bindings under this feature are persisted separately to `config/lilconfig_vanilla_keys.json`.

---

## 8. Advanced: Manual Provider

> **Note:** `IConfigProvider` is a public-but-internal interface. It may become package-private in a future release. Prefer the annotation API for new integrations.

If the annotation API does not fit your use case (e.g. dynamically built config groups), implement `IConfigProvider` directly:

```java
public class MyModProvider implements IConfigProvider {

    public static final ConfigBoolean showHud   = new ConfigBoolean("showHud", true);
    public static final ConfigInteger maxItems  = new ConfigInteger("maxItems", 64, 1, 256);

    private final List<ConfigGroup> groups;

    public MyModProvider() {
        groups = List.of(new ConfigGroup("general", List.of(showHud, maxItems)));
    }

    @Override public String getModId()                      { return "mymod"; }
    @Override public String getDisplayName()                { return "My Mod"; }
    @Override public List<ConfigGroup> getConfigGroups()    { return groups; }
    @Override public KeyBind getMenuOpenKey()               { return KeyBind.NONE; }
}
```

Register it:

```java
ConfigManager.getInstance().register(new MyModProvider());
```

`ConfigGroup` maps directly to a tab in the GUI. Its name is used as the tab label translation key (`<modId>.tabs.<groupName>`).
