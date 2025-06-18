package com.pocketlazy.classify.items;

import com.pocketlazy.classify.gui.ReviveGUI;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.classes.PlayerClass;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public class CustomItemsListener implements Listener {

    private static final Random RANDOM = new Random();

    @EventHandler
    public void onUseCustomItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        ItemMeta meta = item.getItemMeta();
        String display = ChatColor.stripColor(meta.getDisplayName());
        Material type = item.getType();

        PlayerData data = PlayerClassManager.getInstance().get(player);

        // --- CLASS GEMSTONE: randomize class, save/restore level ---
        if (type == Material.EMERALD && display.equalsIgnoreCase("Class Gemstone")) {
            if (data.getPlayerClass() != null) {
                data.saveCurrentClassLevel();
            }
            List<PlayerClass> classPool = PlayerClassManager.getAllRegisteredClasses();
            PlayerClass oldClass = data.getPlayerClass();
            PlayerClass newClass;
            do {
                newClass = classPool.get(RANDOM.nextInt(classPool.size()));
            } while (oldClass != null && newClass.getName().equalsIgnoreCase(oldClass.getName()) && classPool.size() > 1);

            data.setPlayerClass(newClass);
            int restoredLevel = data.restoreClassLevel(newClass);
            data.setClassLevel(restoredLevel);

            player.sendMessage(ChatColor.LIGHT_PURPLE + "Your Class Was Changed To " + ChatColor.YELLOW + newClass.getName() + ChatColor.LIGHT_PURPLE + "!");
            player.sendMessage(ChatColor.YELLOW + "Your class level for " + newClass.getName() + " is now " + restoredLevel + ".");

            consumeOne(player, item);
            event.setCancelled(true);
            return;
        }

        // --- Upgrader Geode: Amethyst Shard (upgrade class level, max 3) ---
        if (type == Material.AMETHYST_SHARD && display.equalsIgnoreCase("Upgrader Geode")) {
            int level = data.getClassLevel();
            if (level < 3) {
                data.setClassLevel(level + 1);
                player.sendMessage(ChatColor.AQUA + "Your class has been upgraded to level " + (level + 1) + "!");
                consumeOne(player, item);
            } else {
                player.sendMessage(ChatColor.RED + "Your class is already at max level!");
            }
            event.setCancelled(true);
            return;
        }

        // --- Healthy Gemstone: Diamond (+1 life, up to max) ---
        if (type == Material.DIAMOND && display.equalsIgnoreCase("Healthy Gemstone")) {
            int lives = data.getLives();
            int maxLives = 5;
            if (data.getPlayerClass() != null) {
                maxLives = data.getPlayerClass().getMaxLives();
            }
            if (lives < maxLives) {
                data.setLives(lives + 1);
                player.sendMessage(ChatColor.GREEN + "You gained a life! (" + (lives + 1) + "/" + maxLives + ")");
                consumeOne(player, item);
            } else {
                player.sendMessage(ChatColor.RED + "You are already at max lives!");
            }
            event.setCancelled(true);
            return;
        }

        // --- Charged Crystal: Quartz (max charge upgrades, max 5 uses, additive bonus logic) ---
        if (type == Material.QUARTZ && display.equalsIgnoreCase("Charged Crystal")) {
            int uses = data.getExtraChargeUses();
            int maxUses = 5;
            int toAdd;

            if (uses >= maxUses) {
                player.sendMessage(ChatColor.RED + "You cannot gain more max charge right now!");
            } else {
                toAdd = (uses == 0) ? 50 : 100;
                data.setChargeCrystalBonus(data.getChargeCrystalBonus() + toAdd);
                data.setExtraChargeUses(uses + 1);
                player.sendMessage(ChatColor.AQUA + "Your max charge increased by +" + toAdd + "! (Now " + data.getChargeCap() + ")");
                consumeOne(player, item);
            }
            event.setCancelled(true);
            return;
        }

        // --- Revive Stone: Nether Star (open revive GUI, consume on player revive only) ---
        if (type == Material.NETHER_STAR && display.equalsIgnoreCase("Revive Stone")) {
            ReviveGUI.open(player);
            event.setCancelled(true);
        }
    }

    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }
}