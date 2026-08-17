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

import java.util.ArrayList;
import java.util.List;

/**
 * Server-wide administrator dashboard to view, inspect, teleport to, or purge all target dummies across all worlds (/sd admin).
 */
public class AdminDummyManagerGUI implements InventoryHolder {

    private final SomeDummyPlugin plugin;
    private final SoundManager soundManager;
    private final Player admin;
    private final List<DummyData> allDummies;
    private int page = 0;
    private final Inventory inventory;

    private static final int ITEMS_PER_PAGE = 36;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int PURGE_ALL_SLOT = 47;
    private static final int CLOSE_SLOT = 49;
    private static final int CONFIG_SLOT = 51;
    private static final int NEXT_PAGE_SLOT = 53;

    public AdminDummyManagerGUI(SomeDummyPlugin plugin, Player admin) {
        this.plugin = plugin;
        this.soundManager = plugin.getSoundManager();
        this.admin = admin;
        this.allDummies = new ArrayList<>(plugin.getStorageManager().getAllDummies());

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>Server Dummy Manager (" + allDummies.size() + ")</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allDummies.size());

        for (int i = start; i < end; i++) {
            DummyData d = allDummies.get(i);
            int slot = i - start;

            Location loc = d.getLocation();
            String coords = loc != null ? loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")" : "Unknown";

            inventory.setItem(slot, ItemBuilder.from(Material.ARMOR_STAND)
                    .name("<gold><bold>" + d.getCustomName() + "</bold></gold>")
                    .loreStrings(List.of(
                            "<gray>Owner: <gold>" + d.getOwnerName() + "</gold></gray>",
                            "<gray>Type: <white>" + d.getEntityType().name() + "</white></gray>",
                            "<gray>Health: <green>" + (d.isInfiniteHealth() ? "Infinite" : d.getMaxHealth() + " HP") + "</green></gray>",
                            "<gray>Coords: <yellow>" + coords + "</yellow></gray>",
                            "",
                            "<yellow>[Left-Click: Open Editor]</yellow>",
                            "<aqua>[Right-Click: Teleport to Dummy]</aqua>",
                            "<red>[Shift+Right-Click: Delete Dummy]</red>"
                    ))
                    .glow(true)
                    .build());
        }

        // Nav row
        ItemStack navFiller = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 36; i < 54; i++) {
            inventory.setItem(i, navFiller);
        }

        if (page > 0) {
            inventory.setItem(PREV_PAGE_SLOT, ItemBuilder.from(Material.ARROW)
                    .name("<yellow><bold>← Previous Page (" + page + ")</bold></yellow>")
                    .build());
        }

        inventory.setItem(PURGE_ALL_SLOT, ItemBuilder.from(Material.TNT)
                .name("<red><bold>Purge All Dummies (" + allDummies.size() + ")</bold></red>")
                .loreStrings(List.of("<gray>Permanently deletes all dummies across the server.</gray>"))
                .build());

        inventory.setItem(CLOSE_SLOT, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close Menu</bold></red>")
                .build());

        inventory.setItem(CONFIG_SLOT, ItemBuilder.from(Material.COMMAND_BLOCK)
                .name("<yellow><bold>Plugin Configuration</bold></yellow>")
                .loreStrings(List.of("<gray>Open /sd config settings panel.</gray>"))
                .build());

        if (end < allDummies.size()) {
            inventory.setItem(NEXT_PAGE_SLOT, ItemBuilder.from(Material.ARROW)
                    .name("<yellow><bold>Next Page (" + (page + 2) + ") →</bold></yellow>")
                    .build());
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == CLOSE_SLOT) {
            admin.closeInventory();
            return;
        }

        if (slot == CONFIG_SLOT) {
            new AdminConfigGUI(plugin, admin).open();
            return;
        }

        if (slot == PURGE_ALL_SLOT) {
            for (DummyData d : new ArrayList<>(allDummies)) {
                plugin.getEntityManager().removeDummy(d);
            }
            soundManager.playSound(admin, "dummy-die", Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            MessageUtil.sendMessage(admin, plugin.getConfigManager().getPrefix() + "<red>Purged all target dummies from the server.</red>");
            new AdminDummyManagerGUI(plugin, admin).open();
            return;
        }

        if (slot == PREV_PAGE_SLOT && page > 0) {
            page--;
            populate();
            soundManager.playSound(admin, "gui-click", Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            return;
        }

        if (slot == NEXT_PAGE_SLOT && (page + 1) * ITEMS_PER_PAGE < allDummies.size()) {
            page++;
            populate();
            soundManager.playSound(admin, "gui-click", Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
            return;
        }

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = page * ITEMS_PER_PAGE + slot;
            if (index < allDummies.size()) {
                DummyData d = allDummies.get(index);

                if (event.isShiftClick() && event.isRightClick()) {
                    plugin.getEntityManager().removeDummy(d);
                    soundManager.playSound(admin, "dummy-die", Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                    new AdminDummyManagerGUI(plugin, admin).open();
                    return;
                }

                if (event.isRightClick()) {
                    if (d.getLocation() != null) {
                        admin.teleport(d.getLocation());
                        soundManager.playSound(admin, "dummy-spawn", Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        MessageUtil.sendMessage(admin, plugin.getConfigManager().getPrefix() + "<green>Teleported to dummy.</green>");
                        admin.closeInventory();
                    }
                    return;
                }

                new DummyEditorGUI(plugin, admin, d).open();
            }
        }
    }

    public void open() {
        admin.openInventory(inventory);
        soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
