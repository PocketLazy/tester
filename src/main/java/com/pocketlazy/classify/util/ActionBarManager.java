package com.pocketlazy.classify.util;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ender Dragon theme: No functional changes,
 * but all plugin systems using this class will now show Ender-themed action bars if they use suggested color codes.
 */
public final class ActionBarManager {
    // Thread-safe set for action bar lock
    private static final Set<UUID> lockedPlayers = Collections.synchronizedSet(new HashSet<>());

    private ActionBarManager() {}

    /**
     * Lock the action bar for this player, so only tracking/etc can send updates.
     */
    public static void lock(Player player) {
        if (player != null) {
            lockedPlayers.add(player.getUniqueId());
        }
    }

    /**
     * Unlock the action bar for this player, so normal plugin messages can resume.
     */
    public static void unlock(Player player) {
        if (player != null) {
            lockedPlayers.remove(player.getUniqueId());
        }
    }

    /**
     * Check if plugin features (charge/lives/etc) are allowed to send action bar messages for this player.
     * @return true if plugin is allowed to send (no lock active), false if locked.
     */
    public static boolean canSend(Player player) {
        return player != null && !lockedPlayers.contains(player.getUniqueId());
    }

    /**
     * Unlock all players (useful for plugin disable or reload).
     */
    public static void unlockAll() {
        lockedPlayers.clear();
    }
}