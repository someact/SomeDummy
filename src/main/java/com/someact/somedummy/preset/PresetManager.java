package com.someact.somedummy.preset;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages placeable target dummy preset templates and inventory item figurines.
 */
public class PresetManager {

    private final SomeDummyPlugin plugin;
    private final File presetsFolder;
    private final NamespacedKey presetPdcKey;
    private final Map<String, DummyData> presets = new ConcurrentHashMap<>();

    public PresetManager(SomeDummyPlugin plugin) {
        this.plugin = plugin;
        this.presetsFolder = new File(plugin.getDataFolder(), "presets");
        this.presetPdcKey = new NamespacedKey(plugin, "dummy_preset_id");
    }

    public void init() {
        if (!presetsFolder.exists()) {
            presetsFolder.mkdirs();
            createDefaultPresets();
        }
        loadAllPresets();
    }

    public NamespacedKey getPresetPdcKey() {
        return presetPdcKey;
    }

    public void savePreset(String id, DummyData data) {
        presets.put(id.toLowerCase(), data);
        File file = new File(presetsFolder, id.toLowerCase() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", data.getCustomName());
        yaml.set("entityType", data.getEntityType().name());
        yaml.set("maxHealth", data.getMaxHealth());
        yaml.set("infiniteHealth", data.isInfiniteHealth());
        yaml.set("hasAi", data.hasAi());
        yaml.set("hasGravity", data.hasGravity());
        yaml.set("isSilent", data.isSilent());
        yaml.set("isSmall", data.isSmall());
        yaml.set("respawnable", data.isRespawnable());
        yaml.set("respawnDelaySeconds", data.getRespawnDelaySeconds());
        yaml.set("burnInSun", data.isBurnInSun());
        yaml.set("isStatic", data.isStatic());

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save preset " + id + ": " + e.getMessage());
        }
    }

    public DummyData getPreset(String id) {
        return presets.get(id.toLowerCase());
    }

    public Map<String, DummyData> getAllPresets() {
        return presets;
    }

    public void loadAllPresets() {
        presets.clear();
        File[] files = presetsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            String id = f.getName().replace(".yml", "").toLowerCase();
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                DummyData data = new DummyData(UUID.randomUUID(), UUID.randomUUID(), "Preset", null);
                data.setCustomName(yaml.getString("name", "Target Dummy"));
                data.setEntityType(EntityType.valueOf(yaml.getString("entityType", "ZOMBIE")));
                data.setMaxHealth(yaml.getDouble("maxHealth", 1000.0));
                data.setInfiniteHealth(yaml.getBoolean("infiniteHealth", true));
                data.setHasAi(yaml.getBoolean("hasAi", false));
                data.setHasGravity(yaml.getBoolean("hasGravity", true));
                data.setSilent(yaml.getBoolean("isSilent", true));
                data.setSmall(yaml.getBoolean("isSmall", false));
                data.setRespawnable(yaml.getBoolean("respawnable", true));
                data.setRespawnDelaySeconds(yaml.getInt("respawnDelaySeconds", 3));
                data.setBurnInSun(yaml.getBoolean("burnInSun", false));
                data.setStatic(yaml.getBoolean("isStatic", false));

                presets.put(id, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load preset " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    public ItemStack createPresetItem(String presetId, int amount) {
        DummyData data = getPreset(presetId);
        if (data == null) return null;

        Material icon = Material.ARMOR_STAND;
        String mobName = data.getEntityType().name();

        return ItemBuilder.from(icon, amount)
                .name("<gradient:#ff7675:#fab1a0><bold>" + data.getCustomName() + "</bold></gradient>")
                .loreStrings(List.of(
                        "<gray>Type: <gold>" + mobName + "</gold></gray>",
                        "<gray>Health: <green>" + (data.isInfiniteHealth() ? "Infinite" : data.getMaxHealth()) + "</green></gray>",
                        "<gray>AI: <white>" + (data.hasAi() ? "Enabled" : "Disabled (Frozen)") + "</white></gray>",
                        "",
                        "<yellow>[Right-Click Ground to Place Dummy]</yellow>"
                ))
                .glow(true)
                .pdc(presetPdcKey, PersistentDataType.STRING, presetId.toLowerCase())
                .build();
    }

    public boolean isPresetItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(presetPdcKey, PersistentDataType.STRING);
    }

    public String getPresetIdFromItem(ItemStack item) {
        if (!isPresetItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(presetPdcKey, PersistentDataType.STRING);
    }

    private void createDefaultPresets() {
        // 1. Standard Target Dummy
        DummyData standard = new DummyData(UUID.randomUUID(), UUID.randomUUID(), "System", null);
        standard.setCustomName("<gold><bold>Standard Target Dummy</bold></gold>");
        standard.setEntityType(EntityType.ZOMBIE);
        standard.setMaxHealth(1000.0);
        standard.setInfiniteHealth(true);
        standard.setHasAi(false);
        savePreset("standard", standard);

        // 2. Tank Heavy Dummy
        DummyData tank = new DummyData(UUID.randomUUID(), UUID.randomUUID(), "System", null);
        tank.setCustomName("<gradient:#3498db:#9b59b6><bold>Heavy Armored Dummy</bold></gradient>");
        tank.setEntityType(EntityType.IRON_GOLEM);
        tank.setMaxHealth(5000.0);
        tank.setInfiniteHealth(true);
        tank.setHasAi(false);
        savePreset("tank", tank);

        // 3. Boss Target Dummy
        DummyData boss = new DummyData(UUID.randomUUID(), UUID.randomUUID(), "System", null);
        boss.setCustomName("<gradient:#e74c3c:#c0392b><bold>Boss Training Dummy</bold></gradient>");
        boss.setEntityType(EntityType.WARDEN);
        boss.setMaxHealth(10000.0);
        boss.setInfiniteHealth(true);
        boss.setHasAi(false);
        savePreset("boss", boss);
    }
}
