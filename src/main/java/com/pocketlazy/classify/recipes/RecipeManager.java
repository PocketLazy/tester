package com.pocketlazy.classify.recipes;

import com.pocketlazy.classify.items.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RecipeManager {
    private final Plugin plugin;
    private final File recipeFile;
    private final Map<String, ShapedRecipe> recipes = new HashMap<>();

    // The 5 custom item keys
    public static final String CLASS_CRYSTAL = "class_crystal";
    public static final String UPGRADER_GEODE = "upgrader_geode";
    public static final String HEALTHY_GEMSTONE = "healthy_gemstone";
    public static final String CHARGED_CRYSTAL = "charged_crystal";
    public static final String REVIVE_STONE = "revive_stone";

    public RecipeManager(Plugin plugin) {
        this.plugin = plugin;
        this.recipeFile = new File(plugin.getDataFolder(), "recipes.yml");
    }

    public void loadRecipes() {
        recipes.clear();
        if (!recipeFile.exists()) {
            saveDefaultRecipes();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);

        registerRecipe(CLASS_CRYSTAL, CustomItems.getClassCrystal(), config);
        registerRecipe(UPGRADER_GEODE, CustomItems.getUpgraderGeode(), config);
        registerRecipe(HEALTHY_GEMSTONE, CustomItems.getHealthyGemstone(), config);
        registerRecipe(CHARGED_CRYSTAL, CustomItems.getChargedCrystal(), config);
        registerRecipe(REVIVE_STONE, CustomItems.getReviveStone(), config);
    }

    private void registerRecipe(String key, ItemStack result, YamlConfiguration config) {
        List<String> shape = config.getStringList(key + ".shape");
        List<String> ingredients = config.getStringList(key + ".ingredients");

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key), result.clone());
        if (shape.size() == 3) {
            recipe.shape(shape.get(0), shape.get(1), shape.get(2));
        } else {
            // Default to empty 3x3
            recipe.shape("   ", "   ", "   ");
        }

        for (String ing : ingredients) {
            // Each ingredient: <char>:<MATERIAL>
            String[] split = ing.split(":");
            if (split.length != 2) continue;
            char c = split[0].charAt(0);
            Material mat = Material.getMaterial(split[1]);
            if (mat != null) {
                recipe.setIngredient(c, mat);
            }
        }

        Bukkit.removeRecipe(recipe.getKey()); // Remove old recipe if exists
        Bukkit.addRecipe(recipe);
        recipes.put(key, recipe);
    }

    public void saveRecipes(Map<String, ShapedRecipe> newRecipes) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, ShapedRecipe> entry : newRecipes.entrySet()) {
            ShapedRecipe recipe = entry.getValue();
            config.set(entry.getKey() + ".shape", Arrays.asList(recipe.getShape()));
            List<String> ingredients = new ArrayList<>();
            // Get all ingredient characters and their mats
            Map<Character, ItemStack> map = recipe.getIngredientMap();
            for (Map.Entry<Character, ItemStack> ing : map.entrySet()) {
                if (ing.getValue() != null)
                    ingredients.add(ing.getKey() + ":" + ing.getValue().getType().name());
            }
            config.set(entry.getKey() + ".ingredients", ingredients);
        }
        try {
            config.save(recipeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDefaultRecipes() {
        YamlConfiguration config = new YamlConfiguration();

        // Class Crystal (Emerald in center)
        config.set(CLASS_CRYSTAL + ".shape", Arrays.asList(" G ", "GEG", " G "));
        config.set(CLASS_CRYSTAL + ".ingredients", Arrays.asList(
                "E:EMERALD", "G:GLASS"
        ));

        // Upgrader Geode (Amethyst Shard in center)
        config.set(UPGRADER_GEODE + ".shape", Arrays.asList(" A ", "AGA", " A "));
        config.set(UPGRADER_GEODE + ".ingredients", Arrays.asList(
                "A:AMETHYST_SHARD", "G:GLASS"
        ));

        // Healthy Gemstone (Diamond in center)
        config.set(HEALTHY_GEMSTONE + ".shape", Arrays.asList(" D ", "DGD", " D "));
        config.set(HEALTHY_GEMSTONE + ".ingredients", Arrays.asList(
                "D:DIAMOND", "G:GLASS"
        ));

        // Charged Crystal (Quartz in center)
        config.set(CHARGED_CRYSTAL + ".shape", Arrays.asList(" Q ", "QGQ", " Q "));
        config.set(CHARGED_CRYSTAL + ".ingredients", Arrays.asList(
                "Q:QUARTZ", "G:GLASS"
        ));

        // Revive Stone (Nether Star in center)
        config.set(REVIVE_STONE + ".shape", Arrays.asList(" O ", "OSO", " O "));
        config.set(REVIVE_STONE + ".ingredients", Arrays.asList(
                "S:NETHER_STAR", "O:OBSIDIAN"
        ));

        try {
            config.save(recipeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, ShapedRecipe> getRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    public ShapedRecipe getRecipe(String key) {
        return recipes.get(key);
    }
}