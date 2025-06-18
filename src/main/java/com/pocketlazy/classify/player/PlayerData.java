package com.pocketlazy.classify.player;

import com.pocketlazy.classify.ClassifyPlugin;
import com.pocketlazy.classify.classes.PlayerClass;
import com.pocketlazy.classify.util.ActionBarManager; // <-- Add this import
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private PlayerClass playerClass;
    private int classLevel;
    private int lives;
    private int charge;
    private int baseChargeCap;
    private int chargeCrystalBonus;
    private int mageChargeBonus; // runtime only, not persisted
    private boolean isGhost;
    private int spiritCharge;
    private int extraChargeUses = 0;

    // For Healer/Mage/etc.
    private int healCharges = 0;
    private int chargedGemstones = 0;
    private int healthyGems = 0;
    private int chargedGems = 0;
    private long lastAbilityUse = 0;

    // Per-class level memory
    private final Map<String, Integer> lastClassLevels = new HashMap<>();

    // --- ACTION BAR PRIORITY MESSAGE SYSTEM ---
    private transient long priorityActionBarUntil = 0;
    private transient String priorityActionBarMessage = null;

    public static final int DEFAULT_CHARGE_CAP = 50;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.classLevel = 1;
        this.lives = 3;
        this.charge = 50;
        this.baseChargeCap = 50;
        this.chargeCrystalBonus = 0;
        this.mageChargeBonus = 0;
        this.isGhost = false;
        this.spiritCharge = 100;
    }

    public UUID getUuid() { return uuid; }
    public PlayerClass getPlayerClass() { return playerClass; }
    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
        Player player = Bukkit.getPlayer(uuid);
        if (playerClass != null && player != null) {
            playerClass.onClassAssigned(player, this.classLevel);
        }
        updateActionBar();
    }
    public int getClassLevel() { return classLevel; }
    public void setClassLevel(int classLevel) {
        this.classLevel = classLevel;
        Player player = Bukkit.getPlayer(uuid);
        if (playerClass != null && player != null) {
            playerClass.onLevelUp(player, classLevel);
            playerClass.onClassAssigned(player, classLevel);
        }
        updateActionBar();
    }
    public int getLives() { return lives; }
    public void setLives(int lives) {
        this.lives = lives;
        updateActionBar();
    }
    public int getCharge() { return isGhost ? 0 : charge; }
    public void setCharge(int charge) {
        if (!isGhost) {
            this.charge = Math.max(0, Math.min(charge, getChargeCap()));
            updateActionBar();
        }
    }
    public int getBaseChargeCap() { return isGhost ? 0 : baseChargeCap; }
    public void setBaseChargeCap(int baseChargeCap) { if (!isGhost) { this.baseChargeCap = baseChargeCap; updateActionBar(); } }
    public int getChargeCrystalBonus() { return isGhost ? 0 : chargeCrystalBonus; }
    public void setChargeCrystalBonus(int chargeCrystalBonus) { if (!isGhost) { this.chargeCrystalBonus = chargeCrystalBonus; updateActionBar(); } }
    public int getMageChargeBonus() { return isGhost ? 0 : mageChargeBonus; }
    public void setMageChargeBonus(int mageChargeBonus) { if (!isGhost) { this.mageChargeBonus = mageChargeBonus; updateActionBar(); } }
    /**
     * The total charge cap is the sum of base, crystal, and mage cap.
     */
    public int getChargeCap() { return isGhost ? 0 : (getBaseChargeCap() + getChargeCrystalBonus() + getMageChargeBonus()); }
    @Deprecated
    public void setChargeCap(int ignored) { /* do nothing: use base/crystal/mage setters instead */ }

    public boolean isGhost() { return isGhost; }
    public void setGhost(boolean ghost) {
        this.isGhost = ghost;
        updateActionBar();
    }
    public int getSpiritCharge() { return isGhost ? spiritCharge : 0; }
    public void setSpiritCharge(int spiritCharge) {
        if (isGhost) {
            this.spiritCharge = Math.max(0, Math.min(getMaxSpiritCharge(), spiritCharge));
            updateActionBar();
        }
    }
    public int getExtraChargeUses() { return extraChargeUses; }
    public void setExtraChargeUses(int uses) { this.extraChargeUses = uses; }

    // For Healer/Mage/etc.
    public int getHealCharges() { return healCharges; }
    public void setHealCharges(int healCharges) { this.healCharges = healCharges; }

    public int getChargedGemstones() { return chargedGemstones; }
    public void setChargedGemstones(int chargedGemstones) { this.chargedGemstones = chargedGemstones; }

    public int getHealthyGems() { return healthyGems; }
    public void setHealthyGems(int healthyGems) { this.healthyGems = healthyGems; }

    public int getChargedGems() { return chargedGems; }
    public void setChargedGems(int chargedGems) { this.chargedGems = chargedGems; }

    public long getLastAbilityUse() { return lastAbilityUse; }
    public void setLastAbilityUse(long lastAbilityUse) { this.lastAbilityUse = lastAbilityUse; }

    // Save current level for this class
    public void saveCurrentClassLevel() {
        if (playerClass != null) {
            lastClassLevels.put(playerClass.getName().toLowerCase(), classLevel);
        }
    }
    // Restore last known level for a class, or 1 if not found
    public int restoreClassLevel(PlayerClass newClass) {
        return lastClassLevels.getOrDefault(newClass.getName().toLowerCase(), 1);
    }

    public static int getMaxSpiritCharge() {
        return 100;
    }

    // --- PRIORITY ACTION BAR ---
    /**
     * Show a temporary action bar message that takes priority over the normal bar.
     * @param message Message to display
     * @param durationTicks Duration in ticks (20 ticks = 1 second)
     */
    public void showPriorityActionBar(String message, int durationTicks) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        this.priorityActionBarMessage = message;
        this.priorityActionBarUntil = System.currentTimeMillis() + durationTicks * 50L;
        player.sendActionBar(message);

        // Schedule reset after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= priorityActionBarUntil) {
                    priorityActionBarMessage = null;
                    priorityActionBarUntil = 0;
                    updateActionBar(); // Resume normal bar
                }
            }
        }.runTaskLater(ClassifyPlugin.getInstance(), durationTicks);
    }

    // --- UNIVERSAL ACTION BAR UI ---
    /**
     * Call this to update the action bar for this player (e.g. after charge/lives/class change).
     * If a priority message is active, the normal bar will NOT be shown.
     */
    public void updateActionBar() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        // PREVENT HIGHLIGHT/TRACKER FLICKER: skip if tracking/player is locked by tracker
        if (!ActionBarManager.canSend(player)) return;

        // If priority message is active, don't override it
        if (priorityActionBarMessage != null && System.currentTimeMillis() < priorityActionBarUntil) {
            player.sendActionBar(priorityActionBarMessage);
            return;
        }

        String gap = "               "; // 15 spaces for a big gap (Hypixel style)

        String left;
        String right;

        if (isGhost) {
            left = "" + ChatColor.DARK_GRAY + "☠ Ghost";
            right = "" + ChatColor.DARK_PURPLE + getSpiritCharge() + ChatColor.DARK_GRAY + "/" +
                    ChatColor.DARK_PURPLE + getMaxSpiritCharge() + ChatColor.DARK_PURPLE + "👻";
        } else {
            left = "" + ChatColor.RED + getLives() + ChatColor.DARK_GRAY + "/" + ChatColor.RED +
                    ClassifyPlugin.getInstance().getMaxLives() + ChatColor.RED + "❤";
            right = "" + ChatColor.AQUA + getCharge() + ChatColor.DARK_GRAY + "/" +
                    ChatColor.AQUA + getChargeCap() + ChatColor.AQUA + "⚡";
        }

        String bar = left + gap + right;
        player.sendActionBar(bar);
    }
}