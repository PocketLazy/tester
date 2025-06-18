package com.pocketlazy.classify.recipes;

import com.pocketlazy.classify.items.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class Recipes {

    public static void registerAll(Plugin plugin) {
        // Class Crystal: Shown as
        // [AMETHYST_SHARD][DIAMOND_BLOCK][AMETHYST_SHARD]
        // [TOTEM][NETHER_STAR][TOTEM]
        // [AMETHYST_SHARD][DIAMOND_BLOCK][AMETHYST_SHARD]
        ShapedRecipe classCrystal = new ShapedRecipe(new NamespacedKey(plugin, "class_crystal"), CustomItems.getClassCrystal());
        classCrystal.shape("ADA", "TNT", "ADA");
        classCrystal.setIngredient('A', Material.AMETHYST_SHARD);
        classCrystal.setIngredient('D', Material.DIAMOND_BLOCK);
        classCrystal.setIngredient('T', Material.TOTEM_OF_UNDYING);
        classCrystal.setIngredient('N', Material.NETHER_STAR);
        Bukkit.addRecipe(classCrystal);

        // Upgrader Geode: Shown as
        // [DIAMOND_BLOCK][AMETHYST_SHARD][DIAMOND_BLOCK]
        // [ENCHANTED_GOLDEN_APPLE][NETHERITE_INGOT][ENCHANTED_GOLDEN_APPLE]
        // [DIAMOND_BLOCK][AMETHYST_SHARD][DIAMOND_BLOCK]
        ShapedRecipe upgraderGeode = new ShapedRecipe(new NamespacedKey(plugin, "upgrader_geode"), CustomItems.getUpgraderGeode());
        upgraderGeode.shape("DAD", "ENE", "DAD");
        upgraderGeode.setIngredient('D', Material.DIAMOND_BLOCK);
        upgraderGeode.setIngredient('A', Material.AMETHYST_SHARD);
        upgraderGeode.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
        upgraderGeode.setIngredient('N', Material.NETHERITE_INGOT);
        Bukkit.addRecipe(upgraderGeode);

        // Healthy Gemstone: Shown as
        // [GOLD_BLOCK][GOLDEN_APPLE][TOTEM]
        // [GLISTERING_MELON][ENCHANTED_GOLDEN_APPLE][GLISTERING_MELON]
        // [TOTEM][GOLDEN_APPLE][GOLD_BLOCK]
        ShapedRecipe healthyGem = new ShapedRecipe(new NamespacedKey(plugin, "healthy_gemstone"), CustomItems.getHealthyGemstone());
        healthyGem.shape("GAT", "MEM", "TAG");
        healthyGem.setIngredient('G', Material.GOLD_BLOCK);
        healthyGem.setIngredient('A', Material.GOLDEN_APPLE);
        healthyGem.setIngredient('T', Material.TOTEM_OF_UNDYING);
        healthyGem.setIngredient('M', Material.GLISTERING_MELON_SLICE);
        healthyGem.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
        Bukkit.addRecipe(healthyGem);

        // Charged Crystal: Shown as
        // [DRAGON_BREATH][BLAZE_ROD][LAPIS_BLOCK]
        // [NETHERITE_INGOT][SUGAR][NETHERITE_INGOT]
        // [LAPIS_BLOCK][BLAZE_ROD][DRAGON_BREATH]
        ShapedRecipe chargedCrystal = new ShapedRecipe(new NamespacedKey(plugin, "charged_crystal"), CustomItems.getChargedCrystal());
        chargedCrystal.shape("DBL", "NSN", "LBD");
        chargedCrystal.setIngredient('D', Material.DRAGON_BREATH);
        chargedCrystal.setIngredient('B', Material.BLAZE_ROD);
        chargedCrystal.setIngredient('L', Material.LAPIS_BLOCK);
        chargedCrystal.setIngredient('N', Material.NETHERITE_INGOT);
        chargedCrystal.setIngredient('S', Material.SUGAR);
        Bukkit.addRecipe(chargedCrystal);

        // Revive Stone: Shown as
        // [null][NETHER_STAR][null]
        // [EMERALD][SKELETON_SKULL][EMERALD]
        // [null][NETHER_STAR][null]
        ShapedRecipe reviveStone = new ShapedRecipe(new NamespacedKey(plugin, "revive_stone"), CustomItems.getReviveStone());
        reviveStone.shape(" N ", "ESE", " N ");
        reviveStone.setIngredient('N', Material.NETHER_STAR);
        reviveStone.setIngredient('E', Material.EMERALD);
        reviveStone.setIngredient('S', Material.SKELETON_SKULL);
        Bukkit.addRecipe(reviveStone);
    }
}