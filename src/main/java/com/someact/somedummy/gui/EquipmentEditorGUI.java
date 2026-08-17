package com.someact.somedummy.gui;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.sound.SoundManager;
import com.someact.somedummy.util.ItemBuilder;
import com.someact.somedummy.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 6-slot visual equipment matrix editor for target dummies.
 */
public class EquipmentEditorGUI implements InventoryHolder {

    private final SomeDummyPlugin plugin;
    private final SoundManager soundManager;
    private final Player player;
    private final DummyData dummy;
    private final Inventory inventory;

    public static final int HELMET_SLOT = 11;
    public static final int CHESTPLATE_SLOT = 20;
    public static final int LEGGINGS_SLOT = 29;
    public static final int BOOTS_SLOT = 38;

    public static final int MAIN_HAND_SLOT = 24;
    public static final int OFF_HAND_SLOT = 25;

    public static final int BACK_SLOT = 49;
    public static final int CLEAR_SLOT = 45;

    public EquipmentEditorGUI(SomeDummyPlugin plugin, Player player, DummyData dummy) {
        this.plugin = plugin;
        this.soundManager = plugin.getSoundManager();
        this.player = player;
        this.dummy = dummy;

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>Edit Dummy Equipment</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // Filler
        ItemStack filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Set items
        inventory.setItem(HELMET_SLOT, dummy.getHelmet());
        inventory.setItem(CHESTPLATE_SLOT, dummy.getChestplate());
        inventory.setItem(LEGGINGS_SLOT, dummy.getLeggings());
        inventory.setItem(BOOTS_SLOT, dummy.getBoots());

        inventory.setItem(MAIN_HAND_SLOT, dummy.getMainHand());
        inventory.setItem(OFF_HAND_SLOT, dummy.getOffHand());

        // Labels
        inventory.setItem(10, ItemBuilder.from(Material.IRON_HELMET).name("<yellow>Helmet Slot</yellow>").build());
        inventory.setItem(19, ItemBuilder.from(Material.IRON_CHESTPLATE).name("<yellow>Chestplate Slot</yellow>").build());
        inventory.setItem(28, ItemBuilder.from(Material.IRON_LEGGINGS).name("<yellow>Leggings Slot</yellow>").build());
        inventory.setItem(37, ItemBuilder.from(Material.IRON_BOOTS).name("<yellow>Boots Slot</yellow>").build());

        inventory.setItem(15, ItemBuilder.from(Material.DIAMOND_SWORD).name("<yellow>Main Hand / Weapon</yellow>").build());
        inventory.setItem(16, ItemBuilder.from(Material.SHIELD).name("<yellow>Off Hand / Shield</yellow>").build());

        inventory.setItem(CLEAR_SLOT, ItemBuilder.from(Material.LAVA_BUCKET)
                .name("<red><bold>Unequip All Items</bold></red>")
                .loreStrings(List.of("<gray>Clears all equipment from the dummy.</gray>"))
                .build());

        inventory.setItem(BACK_SLOT, ItemBuilder.from(Material.EMERALD_BLOCK)
                .name("<green><bold>Save & Return to Editor</bold></green>")
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        if (rawSlot >= 54) return; // Player clicking own inventory

        if (rawSlot == HELMET_SLOT || rawSlot == CHESTPLATE_SLOT || rawSlot == LEGGINGS_SLOT ||
                rawSlot == BOOTS_SLOT || rawSlot == MAIN_HAND_SLOT || rawSlot == OFF_HAND_SLOT) {
            // Allow taking and placing equipment in these slots
            return;
        }

        event.setCancelled(true);

        if (rawSlot == BACK_SLOT) {
            saveEquipment();
            new DummyEditorGUI(plugin, player, dummy).open();
            return;
        }

        if (rawSlot == CLEAR_SLOT) {
            dummy.setHelmet(null);
            dummy.setChestplate(null);
            dummy.setLeggings(null);
            dummy.setBoots(null);
            dummy.setMainHand(null);
            dummy.setOffHand(null);
            populate();
            soundManager.playSound(player, "gui-click", Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        saveEquipment();
    }

    private void saveEquipment() {
        dummy.setHelmet(inventory.getItem(HELMET_SLOT));
        dummy.setChestplate(inventory.getItem(CHESTPLATE_SLOT));
        dummy.setLeggings(inventory.getItem(LEGGINGS_SLOT));
        dummy.setBoots(inventory.getItem(BOOTS_SLOT));
        dummy.setMainHand(inventory.getItem(MAIN_HAND_SLOT));
        dummy.setOffHand(inventory.getItem(OFF_HAND_SLOT));

        plugin.getStorageManager().saveDummyAsync(dummy);
        Bukkit.getRegionScheduler().run(plugin, dummy.getLocation(), t -> {
            plugin.getEntityManager().spawnDummyEntity(dummy);
        });
        soundManager.playSound(player, "dummy-spawn", Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.2f);
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
