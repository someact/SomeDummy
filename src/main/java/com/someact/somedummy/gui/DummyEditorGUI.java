package com.someact.somedummy.gui;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.api.event.DummyEditEvent;
import com.someact.somedummy.config.ConfigManager;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.sound.SoundManager;
import com.someact.somedummy.util.ItemBuilder;
import com.someact.somedummy.util.MessageUtil;
import com.someact.somedummy.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 54-slot comprehensive Dummy Editor GUI for tweaking target dummy attributes, equipment, and behavior.
 */
public class DummyEditorGUI implements InventoryHolder {

    private final SomeDummyPlugin plugin;
    private final ConfigManager config;
    private final SoundManager soundManager;
    private final Player player;
    private final DummyData dummy;
    private final Inventory inventory;

    private static final int ENTITY_TYPE_SLOT = 10;
    private static final int NAME_SLOT = 12;
    private static final int HEALTH_SLOT = 14;
    private static final int INFINITE_HEALTH_SLOT = 16;

    private static final int AI_SLOT = 28;
    private static final int GRAVITY_SLOT = 30;
    private static final int SILENT_SLOT = 32;
    private static final int RESPAWN_SLOT = 34;

    private static final int EQUIPMENT_SLOT = 38;
    private static final int DURATION_SLOT = 40;
    private static final int EXPORT_PRESET_SLOT = 42;
    private static final int SUN_BURN_SLOT = 44;

    private static final int TELEPORT_HERE_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int DELETE_SLOT = 50;

    public DummyEditorGUI(SomeDummyPlugin plugin, Player player, DummyData dummy) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.soundManager = plugin.getSoundManager();
        this.player = player;
        this.dummy = dummy;

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>Edit Target Dummy</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // Sleek Background Fillers
        ItemStack borderFiller = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();
        ItemStack innerFiller = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        for (int i = 0; i < 54; i++) {
            boolean isBorder = (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8);
            inventory.setItem(i, isBorder ? borderFiller : innerFiller);
        }

        // ==========================================
        // ROW 1: Identity & Stats
        // ==========================================

        // 1. Entity Type
        inventory.setItem(ENTITY_TYPE_SLOT, ItemBuilder.from(Material.ZOMBIE_HEAD)
                .name("<yellow><bold>Entity Model / Type</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current: <gold><bold>" + dummy.getEntityType().name() + "</bold></gold>",
                        "<gray>Transform into any mob, ArmorStand,</gray>",
                        "<gray>or Display Entity in the game.</gray>",
                        "",
                        "<yellow>[Click to Select Model Type]</yellow>"
                ))
                .build());

        // 2. Custom Name
        inventory.setItem(NAME_SLOT, ItemBuilder.from(Material.NAME_TAG)
                .name("<yellow><bold>Custom Display Name</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current: " + dummy.getCustomName(),
                        "<gray>Supports MiniMessage & custom fonts.</gray>",
                        "",
                        "<yellow>[Click to Edit via Chat]</yellow>"
                ))
                .build());

        // 3. Max Health
        inventory.setItem(HEALTH_SLOT, ItemBuilder.from(Material.GLISTERING_MELON_SLICE)
                .name("<yellow><bold>Maximum Health</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Current Health: <green><bold>" + dummy.getMaxHealth() + " HP</bold></green> (" + (dummy.getMaxHealth() / 2.0) + " ❤)",
                        "<gray>Customizable from 1 to 100,000+ HP.</gray>",
                        "",
                        "<yellow>[Click to Edit via Chat]</yellow>"
                ))
                .build());

        // 4. Infinite Health / Godmode
        boolean inf = dummy.isInfiniteHealth();
        inventory.setItem(INFINITE_HEALTH_SLOT, ItemBuilder.from(inf ? Material.TOTEM_OF_UNDYING : Material.GOLDEN_APPLE)
                .name("<yellow><bold>Infinite Health (DPS Mode)</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (inf ? "<green><bold>ENABLED (Indestructible)</bold></green>" : "<red><bold>DISABLED (Can Die)</bold></red>"),
                        "<gray>When enabled, tracks DPS continuously</gray>",
                        "<gray>without the dummy ever expiring.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(inf)
                .build());

        // ==========================================
        // ROW 2: Behavior & Physics
        // ==========================================

        // 5. AI Behavior
        boolean ai = dummy.hasAi();
        inventory.setItem(AI_SLOT, ItemBuilder.from(ai ? Material.SLIME_BALL : Material.LEAD)
                .name("<yellow><bold>Mob AI Behavior</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (ai ? "<green><bold>AI ENABLED (Active)</bold></green>" : "<aqua><bold>FROZEN (Stationary Target)</bold></aqua>"),
                        "<gray>Freezes dummy in place or allows normal mob AI.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(!ai)
                .build());

        // 6. Gravity & Physics Mode
        Material physIcon;
        String physStatus;
        List<String> physDesc;
        boolean physGlow;

        if (dummy.isStatic()) {
            physIcon = Material.BLUE_ICE;
            physStatus = "<blue><bold>STATIC (HARD FREEZE)</bold></blue>";
            physDesc = List.of(
                    "<gray>Completely locked in place. Zero gravity,</gray>",
                    "<gray>100% knockback immunity, fixed statue.</gray>"
            );
            physGlow = true;
        } else if (!dummy.hasGravity()) {
            physIcon = Material.FEATHER;
            physStatus = "<aqua><bold>FLOATING (ZERO-G)</bold></aqua>";
            physDesc = List.of(
                    "<gray>Floats mid-air for aerial testing,</gray>",
                    "<gray>still reacts to knockback & attacks.</gray>"
            );
            physGlow = true;
        } else {
            physIcon = Material.ANVIL;
            physStatus = "<green><bold>DYNAMIC (NATURAL PHYSICS)</bold></green>";
            physDesc = List.of(
                    "<gray>Natural ground physics, falling gravity,</gray>",
                    "<gray>and normal combat knockback simulation.</gray>"
            );
            physGlow = false;
        }

        inventory.setItem(GRAVITY_SLOT, ItemBuilder.from(physIcon)
                .name("<yellow><bold>Physics & Gravity Mode</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Mode: " + physStatus,
                        physDesc.get(0),
                        physDesc.get(1),
                        "",
                        "<yellow>[Click to Cycle: Dynamic → Floating → Static]</yellow>"
                ))
                .glow(physGlow)
                .build());

        // 7. Silent Mode
        boolean silent = dummy.isSilent();
        inventory.setItem(SILENT_SLOT, ItemBuilder.from(silent ? Material.NOTE_BLOCK : Material.JUKEBOX)
                .name("<yellow><bold>Silent Mode</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (silent ? "<green><bold>MUTED</bold></green>" : "<red><bold>AUDIBLE</bold></red>"),
                        "<gray>Mutes ambient mob groans & grunts.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(silent)
                .build());

        // 8. Auto-Respawn
        boolean respawn = dummy.isRespawnable();
        inventory.setItem(RESPAWN_SLOT, ItemBuilder.from(respawn ? Material.BEACON : Material.ENDER_EYE)
                .name("<yellow><bold>Auto-Respawn on Defeat</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (respawn ? "<green><bold>ENABLED (" + dummy.getRespawnDelaySeconds() + "s delay)</bold></green>" : "<red><bold>DISABLED</bold></red>"),
                        "<gray>Respawns automatically at its spawn point.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(respawn)
                .build());

        // ==========================================
        // ROW 3: Equipment, Lifespan & Presets
        // ==========================================

        // 9. Equipment Editor
        inventory.setItem(EQUIPMENT_SLOT, ItemBuilder.from(Material.DIAMOND_CHESTPLATE)
                .name("<gold><bold>Equipment & Weapons Editor</bold></gold>")
                .loreStrings(List.of(
                        "<gray>Equip custom Armor, Weapons, Shields,</gray>",
                        "<gray>and items on the dummy for defense testing.</gray>",
                        "",
                        "<yellow>[Click to Open Equipment Editor]</yellow>"
                ))
                .build());

        // 10. Lifespan Duration
        long duration = dummy.getLifespanSeconds();
        inventory.setItem(DURATION_SLOT, ItemBuilder.from(Material.CLOCK)
                .name("<yellow><bold>Lifespan Duration</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Duration: <gold><bold>" + TimeUtil.formatDuration(duration) + "</bold></gold>",
                        "<gray>Remaining: <white>" + (duration <= 0 ? "Infinite" : TimeUtil.formatDuration(dummy.getRemainingLifespanSeconds())) + "</white></gray>",
                        "",
                        "<yellow>[Click to Edit via Chat (0 = Permanent)]</yellow>"
                ))
                .build());

        // 11. Export as Preset Item
        inventory.setItem(EXPORT_PRESET_SLOT, ItemBuilder.from(Material.ARMOR_STAND)
                .name("<light_purple><bold>Export as Placeable Figurine</bold></light_purple>")
                .loreStrings(List.of(
                        "<gray>Saves this dummy into a placeable item</gray>",
                        "<gray>in your inventory to spawn anywhere!</gray>",
                        "",
                        "<yellow>[Click to Export Preset Item]</yellow>"
                ))
                .glow(true)
                .build());

        // 12. Sunlight Combustion
        boolean burn = dummy.isBurnInSun();
        inventory.setItem(SUN_BURN_SLOT, ItemBuilder.from(burn ? Material.FLINT_AND_STEEL : Material.CAMPFIRE)
                .name("<yellow><bold>Sunlight Combustion</bold></yellow>")
                .loreStrings(List.of(
                        "<gray>Status: " + (burn ? "<red><bold>BURNS IN SUN</bold></red>" : "<green><bold>IMMUNE TO SUN</bold></green>"),
                        "<gray>Toggle whether undead mobs catch fire</gray>",
                        "<gray>under direct daytime sunlight.</gray>",
                        "",
                        "<yellow>[Click to Toggle]</yellow>"
                ))
                .glow(burn)
                .build());

        // ==========================================
        // ROW 4: Actions & Navigation
        // ==========================================

        // Teleport Dummy Here
        inventory.setItem(TELEPORT_HERE_SLOT, ItemBuilder.from(Material.ENDER_PEARL)
                .name("<aqua><bold>Move Dummy to My Position</bold></aqua>")
                .loreStrings(List.of("<gray>Teleports the dummy right in front of you.</gray>"))
                .build());

        // Close Menu
        inventory.setItem(CLOSE_SLOT, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Close Menu</bold></red>")
                .build());

        // Delete Dummy
        inventory.setItem(DELETE_SLOT, ItemBuilder.from(Material.LAVA_BUCKET)
                .name("<red><bold>Despawn & Delete Dummy</bold></red>")
                .loreStrings(List.of("<gray>Permanently deletes this target dummy.</gray>"))
                .build());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (slot == ENTITY_TYPE_SLOT) {
            new EntityTypeSelectGUI(plugin, player, dummy).open();
            return;
        }

        if (slot == NAME_SLOT) {
            promptNameInChat();
            return;
        }

        if (slot == HEALTH_SLOT) {
            promptHealthInChat();
            return;
        }

        if (slot == INFINITE_HEALTH_SLOT) {
            dummy.setInfiniteHealth(!dummy.isInfiniteHealth());
            saveAndRefresh();
            return;
        }

        if (slot == AI_SLOT) {
            dummy.setHasAi(!dummy.hasAi());
            saveAndRefresh();
            return;
        }

        if (slot == GRAVITY_SLOT) {
            if (dummy.isStatic()) {
                // Switch to Dynamic (Natural Physics)
                dummy.setStatic(false);
                dummy.setHasGravity(true);
            } else if (dummy.hasGravity()) {
                // Switch to Floating (Zero-G)
                dummy.setStatic(false);
                dummy.setHasGravity(false);
            } else {
                // Switch to Static (Hard Freeze Statue)
                dummy.setStatic(true);
                dummy.setHasGravity(false);
            }
            saveAndRefresh();
            return;
        }

        if (slot == SILENT_SLOT) {
            dummy.setSilent(!dummy.isSilent());
            saveAndRefresh();
            return;
        }

        if (slot == RESPAWN_SLOT) {
            dummy.setRespawnable(!dummy.isRespawnable());
            saveAndRefresh();
            return;
        }

        if (slot == EQUIPMENT_SLOT) {
            new EquipmentEditorGUI(plugin, player, dummy).open();
            return;
        }

        if (slot == DURATION_SLOT) {
            promptDurationInChat();
            return;
        }

        if (slot == EXPORT_PRESET_SLOT) {
            exportPresetItem();
            return;
        }

        if (slot == SUN_BURN_SLOT) {
            dummy.setBurnInSun(!dummy.isBurnInSun());
            saveAndRefresh();
            return;
        }

        if (slot == TELEPORT_HERE_SLOT) {
            dummy.setLocation(player.getLocation());
            saveAndRefresh();
            soundManager.playSound(player, "dummy-spawn", Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            MessageUtil.sendMessage(player, config.getPrefix() + "<green>Teleported target dummy to your location.</green>");
            return;
        }

        if (slot == DELETE_SLOT) {
            player.closeInventory();
            plugin.getEntityManager().removeDummy(dummy);
            soundManager.playSound(player, "dummy-die", Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("dummy-removed", "<yellow>Target dummy removed.</yellow>"));
        }
    }

    private void saveAndRefresh() {
        DummyEditEvent event = new DummyEditEvent(player, dummy);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        plugin.getStorageManager().saveDummyAsync(dummy);
        Bukkit.getRegionScheduler().run(plugin, dummy.getLocation(), t -> {
            plugin.getEntityManager().spawnDummyEntity(dummy);
        });
        populate();
        soundManager.playSound(player, "gui-click", Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
    }

    private void promptNameInChat() {
        player.closeInventory();
        MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("name-prompt",
                "<yellow>Please enter the new dummy name in chat. Supports MiniMessage formatting. Type <red>cancel</red> to abort.</yellow>"));
        soundManager.playSound(player, "gui-click", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getChatInputListener().requestInput(player, text -> {
            if (text.equalsIgnoreCase("cancel")) {
                new DummyEditorGUI(plugin, player, dummy).open();
                return;
            }
            dummy.setCustomName(text);
            saveAndRefresh();
            new DummyEditorGUI(plugin, player, dummy).open();
        });
    }

    private void promptHealthInChat() {
        player.closeInventory();
        MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("health-prompt",
                "<yellow>Please enter the new max health in chat (e.g. <white>100</white>, <white>1000</white>, <white>5000</white>). Type <red>cancel</red> to abort.</yellow>"));
        soundManager.playSound(player, "gui-click", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getChatInputListener().requestInput(player, text -> {
            if (text.equalsIgnoreCase("cancel")) {
                new DummyEditorGUI(plugin, player, dummy).open();
                return;
            }
            try {
                double hp = Double.parseDouble(text.trim());
                dummy.setMaxHealth(hp);
                saveAndRefresh();
                new DummyEditorGUI(plugin, player, dummy).open();
            } catch (Exception e) {
                MessageUtil.sendMessage(player, config.getPrefix() + "<red>Invalid number: " + e.getMessage() + "</red>");
                soundManager.playSound(player, "dummy-die", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                new DummyEditorGUI(plugin, player, dummy).open();
            }
        });
    }

    private void promptDurationInChat() {
        player.closeInventory();
        MessageUtil.sendMessage(player, config.getPrefix() + config.getMessage("duration-prompt",
                "<yellow>Please enter lifespan duration in chat (e.g. <white>30m</white>, <white>1h</white>, or <white>0</white> for permanent). Type <red>cancel</red> to abort.</yellow>"));
        soundManager.playSound(player, "gui-click", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getChatInputListener().requestInput(player, text -> {
            if (text.equalsIgnoreCase("cancel")) {
                new DummyEditorGUI(plugin, player, dummy).open();
                return;
            }
            try {
                long duration = TimeUtil.parseDurationSeconds(text);
                dummy.setLifespanSeconds(duration);
                saveAndRefresh();
                new DummyEditorGUI(plugin, player, dummy).open();
            } catch (Exception e) {
                MessageUtil.sendMessage(player, config.getPrefix() + "<red>Invalid time duration: " + e.getMessage() + "</red>");
                soundManager.playSound(player, "dummy-die", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                new DummyEditorGUI(plugin, player, dummy).open();
            }
        });
    }

    private void exportPresetItem() {
        String presetId = "custom_" + dummy.getDummyId().toString().substring(0, 8);
        plugin.getPresetManager().savePreset(presetId, dummy);

        ItemStack item = plugin.getPresetManager().createPresetItem(presetId, 1);
        if (item != null) {
            player.getInventory().addItem(item);
            soundManager.playSound(player, "dummy-spawn", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            MessageUtil.sendMessage(player, config.getPrefix() + "<green>Exported dummy figurine preset item to your inventory!</green>");
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
