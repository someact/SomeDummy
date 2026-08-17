package com.someact.somedummy.gui;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.config.ConfigManager;
import com.someact.somedummy.sound.SoundManager;
import com.someact.somedummy.util.ItemBuilder;
import com.someact.somedummy.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * In-game configuration control panel for SomeDummy (/sd config).
 */
public class AdminConfigGUI implements InventoryHolder {

    private final SomeDummyPlugin plugin;
    private final ConfigManager config;
    private final SoundManager soundManager;
    private final Player admin;
    private final Inventory inventory;

    private static final int DEFAULT_MOB_SLOT = 11;
    private static final int MAX_DUMMIES_SLOT = 13;
    private static final int WAND_MATERIAL_SLOT = 15;

    private static final int FLOATING_POPUPS_SLOT = 20;
    private static final int ACTIONBAR_DPS_SLOT = 22;
    private static final int SOUNDS_SLOT = 24;

    private static final int RELOAD_SLOT = 40;
    private static final int CLOSE_SLOT = 49;

    public AdminConfigGUI(SomeDummyPlugin plugin, Player admin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.soundManager = plugin.getSoundManager();
        this.admin = admin;

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>SomeDummy Configuration</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // Fillers
        ItemStack border = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();
        ItemStack inner = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        for (int i = 0; i < 54; i++) {
            boolean isBorder = (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8);
            inventory.setItem(i, isBorder ? border : inner);
        }

        // 1. Default Mob Type
        EntityType defType = config.getDefaultEntityType();
        inventory.setItem(DEFAULT_MOB_SLOT, ItemBuilder.from(Material.ZOMBIE_HEAD)
                .name("<yellow><bold>Default Dummy Mob Type</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current: <gold><bold>" + defType.name() + "</bold></gold>",
                        "<gray>Default entity spawned for new targets.</gray>",
                        "",
                        "<yellow>[Click to Cycle Common Mobs]</yellow>"
                ))
                .build());

        // 2. Max Dummies Per Player
        int max = config.getMaxDummiesPerPlayer();
        String maxStr = max > 0 ? max + " Dummies" : "UNLIMITED (∞)";
        inventory.setItem(MAX_DUMMIES_SLOT, ItemBuilder.from(Material.ARMOR_STAND)
                .name("<yellow><bold>Max Dummies Per Player</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current Limit: <gold><bold>" + maxStr + "</bold></gold>",
                        "",
                        "<yellow>[Click to Enter in Chat (0 = Unlimited)]</yellow>"
                ))
                .build());

        // 3. Wand Tool Material
        Material wandMat = config.getWandMaterial();
        inventory.setItem(WAND_MATERIAL_SLOT, ItemBuilder.from(wandMat)
                .name("<yellow><bold>Dummy Wand Tool Item</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current: <gold><bold>" + wandMat.name() + "</bold></gold>",
                        "<gray>Shift+Right-Click with this item to edit.</gray>",
                        "",
                        "<yellow>[Click to Cycle Material]</yellow>"
                ))
                .build());

        // 4. Floating Damage Popups
        boolean popups = config.isShowFloatingPopups();
        inventory.setItem(FLOATING_POPUPS_SLOT, ItemBuilder.from(popups ? Material.FEATHER : Material.STRING)
                .name("<yellow><bold>Floating Damage Pop-Ups</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (popups ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Displays floating damage & critical indicators.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(popups)
                .build());

        // 5. Actionbar Live DPS
        boolean dps = config.isShowActionbarDps();
        inventory.setItem(ACTIONBAR_DPS_SLOT, ItemBuilder.from(dps ? Material.COMPASS : Material.RECOVERY_COMPASS)
                .name("<yellow><bold>Actionbar Live DPS</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (dps ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Sends live DPS & total damage to actionbar.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(dps)
                .build());

        // 6. Sound Effects
        boolean sounds = config.isSoundsEnabled();
        inventory.setItem(SOUNDS_SLOT, ItemBuilder.from(sounds ? Material.NOTE_BLOCK : Material.JUKEBOX)
                .name("<yellow><bold>Sound Effects Engine</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (sounds ? "<green><bold>ENABLED</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(sounds)
                .build());

        // Reload
        inventory.setItem(RELOAD_SLOT, ItemBuilder.from(Material.NETHER_STAR)
                .name("<green><bold>Reload Config from Disk</bold></green>")
                .loreStrings(List.of("<gray>Reloads setting.conf values immediately.</gray>"))
                .glow(true)
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
            admin.closeInventory();
            return;
        }

        if (slot == DEFAULT_MOB_SLOT) {
            List<EntityType> types = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.IRON_GOLEM, EntityType.WARDEN, EntityType.ARMOR_STAND);
            int idx = types.indexOf(config.getDefaultEntityType());
            int nextIdx = (idx + 1) % types.size();
            config.setDefaultEntityType(types.get(nextIdx));
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        if (slot == MAX_DUMMIES_SLOT) {
            promptMaxDummiesInChat();
            return;
        }

        if (slot == WAND_MATERIAL_SLOT) {
            List<Material> mats = List.of(Material.STICK, Material.BLAZE_ROD, Material.WOODEN_SWORD, Material.AMETHYST_SHARD);
            int idx = mats.indexOf(config.getWandMaterial());
            int nextIdx = (idx + 1) % mats.size();
            config.setWandMaterial(mats.get(nextIdx));
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        if (slot == FLOATING_POPUPS_SLOT) {
            config.setShowFloatingPopups(!config.isShowFloatingPopups());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        if (slot == ACTIONBAR_DPS_SLOT) {
            config.setShowActionbarDps(!config.isShowActionbarDps());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        if (slot == SOUNDS_SLOT) {
            config.setSoundsEnabled(!config.isSoundsEnabled());
            config.save();
            populate();
            soundManager.playSound(admin, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            return;
        }

        if (slot == RELOAD_SLOT) {
            config.load();
            populate();
            soundManager.playSound(admin, "dummy-spawn", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            MessageUtil.sendMessage(admin, config.getPrefix() + config.getMessage("config-reloaded", "<green>SomeDummy configuration reloaded successfully!</green>"));
        }
    }

    private void promptMaxDummiesInChat() {
        admin.closeInventory();
        MessageUtil.sendMessage(admin, config.getPrefix() + "<yellow>Please enter max dummies per player in chat (0 = Unlimited). Type <red>cancel</red> to abort.</yellow>");
        soundManager.playSound(admin, "gui-click", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getChatInputListener().requestInput(admin, text -> {
            if (text.equalsIgnoreCase("cancel")) {
                new AdminConfigGUI(plugin, admin).open();
                return;
            }
            try {
                int max = Integer.parseInt(text.trim());
                config.setMaxDummiesPerPlayer(max);
                config.save();
                MessageUtil.sendMessage(admin, config.getPrefix() + "<green>Max dummies per player updated to: <gold>" + (max > 0 ? max : "UNLIMITED") + "</gold>.</green>");
                soundManager.playSound(admin, "dummy-spawn", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                new AdminConfigGUI(plugin, admin).open();
            } catch (Exception e) {
                MessageUtil.sendMessage(admin, config.getPrefix() + "<red>Invalid number: " + e.getMessage() + "</red>");
                soundManager.playSound(admin, "dummy-die", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                new AdminConfigGUI(plugin, admin).open();
            }
        });
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
