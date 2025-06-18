package com.pocketlazy.classify.gui;

import com.pocketlazy.classify.items.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RecipesGUI implements Listener {
    public static final String RECIPE_LIST_TITLE = ChatColor.LIGHT_PURPLE + "Gemstone Recipes";
    public static final String RECIPE_SHOW_TITLE = ChatColor.GOLD + "Gemstone Recipe";

    /**
     * Opens the main recipes GUI.
     * @param player Player to open for
     * @param editMode If true, will open in edit mode (not used here but for future admin use)
     */
    public static void openMain(Player player, boolean editMode) {
        Inventory inv = Bukkit.createInventory(null, 9, RECIPE_LIST_TITLE);
        inv.setItem(1, CustomItems.getClassCrystal());
        inv.setItem(3, CustomItems.getUpgraderGeode());
        inv.setItem(5, CustomItems.getHealthyGemstone());
        inv.setItem(7, CustomItems.getChargedCrystal());
        player.openInventory(inv);
    }

    /**
     * Overload that opens the main recipes GUI in normal (non-edit) mode.
     */
    public static void openMain(Player player) {
        openMain(player, false);
    }

    public static void openRecipe(Player player, String type) {
        Inventory inv = Bukkit.createInventory(null, 27, RECIPE_SHOW_TITLE);
        switch (type) {
            case "class":
                setRecipe(inv, new ItemStack[][]{
                        {shard(), diamondBlock(), shard()},
                        {totem(), netherStar(), totem()},
                        {shard(), diamondBlock(), shard()}
                }, CustomItems.getClassCrystal());
                break;
            case "upgrade":
                setRecipe(inv, new ItemStack[][]{
                        {diamondBlock(), shard(), diamondBlock()},
                        {enchantedGoldenApple(), netherite(), enchantedGoldenApple()},
                        {diamondBlock(), shard(), diamondBlock()}
                }, CustomItems.getUpgraderGeode());
                break;
            case "health":
                setRecipe(inv, new ItemStack[][]{
                        {goldBlock(), goldenApple(), totem()},
                        {glisteringMelon(), enchantedGoldenApple(), glisteringMelon()},
                        {totem(), goldenApple(), goldBlock()}
                }, CustomItems.getHealthyGemstone());
                break;
            case "charged":
                setRecipe(inv, new ItemStack[][]{
                        {dragonsBreath(), blazeRod(), lapisBlock()},
                        {netherite(), sugar(), netherite()},
                        {lapisBlock(), blazeRod(), dragonsBreath()}
                }, CustomItems.getChargedCrystal());
                break;
            default:
                // Optionally show an error or a fallback
                break;
        }
        player.openInventory(inv);
    }

    private static void setRecipe(Inventory inv, ItemStack[][] matrix, ItemStack result) {
        int[] slots = {2, 3, 4, 11, 12, 13, 20, 21, 22};
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                inv.setItem(slots[row * 3 + col], matrix[row][col]);
        inv.setItem(15, result); // Result item
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();
        if (title.equals(RECIPE_LIST_TITLE)) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
            String dName = clicked.getItemMeta().getDisplayName();
            if (dName.contains("Class")) openRecipe(player, "class");
            if (dName.contains("Upgrader")) openRecipe(player, "upgrade");
            if (dName.contains("Health")) openRecipe(player, "health");
            if (dName.contains("Charged")) openRecipe(player, "charged");
        } else if (title.equals(RECIPE_SHOW_TITLE)) {
            e.setCancelled(true);
        }
    }

    // Material helpers
    private static ItemStack diamondBlock() { return new ItemStack(Material.DIAMOND_BLOCK); }
    private static ItemStack shard() { return new ItemStack(Material.AMETHYST_SHARD); }
    private static ItemStack totem() { return new ItemStack(Material.TOTEM_OF_UNDYING); }
    private static ItemStack netherStar() { return new ItemStack(Material.NETHER_STAR); }
    private static ItemStack enchantedGoldenApple() { return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE); }
    private static ItemStack netherite() { return new ItemStack(Material.NETHERITE_INGOT); }
    private static ItemStack goldBlock() { return new ItemStack(Material.GOLD_BLOCK); }
    private static ItemStack goldenApple() { return new ItemStack(Material.GOLDEN_APPLE); }
    private static ItemStack glisteringMelon() { return new ItemStack(Material.GLISTERING_MELON_SLICE); }
    private static ItemStack blazeRod() { return new ItemStack(Material.BLAZE_ROD); }
    private static ItemStack lapisBlock() { return new ItemStack(Material.LAPIS_BLOCK); }
    private static ItemStack dragonsBreath() { return new ItemStack(Material.DRAGON_BREATH); }
    private static ItemStack sugar() { return new ItemStack(Material.SUGAR); }
}