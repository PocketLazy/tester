package com.pocketlazy.classify.gui;

import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class ClassifyGUI implements Listener {

    public static final String INFO_TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Your Class Info";

    public static void openClassInfo(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        Inventory inv = Bukkit.createInventory(player, 27, INFO_TITLE);

        // Defensive: Handle if player class is null
        if (data.getPlayerClass() == null) {
            ItemStack noClass = new ItemStack(Material.BARRIER);
            ItemMeta meta = noClass.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "NO CLASS SELECTED!");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Use §e/class §7to pick a class.");
            meta.setLore(lore);
            noClass.setItemMeta(meta);
            inv.setItem(13, noClass);
            fillRestWithGlass(inv);
            player.openInventory(inv);
            return;
        }

        // Slot 11: Class icon (Nether Star)
        ItemStack classIcon = new ItemStack(Material.NETHER_STAR);
        ItemMeta classMeta = classIcon.getItemMeta();
        classMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "CLASS: " + data.getPlayerClass().getName());
        List<String> lore = new ArrayList<>();

        // Hypixel Skyblock style description
        String description = data.getPlayerClass().getDescription(data.getClassLevel());
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Ability:");
        for (String line : wrapText(description, 35)) {
            lore.add(ChatColor.YELLOW + " " + line);
        }
        lore.add("");

        lore.add(ChatColor.BLUE + "" + ChatColor.BOLD + "✦ Level: " + ChatColor.WHITE + data.getClassLevel());
        lore.add(ChatColor.RED + "" + ChatColor.BOLD + "♥ Lives: " + ChatColor.WHITE + data.getLives());
        // Show appropriate charge type
        if (data.isGhost()) {
            lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "☁ Spirit Charge: " + ChatColor.WHITE + data.getSpiritCharge() + "/" + PlayerData.getMaxSpiritCharge());
        } else {
            lore.add(ChatColor.AQUA + "" + ChatColor.BOLD + "⚡ Charge: " + ChatColor.WHITE + data.getCharge() + "/" + data.getChargeCap());
        }
        classMeta.setLore(lore);
        classIcon.setItemMeta(classMeta);
        inv.setItem(11, classIcon);

        // Slot 13: Ghost status or player head
        ItemStack statusItem;
        if (data.isGhost()) {
            statusItem = new ItemStack(Material.SKELETON_SKULL);
            ItemMeta ghostMeta = statusItem.getItemMeta();
            ghostMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Status: Ghost");
            ghostMeta.setLore(Arrays.asList(
                    ChatColor.DARK_GRAY + "You are currently eliminated.",
                    ChatColor.GRAY + "Use a " + ChatColor.LIGHT_PURPLE + "Revive Stone" + ChatColor.GRAY + " to return!"
            ));
            statusItem.setItemMeta(ghostMeta);
        } else {
            statusItem = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta skullMeta = (SkullMeta) statusItem.getItemMeta();
            try {
                skullMeta.setOwningPlayer(player);
            } catch (Exception ignore) {}
            skullMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Status: Alive");
            skullMeta.setLore(Collections.singletonList(
                    ChatColor.GRAY + "You are " + ChatColor.GREEN + "alive" + ChatColor.GRAY + " and able to fight!"
            ));
            statusItem.setItemMeta(skullMeta);
        }
        inv.setItem(13, statusItem);

        // Slot 15: Ability item (example: show current ability)
        ItemStack abilityItem = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta abilityMeta = abilityItem.getItemMeta();
        abilityMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Class Ability");
        abilityMeta.setLore(Arrays.asList(
                ChatColor.GOLD + "Each class has its own ability.",
                ChatColor.DARK_GRAY + "Level up to unlock more power!"
        ));
        abilityItem.setItemMeta(abilityMeta);
        inv.setItem(15, abilityItem);

        fillRestWithGlass(inv);
        player.openInventory(inv);
    }

    // Fill all empty slots with locked glass (for slot locking)
    private static void fillRestWithGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GRAY + " ");
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(INFO_TITLE)) return;
        // Cancel ALL interactions (move, pickup, drop, swap, etc.)
        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals(INFO_TITLE)) {
            // Cancel ALL drag actions in this GUI
            e.setCancelled(true);
        }
    }

    // Utility for wrapping lore text
    private static List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (ChatColor.stripColor(line.toString()).length() + word.length() + 1 > maxLineLength) {
                lines.add(line.toString().trim());
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (line.length() > 0) {
            lines.add(line.toString().trim());
        }
        return lines;
    }
}