package com.pocketlazy.classify.abilities;

import org.bukkit.entity.Player;

import java.util.*;

public class AssassinAbilityManager {

    private static final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private static final Map<UUID, Long> backstabCooldowns = new HashMap<>();
    private static final Map<UUID, Long> cloakCooldowns = new HashMap<>();
    // Track cloaked players by UUID for packet hiding
    private static final Set<UUID> cloakedPlayers = new HashSet<>();

    // Cooldown durations in seconds
    public static final int DASH_COOLDOWN = 25;
    public static final int BACKSTAB_COOLDOWN = 90;
    public static final int CLOAK_COOLDOWN = 120;
    public static final int CLOAK_DURATION = 60;

    // ----- Dash -----
    public static boolean isDashOnCooldown(Player player) {
        return getDashCooldownSecondsLeft(player) > 0;
    }
    public static int getDashCooldownSecondsLeft(Player player) {
        return getSecondsLeft(dashCooldowns, player, DASH_COOLDOWN);
    }
    public static void startDashCooldown(Player player) {
        dashCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // ----- Backstab -----
    public static boolean isBackstabOnCooldown(Player player) {
        return getBackstabCooldownSecondsLeft(player) > 0;
    }
    public static int getBackstabCooldownSecondsLeft(Player player) {
        return getSecondsLeft(backstabCooldowns, player, BACKSTAB_COOLDOWN);
    }
    public static void startBackstabCooldown(Player player) {
        backstabCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // ----- Cloak -----
    public static boolean isCloakOnCooldown(Player player) {
        return getCloakCooldownSecondsLeft(player) > 0;
    }
    public static int getCloakCooldownSecondsLeft(Player player) {
        return getSecondsLeft(cloakCooldowns, player, CLOAK_COOLDOWN);
    }
    public static void startCloakCooldown(Player player) {
        cloakCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // ----- Cloak Effect -----
    public static boolean isCloaked(Player player) {
        return cloakedPlayers.contains(player.getUniqueId());
    }
    /**
     * Sets a player as cloaked (true) or uncloaked (false).
     * Use this to control whether armor should be hidden via ProtocolLib.
     */
    public static void setCloaked(Player player, boolean state) {
        if (state) cloakedPlayers.add(player.getUniqueId());
        else cloakedPlayers.remove(player.getUniqueId());
    }
    public static void removeCloak(Player player) {
        cloakedPlayers.remove(player.getUniqueId());
    }

    // Call this when cloak begins
    public static void startCloak(Player player) {
        setCloaked(player, true);
    }
    // Call this when cloak ends
    public static void endCloak(Player player) {
        setCloaked(player, false);
    }

    // ----- Reset all Assassin states for player -----
    public static void reset(Player player) {
        dashCooldowns.remove(player.getUniqueId());
        backstabCooldowns.remove(player.getUniqueId());
        cloakCooldowns.remove(player.getUniqueId());
        cloakedPlayers.remove(player.getUniqueId());
    }

    // ----- Internal -----
    private static int getSecondsLeft(Map<UUID, Long> cooldowns, Player player, int cooldownSeconds) {
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long elapsed = System.currentTimeMillis() - last;
        int left = (int) (cooldownSeconds - elapsed / 1000L);
        return Math.max(left, 0);
    }
}