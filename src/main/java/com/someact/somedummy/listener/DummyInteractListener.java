package com.someact.somedummy.listener;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.config.ConfigManager;
import com.someact.somedummy.gui.DummyEditorGUI;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles stick wand interactions with dummy entities and placing preset figurine items.
 */
public class DummyInteractListener implements Listener {

    private final SomeDummyPlugin plugin;
    private final ConfigManager config;

    public DummyInteractListener(SomeDummyPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        DummyData dummy = plugin.getEntityManager().getDummyFromEntity(entity);
        if (dummy == null) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();

        // Check if player is holding the configured wand item (e.g. STICK)
        boolean isWand = handItem.getType() == config.getWandMaterial();
        boolean sneakMatch = !config.isWandRequireSneak() || player.isSneaking();

        if (isWand && sneakMatch) {
            event.setCancelled(true);

            boolean isOwner = dummy.getOwnerUuid().equals(player.getUniqueId());
            if (!isOwner && !player.hasPermission("somedummy.edit.other")) {
                MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("no-permission",
                        "<red>You do not have permission to edit another player's target dummy.</red>"));
                return;
            }

            if (isOwner && !player.hasPermission("somedummy.edit.own") && !player.hasPermission("somedummy.use")) {
                MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("no-permission",
                        "<red>You do not have permission to edit target dummies.</red>"));
                return;
            }

            new DummyEditorGUI(plugin, player, dummy).open();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // ArmorStand click interception
        Entity entity = event.getRightClicked();
        DummyData dummy = plugin.getEntityManager().getDummyFromEntity(entity);
        if (dummy == null) return;

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        boolean isWand = handItem.getType() == config.getWandMaterial();
        boolean sneakMatch = !config.isWandRequireSneak() || player.isSneaking();

        if (isWand && sneakMatch) {
            event.setCancelled(true);
            new DummyEditorGUI(plugin, player, dummy).open();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !plugin.getPresetManager().isPresetItem(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("somedummy.preset")) {
            MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("no-permission",
                    "<red>You do not have permission to place preset dummies.</red>"));
            return;
        }

        String presetId = plugin.getPresetManager().getPresetIdFromItem(item);
        DummyData template = plugin.getPresetManager().getPreset(presetId);

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Location spawnLoc = clicked.getLocation().add(0.5, 1.0, 0.5);
        spawnLoc.setYaw(player.getLocation().getYaw() + 180f);

        DummyData spawned = plugin.getEntityManager().spawnDummy(player, spawnLoc);
        if (spawned != null && template != null) {
            spawned.setEntityType(template.getEntityType());
            spawned.setCustomName(template.getCustomName());
            spawned.setMaxHealth(template.getMaxHealth());
            spawned.setInfiniteHealth(template.isInfiniteHealth());
            spawned.setHasAi(template.hasAi());
            spawned.setHasGravity(template.hasGravity());
            spawned.setSilent(template.isSilent());
            spawned.setSmall(template.isSmall());
            spawned.setRespawnable(template.isRespawnable());
            spawned.setRespawnDelaySeconds(template.getRespawnDelaySeconds());

            plugin.getStorageManager().saveDummyAsync(spawned);
            player.getScheduler().run(plugin, t -> {
                plugin.getEntityManager().spawnDummyEntity(spawned);
            }, null);

            item.setAmount(item.getAmount() - 1);
        }
    }
}
