package com.pocketlazy.classify.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GhostItems {
    public static ItemStack getPhantomAbilityItem() {
        ItemStack item = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Ghost Ability: Phantom");
        meta.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "Ability: " + ChatColor.LIGHT_PURPLE + "Phantom " + ChatColor.GRAY + "(Right Click)",
                ChatColor.GRAY + "Become visible and glow for " + ChatColor.GREEN + "1 minute",
                ChatColor.GRAY + "Your head appears on your helmet slot.",
                "",
                ChatColor.DARK_GRAY + "Spirit Charge Cost: " + ChatColor.AQUA + "50",
                ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "2m",
                "",
                ChatColor.DARK_GRAY + "Ability: " + ChatColor.LIGHT_PURPLE + "Spectral Flight " + ChatColor.GRAY + "(Shift Right Click)",
                ChatColor.GRAY + "Fly and glow for " + ChatColor.GREEN + "1 minute",
                ChatColor.GRAY + "No helmet while flying.",
                "",
                ChatColor.DARK_GRAY + "Spirit Charge Cost: " + ChatColor.AQUA + "100",
                ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "2m",
                "",
                ChatColor.DARK_GRAY + "Passive: " + ChatColor.GRAY + "Punch to blind for " + ChatColor.GREEN + "5s" + ChatColor.GRAY + " (25 Spirit Charge)"
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);
        meta.setCustomModelData(1); // Optional: For resource pack icon
        item.setItemMeta(meta);
        // Add glint
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        return item;
    }

    public static boolean matches(ItemStack test, ItemStack ref) {
        if (test == null || ref == null) return false;
        if (test.getType() != ref.getType()) return false;
        if (!test.hasItemMeta() || !ref.hasItemMeta()) return false;
        ItemMeta tm = test.getItemMeta(), rm = ref.getItemMeta();
        return tm.hasDisplayName() && rm.hasDisplayName() && tm.getDisplayName().equals(rm.getDisplayName());
    }
}