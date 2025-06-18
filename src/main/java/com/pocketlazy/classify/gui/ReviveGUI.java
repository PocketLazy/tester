package com.pocketlazy.classify.gui;

import com.pocketlazy.classify.items.ReviveStoneHandler;
import com.pocketlazy.classify.player.GhostManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.classes.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class ReviveGUI implements Listener {

    public static final String GUI_TITLE = ChatColor.LIGHT_PURPLE + "Revive a Ghost";
    private static final List<PlayerClass> CLASS_POOL = PlayerClassManager.getAllRegisteredClasses();

    public static void open(Player opener) {
        List<OfflinePlayer> ghosts = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            PlayerData data = PlayerClassManager.getInstance().get(p.getUniqueId());
            if (data != null && data.isGhost()) {
                ghosts.add(p);
            }
        }
        Inventory inv = Bukkit.createInventory(opener, 27, GUI_TITLE);

        setBorders(inv);

        int[] headSlots = {10, 11, 12, 13, 14, 15, 16};
        int h = 0;
        for (OfflinePlayer p : ghosts) {
            if (h >= headSlots.length) break;
            ItemStack skull = getGhostSkull(p);
            inv.setItem(headSlots[h++], skull);
        }

        opener.openInventory(inv);
    }

    private static void setBorders(Inventory inv) {
        ItemStack purplePane = makePane(Material.MAGENTA_STAINED_GLASS_PANE, ChatColor.DARK_PURPLE + " ");
        ItemStack grayPane = makePane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + " ");
        ItemStack cyanPane = makePane(Material.CYAN_STAINED_GLASS_PANE, ChatColor.AQUA + " ");

        int[] corners = {0, 8, 18, 26};
        for (int i : corners) inv.setItem(i, purplePane);

        for (int i = 1; i <= 7; i++) inv.setItem(i, grayPane);
        for (int i = 19; i <= 25; i++) inv.setItem(i, grayPane);

        inv.setItem(9, grayPane);
        inv.setItem(17, grayPane);

        for (int i = 10; i <= 16; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, cyanPane);
        }
    }

    private static ItemStack makePane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getGhostSkull(OfflinePlayer p) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(p);
        meta.setDisplayName((p.isOnline() ? ChatColor.AQUA : ChatColor.GRAY) + p.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Click to revive" + (p.isOnline() ? "" : ChatColor.DARK_GRAY + " (offline)"));
        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player opener)) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        OfflinePlayer target = meta.getOwningPlayer();
        PlayerData targetData = PlayerClassManager.getInstance().get(target.getUniqueId());
        if (targetData == null || !targetData.isGhost()) {
            opener.sendMessage(ChatColor.RED + (target.getName() != null ? target.getName() : "This player") + " is not a ghost!");
            opener.closeInventory();
            return;
        }

        boolean usedStone = ReviveStoneHandler.removeReviveStone(opener);

        if (!usedStone && !opener.hasPermission("classify.revive.free")) {
            opener.sendMessage(ChatColor.RED + "You don't have a Revive Stone!");
            opener.closeInventory();
            return;
        }

        GhostManager.getInstance().setGhost(target, false);

        targetData.setLives(3);
        PlayerClass randomClass = CLASS_POOL.get(new Random().nextInt(CLASS_POOL.size()));
        targetData.setPlayerClass(randomClass);
        targetData.setClassLevel(1);

        if (target.isOnline()) {
            Player tPlayer = (Player) target;
            tPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "You have been revived by " + opener.getName() + "!");
            tPlayer.playSound(tPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);
        }

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[Classify] " + (target.getName() != null ? target.getName() : "A player")
                + ChatColor.GREEN + " has been revived by " + ChatColor.AQUA + opener.getName() + ChatColor.GREEN + "!");

        opener.closeInventory();

        opener.sendMessage(ChatColor.GREEN + "Revived " + (target.getName() != null ? target.getName() : "player") + "!");
        opener.playSound(opener.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals(GUI_TITLE)) {
            e.setCancelled(true);
        }
    }
}