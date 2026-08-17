package com.someact.somedummy.api.event;

import com.someact.somedummy.model.DummyData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a target dummy is defeated/killed.
 */
public class DummyDeathEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player killer;
    private final DummyData dummyData;
    private boolean respawn;
    private int respawnDelaySeconds;
    private boolean cancelled = false;

    public DummyDeathEvent(Player killer, DummyData dummyData, boolean respawn, int respawnDelaySeconds) {
        this.killer = killer;
        this.dummyData = dummyData;
        this.respawn = respawn;
        this.respawnDelaySeconds = respawnDelaySeconds;
    }

    public Player getKiller() {
        return killer;
    }

    public DummyData getDummyData() {
        return dummyData;
    }

    public boolean isRespawn() {
        return respawn;
    }

    public void setRespawn(boolean respawn) {
        this.respawn = respawn;
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    public void setRespawnDelaySeconds(int respawnDelaySeconds) {
        this.respawnDelaySeconds = respawnDelaySeconds;
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
