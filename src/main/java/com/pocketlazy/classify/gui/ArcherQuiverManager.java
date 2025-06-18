package com.pocketlazy.classify.gui;

import org.bukkit.inventory.InventoryView;
import com.pocketlazy.classify.classes.Archer;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArcherQuiverManager implements Listener {
    private static final int INVENTORY_SIZE = 27;
    private static final int QUIVER_SAVE_SIZE = 20;
    private static final Map<UUID, ItemStack[]> quiverContents = new HashMap<>();

    private static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Classify");
    }

    public static int getUnlockedSlots(int archerLevel) {
        if (archerLevel >= 3) return 20;
        if (archerLevel == 2) return 8;
        return 3;
    }

    public static String getQuiverTitle(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return "Quiver";
        int level = data.getClassLevel();
        if (level >= 3) return "§dAstral Quiver";
        if (level == 2) return "§5Ferocious Quiver";
        return "§5Clawed Quiver";
    }

    public static boolean isQuiverGUI(InventoryView view) {
        String title = view.getTitle();
        return title.contains("Quiver");
    }

    public static void openQuiver(Player player) {
        String guiTitle = getQuiverTitle(player);
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;
        int unlocked = getUnlockedSlots(data.getClassLevel());

        ItemStack[] saved = getQuiverContents(player);

        // Handle arrows in locked slots (if any) and give them to player
        boolean gaveLockedArrows = false;
        for (int i = unlocked; i < QUIVER_SAVE_SIZE; i++) {
            if (saved[i] != null && saved[i].getType() != Material.AIR) {
                // Give to player (as much as possible, drop rest at feet)
                HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(saved[i]);
                if (!notFit.isEmpty()) {
                    for (ItemStack leftover : notFit.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
                saved[i] = null;
                gaveLockedArrows = true;
            }
        }
        if (gaveLockedArrows) {
            player.sendMessage("§8[Quiver] Arrows from locked slots have been returned to your inventory.");
        }

        // Now build and open the inventory with the cleaned contents
        Inventory inv = Bukkit.createInventory(player, INVENTORY_SIZE, guiTitle);
        for (int i = 0; i < unlocked; i++) {
            if (saved[i] != null && saved[i].getType() != Material.AIR) {
                ItemStack arrow = saved[i].clone();
                ItemMeta meta = arrow.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(null);
                    meta.setLore(null);
                    arrow.setItemMeta(meta);
                }
                inv.setItem(i, arrow);
            }
        }
        // Fill locked slots with nameless glass panes (fully locked)
        ItemStack locked = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = locked.getItemMeta();
        meta.setDisplayName("§8Locked Slot");
        locked.setItemMeta(meta);
        for (int i = unlocked; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, locked);
        }
        player.openInventory(inv);

        // Save the cleaned quiver contents (to avoid double-giving on next open)
        quiverContents.put(player.getUniqueId(), saved);
        saveQuiver(player);
    }

    // Helper: returns true if the material is any type of arrow
    private static boolean isArrowType(Material mat) {
        return mat == Material.ARROW ||
                mat == Material.SPECTRAL_ARROW ||
                mat == Material.TIPPED_ARROW;
        // Add custom arrows here if needed
    }

    // Only prevent interaction with locked slots (glass pane) in the GUI
    // Now: Only allow arrow types to be deposited into unlocked slots
    @EventHandler
    public void onQuiverClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;
        String expectedTitle = getQuiverTitle(player);
        if (!event.getView().getTitle().equals(expectedTitle)) return;

        // Only handle top inventory (the quiver itself)
        if (event.getRawSlot() < 0 || event.getRawSlot() >= INVENTORY_SIZE) return;

        ItemStack clicked = event.getInventory().getItem(event.getRawSlot());
        if (clicked != null && clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true);
            player.sendMessage("§8[Quiver] This slot is locked at your current Archer level.");
            return;
        }

        int unlocked = getUnlockedSlots(data.getClassLevel());

        // Restrict placement in unlocked slots to arrow types only
        if (event.getSlot() < unlocked) {
            // Check if player is trying to place a non-arrow item
            if (event.getCursor() != null
                    && event.getCursor().getType() != Material.AIR
                    && !isArrowType(event.getCursor().getType())) {
                event.setCancelled(true);
                player.sendMessage("§8[Quiver] Only arrows can be deposited into your quiver.");
                return;
            }
            // Also check if player is moving from their inventory into the quiver via shift-click
            if ((event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getAction() == InventoryAction.HOTBAR_SWAP)
                    && event.getCurrentItem() != null
                    && !isArrowType(event.getCurrentItem().getType())) {
                event.setCancelled(true);
                player.sendMessage("§8[Quiver] Only arrows can be deposited into your quiver.");
                return;
            }
        }

        // Prevent moving items into locked slots via number keys or shift-clicks in source inventories
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // If a shift-click would move an item into a locked slot, cancel it
            for (int i = unlocked; i < INVENTORY_SIZE; i++) {
                ItemStack is = event.getInventory().getItem(i);
                if (is == null || is.getType() == Material.AIR || is.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // Prevent dragging items into locked slots ONLY
    // Now: Also prevent dragging non-arrow items into unlocked slots
    @EventHandler
    public void onQuiverDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;
        String expectedTitle = getQuiverTitle(player);
        if (!event.getView().getTitle().equals(expectedTitle)) return;

        int unlocked = getUnlockedSlots(data.getClassLevel());

        for (int slot : event.getRawSlots()) {
            // Only care about top inventory (quiver)
            if (slot < 0 || slot >= INVENTORY_SIZE) continue;
            ItemStack item = event.getNewItems().get(slot);
            if (item != null && item.getType() != Material.AIR) {
                if (slot < unlocked && !isArrowType(item.getType())) {
                    event.setCancelled(true);
                    player.sendMessage("§8[Quiver] Only arrows can be deposited into your quiver.");
                    return;
                }
                if (slot >= unlocked) {
                    event.setCancelled(true);
                    player.sendMessage("§8[Quiver] You cannot drag items onto locked slots.");
                    return;
                }
            }
        }
    }

    // On GUI close, save first 20 slots, ignoring glass/empty
    @EventHandler
    public void onQuiverClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;
        String expectedTitle = getQuiverTitle(player);
        if (!event.getView().getTitle().equals(expectedTitle)) return;

        Inventory inv = event.getInventory();
        ItemStack[] save = new ItemStack[QUIVER_SAVE_SIZE];
        for (int i = 0; i < QUIVER_SAVE_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR && it.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                // Only save arrow types!
                if (isArrowType(it.getType())) {
                    save[i] = it.clone();
                } else {
                    save[i] = null;
                }
            } else {
                save[i] = null;
            }
        }
        quiverContents.put(player.getUniqueId(), save);
        saveQuiver(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadQuiver(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveQuiver(event.getPlayer());
    }

    // Always return array of length 20, for all Archer levels
    public static ItemStack[] getQuiverContents(Player player) {
        UUID uuid = player.getUniqueId();
        if (!quiverContents.containsKey(uuid)) {
            loadQuiver(player);
        }
        ItemStack[] arr = quiverContents.get(uuid);
        if (arr == null || arr.length != QUIVER_SAVE_SIZE) {
            arr = new ItemStack[QUIVER_SAVE_SIZE];
            quiverContents.put(uuid, arr);
        }
        return arr;
    }

    public static void setQuiverContents(Player player, ItemStack[] contents) {
        ItemStack[] arr = new ItemStack[QUIVER_SAVE_SIZE];
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.length, QUIVER_SAVE_SIZE); i++) {
                if (contents[i] != null && isArrowType(contents[i].getType())) {
                    arr[i] = contents[i];
                } else {
                    arr[i] = null;
                }
            }
        }
        quiverContents.put(player.getUniqueId(), arr);
        saveQuiver(player);
    }

    private static File getQuiverFile(Player player) {
        Plugin plugin = getPlugin();
        File dir = new File(plugin.getDataFolder(), "quivers");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, player.getUniqueId() + ".yml");
    }

    public static void saveQuiver(Player player) {
        ItemStack[] arr = getQuiverContents(player);
        YamlConfiguration yml = new YamlConfiguration();
        for (int i = 0; i < QUIVER_SAVE_SIZE; i++) {
            yml.set("slot" + i, arr[i]);
        }
        try {
            yml.save(getQuiverFile(player));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadQuiver(Player player) {
        File file = getQuiverFile(player);
        ItemStack[] arr = new ItemStack[QUIVER_SAVE_SIZE];
        if (file.exists()) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            for (int i = 0; i < QUIVER_SAVE_SIZE; i++) {
                ItemStack is = yml.getItemStack("slot" + i);
                if (is != null && isArrowType(is.getType())) {
                    arr[i] = is;
                } else {
                    arr[i] = null;
                }
            }
        }
        quiverContents.put(player.getUniqueId(), arr);
    }

    // For level up/death compat
    public static void saveQuiverOnLevelUpOrDeath(Player player) {
        saveQuiver(player);
    }

    public static void restoreQuiverAfterLevelUpOrDeath(Player player) {
        loadQuiver(player);
    }

    public static void saveQuiverFromGUI(Player player, Inventory inventory) {
        ItemStack[] save = new ItemStack[QUIVER_SAVE_SIZE];
        for (int i = 0; i < QUIVER_SAVE_SIZE; i++) {
            ItemStack it = inventory.getItem(i);
            if (it != null && it.getType() != Material.AIR && it.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                if (isArrowType(it.getType())) {
                    save[i] = it.clone();
                } else {
                    save[i] = null;
                }
            } else {
                save[i] = null;
            }
        }
        quiverContents.put(player.getUniqueId(), save);
        saveQuiver(player);
    }
}