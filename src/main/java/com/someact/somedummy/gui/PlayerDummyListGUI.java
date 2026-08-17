package com.someact.somedummy.gui;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.sound.SoundManager;
import com.someact.somedummy.util.ItemBuilder;
import com.someact.somedummy.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI for normal players to view, edit, locate, and manage their placed target dummies (/sd list).
 */
public class PlayerDummyListGUI implements InventoryHolder {

    private final SomeDummyPlugin plugin;
    private final SoundManager soundManager;
    private final Player player;
    private final List<DummyData> dummies;
    private final Inventory inventory;

    private static final int CLOSE_SLOT = 49;
    private static final int SPAWN_NEW_SLOT = 45;

    public PlayerDummyListGUI(SomeDummyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.soundManager = plugin.getSoundManager();
        this.player = player;
        this.dummies = plugin.getStorageManager().getDummiesForPlayer(player.getUniqueId());

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>My Target Dummies (" + dummies.size() + ")</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // Filler
        ItemStack filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 36; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        for (int i = 0; i < dummies.size() && i < 36; i++) {
            DummyData d = dummies.get(i);
            Location loc = d.getLocation();
            String coords = loc != null ? loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")" : "Unknown";

            inventory.setItem(i, ItemBuilder.from(Material.ARMOR_STAND)
                    .name("<gold><bold>" + d.getCustomName() + "</bold></gold>")
                    .loreStrings(List.of(
                            "<gray>Entity Type: <white>" + d.getEntityType().name() + "</white></gray>",
                            "<gray>Health: <green>" + (d.isInfiniteHealth() ? "Infinite (DPS Mode)" : d.getMaxHealth() + " HP") + "</green></gray>",
                            "<gray>AI: <white>" + (d.hasAi() ? "Active" : "Frozen Target") + "</white></gray>",
                            "<gray>Location: <yellow>" + coords + "</yellow></gray>",
                            "",
                            "<yellow>[Left-Click: Open Editor GUI]</yellow>",
                            "<aqua>[Right-Click: Move Dummy Here]</aqua>",
                            "<red>[Shift+Right-Click: Delete Dummy]</red>"
                    ))
                    .glow(true)
                    .build());
        }

        // Spawn new button
        inventory.setItem(SPAWN_NEW_SLOT, ItemBuilder.from(Material.EMERALD)
                .name("<green><bold>+ Spawn New Dummy Here</bold></green>")
                .loreStrings(List.of("<gray>Spawns a new target dummy at your feet.</gray>"))
                .build());

        // Close
        inventory.setItem(CLOSE_SLOT, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close Menu</bold></red>")
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (slot == SPAWN_NEW_SLOT) {
            player.closeInventory();
            plugin.getEntityManager().spawnDummy(player, player.getLocation());
            return;
        }

        if (slot >= 0 && slot < dummies.size()) {
            DummyData d = dummies.get(slot);

            if (event.isShiftClick() && event.isRightClick()) {
                plugin.getEntityManager().removeDummy(d);
                soundManager.playSound(player, "dummy-die", Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                new PlayerDummyListGUI(plugin, player).open();
                return;
            }

            if (event.isRightClick()) {
                d.setLocation(player.getLocation());
                plugin.getStorageManager().saveDummyAsync(d);
                Bukkit.getRegionScheduler().run(plugin, d.getLocation(), t -> {
                    plugin.getEntityManager().spawnDummyEntity(d);
                });
                soundManager.playSound(player, "dummy-spawn", Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                MessageUtil.sendMessage(player, plugin.getConfigManager().getPrefix() + "<green>Moved dummy to your location.</green>");
                new PlayerDummyListGUI(plugin, player).open();
                return;
            }

            // Left click -> open editor
            new DummyEditorGUI(plugin, player, d).open();
        }
    }

    public void open() {
        player.openInventory(inventory);
        soundManager.playSound(player, "gui-click", Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
