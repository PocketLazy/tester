package com.pocketlazy.classify.gui;

import com.pocketlazy.classify.recipes.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RecipeEditGUI implements Listener {

    private static final String TITLE = "Classify Recipe Editor";
    private static final int SIZE = 9 * 6;
    private static final List<String> ITEM_KEYS = Arrays.asList(
            RecipeManager.CLASS_CRYSTAL,
            RecipeManager.UPGRADER_GEODE,
            RecipeManager.HEALTHY_GEMSTONE,
            RecipeManager.CHARGED_CRYSTAL,
            RecipeManager.REVIVE_STONE
    );
    private static final Map<UUID, String> editingMap = new HashMap<>(); // player -> key

    private static RecipeManager recipeManager;
    private static Plugin plugin;

    public static void init(Plugin pluginInstance, RecipeManager rm) {
        plugin = pluginInstance;
        recipeManager = rm;
        Bukkit.getPluginManager().registerEvents(new RecipeEditGUI(), plugin);
    }

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(player, SIZE, TITLE);
        // First row: custom items as buttons to select which recipe to edit
        for (int i = 0; i < ITEM_KEYS.size(); i++) {
            String key = ITEM_KEYS.get(i);
            ShapedRecipe recipe = recipeManager.getRecipe(key);
            if (recipe == null) continue;
            ItemStack button = recipe.getResult().clone();
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName("§eEdit recipe: " + key.replace('_', ' ').toUpperCase());
            button.setItemMeta(meta);
            gui.setItem(i, button);
        }
        // 3x3 grid for recipe (start empty, filled once an item is picked)
        for (int i = 27; i < 36; i++)
            gui.setItem(i, new ItemStack(Material.AIR));
        // Save button
        ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.setDisplayName("§a§lSave Recipe");
        save.setItemMeta(saveMeta);
        gui.setItem(53, save); // bottom right
        player.openInventory(gui);
        editingMap.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView().getTitle().equals(TITLE)) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            Inventory inv = e.getInventory();

            // Select a recipe to edit
            if (slot >= 0 && slot < ITEM_KEYS.size()) {
                String key = ITEM_KEYS.get(slot);
                editingMap.put(player.getUniqueId(), key);
                // Show the 3x3 grid for this recipe
                fillRecipeGrid(inv, key);
                player.sendMessage("§eEditing recipe for " + key.replace('_', ' ').toUpperCase());
                return;
            }

            // Allow editing recipe grid (slots 27-35)
            String key = editingMap.get(player.getUniqueId());
            if (key != null && slot >= 27 && slot < 36) {
                e.setCancelled(false); // allow drag/drop in grid
                return;
            }

            // Save button
            if (slot == 53 && key != null) {
                // Get recipe grid contents
                ItemStack[] grid = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    grid[i] = inv.getItem(27 + i);
                }
                // Save recipe
                saveRecipe(key, grid);
                player.sendMessage("§aRecipe saved for " + key.replace('_', ' ').toUpperCase() + "!");
                // Close after a short delay
                new BukkitRunnable() {
                    public void run() { player.closeInventory(); }
                }.runTaskLater(plugin, 10L);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView().getTitle().equals(TITLE)) {
            String key = editingMap.get(player.getUniqueId());
            if (key == null) { e.setCancelled(true); return; }
            // Only allow dragging into the 3x3 grid
            for (int slot : e.getRawSlots())
                if (slot < 27 || slot > 35) { e.setCancelled(true); return; }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals(TITLE)) {
            editingMap.remove(e.getPlayer().getUniqueId());
        }
    }

    private void fillRecipeGrid(Inventory inv, String key) {
        ShapedRecipe recipe = recipeManager.getRecipe(key);
        if (recipe == null) return;
        // 3x3 grid, fill with shaped recipe
        String[] shape = recipe.getShape();
        Map<Character, ItemStack> map = recipe.getIngredientMap();
        for (int row = 0; row < 3; row++) {
            String s = shape.length > row ? shape[row] : "   ";
            for (int col = 0; col < 3; col++) {
                char c = s.charAt(col);
                ItemStack ing = null;
                if (c != ' ' && map.containsKey(c)) {
                    ing = map.get(c);
                }
                inv.setItem(27 + row * 3 + col, ing == null ? new ItemStack(Material.AIR) : ing.clone());
            }
        }
    }

    private void saveRecipe(String key, ItemStack[] grid) {
        // Build shape and ingredients
        String[] shape = new String[3];
        Map<Character, Material> charMat = new HashMap<>();
        char nextChar = 'A';
        Map<Integer, Character> slotChar = new HashMap<>();

        // Assign unique chars for each material in grid
        for (int row = 0; row < 3; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                ItemStack item = grid[idx];
                if (item != null && item.getType() != Material.AIR) {
                    Material mat = item.getType();
                    Character c = null;
                    // Reuse char if already assigned
                    for (Map.Entry<Character, Material> e : charMat.entrySet())
                        if (e.getValue() == mat) c = e.getKey();
                    if (c == null) {
                        c = nextChar++;
                        charMat.put(c, mat);
                    }
                    sb.append(c);
                    slotChar.put(idx, c);
                } else {
                    sb.append(' ');
                }
            }
            shape[row] = sb.toString();
        }

        // Build ingredient list
        List<String> ingredients = new ArrayList<>();
        for (Map.Entry<Character, Material> e : charMat.entrySet())
            ingredients.add(e.getKey() + ":" + e.getValue().name());

        // Build shaped recipe
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key), recipeManager.getRecipe(key).getResult().clone());
        recipe.shape(shape);
        for (Map.Entry<Character, Material> e : charMat.entrySet())
            recipe.setIngredient(e.getKey(), e.getValue());

        // Save to disk and reload recipes
        Map<String, ShapedRecipe> all = new HashMap<>(recipeManager.getRecipes());
        all.put(key, recipe);
        recipeManager.saveRecipes(all);
        recipeManager.loadRecipes(); // re-register all
    }
}