# SomeDummy

SomeDummy is a lightweight target dummy and damage testing plugin for PaperMC and Folia. It allows players and administrators to summon training dummies with universal mob model transformations, customizable armor and weapons, multiple physics modes (Dynamic, Floating, and Static Hard Freeze), floating damage indicators, real-time Actionbar DPS tracking, and placeable preset figurine items.

---
## Download

link
link
link

---
## Commands

| Command                                     | Permission        | Description                                                  |
| :------------------------------------------ | :---------------- | :----------------------------------------------------------- |
| `/somedummy` or `/sd`                       | `somedummy.use`   | Opens your active target dummy management list.             |
| `/sd list`                                  | `somedummy.use`   | Opens your active target dummy management list.             |
| `/sd wand`                                  | `somedummy.use`   | Gives you the dummy editing stick wand tool.                 |
| `/sd spawn [mob]`                           | `somedummy.spawn` | Spawns a target dummy at your feet.                          |
| `/sd presets`                               | `somedummy.preset`| Opens the preset dummy figurine template library.            |
| `/sd help`                                  | `somedummy.use`   | Displays the formatted command guide.                        |
| `/sd admin`                                 | `somedummy.admin` | Opens the server-wide dummy manager dashboard.               |
| `/sd config`                                | `somedummy.admin` | Opens the in-game settings control panel.                    |
| `/sd givepreset <player> <preset> [amount]` | `somedummy.admin` | Gives placeable dummy preset items to the specified player.  |
| `/sd reload`                                | `somedummy.admin` | Reloads `setting.conf` and preset templates from disk.       |
| `/sd purge`                                 | `somedummy.admin` | Permanently deletes all target dummies across all worlds.    |

---

## Permissions

| Permission             | Default | Description                                                                                                    |
| :--------------------- | :------ | :------------------------------------------------------------------------------------------------------------- |
| `somedummy.use`        | `true`  | Allows using base dummy commands (`/sd`, `/sd list`, `/sd wand`).                                              |
| `somedummy.spawn`      | `op`    | Allows spawning target dummies via `/sd spawn` when `allow-player-spawn` is false.                             |
| `somedummy.edit.own`   | `true`  | Allows editing own spawned dummies using the stick wand (subject to `player-editor-permissions` in config).    |
| `somedummy.edit.other` | `op`    | Allows editing or deleting other players' target dummies.                                                      |
| `somedummy.preset`     | `op`    | Allows placing preset figurine items and browsing the preset library.                                          |
| `somedummy.admin`      | `op`    | Grants full access to admin commands (`/sd admin`, `/sd config`, `/sd givepreset`, `/sd reload`, `/sd purge`). |

---

## Customizability & Support

### Customization Options
* Full text, name, and actionbar formatting via Kyori Adventure MiniMessage (HEX colors, gradients, bold).
* Universal mob transformation: Change dummy models to any living mob in the game, Armor Stands, or custom shapes.
* Multiple physics modes: Dynamic (reacts to gravity and knockback), Floating (zero gravity), and Static Hard Freeze (locked in space).
* Granular player permission toggles: 14 distinct configuration options allowing administrators to choose exactly which customization tools normal players can use.
* In-game settings control panel (`/sd config`) to toggle spawning rules, default attributes, and DPS indicators.
* Placeable preset figurine library: Save and distribute custom dummy loadouts as placeable inventory items.
* Floating damage indicators and real-time Actionbar DPS tracking with configurable formats and colors.

### System & Platform Support
* Built for PaperMC and Folia region-based multi-threading environments.
* Resource Pack Support: Built-in `custom-model-data` and custom font namespace parameters (`custom-ui-font`, e.g. `minecraft:uniform` or custom glyphs).
* Native TextDisplay entities for floating damage indicators with automatic lifecycle cleanup.
* Crash-safe asynchronous atomic disk storage with automatic entity restoration across server reboots.

---

## Developer API
* Direct Java access via `SomeDummyAPI` provider.
* Custom cancellable Bukkit events:
  * `DummySpawnEvent`
  * `DummyDamageEvent`
  * `DummyDeathEvent`
  * `DummyDespawnEvent`
  * `DummyEditEvent`

To integrate SomeDummy into your plugin, access the public API class:

```java
import com.someact.somedummy.api.SomeDummyAPI;
import com.someact.somedummy.model.DummyData;

// Retrieve all dummies owned by a player
List<DummyData> dummies = SomeDummyAPI.getDummiesForPlayer(player.getUniqueId());

// Spawn a dummy programmatically
DummyData dummy = SomeDummyAPI.spawnDummy(player, location);

// Listen to custom damage events
@EventHandler
public void onDummyDamage(DummyDamageEvent event) {
    Player damager = event.getDamager();
    DummyData dummy = event.getDummy();
    double damage = event.getDamage();
    // Custom logic here
}
```

---
