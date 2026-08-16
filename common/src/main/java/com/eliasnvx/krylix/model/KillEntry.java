package com.eliasnvx.krylix.model;

import java.util.Objects;

public class KillEntry {
    private final String killerName;
    private final String killerUUIDString;
    private final String victimName;
    private final String victimUUIDString;
    private final float killerHealth;
    private final String weaponName;
    private final Double distance;
    private final long timestamp;
    private final boolean isHeadshot;
    private final boolean isSmash;
    private final boolean isCritical;

    public KillEntry(String killerName, String killerUUIDString, String victimName, String victimUUIDString, 
                     float killerHealth, String weaponName, Double distance, long timestamp, 
                     boolean isHeadshot, boolean isSmash, boolean isCritical) {
        this.killerName = killerName;
        this.killerUUIDString = killerUUIDString;
        this.victimName = victimName;
        this.victimUUIDString = victimUUIDString;
        this.killerHealth = killerHealth;
        this.weaponName = weaponName;
        this.distance = distance;
        this.timestamp = timestamp;
        this.isHeadshot = isHeadshot;
        this.isSmash = isSmash;
        this.isCritical = isCritical;
    }

    public String getKillerName() { return killerName; }
    public String getKillerUUIDString() { return killerUUIDString; }
    public String getVictimName() { return victimName; }
    public String getVictimUUIDString() { return victimUUIDString; }
    public float getKillerHealth() { return killerHealth; }
    public String getWeaponName() { return weaponName; }
    public Double getDistance() { return distance; }
    public long getTimestamp() { return timestamp; }
    public boolean isHeadshot() { return isHeadshot; }
    public boolean isSmash() { return isSmash; }
    public boolean isCritical() { return isCritical; }

    public boolean isLongshot() {
        double dist = distance != null ? distance : 0.0;
        if (dist < 30.0) return false;
        if (weaponName == null) return false;
        String w = weaponName.toLowerCase();
        return w.contains("bow") || w.contains("trident") || w.contains("arrow") || w.contains("firework");
    }

    public boolean isExpired(int fadeTime) {
        double ageInSeconds = (System.currentTimeMillis() - timestamp) / 1000.0;
        return ageInSeconds > fadeTime;
    }

    public float getAlpha(int fadeTime) {
        double ageInSeconds = (System.currentTimeMillis() - timestamp) / 1000.0;
        double fadeStartTime = fadeTime - 1.0;

        if (ageInSeconds < fadeStartTime) {
            return 1.0f;
        } else if (ageInSeconds >= fadeTime) {
            return 0.0f;
        } else {
            double fadeProgress = (ageInSeconds - fadeStartTime) / 1.0;
            return Math.max(0.0f, Math.min(1.0f, 1.0f - (float) fadeProgress));
        }
    }

    public boolean isEnvironmentalDeath() {
        return killerName == null;
    }

    public boolean isSuicide() {
        return Objects.equals(killerName, victimName);
    }
}
