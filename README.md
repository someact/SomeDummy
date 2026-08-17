# SomeDummy

A versatile target dummy and combat damage testing plugin designed for PaperMC servers running Minecraft 1.20.5 through 1.21.4 and 26.2+ on Java 21 or newer.

SomeDummy allows players and administrators to summon fully customizable combat dummies for weapon testing, DPS measuring, RPG training arenas, or decorative mannequins. It features universal mob transformation, floating damage indicators, real-time Actionbar metrics, placeable preset figurines, and granular physics controls.

---

## Features

### Target Customization and Transformation
* Universal Entity Types: Transform dummies into any vanilla mob (Zombie, Skeleton, Warden, Iron Golem, Villager, Pillager, etc.), Armor Stand, or Display Entity.
* 54-Slot Interactive Wand Editor: Shift and right-click any dummy with a configurable wand item (default: stick) to open the comprehensive attribute and behavior editor.
* Custom Equipment: Equip custom armor sets, weapons, shields, and off-hand items to test armor penetration, enchantments, and defense values.
* Infinite Health / DPS Mode: Dummies can absorb unlimited damage to measure continuous DPS metrics without dying.
* Sunlight Combustion Toggle: Enable or disable daytime sunlight burning for undead mobs (Zombies, Skeletons, Phantoms) per dummy or globally in `setting.conf`.

### 3-Way Physics and Movement Modes
1. Dynamic Mode (Natural Physics): Subject to standard gravity, water flow, and natural combat knockback.
2. Floating Mode (Zero-G): Suspends the dummy mid-air for aerial and archery testing while still responding to attacks.
3. Static Mode (Hard Freeze): Locks the dummy completely in 3D space with zero gravity and 100% knockback immunity, creating an immovable training statue.

### Combat Analytics and Damage Indicators
* Floating Damage Pop-ups: Displays animated floating text above the dummy on each hit with distinct styles for normal and critical hits.
* Live Actionbar DPS Counter: Shows real-time combat statistics during attack sessions, including recent hit damage, DPS, total damage, and hit counts.

### Presets and Placeable Figurines
* Export to Item: Convert any configured dummy into a placeable figurine item that can be stored in player inventories or placed in arenas.
* Built-In Preset Library: Includes pre-configured templates like Standard Target Dummy, Heavy Armored Dummy, and Boss Training Dummy.
* Dual Player & Admin GUIs:
  * `/sd list`: Player interface to manage and locate personal dummies.
  * `/sd admin`: Server dashboard for inspecting, teleporting to, or purging all dummies across all worlds.

### Developer API
* Direct Java access via `SomeDummyAPI` provider.
* Custom cancellable Bukkit events:
  * `DummySpawnEvent`
  * `DummyDamageEvent`
  * `DummyDeathEvent`
  * `DummyDespawnEvent`
  * `DummyEditEvent`

---

## Requirements

* Server Software: Paper, Purpur, or Folia (Version 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 26.2+)
* Java Runtime: Java 21 or higher (JDK 21+)

---

## Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/somedummy` or `/sd` | `somedummy.use` | Opens the active dummy management menu. |
| `/sd help` | `somedummy.use` | Displays the command help guide. |
| `/sd spawn [entity_type]` | `somedummy.spawn` | Spawns a target dummy at your current position. |
| `/sd list` | `somedummy.use` | Opens the player's personal dummy manager. |
| `/sd presets` | `somedummy.preset` | Opens the preset template library. |
| `/sd admin` | `somedummy.admin` | Opens the server-wide dummy manager dashboard. |
| `/sd config` | `somedummy.admin` | Opens the in-game configuration editor. |
| `/sd reload` | `somedummy.admin` | Reloads `setting.conf` from disk. |
| `/sd purge [world]` | `somedummy.admin` | Despawns and removes all active dummies. |

---

## Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `somedummy.use` | `true` | Allows using base dummy commands and viewing own dummies. |
| `somedummy.spawn` | `true` | Allows spawning target dummies. |
| `somedummy.edit.own` | `true` | Allows editing own spawned dummies with the wand tool. |
| `somedummy.edit.other` | `op` | Allows editing or deleting other players' dummies. |
| `somedummy.preset` | `true` | Allows using placeable dummy preset figurines and browsing presets. |
| `somedummy.admin` | `op` | Full access to administrative commands (`/sd admin`, `/sd config`, `/sd reload`, `/sd purge`). |

---

## Developer API

To integrate SomeDummy into your plugin:

```java
import com.someact.somedummy.api.SomeDummyAPI;
import com.someact.somedummy.model.DummyData;

// Spawn a dummy programmatically
DummyData dummy = SomeDummyAPI.spawnDummy(player, location);

// Tweak dummy parameters
dummy.setEntityType(EntityType.WARDEN);
dummy.setMaxHealth(5000.0);
dummy.setStatic(true);
SomeDummyAPI.updateDummy(dummy);

// Listen to combat damage events
@EventHandler
public void onDummyDamage(DummyDamageEvent event) {
    Player damager = event.getDamager();
    double damage = event.getDamage();
    boolean isCrit = event.isCrit();
    // Custom logic here
}
```

---

## Building from Source

Clone the repository and compile using Gradle:

```bash
git clone https://github.com/someact/SomeDummy.git
cd SomeDummy
./gradlew build
```

The compiled JAR file will be generated at `build/libs/SomeDummy-1.0.0.jar`.

---

## Author

Created and maintained by **someact**.
