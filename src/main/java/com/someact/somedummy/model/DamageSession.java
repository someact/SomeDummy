package com.someact.somedummy.model;

/**
 * Tracks combat metrics, damage dealt, and real-time DPS for a player attacking a dummy.
 */
public class DamageSession {

    private double totalDamage = 0.0;
    private int hits = 0;
    private double lastDamage = 0.0;
    private long firstHitMillis = 0L;
    private long lastHitMillis = 0L;

    public void registerHit(double damage) {
        long now = System.currentTimeMillis();
        if (hits == 0 || (now - lastHitMillis) > 8000) {
            // Reset combat window if inactive for > 8 seconds
            totalDamage = 0.0;
            hits = 0;
            firstHitMillis = now;
        }

        totalDamage += damage;
        hits++;
        lastDamage = damage;
        lastHitMillis = now;
    }

    public double getDPS() {
        if (hits <= 1 || firstHitMillis == lastHitMillis) {
            return lastDamage;
        }
        double seconds = Math.max(1.0, (lastHitMillis - firstHitMillis) / 1000.0);
        return totalDamage / seconds;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public int getHits() {
        return hits;
    }

    public double getLastDamage() {
        return lastDamage;
    }

    public void reset() {
        totalDamage = 0.0;
        hits = 0;
        lastDamage = 0.0;
        firstHitMillis = 0L;
        lastHitMillis = 0L;
    }
}
