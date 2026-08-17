package com.someact.somedummy.listener;

import com.someact.somedummy.gui.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes inventory interaction events to SomeDummy GUI holders.
 */
public class InventoryClickListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof DummyEditorGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof EntityTypeSelectGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof EquipmentEditorGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof PlayerDummyListGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof AdminDummyManagerGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof PresetLibraryGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof AdminConfigGUI gui) {
            gui.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof EquipmentEditorGUI gui) {
            gui.handleClose(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof DummyEditorGUI || holder instanceof EntityTypeSelectGUI ||
                holder instanceof PlayerDummyListGUI || holder instanceof AdminDummyManagerGUI ||
                holder instanceof PresetLibraryGUI || holder instanceof AdminConfigGUI) {
            event.setCancelled(true);
        }
    }
}
