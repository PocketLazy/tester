package com.pocketlazy.classify.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AbilitySoundUtil {
    // Play cooldown sound (e.g. portal ambient)
    public static void playCooldownSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }
    // Play "not enough charge" sound (e.g. bass note)
    public static void playNoChargeSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, 0.7f);
    }
}