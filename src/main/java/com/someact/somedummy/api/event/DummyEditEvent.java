package com.someact.somedummy.api.event;

import com.someact.somedummy.model.DummyData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player modifies a target dummy via the Dummy Editor GUI.
 */
public class DummyEditEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final DummyData dummyData;
    private boolean cancelled = false;

    public DummyEditEvent(Player player, DummyData dummyData) {
        this.player = player;
        this.dummyData = dummyData;
    }

    public Player getPlayer() {
        return player;
    }

    public DummyData getDummyData() {
        return dummyData;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
