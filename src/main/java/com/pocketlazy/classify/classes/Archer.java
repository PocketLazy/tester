package com.pocketlazy.classify.classes;

import com.pocketlazy.classify.gui.ArcherQuiverManager;
import com.pocketlazy.classify.items.AbilityItemManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.util.AbilitySoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Archer extends PlayerClass implements Listener {

    public Archer() {
        Bukkit.getPluginManager().registerEvents(this, com.pocketlazy.classify.ClassifyPlugin.getInstance());
    }

    @Override
    public String getName() { return "Archer"; }

    @Override
    public String getDescription(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GREEN + "Archer\n");
        sb.append(ChatColor.GRAY + "Class: " + ChatColor.GREEN + "Archer\n");
        sb.append(ChatColor.DARK_GREEN + "Ability: Quiver " + ChatColor.GRAY + "(Shift + Right Click)\n");
        sb.append(ChatColor.GRAY + "Open a quiver GUI to store arrows. Bows & crossbows use arrows from your quiver.\n");
        if (level == 1) sb.append(ChatColor.DARK_GREEN + "Quiver holds up to 5 stacks of arrows.\n");
        if (level == 2) sb.append(ChatColor.DARK_GREEN + "Quiver holds up to 10 stacks of arrows.\n");
        if (level >= 3) sb.append(ChatColor.DARK_GREEN + "Quiver holds up to 20 stacks of arrows.\n");
        if (level >= 2) {
            sb.append("\n" + ChatColor.DARK_GREEN + "Ability: Tracker Arrow " + ChatColor.GRAY + "(Shift + Shoot)\n");
            sb.append(ChatColor.GRAY + "Targets hit while sneaking are highlighted for 30s (only visible to you).\n");
            sb.append(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10\n");
            sb.append(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "15s\n");
        }
        if (level == 3) {
            sb.append("\n" + ChatColor.DARK_GREEN + "Ability: Wildshot " + ChatColor.GRAY + "(Right Click)\n");
            sb.append(ChatColor.GRAY + "Shoot an arrow that explodes on hit (25% max HP area damage).\n");
            sb.append(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10\n");
            sb.append(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "30s\n");
            sb.append("\n" + ChatColor.DARK_GREEN + "Ability: Shortbow " + ChatColor.GRAY + "(Left Click)\n");
            sb.append(ChatColor.GRAY + "Shoot 3 arrows at once at reduced damage (costs 10 charge, no cooldown, uses arrows from your quiver).\n");
            sb.append(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10\n");
        } else {
            sb.append("\n" + ChatColor.DARK_GREEN + "Ability: Shortbow " + ChatColor.GRAY + "(Left Click)\n");
            if (level == 1)
                sb.append(ChatColor.GRAY + "Shoot 1 arrow at reduced damage (costs 10 charge, no cooldown, uses arrows from your quiver).\n");
            else
                sb.append(ChatColor.GRAY + "Shoot 2 arrows at reduced damage (costs 10 charge, no cooldown, uses arrows from your quiver).\n");
            sb.append(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10\n");
        }
        return sb.toString();
    }

    @Override
    public ItemStack getAbilityItem(int level, Player player, int lives) {
        ItemStack item;
        ItemMeta meta;
        List<String> lore = new ArrayList<>();
        ChatColor titleColor = ChatColor.DARK_GREEN;
        ChatColor detailColor = ChatColor.GREEN;

        if (level == 1) {
            item = new ItemStack(Material.FLINT);
            meta = item.getItemMeta();
            meta.setDisplayName(titleColor + "Clawed Arrowhead");
            lore.add(detailColor + "Ability: Quiver");
            lore.add(ChatColor.GRAY + "Shift + Right Click to open quiver GUI.");
            lore.add(detailColor + "Store up to 5 stacks of arrows.");
            lore.add("");
            lore.add(titleColor + "Ability: Shortbow");
            lore.add(ChatColor.GRAY + "Left Click to shoot 1 arrow at reduced damage.");
            lore.add(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10");
        } else if (level == 2) {
            item = new ItemStack(Material.FLINT);
            meta = item.getItemMeta();
            meta.setDisplayName(titleColor + "Ferocious Arrowhead");
            lore.add(detailColor + "Ability: Quiver");
            lore.add(ChatColor.GRAY + "Shift + Right Click to open quiver GUI.");
            lore.add(detailColor + "Store up to 10 stacks of arrows.");
            lore.add("");
            lore.add(titleColor + "Ability: Tracker Arrow");
            lore.add(ChatColor.GRAY + "Shift + shoot to highlight targets for 30s.");
            lore.add(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10");
            lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "15s");
            lore.add("");
            lore.add(titleColor + "Ability: Shortbow");
            lore.add(ChatColor.GRAY + "Left Click to shoot 2 arrows at reduced damage.");
            lore.add(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10");
        } else {
            item = new ItemStack(Material.FEATHER);
            meta = item.getItemMeta();
            meta.setDisplayName(titleColor + "Astral Feather");
            lore.add(detailColor + "Ability: Quiver");
            lore.add(ChatColor.GRAY + "Shift + Right Click to open quiver GUI.");
            lore.add(detailColor + "Store up to 20 stacks of arrows.");
            lore.add("");
            lore.add(titleColor + "Ability: Tracker Arrow");
            lore.add(ChatColor.GRAY + "Shift + shoot to highlight targets for 30s.");
            lore.add(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10");
            lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "15s");
            lore.add("");
            lore.add(titleColor + "Ability: Shortbow");
            lore.add(ChatColor.GRAY + "Left Click to shoot 3 arrows at reduced damage.");
            lore.add(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10");
            lore.add("");
            lore.add(titleColor + "Ability: WildShot");
            lore.add(ChatColor.GRAY + "Right Click to shoot an explosive arrow (25% max HP area damage, no block damage).");
            lore.add(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10");
            lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "30s");
        }

        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.setCustomModelData(level == 3 ? 6003 : (level == 2 ? 6002 : 6001));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onLevelUp(Player player, int newLevel) {
        ArcherQuiverManager.saveQuiverOnLevelUpOrDeath(player);
        ArcherQuiverManager.restoreQuiverAfterLevelUpOrDeath(player);
        player.sendMessage(ChatColor.GREEN + "[Classify] You leveled up to Archer level " + newLevel + "!");
    }

    @Override
    public void onAbilityItemUse(Player player, int level) {
        // Handled by onAbilityItemInteract below
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ArcherQuiverManager.loadQuiver(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ArcherQuiverManager.saveQuiver(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        ArcherQuiverManager.saveQuiverOnLevelUpOrDeath(event.getEntity());
        ArcherQuiverManager.restoreQuiverAfterLevelUpOrDeath(event.getEntity());
    }

    @EventHandler
    public void onAbilityItemInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (!(data != null && data.getPlayerClass() instanceof Archer)) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        boolean isAbilityItem = AbilityItemManager.isAbilityItem(player, item);
        boolean isShift = player.isSneaking();
        boolean isRight = event.getAction().toString().contains("RIGHT");
        if (isAbilityItem && isShift && isRight) {
            ArcherQuiverManager.openQuiver(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuiverGUIClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Archer Quiver")) {
            Player player = (Player) event.getPlayer();
            ArcherQuiverManager.saveQuiverFromGUI(player, event.getInventory());
        }
    }

    // Example: Add cooldown/charge checks for special arrows or wildshot here
    // (Assume you call this when a special ability is triggered)
    public static boolean checkChargeAndCooldown(Player player, int cost, int cooldownSeconds, long lastUsedTime, String abilityName) {
        long now = System.currentTimeMillis();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (now - lastUsedTime < cooldownSeconds * 1000) {
            int secondsLeft = (int) ((cooldownSeconds * 1000 - (now - lastUsedTime)) / 1000) + 1;
            player.sendMessage(ChatColor.RED + "[Archer] " + abilityName + " is on cooldown! " + secondsLeft + "s left.");
            AbilitySoundUtil.playCooldownSound(player);
            return false;
        }
        if (data.getCharge() < cost) {
            player.sendMessage(ChatColor.RED + "[Archer] Not enough charge! Need " + cost + ".");
            AbilitySoundUtil.playNoChargeSound(player);
            return false;
        }
        return true;
    }
}