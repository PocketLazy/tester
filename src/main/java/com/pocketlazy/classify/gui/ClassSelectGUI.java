package com.pocketlazy.classify.gui;

import com.pocketlazy.classify.classes.*;
import com.pocketlazy.classify.classes.ClassType;
import com.pocketlazy.classify.items.AbilityItemManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ClassSelectGUI implements Listener {
    public static final String GUI_TITLE = ChatColor.GOLD + "Select Your Class"; // made public

    // Map slot to ClassType for layout
    private static final Map<Integer, ClassType> slotToClass = new HashMap<>();
    static {
        slotToClass.put(1, ClassType.BERSERKER);
        slotToClass.put(3, ClassType.ARCHER);
        slotToClass.put(4, ClassType.ASSASSIN);
        slotToClass.put(5, ClassType.HEALER);
        slotToClass.put(7, ClassType.MAGE);
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);

        for (Map.Entry<Integer, ClassType> entry : slotToClass.entrySet()) {
            PlayerClass clazz = getClassInstance(entry.getValue());
            ItemStack item = clazz.getAbilityItem(1, player, 3);
            inv.setItem(entry.getKey(), item);
        }
        player.openInventory(inv);
    }

    private static PlayerClass getClassInstance(ClassType type) {
        return switch (type) {
            case BERSERKER -> new Berserker();
            case ARCHER -> new Archer();
            case ASSASSIN -> new Assassin();
            case HEALER -> new Healer();
            case MAGE -> new Mage();
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (!slotToClass.containsKey(slot)) return;

        Player player = (Player) e.getWhoClicked();
        // Assign the class corresponding to the clicked slot
        ClassType selectedType = slotToClass.get(slot);
        PlayerClass selectedClass = getClassInstance(selectedType);

        PlayerData data = PlayerClassManager.getInstance().get(player);
        data.setPlayerClass(selectedClass);
        data.setClassLevel(1);
        AbilityItemManager.giveAbilityItem(player, selectedClass, 1, data.getLives());

        player.sendMessage(ChatColor.GREEN + "You have selected: " + selectedClass.getName());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        player.closeInventory();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title.equals(GUI_TITLE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        String title = e.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;

        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data.getPlayerClass() == null && !data.isGhost()) {
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Classify"), () -> open(player), 2L);
        }
    }
}