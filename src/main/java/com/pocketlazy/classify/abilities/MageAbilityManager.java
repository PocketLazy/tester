package com.pocketlazy.classify.abilities;

import com.pocketlazy.classify.ClassifyPlugin;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class MageAbilityManager {
    private static final Map<UUID, Long> arcaneChargeCooldown = new HashMap<>();
    private static final Map<UUID, Long> overflowCooldown = new HashMap<>();
    private static final Set<UUID> arcaneCharged = new HashSet<>();

    // Returns true if Arcane Charge is currently empowered/active (for 25s after use)
    public static boolean isArcaneChargeActive(UUID uuid) {
        return arcaneCharged.contains(uuid);
    }

    // Returns seconds left on Arcane Charge cooldown (0 if ready)
    public static int getArcaneChargeCooldownSecondsLeft(Player player) {
        Long until = arcaneChargeCooldown.get(player.getUniqueId());
        if (until == null) return 0;
        long now = System.currentTimeMillis();
        return (int) Math.max(0, (until - now) / 1000);
    }

    // Begins Arcane Charge cooldown and sets arcaneCharged active for 25s
    public static void startArcaneCharge(Player player, int cooldownSec) {
        UUID uuid = player.getUniqueId();
        arcaneCharged.add(uuid);
        arcaneChargeCooldown.put(uuid, System.currentTimeMillis() + cooldownSec * 1000L);
        // Remove arcane charge effect after 25 seconds (duration of empowered beam)
        Bukkit.getScheduler().runTaskLater(ClassifyPlugin.getInstance(), () -> {
            arcaneCharged.remove(uuid);
        }, 25 * 20L); // 25s in ticks
    }

    // Returns seconds left on Overflow cooldown (0 if ready)
    public static int getOverflowCooldownSecondsLeft(Player player) {
        Long until = overflowCooldown.get(player.getUniqueId());
        if (until == null) return 0;
        long now = System.currentTimeMillis();
        return (int) Math.max(0, (until - now) / 1000);
    }

    // Begins Overflow cooldown
    public static void startOverflowCooldown(Player player, int cooldownSec) {
        overflowCooldown.put(player.getUniqueId(), System.currentTimeMillis() + cooldownSec * 1000L);
    }

    // Resets ability cooldowns/state for a player (call on class switch/level-up)
    public static void reset(Player player) {
        UUID uuid = player.getUniqueId();
        arcaneChargeCooldown.remove(uuid);
        overflowCooldown.remove(uuid);
        arcaneCharged.remove(uuid);
    }

    // Utility: get PlayerData for a player (not used, but kept for future expansion)
    private static PlayerData getPlayerData(Player player) {
        return com.pocketlazy.classify.player.PlayerClassManager.getInstance().get(player);
    }
}