package com.pocketlazy.classify.items;

import com.pocketlazy.classify.gui.ReviveGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ReviveStoneHandler implements Listener {
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        if (item.getType() == Material.NETHER_STAR &&
                item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().contains("Revive Stone")) {
            e.setCancelled(true);
            ReviveGUI.open(player);
        }
    }

    public static boolean removeReviveStone(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NETHER_STAR &&
                    item.hasItemMeta() &&
                    item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().contains("Revive Stone")) {
                if (item.getAmount() <= 1) {
                    player.getInventory().remove(item);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                player.updateInventory();
                return true;
            }
        }
        return false;
    }
}