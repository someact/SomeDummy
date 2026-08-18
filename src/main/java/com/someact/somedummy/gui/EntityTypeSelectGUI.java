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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated mob and entity type selector GUI for target dummies.
 */
public class EntityTypeSelectGUI implements InventoryHolder {

    private final SomeDummyPlugin plugin;
    private final SoundManager soundManager;
    private final Player player;
    private final DummyData dummy;
    private int page = 0;
    private final Inventory inventory;

    private static final List<EntityType> AVAILABLE_TYPES = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK, EntityType.DROWNED, EntityType.WITHER_SKELETON,
            EntityType.CREEPER, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ENDERMAN, EntityType.BLAZE,
            EntityType.IRON_GOLEM, EntityType.WARDEN, EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.PIGLIN,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
            EntityType.RAVAGER, EntityType.WITCH, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.SHULKER,
            EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.ARMOR_STAND, EntityType.COW, EntityType.PIG,
            EntityType.SHEEP, EntityType.CHICKEN, EntityType.HORSE, EntityType.WOLF, EntityType.CAT,
            EntityType.SNOW_GOLEM, EntityType.BEE, EntityType.ALLAY, EntityType.FROG, EntityType.ARMADILLO,
            EntityType.BREEZE, EntityType.BOGGED
    );

    private static final int ITEMS_PER_PAGE = 36;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    public EntityTypeSelectGUI(SomeDummyPlugin plugin, Player player, DummyData dummy) {
        this.plugin = plugin;
        this.soundManager = plugin.getSoundManager();
        this.player = player;
        this.dummy = dummy;

        Component title = MessageUtil.parse("<gradient:#ff7675:#fab1a0><bold>Select Dummy Entity Type</bold></gradient>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        populate();
    }

    private void populate() {
        inventory.clear();

        // Populate entity types (0..35)
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, AVAILABLE_TYPES.size());

        for (int i = startIndex; i < endIndex; i++) {
            EntityType type = AVAILABLE_TYPES.get(i);
            int slot = i - startIndex;

            Material icon = getIconForType(type);
            boolean selected = (type == dummy.getEntityType());

            inventory.setItem(slot, ItemBuilder.from(icon)
                    .name("<gold><bold>" + type.name() + "</bold></gold>")
                    .loreStrings(List.of(
                            selected ? "<green><bold>✔ Currently Selected</bold></green>" : "<gray>Click to transform into this entity model.</gray>",
                            "",
                            "<yellow>[Click to Select]</yellow>"
                    ))
                    .glow(selected)
                    .build());
        }

        // Bottom nav row
        ItemStack navFiller = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 36; i < 54; i++) {
            inventory.setItem(i, navFiller);
        }

        if (page > 0) {
            inventory.setItem(PREV_PAGE_SLOT, ItemBuilder.from(Material.ARROW)
                    .name("<yellow><bold>← Previous Page (" + page + ")</bold></yellow>")
                    .build());
        }

        inventory.setItem(BACK_SLOT, ItemBuilder.from(Material.BARRIER)
                .name("<red><bold>Back to Editor</bold></red>")
                .build());

        if (endIndex < AVAILABLE_TYPES.size()) {
            inventory.setItem(NEXT_PAGE_SLOT, ItemBuilder.from(Material.ARROW)
                    .name("<yellow><bold>Next Page (" + (page + 2) + ") →</bold></yellow>")
                    .build());
        }
    }

    private Material getIconForType(EntityType type) {
        String name = type.name() + "_SPAWN_EGG";
        Material m = Material.matchMaterial(name);
        if (m != null) return m;

        return switch (type) {
            case ARMOR_STAND -> Material.ARMOR_STAND;
            case IRON_GOLEM -> Material.IRON_BLOCK;
            case SNOW_GOLEM -> Material.SNOW_BLOCK;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case SKELETON -> Material.SKELETON_SKULL;
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case CREEPER -> Material.CREEPER_HEAD;
            case ENDERMAN -> Material.ENDER_EYE;
            case BLAZE -> Material.BLAZE_ROD;
            default -> Material.EGG;
        };
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == BACK_SLOT) {
            new DummyEditorGUI(plugin, player, dummy).open();
            return;
        }

        if (slot == PREV_PAGE_SLOT && page > 0) {
            page--;
            populate();
            soundManager.playSound(player, "gui-click", Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            return;
        }

        if (slot == NEXT_PAGE_SLOT && (page + 1) * ITEMS_PER_PAGE < AVAILABLE_TYPES.size()) {
            page++;
            populate();
            soundManager.playSound(player, "gui-click", Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
            return;
        }

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            if (!player.hasPermission("somedummy.admin") && !plugin.getConfigManager().isAllowChangeType()) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("editor-locked",
                        "<red>This dummy customization is locked by the server administrator.</red>"));
                soundManager.playSound(player, "error", Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                new DummyEditorGUI(plugin, player, dummy).open();
                return;
            }

            int index = page * ITEMS_PER_PAGE + slot;
            if (index < AVAILABLE_TYPES.size()) {
                EntityType selected = AVAILABLE_TYPES.get(index);
                dummy.setEntityType(selected);
                plugin.getStorageManager().saveDummyAsync(dummy);

                Bukkit.getRegionScheduler().run(plugin, dummy.getLocation(), t -> {
                    plugin.getEntityManager().spawnDummyEntity(dummy);
                });

                soundManager.playSound(player, "dummy-spawn", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                new DummyEditorGUI(plugin, player, dummy).open();
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
