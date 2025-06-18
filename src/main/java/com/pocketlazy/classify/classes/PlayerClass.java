package com.pocketlazy.classify.classes;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for all custom player classes.
 * Each class should extend this and implement its own logic.
 */
public abstract class PlayerClass {
    /**
     * @return The display name of the class (with color codes).
     */
    public abstract String getName();

    /**
     * @param level The class level.
     * @return The description of the class and its abilities for the given level.
     */
    public abstract String getDescription(int level);

    /**
     * Provides the ability item for the player for this class.
     * @param level The class level.
     * @param player The player.
     * @param lives The number of lives the player currently has.
     * @return The ItemStack representing the ability item.
     */
    public abstract ItemStack getAbilityItem(int level, Player player, int lives);

    /**
     * Called when the player levels up in this class.
     * @param player The player.
     * @param newLevel The new class level.
     */
    public abstract void onLevelUp(Player player, int newLevel);

    /**
     * Called when the player uses their ability item (right-click), at the given class level.
     * @param player The player.
     * @param level The class level.
     */
    public abstract void onAbilityItemUse(Player player, int level);

    /**
     * Called when the class is removed from the player (e.g. on class change),
     * so you can clear buffs, reset stats, etc.
     * This is optional and can be overridden as needed.
     * @param player The player.
     * @param level The class level.
     */
    public void onRemove(Player player, int level) {
        // Default: do nothing. Override if needed.
    }

    /**
     * Called when the class is assigned to a player (including level up).
     * Override this to refresh stats, items, passives, etc.
     * @param player The player.
     * @param level The class level.
     */
    public void onClassAssigned(Player player, int level) {
        // Default: do nothing. Override for setup!
    }

    /**
     * Returns the max lives for this class.
     * Override in subclasses if different.
     */
    public int getMaxLives() {
        return 5;
    }
}