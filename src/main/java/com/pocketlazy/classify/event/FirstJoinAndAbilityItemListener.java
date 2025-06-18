package com.pocketlazy.classify.event;

import com.pocketlazy.classify.classes.*;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Random;

public class FirstJoinAndAbilityItemListener implements Listener {
    private static final List<PlayerClass> CLASSES = List.of(
            new Berserker(), new Archer(), new Assassin(), new Healer(), new Mage()
    );

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (!data.isGhost() && data.getPlayerClass() == null) {
            PlayerClass chosen = CLASSES.get(new Random().nextInt(CLASSES.size()));
            data.setPlayerClass(chosen);
            data.setClassLevel(1);
            player.sendMessage("§aYou have been assigned: " + chosen.getName());
            giveAbilityItem(player, chosen, 1, data.getLives());
        } else if (!data.isGhost() && data.getPlayerClass() != null) {
            giveAbilityItem(player, data.getPlayerClass(), data.getClassLevel(), data.getLives());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> {
            PlayerData data = PlayerClassManager.getInstance().get(player);
            if (!data.isGhost() && data.getPlayerClass() != null)
                giveAbilityItem(player, data.getPlayerClass(), data.getClassLevel(), data.getLives());
        }, 2L);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().equals(e.getPlayer().getInventory().getItem(8)))
            e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getSlot() == 8 && e.getWhoClicked() instanceof Player)
            e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || e.getHand() == null || e.getHand().name().contains("OFF")) return;
        if (item.equals(player.getInventory().getItem(8))) {
            PlayerData data = PlayerClassManager.getInstance().get(player);
            if (!data.isGhost() && data.getPlayerClass() != null)
                data.getPlayerClass().onAbilityItemUse(player, data.getClassLevel());
            e.setCancelled(true);
        }
    }

    public static void giveAbilityItem(Player player, PlayerClass pclass, int level, int lives) {
        ItemStack item = pclass.getAbilityItem(level, player, lives);
        player.getInventory().setItem(8, item);
    }
}