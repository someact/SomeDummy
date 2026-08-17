package com.someact.somedummy.listener;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.api.event.DummyDamageEvent;
import com.someact.somedummy.api.event.DummyDeathEvent;
import com.someact.somedummy.config.ConfigManager;
import com.someact.somedummy.dummy.DummyEntityManager;
import com.someact.somedummy.model.DamageSession;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.sound.SoundManager;
import com.someact.somedummy.util.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles damage calculation, live DPS tracking, floating pop-up text displays, and auto-respawn mechanics.
 */
public class DummyDamageListener implements Listener {

    private final SomeDummyPlugin plugin;
    private final ConfigManager config;
    private final DummyEntityManager entityManager;
    private final SoundManager soundManager;
    private final Map<UUID, DamageSession> playerSessions = new ConcurrentHashMap<>();

    public DummyDamageListener(SomeDummyPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.entityManager = plugin.getEntityManager();
        this.soundManager = plugin.getSoundManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        DummyData dummy = entityManager.getDummyFromEntity(entity);
        if (dummy == null) return;

        Player damager = null;
        if (event.getDamager() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
        }

        double damage = event.getFinalDamage();
        boolean isCrit = event.isCritical();

        if (damager != null && !isCrit) {
            // Check vanilla critical conditions if event flag was false
            if (damager.getFallDistance() > 0.0f && !damager.isOnGround() && !damager.isClimbing() && !damager.isInWater()) {
                isCrit = true;
            }
        }

        if (damager != null) {
            DummyDamageEvent damageEvent = new DummyDamageEvent(damager, dummy, damage, isCrit);
            Bukkit.getPluginManager().callEvent(damageEvent);
            if (damageEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }
            damage = damageEvent.getDamage();
            isCrit = damageEvent.isCritical();
        }

        Location loc = entity.getLocation();

        // 1. Floating pop-up damage number
        entityManager.spawnDamagePopup(loc, damage, isCrit);

        // 2. Play sound effects
        if (damager != null) {
            if (isCrit) {
                soundManager.playSound(damager, "dummy-crit", Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
                loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
            } else {
                soundManager.playSound(damager, "dummy-hit", Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.0f);
            }

            // 3. Live DPS session & Actionbar stats
            if (config.isShowActionbarDps()) {
                DamageSession session = playerSessions.computeIfAbsent(damager.getUniqueId(), k -> new DamageSession());
                session.registerHit(damage);

                String dpsStr = String.format("%.1f", session.getDPS());
                String totalStr = String.format("%.1f", session.getTotalDamage());
                String lastStr = String.format("%.1f", damage);

                TagResolver res = TagResolver.resolver(
                        Placeholder.parsed("damage", lastStr),
                        Placeholder.parsed("dps", dpsStr),
                        Placeholder.parsed("total_damage", totalStr),
                        Placeholder.parsed("hits", String.valueOf(session.getHits())),
                        Placeholder.parsed("dummy_name", dummy.getCustomName())
                );
                MessageUtil.sendActionBar(damager, config.getActionbarFormat(), res);
            }
        }

        // 4. Handle Infinite Health (DPS Mode) vs Normal Death
        if (dummy.isStatic()) {
            entity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        }

        if (dummy.isInfiniteHealth()) {
            if (entity instanceof LivingEntity living) {
                living.setHealth(dummy.getMaxHealth());
            }
        } else {
            if (entity instanceof LivingEntity living) {
                if (living.getHealth() - damage <= 0) {
                    event.setCancelled(true);
                    handleDummyDeath(damager, dummy, entity);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        DummyData dummy = entityManager.getDummyFromEntity(entity);
        if (dummy == null) return;

        if (dummy.isInfiniteHealth()) {
            if (entity instanceof LivingEntity living) {
                living.setHealth(dummy.getMaxHealth());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityCombust(org.bukkit.event.entity.EntityCombustEvent event) {
        Entity entity = event.getEntity();
        DummyData dummy = entityManager.getDummyFromEntity(entity);
        if (dummy == null) return;

        if (!dummy.isBurnInSun()) {
            // Prevent sun combustion while still allowing flint & steel / fire aspect / lava attacks
            if (!(event instanceof org.bukkit.event.entity.EntityCombustByEntityEvent) &&
                !(event instanceof org.bukkit.event.entity.EntityCombustByBlockEvent)) {
                event.setCancelled(true);
            }
        }
    }

    private void handleDummyDeath(Player killer, DummyData dummy, Entity entity) {
        DummyDeathEvent deathEvent = new DummyDeathEvent(killer, dummy, dummy.isRespawnable(), dummy.getRespawnDelaySeconds());
        Bukkit.getPluginManager().callEvent(deathEvent);
        if (deathEvent.isCancelled()) return;

        Location loc = dummy.getLocation();
        soundManager.playSound(loc, "dummy-die", Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
        loc.getWorld().spawnParticle(Particle.POOF, loc.add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);

        entity.remove();

        if (deathEvent.isRespawn()) {
            long delayTicks = Math.max(1, deathEvent.getRespawnDelaySeconds() * 20L);
            Bukkit.getRegionScheduler().runDelayed(plugin, loc, task -> {
                entityManager.spawnDummyEntity(dummy);
                soundManager.playSound(loc, "dummy-respawn", Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            }, delayTicks);
        } else {
            plugin.getStorageManager().removeDummy(dummy);
        }
    }
}
