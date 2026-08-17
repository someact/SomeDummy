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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Preset Library GUI to browse and acquire placeable dummy templates (/sd presets).
 */
public class PresetLibraryGUI implements InventoryHolder {

    private final SomeDummyPlugin plugin;
    private final SoundManager soundManager;
    private final Player player;
    private final List<Map.Entry<String, DummyData>> presets;
    private final Inventory inventory;

    private static final int CLOSE_SLOT = 49;

    public PresetLibraryGUI(SomeDummyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.soundManager = plugin.getSoundManager();
        this.player = player;
        this.presets = new ArrayList<>(plugin.getPresetManager().getAllPresets().entrySet());

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>Dummy Preset Library</bold></gradient>");
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

        for (int i = 0; i < presets.size() && i < 36; i++) {
            Map.Entry<String, DummyData> entry = presets.get(i);
            String id = entry.getKey();
            DummyData data = entry.getValue();

            inventory.setItem(i, ItemBuilder.from(Material.ARMOR_STAND)
                    .name("<gold><bold>" + data.getCustomName() + "</bold></gold>")
                    .loreStrings(List.of(
                            "<gray>Preset ID: <white>" + id + "</white></gray>",
                            "<gray>Type: <gold>" + data.getEntityType().name() + "</gold></gray>",
                            "<gray>Health: <green>" + (data.isInfiniteHealth() ? "Infinite" : data.getMaxHealth()) + "</green></gray>",
                            "<gray>AI: <white>" + (data.hasAi() ? "Enabled" : "Frozen Target") + "</white></gray>",
                            "",
                            "<yellow>[Click to Receive Placeable Item]</yellow>"
                    ))
                    .glow(true)
                    .build());
        }

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

        if (slot >= 0 && slot < presets.size()) {
            String id = presets.get(slot).getKey();
            ItemStack item = plugin.getPresetManager().createPresetItem(id, 1);
            if (item != null) {
                player.getInventory().addItem(item);
                soundManager.playSound(player, "dummy-spawn", Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                MessageUtil.sendMessage(player, plugin.getConfigManager().getPrefix() + "<green>Received placeable dummy preset: <gold>" + id + "</gold>!</green>");
            }
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
