package com.pocketlazy.classify.util;

import org.bukkit.entity.Player;

public class ClassifyTagUtil {
    public static final String TAG = "invis_tag";

    /** Add the invis_tag if not present. */
    public static void addTag(Player player) {
        if (!player.getScoreboardTags().contains(TAG)) {
            player.addScoreboardTag(TAG);
        }
    }
    /** Remove the invis_tag if present. */
    public static void removeTag(Player player) {
        if (player.getScoreboardTags().contains(TAG)) {
            player.removeScoreboardTag(TAG);
        }
    }
    /** Check if player has the invis_tag. */
    public static boolean hasTag(Player player) {
        return player.getScoreboardTags().contains(TAG);
    }
}