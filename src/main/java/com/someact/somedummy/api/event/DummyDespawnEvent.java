package com.someact.somedummy.api.event;

import com.someact.somedummy.model.DummyData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a target dummy despawns or is permanently removed.
 */
public class DummyDespawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final DummyData dummyData;
    private boolean cancelled = false;

    public DummyDespawnEvent(DummyData dummyData) {
        this.dummyData = dummyData;
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
