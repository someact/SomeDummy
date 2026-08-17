package com.someact.somedummy.model;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Enterprise data model for a customizable target dummy instance.
 */
public class DummyData {

    private final UUID dummyId;
    private final UUID ownerUuid;
    private final String ownerName;
    private Location location;

    private EntityType entityType;
    private String customName;
    private double maxHealth;
    private boolean infiniteHealth;
    private boolean hasAi;
    private boolean hasGravity;
    private boolean isSilent;
    private boolean isSmall;
    private boolean isInvulnerable;
    private boolean respawnable;
    private int respawnDelaySeconds;
    private long lifespanSeconds;
    private boolean burnInSun;
    private boolean isStatic;
    private final long createdAtMillis;

    // Equipment
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;

    // Runtime
    private UUID currentEntityUuid;

    public DummyData(UUID dummyId, UUID ownerUuid, String ownerName, Location location) {
        this.dummyId = dummyId != null ? dummyId : UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.location = location;

        this.entityType = EntityType.ZOMBIE;
        this.customName = "<gradient:#ff7675:#fab1a0><bold>Combat Target Dummy</bold></gradient>";
        this.maxHealth = 1000.0;
        this.infiniteHealth = true;
        this.hasAi = false;
        this.hasGravity = true;
        this.isSilent = true;
        this.isSmall = false;
        this.isInvulnerable = false;
        this.respawnable = true;
        this.respawnDelaySeconds = 3;
        this.lifespanSeconds = 0L;
        this.burnInSun = false;
        this.isStatic = false;
        this.createdAtMillis = System.currentTimeMillis();
    }

    public UUID getDummyId() {
        return dummyId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public EntityType getEntityType() {
        return entityType != null ? entityType : EntityType.ZOMBIE;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType != null ? entityType : EntityType.ZOMBIE;
    }

    public String getCustomName() {
        return customName != null ? customName : "Combat Dummy";
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = Math.max(1.0, maxHealth);
    }

    public boolean isInfiniteHealth() {
        return infiniteHealth;
    }

    public void setInfiniteHealth(boolean infiniteHealth) {
        this.infiniteHealth = infiniteHealth;
    }

    public boolean hasAi() {
        return hasAi;
    }

    public void setHasAi(boolean hasAi) {
        this.hasAi = hasAi;
    }

    public boolean hasGravity() {
        return hasGravity;
    }

    public void setHasGravity(boolean hasGravity) {
        this.hasGravity = hasGravity;
    }

    public boolean isSilent() {
        return isSilent;
    }

    public void setSilent(boolean silent) {
        isSilent = silent;
    }

    public boolean isSmall() {
        return isSmall;
    }

    public void setSmall(boolean small) {
        isSmall = small;
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        isInvulnerable = invulnerable;
    }

    public boolean isRespawnable() {
        return respawnable;
    }

    public void setRespawnable(boolean respawnable) {
        this.respawnable = respawnable;
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    public void setRespawnDelaySeconds(int respawnDelaySeconds) {
        this.respawnDelaySeconds = Math.max(0, respawnDelaySeconds);
    }

    public long getLifespanSeconds() {
        return lifespanSeconds;
    }

    public void setLifespanSeconds(long lifespanSeconds) {
        this.lifespanSeconds = Math.max(0L, lifespanSeconds);
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet != null && !helmet.getType().isAir() ? helmet.clone() : null;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate != null && !chestplate.getType().isAir() ? chestplate.clone() : null;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings != null && !leggings.getType().isAir() ? leggings.clone() : null;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots != null && !boots.getType().isAir() ? boots.clone() : null;
    }

    public ItemStack getMainHand() {
        return mainHand;
    }

    public void setMainHand(ItemStack mainHand) {
        this.mainHand = mainHand != null && !mainHand.getType().isAir() ? mainHand.clone() : null;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

    public void setOffHand(ItemStack offHand) {
        this.offHand = offHand != null && !offHand.getType().isAir() ? offHand.clone() : null;
    }

    public UUID getCurrentEntityUuid() {
        return currentEntityUuid;
    }

    public void setCurrentEntityUuid(UUID currentEntityUuid) {
        this.currentEntityUuid = currentEntityUuid;
    }

    public boolean isBurnInSun() {
        return burnInSun;
    }

    public void setBurnInSun(boolean burnInSun) {
        this.burnInSun = burnInSun;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        this.isStatic = aStatic;
    }

    public boolean isExpired() {
        if (lifespanSeconds <= 0) return false;
        long elapsed = (System.currentTimeMillis() - createdAtMillis) / 1000;
        return elapsed >= lifespanSeconds;
    }

    public long getRemainingLifespanSeconds() {
        if (lifespanSeconds <= 0) return Long.MAX_VALUE;
        long elapsed = (System.currentTimeMillis() - createdAtMillis) / 1000;
        return Math.max(0, lifespanSeconds - elapsed);
    }
}
