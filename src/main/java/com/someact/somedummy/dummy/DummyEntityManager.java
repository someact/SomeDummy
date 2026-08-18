package com.someact.somedummy.dummy;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.api.event.DummyDespawnEvent;
import com.someact.somedummy.api.event.DummySpawnEvent;
import com.someact.somedummy.config.ConfigManager;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.sound.SoundManager;
import com.someact.somedummy.storage.DummyStorageManager;
import com.someact.somedummy.util.MessageUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages physical spawning, attribute customization, equipment, and damage pop-up animations for target dummies.
 */
public class DummyEntityManager {

    private final SomeDummyPlugin plugin;
    private final ConfigManager config;
    private final DummyStorageManager storage;
    private final SoundManager soundManager;
    private final NamespacedKey dummyPdcKey;
    private ScheduledTask tickerTask;

    public DummyEntityManager(SomeDummyPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.storage = plugin.getStorageManager();
        this.soundManager = plugin.getSoundManager();
        this.dummyPdcKey = new NamespacedKey(plugin, "dummy_id");
    }

    public void start() {
        if (tickerTask != null) tickerTask.cancel();

        // Restore all dummies on startup
        for (DummyData dummy : storage.getAllDummies()) {
            if (dummy.getLocation() != null && dummy.getLocation().getWorld() != null) {
                Bukkit.getRegionScheduler().run(plugin, dummy.getLocation(), task -> {
                    spawnDummyEntity(dummy);
                });
            }
        }

        // Periodic ticker for lifespan expiration
        this.tickerTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (DummyData dummy : storage.getAllDummies()) {
                if (dummy.isExpired()) {
                    Location loc = dummy.getLocation();
                    if (loc != null && loc.getWorld() != null) {
                        Bukkit.getRegionScheduler().run(plugin, loc, t -> removeDummy(dummy));
                    } else {
                        storage.removeDummy(dummy);
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }


    public void stop() {
        if (tickerTask != null) tickerTask.cancel();

        // Clean up spawned entities
        for (DummyData dummy : storage.getAllDummies()) {
            if (dummy.getCurrentEntityUuid() != null) {
                Entity e = Bukkit.getEntity(dummy.getCurrentEntityUuid());
                if (e != null && e.isValid()) {
                    e.remove();
                }
            }
        }
    }

    public DummyData spawnDummy(Player player, Location location) {
        int max = config.getMaxDummiesPerPlayer();
        if (max > 0 && !player.hasPermission("somedummy.admin")) {
            int current = storage.getDummiesForPlayer(player.getUniqueId()).size();
            if (current >= max) {
                TagResolver res = Placeholder.parsed("max_dummies", String.valueOf(max));
                MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("dummy-limit-reached",
                        "<red>You have reached your maximum limit of <gold><max_dummies></gold> active dummies!</red>"), res);
                return null;
            }
        }

        DummyData dummy = new DummyData(UUID.randomUUID(), player.getUniqueId(), player.getName(), location);
        dummy.setEntityType(config.getDefaultEntityType());
        dummy.setCustomName(config.getDefaultCustomName());
        dummy.setMaxHealth(config.getDefaultMaxHealth());
        dummy.setInfiniteHealth(config.isDefaultInfiniteHealth());
        dummy.setHasAi(config.isDefaultHasAi());
        dummy.setHasGravity(config.isDefaultHasGravity());
        dummy.setSilent(config.isDefaultSilent());
        dummy.setRespawnable(config.isDefaultRespawnable());
        dummy.setRespawnDelaySeconds(config.getDefaultRespawnDelay());
        dummy.setLifespanSeconds(config.getDefaultLifespan());
        dummy.setBurnInSun(config.isDefaultBurnInSun());
        dummy.setStatic(config.isDefaultStatic());

        DummySpawnEvent event = new DummySpawnEvent(player, dummy);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        storage.addDummy(dummy);

        Bukkit.getRegionScheduler().run(plugin, location, t -> {
            spawnDummyEntity(dummy);
        });

        soundManager.playSound(player, "dummy-spawn", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        TagResolver res = TagResolver.resolver(
                Placeholder.parsed("name", dummy.getCustomName()),
                Placeholder.parsed("x", String.valueOf(location.getBlockX())),
                Placeholder.parsed("y", String.valueOf(location.getBlockY())),
                Placeholder.parsed("z", String.valueOf(location.getBlockZ()))
        );
        MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("dummy-spawned",
                "<green>Spawned target dummy <gold><name></gold> at <yellow><x>, <y>, <z></yellow>!</green>"), res);

        return dummy;
    }

    public void spawnDummyEntity(DummyData dummy) {
        Location loc = dummy.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        // Remove old entity if tracked
        if (dummy.getCurrentEntityUuid() != null) {
            Entity old = Bukkit.getEntity(dummy.getCurrentEntityUuid());
            if (old != null && old.isValid()) {
                old.remove();
            }
        }

        // Clean up any lingering entity with this dummyId in the area
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
            if (e.getPersistentDataContainer().has(dummyPdcKey, PersistentDataType.STRING)) {
                String idStr = e.getPersistentDataContainer().get(dummyPdcKey, PersistentDataType.STRING);
                if (dummy.getDummyId().toString().equals(idStr)) {
                    e.remove();
                }
            }
        }

        EntityType type = dummy.getEntityType();
        Class<? extends Entity> entityClass = type.getEntityClass();
        if (entityClass == null) entityClass = Zombie.class;

        Entity spawned = loc.getWorld().spawn(loc, entityClass, entity -> {
            entity.getPersistentDataContainer().set(dummyPdcKey, PersistentDataType.STRING, dummy.getDummyId().toString());

            // Display Name
            Component nameComp = MessageUtil.parse(dummy.getCustomName(), Placeholder.parsed("owner_name", dummy.getOwnerName()));
            entity.customName(nameComp);
            entity.setCustomNameVisible(true);

            entity.setSilent(dummy.isSilent());
            entity.setInvulnerable(dummy.isInvulnerable());
            entity.setPersistent(true);

            if (dummy.isStatic()) {
                // Static Hard Freeze mode: completely locked in space, zero gravity, no movement
                entity.setGravity(false);
                if (entity instanceof LivingEntity living) {
                    living.setAI(false);
                    AttributeInstance knockAttr = living.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                    if (knockAttr != null) knockAttr.setBaseValue(1.0);
                }
            } else {
                // Dynamic physics mode: reacts to gravity, knockback, water, etc.
                entity.setGravity(dummy.hasGravity());

                if (entity instanceof Mob mob) {
                    // Docile: mob brain & targeting disabled, but physical simulation active
                    mob.setAware(dummy.hasAi());
                    mob.setAI(true);
                } else if (entity instanceof LivingEntity living) {
                    living.setAI(dummy.hasAi());
                }

                if (entity instanceof LivingEntity living) {
                    AttributeInstance knockAttr = living.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                    if (knockAttr != null) knockAttr.setBaseValue(0.0);
                }
            }

            if (entity instanceof LivingEntity living) {
                living.setRemoveWhenFarAway(false);

                AttributeInstance maxHealthAttr = living.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(dummy.getMaxHealth());
                }
                living.setHealth(dummy.getMaxHealth());

                // Equipment
                if (living.getEquipment() != null) {
                    if (dummy.getHelmet() != null) living.getEquipment().setHelmet(dummy.getHelmet());
                    if (dummy.getChestplate() != null) living.getEquipment().setChestplate(dummy.getChestplate());
                    if (dummy.getLeggings() != null) living.getEquipment().setLeggings(dummy.getLeggings());
                    if (dummy.getBoots() != null) living.getEquipment().setBoots(dummy.getBoots());
                    if (dummy.getMainHand() != null) living.getEquipment().setItemInMainHand(dummy.getMainHand());
                    if (dummy.getOffHand() != null) living.getEquipment().setItemInOffHand(dummy.getOffHand());
                }
            }

            if (entity instanceof ArmorStand stand) {
                stand.setArms(true);
                stand.setBasePlate(true);
                stand.setSmall(dummy.isSmall());
            }
        });

        dummy.setCurrentEntityUuid(spawned.getUniqueId());
        storage.registerEntity(spawned.getUniqueId(), dummy.getDummyId());
    }

    public void removeDummy(DummyData dummy) {
        DummyDespawnEvent event = new DummyDespawnEvent(dummy);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        if (dummy.getCurrentEntityUuid() != null) {
            Entity e = Bukkit.getEntity(dummy.getCurrentEntityUuid());
            if (e != null && e.isValid()) {
                e.remove();
            }
        }
        storage.removeDummy(dummy);
    }

    public void spawnDamagePopup(Location dummyLoc, double damage, boolean isCrit) {
        if (!config.isShowFloatingPopups()) return;

        Location spawnLoc = dummyLoc.clone().add(
                (Math.random() - 0.5) * 0.8,
                1.5 + (Math.random() * 0.5),
                (Math.random() - 0.5) * 0.8
        );

        String damageStr = String.format("%.1f", damage);
        String format = isCrit ? config.getCritHitFormat() : config.getNormalHitFormat();
        TagResolver res = TagResolver.resolver(
                Placeholder.parsed("damage", damageStr),
                Placeholder.parsed("hearts", String.format("%.1f", damage / 2.0))
        );
        Component text = MessageUtil.parse(format, res);

        TextDisplay display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            float viewRangeMultiplier = (float) config.getPopupViewDistanceBlocks() / 64.0f;
            d.setViewRange(Math.max(0.1f, viewRangeMultiplier));
        });

        // Float up and fade
        long durationTicks = config.getPopupDurationTicks();
        Bukkit.getRegionScheduler().runDelayed(plugin, spawnLoc, task -> {
            if (display.isValid()) {
                display.remove();
            }
        }, durationTicks);
    }

    public DummyData getDummyFromEntity(Entity entity) {
        if (entity == null) return null;
        DummyData found = storage.getDummyByEntityUuid(entity.getUniqueId());
        if (found != null) return found;

        if (entity.getPersistentDataContainer().has(dummyPdcKey, PersistentDataType.STRING)) {
            String idStr = entity.getPersistentDataContainer().get(dummyPdcKey, PersistentDataType.STRING);
            if (idStr != null) {
                try {
                    UUID dummyId = UUID.fromString(idStr);
                    DummyData d = storage.getDummy(dummyId);
                    if (d != null) {
                        d.setCurrentEntityUuid(entity.getUniqueId());
                        storage.registerEntity(entity.getUniqueId(), dummyId);
                        return d;
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
