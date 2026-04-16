# lilConfig

Fabric mod that brings multi-key hotkeys to vanilla Controls — and a config library for developers who want the same for their own mods.

---

## The problem

Minecraft's Controls screen only supports single-key bindings. The only Fabric library that solves this is malilib — but it's large, pulls in features most mods don't need, and has no developer documentation.

lilConfig fills that gap: it's a focused library for multi-key hotkeys and mod config screens, with a documented API and no malilib dependency.

---

## For players and modpack makers

With lilConfig installed, the vanilla Controls screen gains full multi-key combo support. Bindings like `Ctrl+Shift+F3` work out of the box for any key in the game, not just mods that explicitly use lilConfig.

When many mods are installed, single-key bindings run out fast and conflicts are nearly impossible to avoid. Multi-key combos give you a vastly larger space of unique bindings, making conflict-free modpacks practical instead of painful.

Mods built on lilConfig also keep all their settings — toggles, values, and hotkeys — in a single unified screen, instead of splitting between a custom config GUI and the vanilla Controls window.

---

## For mod developers

If your mod has hotkeys or config options that players need to adjust, lilConfig gives you:

- **Multi-key hotkeys** with fire-on-press, order-aware, and superset-inhibited semantics — the same model malilib uses
- **A config screen** where hotkeys live alongside all other settings, with tabs, sections, and automatic JSON persistence
- **A documented API** — annotation-driven, one class + one method call to get a fully functional GUI

**Conflict detection** shows which bindings overlap and highlights them in the GUI, so players can resolve clashes without guesswork.

The only alternative that provides all of this is malilib. lilConfig offers the same core functionality at a fraction of the footprint, with actual documentation.

---

## Features

- Multi-key hotkey combos (up to 4 keys, including mouse buttons) for vanilla and mod bindings
- Config screen with tabs, sections, and scale-adaptive layout
- 8 config value types: boolean, hotkeyed boolean, hotkey, integer, double, string, option list, separator
- Conflict detection with visual highlighting
- Automatic JSON persistence (`config/<modId>.json`)
- Optional Mod Menu integration
- Full i18n support via standard lang files
- MIT license

---

## Quick Start

```java
@Config(modId = "mymod", displayName = "My Mod")
public final class MyModConfig {

    @Tab("general")
    public static ConfigBoolean showHud = ConfigBoolean.of(true);

    @Tab("general")
    public static ConfigInteger maxItems = ConfigInteger.of(64, 1, 256);

    @Tab("hotkeys")
    @Hotkeyed(defaultKey = "LEFT_CONTROL, H")
    public static ConfigBoolean toggleHud = ConfigBoolean.of(false);

    @Tab("hotkeys")
    @MenuKey
    public static ConfigHotkey menuKey = ConfigHotkey.of("LEFT_CONTROL, M");

    private MyModConfig() {}
}
```

```java
// In ClientModInitializer:
ConfigManager.getInstance().register(MyModConfig.class);
```

```java
// Read anywhere:
if (MyModConfig.showHud.getValue()) { ... }
```

See [DOCUMENTATION.md](DOCUMENTATION.md) for the full API reference.

---

## Requirements

| Dependency     | Version        | Required |
|----------------|----------------|----------|
| Minecraft      | 26.1           | Yes      |
| Fabric Loader  | ≥ 0.18.5       | Yes      |
| Fabric API     | 0.144.3+26.1   | Yes      |
| Java           | 25+            | Yes      |
| Mod Menu       | any            | No       |

---

## Installation

Place the jar from [Releases](https://github.com/lil-fox/lilconfig/releases) in your `mods/` folder.

To use as a library dependency, add to `fabric.mod.json`:

```json
"depends": {
    "lilconfig": "*"
}
```

---

## Building from Source

```bash
./gradlew build        # compile + package → build/libs/lilconfig-*.jar
./gradlew genSources   # decompile Minecraft sources for IDE navigation
./gradlew runClient    # launch Minecraft client with the mod loaded
```

---

## License

[MIT](LICENSE) — © lil-fox
