package com.someact.somedummy.api.event;

import com.someact.somedummy.model.DummyData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a target dummy is attacked.
 */
public class DummyDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player damager;
    private final DummyData dummyData;
    private double damage;
    private boolean critical;
    private boolean cancelled = false;

    public DummyDamageEvent(Player damager, DummyData dummyData, double damage, boolean critical) {
        this.damager = damager;
        this.dummyData = dummyData;
        this.damage = damage;
        this.critical = critical;
    }

    public Player getDamager() {
        return damager;
    }

    public DummyData getDummyData() {
        return dummyData;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
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
