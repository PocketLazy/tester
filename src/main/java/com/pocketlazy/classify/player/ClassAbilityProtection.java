package com.pocketlazy.classify.player;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.PlayerInventory;

public class ClassAbilityProtection implements Listener {

    // Checks if an item is the class ability item for the player
    private boolean isClassAbilityItem(ItemStack item, Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data.getPlayerClass() == null) return false;
        ItemStack ability = data.getPlayerClass().getAbilityItem(data.getClassLevel(), player, data.getLives());
        if (ability == null || item == null) return false;
        if (!item.hasItemMeta() || !ability.hasItemMeta()) return false;
        return item.getType() == ability.getType()
                && item.getItemMeta().hasDisplayName()
                && ability.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(ability.getItemMeta().getDisplayName());
    }

    // Prevents moving the class ability item from slot 8 or into inventories
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        // Prevent any interaction with the ability item, especially slot 8 (9th slot)
        if (isClassAbilityItem(clicked, player)) {
            // Prevent picking up from slot 8 or moving to any slot or inventory
            e.setCancelled(true);

            // If somehow removed, restore after a tick
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
                giveAbilityItem(player);
            }, 1L);
        } else if (e.getRawSlot() == 8) {
            // Prevent placing anything into slot 8
            e.setCancelled(true);
        }
    }

    // Prevent shift-drag and normal drag from moving ability item
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        // If any of the slots being dragged over includes slot 8 (9th slot), cancel
        if (e.getRawSlots().contains(8)) {
            e.setCancelled(true);
        }

        // If the item being dragged is the ability item, cancel
        ItemStack dragged = e.getOldCursor();
        if (isClassAbilityItem(dragged, player)) {
            e.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
                giveAbilityItem(player);
            }, 1L);
        }
    }

    // Prevent dropping the ability item with Q/drop key
    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        ItemStack drop = e.getItemDrop().getItemStack();
        if (isClassAbilityItem(drop, player)) {
            e.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
                giveAbilityItem(player);
            }, 1L);
        }
    }

    // Prevent dropping on death, restore after respawn
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        ItemStack ability = null;
        for (ItemStack drop : e.getDrops()) {
            if (isClassAbilityItem(drop, player)) {
                ability = drop.clone();
                e.getDrops().remove(drop);
                break;
            }
        }
        if (ability != null) {
            ItemStack abilityItem = ability;
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Classify");
            new BukkitRunnable() {
                @Override
                public void run() {
                    giveAbilityItem(player);
                }
            }.runTaskLater(plugin, 10L); // give a half second after respawn
        }
    }

    // Prevent offhand swap with F
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isClassAbilityItem(main, player) || isClassAbilityItem(off, player)) {
            e.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
                giveAbilityItem(player);
            }, 1L);
        }
    }

    // Always keep the ability item in slot 8; if swapped, restore it
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
            fixAbilitySlot(player);
        }, 1L);
    }

    // On join/quit, fix the slot as well
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
            giveAbilityItem(player);
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
            fixAbilitySlot(player);
        }, 1L);
    }

    // Utility: Always restore the ability item in slot 8
    public static void giveAbilityItem(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data.getPlayerClass() == null) return;
        ItemStack ability = data.getPlayerClass().getAbilityItem(data.getClassLevel(), player, data.getLives());
        if (ability == null) return;
        PlayerInventory inv = player.getInventory();
        inv.setItem(8, ability); // 9th slot
    }

    // Utility: Ensure only slot 8 has the ability item
    public static void fixAbilitySlot(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data.getPlayerClass() == null) return;
        ItemStack ability = data.getPlayerClass().getAbilityItem(data.getClassLevel(), player, data.getLives());
        if (ability == null) return;
        PlayerInventory inv = player.getInventory();
        // Remove ability item from all slots except slot 8
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != 8) {
                ItemStack stack = inv.getItem(i);
                if (stack != null && stack.isSimilar(ability)) {
                    inv.setItem(i, null);
                }
            }
        }
        // Restore ability item in slot 8
        inv.setItem(8, ability);
    }
}