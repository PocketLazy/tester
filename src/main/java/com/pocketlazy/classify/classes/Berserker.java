package com.pocketlazy.classify.classes;

import com.pocketlazy.classify.abilities.BerserkerAbilityManager;
import com.pocketlazy.classify.items.AbilityItemManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Berserker extends PlayerClass implements Listener {
    // Cooldown/charge constants
    private static final int RAGE_WEAKENED_COOLDOWN = 20;
    private static final int RAGE_EMPOWERED_COOLDOWN = 15;
    private static final int RAGE_WEAKENED_COST = 50;
    private static final int RAGE_EMPOWERED_COST = 80;
    private static final int UNYIELDING_RAGE_COOLDOWN = 60;
    private static final int UNYIELDING_RAGE_COST = 80;
    private static final int INFERNO_BLADE_COOLDOWN = 600;
    private static final int INFERNO_BLADE_COST = 250;
    private static final int BLADE_DURATION = 180;
    private static final int FLAMING_SLASH_COOLDOWN = 10;
    private static final int FLAMING_SLASH_COST = 100;
    private static final int WITHERING_RAGE_COOLDOWN = 25;
    private static final int WITHERING_RAGE_COST = 120;

    public Berserker() {
        Bukkit.getPluginManager().registerEvents(this, com.pocketlazy.classify.ClassifyPlugin.getInstance());
        Bukkit.getScheduler().runTaskTimer(com.pocketlazy.classify.ClassifyPlugin.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = PlayerClassManager.getInstance().get(player);
                if (data != null && data.getPlayerClass() instanceof Berserker)
                    BerserkerAbilityManager.applyBloodlust(player);
            }
        }, 10L, 10L);
    }

    @Override
    public String getName() { return "Berserker"; }

    @Override
    public String getDescription(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("§cBerserker\n§7Class: §cBerserker\n\n");
        sb.append("§4Bloodlust (Passive)\n§7+2% damage per 10% missing health (§7max +6% at 30% HP§7)\n");
        sb.append("§8Charge Cost: None\n§8Cooldown: None\n\n");
        sb.append("§4Syphon (Passive Lifesteal)\n§715% chance to heal 1 heart (2 HP) per melee hit\n");
        sb.append("§8Cooldown: 1.5s\n§8Charge Cost: None\n");
        if (level >= 2) {
            sb.append("\n§6Unyielding Rage (On Kill Buff)\n§7On kill: Strength III, Speed II for 60s\n§7Cannot re-trigger while active\n");
            sb.append("§8Cooldown: " + UNYIELDING_RAGE_COOLDOWN + "s\n§8Charge Cost: §6" + UNYIELDING_RAGE_COST + "\n");
            sb.append("\n§6Rage (Weakened) (Shift+Right Click)\n§7Lose 10% max HP, gain +0.5 attack damage, +0.005 speed\n");
            sb.append("§8Cooldown: " + RAGE_WEAKENED_COOLDOWN + "s\n§8Charge Cost: §6" + RAGE_WEAKENED_COST + "\n");
        }
        if (level >= 3) {
            sb.append("\n§cRage (Empowered) (Shift+Right Click)\n§7Lose 10% max HP, gain +1 attack damage, +0.01 speed, +1 Entity Interaction Rage\n");
            sb.append("§8Cooldown: " + RAGE_EMPOWERED_COOLDOWN + "s\n§8Charge Cost: §c" + RAGE_EMPOWERED_COST + "\n");
            sb.append("\n§cInferno Blade (Right Click)\n§7Transform for 180s: 15 attack damage, 1.6 attack speed\n§7Disables other class abilities during this state\n");
            sb.append("§8Cooldown: §c" + INFERNO_BLADE_COOLDOWN + "s\n§8Charge Cost: §c" + INFERNO_BLADE_COST + "\n");
            sb.append("\n§cFlaming Slash (Right Click with Blade)\n§7Deals 25% of target's current HP (5% vs Wither/Ender Dragon)\n");
            sb.append("§8Cooldown: " + FLAMING_SLASH_COOLDOWN + "s\n§8Charge Cost: §c" + FLAMING_SLASH_COST + "\n");
            sb.append("\n§cWithering Rage (Shift+Right Click with Blade)\n§7Summon Wither Skull, left click fires at target (Wither II for 5s)\n");
            sb.append("§8Cooldown: " + WITHERING_RAGE_COOLDOWN + "s\n§8Charge Cost: §c" + WITHERING_RAGE_COST + "\n");
        }
        return sb.toString();
    }

    @Override
    public ItemStack getAbilityItem(int level, Player player, int lives) {
        ItemStack item;
        ItemMeta meta;
        List<String> lore = new ArrayList<>();
        if (level == 1) {
            item = new ItemStack(Material.RED_DYE);
            meta = item.getItemMeta();
            meta.setDisplayName("§cBlood Vial");
            lore.add("§7Class: §cBerserker");
            lore.add("");
            lore.add("§4Bloodlust (Passive)");
            lore.add("§7+2% damage per 10% missing health (max +6%)");
            lore.add("");
            lore.add("§4Syphon (Passive Lifesteal)");
            lore.add("§715% chance to heal 1 heart per melee hit (1.5s cd)");
        } else if (level == 2) {
            item = new ItemStack(Material.RED_DYE);
            meta = item.getItemMeta();
            meta.setDisplayName("§cBlood Vial");
            lore.add("§7Class: §cBerserker");
            lore.add("");
            lore.add("§4Bloodlust (Passive)");
            lore.add("§7+2% damage per 10% missing health (max +6%)");
            lore.add("");
            lore.add("§4Syphon (Passive Lifesteal)");
            lore.add("§715% chance to heal 1 heart per melee hit (1.5s cd)");
            lore.add("");
            lore.add("§6Unyielding Rage (On Kill Buff)");
            lore.add("§7Strength III & Speed II for 60s (80 charge, 60s cd)");
            lore.add("");
            lore.add("§6Rage (Weakened) (Shift+Right Click)");
            lore.add("§7Lose 10% max HP, gain: +0.5 AD, +0.005 speed (20s cd, 50 charge)");
        } else {
            item = new ItemStack(Material.NETHERITE_SWORD);
            meta = item.getItemMeta();
            meta.setDisplayName("§cInferno Blade");
            lore.add("§7Class: §cBerserker");
            lore.add("");
            lore.add("§4Bloodlust (Passive)");
            lore.add("§7+2% damage per 10% missing health (max +6%)");
            lore.add("");
            lore.add("§4Syphon (Passive Lifesteal)");
            lore.add("§715% chance to heal 1 heart per melee hit (1.5s cd)");
            lore.add("");
            lore.add("§cUnyielding Rage (On Kill Buff)");
            lore.add("§7Strength III & Speed II for 60s (80 charge, 60s cd)");
            lore.add("");
            lore.add("§cRage (Empowered) (Shift+Right Click)");
            lore.add("§7Lose 10% max HP, gain: +1 AD, +0.01 speed, +1 Entity Interaction Rage (15s cd, 80 charge)");
            lore.add("");
            lore.add("§cInferno Blade (Right Click)");
            lore.add("§7Transform for 180s: 15 AD, 1.6 AS, disables other class abilities (250 charge, 10m cd)");
            lore.add("");
            lore.add("§cFlaming Slash (Right Click with Blade)");
            lore.add("§7Deals 25% HP, ignites (100 charge, 10s cd)");
            lore.add("");
            lore.add("§cWithering Rage (Shift+Right Click with Blade)");
            lore.add("§7Summon Wither Skull (120 charge, 25s cd)");
        }
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setCustomModelData(level == 3 ? 7203 : (level == 2 ? 7202 : 7201));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onLevelUp(Player player, int newLevel) {
        BerserkerAbilityManager.resetAll(player);
        AbilityItemManager.giveAbilityItem(player, this, newLevel, PlayerClassManager.getInstance().get(player).getLives());
    }

    @Override
    public void onRemove(Player player, int level) {
        BerserkerAbilityManager.resetAll(player);
    }

    @Override
    public void onAbilityItemUse(Player player, int level) {
        // Optional: Can be used for shift right-click, etc. if you want
    }

    // --- Event Handlers ---

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        BerserkerAbilityManager.handleSyphon(event);
        BerserkerAbilityManager.handleBloodlust(event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        BerserkerAbilityManager.handleInteract(event);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        BerserkerAbilityManager.handleKill(event);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        BerserkerAbilityManager.resetAll(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BerserkerAbilityManager.resetAll(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // If you want to refresh passives/abilities here, do so
    }
}