package com.pocketlazy.classify.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CustomItems {
    public static ItemStack getClassCrystal() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bClass Crystal");
        meta.setCustomModelData(1001);
        meta.setLore(List.of(
                "§aConsumable",
                "§7Use: Unlocks or upgrades your class.",
                "§eClick to consume!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getUpgraderGeode() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dUpgrader Geode");
        meta.setCustomModelData(1002);
        meta.setLore(List.of(
                "§aConsumable",
                "§7Use: Upgrades the level of your class.",
                "§eClick to consume!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getHealthyGemstone() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aHealthy Gemstone");
        meta.setCustomModelData(1003);
        meta.setLore(List.of(
                "§aConsumable",
                "§7Use: Grants an extra life.",
                "§eClick to consume!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    // Updated: Charged Crystal increases max charge (not restore)
    public static ItemStack getChargedCrystal() {
        ItemStack item = new ItemStack(Material.QUARTZ);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eCharged Crystal");
        meta.setCustomModelData(1004);
        meta.setLore(List.of(
                "§aConsumable",
                "§6Use: Increases your §bMax Charge§6!",
                "§7First use: §b+50 Max Charge",
                "§7Each use after: §b+100 Max Charge",
                "",
                "§8\"A mysterious crystal radiating pure energy.\"",
                "§eClick to consume!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getReviveStone() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fRevive Stone");
        meta.setCustomModelData(1005);
        meta.setLore(List.of(
                "§aConsumable",
                "§7Use: Revives a ghost player.",
                "§eClick to consume near a ghost!"
        ));
        item.setItemMeta(meta);
        return item;
    }
}