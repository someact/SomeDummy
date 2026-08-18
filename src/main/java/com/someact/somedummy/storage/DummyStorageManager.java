package com.someact.somedummy.storage;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.model.DummyData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise storage manager for persistent target dummies with atomic file writes.
 */
public class DummyStorageManager {

    private final SomeDummyPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, DummyData> dummiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> entityToDummyMap = new ConcurrentHashMap<>();

    public DummyStorageManager(SomeDummyPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
    }

    public void init() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        loadAllDummies();
    }

    public void registerEntity(UUID entityUuid, UUID dummyId) {
        entityToDummyMap.put(entityUuid, dummyId);
    }

    public void unregisterEntity(UUID entityUuid) {
        entityToDummyMap.remove(entityUuid);
    }

    public DummyData getDummyByEntityUuid(UUID entityUuid) {
        UUID dummyId = entityToDummyMap.get(entityUuid);
        return dummyId != null ? getDummy(dummyId) : null;
    }

    public DummyData getDummy(UUID dummyId) {
        return dummiesById.get(dummyId);
    }

    public void addDummy(DummyData dummy) {
        dummiesById.put(dummy.getDummyId(), dummy);
        saveDummyAsync(dummy);
    }

    public void removeDummy(DummyData dummy) {
        dummiesById.remove(dummy.getDummyId());
        if (dummy.getCurrentEntityUuid() != null) {
            entityToDummyMap.remove(dummy.getCurrentEntityUuid());
        }

        File file = new File(dataFolder, dummy.getDummyId().toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public List<DummyData> getDummiesForPlayer(UUID playerUuid) {
        List<DummyData> list = new ArrayList<>();
        for (DummyData d : dummiesById.values()) {
            if (d.getOwnerUuid().equals(playerUuid)) {
                list.add(d);
            }
        }
        list.sort((a, b) -> Long.compare(b.getCreatedAtMillis(), a.getCreatedAtMillis()));
        return list;
    }

    public Collection<DummyData> getAllDummies() {
        return dummiesById.values();
    }

    public void loadAllDummies() {
        dummiesById.clear();
        entityToDummyMap.clear();

        File[] files = dataFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                UUID dummyId = UUID.fromString(yaml.getString("dummyId"));
                UUID ownerUuid = UUID.fromString(yaml.getString("ownerUuid"));
                String ownerName = yaml.getString("ownerName", "Player");

                String worldName = yaml.getString("location.world", "world");
                double x = yaml.getDouble("location.x");
                double y = yaml.getDouble("location.y");
                double z = yaml.getDouble("location.z");
                float yaw = (float) yaml.getDouble("location.yaw", 0.0);
                float pitch = (float) yaml.getDouble("location.pitch", 0.0);

                DummyData data = new DummyData(dummyId, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch);
                data.setEntityType(EntityType.valueOf(yaml.getString("entityType", "ZOMBIE")));
                data.setCustomName(yaml.getString("customName"));
                data.setMaxHealth(yaml.getDouble("maxHealth", 1000.0));
                data.setInfiniteHealth(yaml.getBoolean("infiniteHealth", true));
                data.setHasAi(yaml.getBoolean("hasAi", false));
                data.setHasGravity(yaml.getBoolean("hasGravity", true));
                data.setSilent(yaml.getBoolean("isSilent", true));
                data.setSmall(yaml.getBoolean("isSmall", false));
                data.setInvulnerable(yaml.getBoolean("isInvulnerable", false));
                data.setRespawnable(yaml.getBoolean("respawnable", true));
                data.setRespawnDelaySeconds(yaml.getInt("respawnDelaySeconds", 3));
                data.setLifespanSeconds(yaml.getLong("lifespanSeconds", 0L));
                data.setBurnInSun(yaml.getBoolean("burnInSun", false));
                data.setStatic(yaml.getBoolean("isStatic", false));

                data.setHelmet(deserializeItem(yaml.getString("equipment.helmet")));
                data.setChestplate(deserializeItem(yaml.getString("equipment.chestplate")));
                data.setLeggings(deserializeItem(yaml.getString("equipment.leggings")));
                data.setBoots(deserializeItem(yaml.getString("equipment.boots")));
                data.setMainHand(deserializeItem(yaml.getString("equipment.mainHand")));
                data.setOffHand(deserializeItem(yaml.getString("equipment.offHand")));

                dummiesById.put(dummyId, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load dummy from " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + dummiesById.size() + " target dummies from disk.");
    }

    public void saveDummyAsync(DummyData dummy) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> saveDummySync(dummy));
    }

    public void saveDummySync(DummyData dummy) {
        File file = new File(dataFolder, dummy.getDummyId().toString() + ".yml");
        File tempFile = new File(dataFolder, dummy.getDummyId().toString() + ".tmp");

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("dummyId", dummy.getDummyId().toString());
        yaml.set("ownerUuid", dummy.getOwnerUuid().toString());
        yaml.set("ownerName", dummy.getOwnerName());
        yaml.set("location.world", dummy.getWorldName());
        yaml.set("location.x", dummy.getX());
        yaml.set("location.y", dummy.getY());
        yaml.set("location.z", dummy.getZ());
        yaml.set("location.yaw", dummy.getYaw());
        yaml.set("location.pitch", dummy.getPitch());

        yaml.set("entityType", dummy.getEntityType().name());
        yaml.set("customName", dummy.getCustomName());
        yaml.set("maxHealth", dummy.getMaxHealth());
        yaml.set("infiniteHealth", dummy.isInfiniteHealth());
        yaml.set("hasAi", dummy.hasAi());
        yaml.set("hasGravity", dummy.hasGravity());
        yaml.set("isSilent", dummy.isSilent());
        yaml.set("isSmall", dummy.isSmall());
        yaml.set("isInvulnerable", dummy.isInvulnerable());
        yaml.set("respawnable", dummy.isRespawnable());
        yaml.set("respawnDelaySeconds", dummy.getRespawnDelaySeconds());
        yaml.set("lifespanSeconds", dummy.getLifespanSeconds());
        yaml.set("burnInSun", dummy.isBurnInSun());
        yaml.set("isStatic", dummy.isStatic());

        yaml.set("equipment.helmet", serializeItem(dummy.getHelmet()));
        yaml.set("equipment.chestplate", serializeItem(dummy.getChestplate()));
        yaml.set("equipment.leggings", serializeItem(dummy.getLeggings()));
        yaml.set("equipment.boots", serializeItem(dummy.getBoots()));
        yaml.set("equipment.mainHand", serializeItem(dummy.getMainHand()));
        yaml.set("equipment.offHand", serializeItem(dummy.getOffHand()));

        try {
            yaml.save(tempFile);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save dummy file: " + e.getMessage());
        }
    }

    public void saveAllSync() {
        for (DummyData d : dummiesById.values()) {
            saveDummySync(d);
        }
    }

    private String serializeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            byte[] bytes = ItemStack.serializeItemsAsBytes(List.of(item));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack deserializeItem(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ItemStack[] deserialized = ItemStack.deserializeItemsFromBytes(bytes);
            return (deserialized.length > 0) ? deserialized[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
}
