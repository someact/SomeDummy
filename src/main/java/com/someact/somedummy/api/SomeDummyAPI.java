package com.someact.somedummy.api;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.model.DummyData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public developer API for interacting with SomeDummy target dummies.
 */
public class SomeDummyAPI {

    private static SomeDummyPlugin plugin;

    public static void setPlugin(SomeDummyPlugin instance) {
        plugin = instance;
    }

    public static SomeDummyPlugin getPlugin() {
        if (plugin == null) {
            throw new IllegalStateException("SomeDummyAPI has not been initialized yet!");
        }
        return plugin;
    }

    public static DummyData getDummy(UUID dummyId) {
        return getPlugin().getStorageManager().getDummy(dummyId);
    }

    public static DummyData getDummyByEntity(Entity entity) {
        return getPlugin().getEntityManager().getDummyFromEntity(entity);
    }

    public static List<DummyData> getDummiesForPlayer(UUID playerUuid) {
        return getPlugin().getStorageManager().getDummiesForPlayer(playerUuid);
    }

    public static Collection<DummyData> getAllDummies() {
        return getPlugin().getStorageManager().getAllDummies();
    }

    public static DummyData spawnDummy(Player player, Location location) {
        return getPlugin().getEntityManager().spawnDummy(player, location);
    }

    public static void removeDummy(DummyData dummy) {
        getPlugin().getEntityManager().removeDummy(dummy);
    }
}
