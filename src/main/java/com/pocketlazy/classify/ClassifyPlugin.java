package com.pocketlazy.classify;

import com.pocketlazy.classify.commands.AdminCommands;
import com.pocketlazy.classify.commands.ClassCommand;
import com.pocketlazy.classify.gui.RecipesGUI;
import com.pocketlazy.classify.gui.ReviveGUI;
import com.pocketlazy.classify.gui.HealerCampfireGUI;
import com.pocketlazy.classify.gui.ArcherQuiverManager;
import com.pocketlazy.classify.items.AbilityItemManager;
import com.pocketlazy.classify.items.CustomItemsListener;
import com.pocketlazy.classify.items.ReviveStoneHandler;
import com.pocketlazy.classify.persist.DataPersistence;
import com.pocketlazy.classify.player.GhostManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.ClassAbilityProtection;
import com.pocketlazy.classify.recipes.Recipes;
import com.pocketlazy.classify.util.ChargeSystem;
import com.pocketlazy.classify.classes.Mage;
import com.pocketlazy.classify.classes.Healer;
import com.pocketlazy.classify.classes.Assassin;
import com.pocketlazy.classify.classes.Archer;
import com.pocketlazy.classify.abilities.ArcherAbilityManager;
import com.pocketlazy.classify.protocol.AssassinArmorHider;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

public class ClassifyPlugin extends JavaPlugin {
    private static ClassifyPlugin instance;
    private int maxLives = 5;

    @Override
    public void onEnable() {
        instance = this;

        // Register all custom recipes
        Recipes.registerAll(this);

        // Register all event listeners
        Bukkit.getPluginManager().registerEvents(PlayerClassManager.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(GhostManager.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(new ReviveStoneHandler(), this);
        Bukkit.getPluginManager().registerEvents(new RecipesGUI(), this);
        Bukkit.getPluginManager().registerEvents(new ReviveGUI(), this);
        Bukkit.getPluginManager().registerEvents(new CustomItemsListener(), this);
        Bukkit.getPluginManager().registerEvents(new ClassAbilityProtection(), this);

        // Register GUI listeners
        Bukkit.getPluginManager().registerEvents(new HealerCampfireGUI(), this);

        // Register class listeners
        Bukkit.getPluginManager().registerEvents(new Mage(), this);
        Bukkit.getPluginManager().registerEvents(new Healer(), this);
        Bukkit.getPluginManager().registerEvents(new Assassin(), this);
        Bukkit.getPluginManager().registerEvents(new Archer(), this);

        // Register Ability Managers
        new ArcherAbilityManager(); // Registers itself

        // Register ArcherQuiverManager as an event listener (REQUIRED for GUI saving/loading)
        Bukkit.getPluginManager().registerEvents(new ArcherQuiverManager(), this);

        Bukkit.getPluginManager().registerEvents(new AbilityItemManager(), this);

        // Register player commands
        ClassCommand classCmd = new ClassCommand();
        getCommand("class").setExecutor(classCmd);
        getCommand("class").setTabCompleter(classCmd);

        // Register admin commands
        AdminCommands adminCmd = new AdminCommands();
        getCommand("classify").setExecutor(adminCmd);
        getCommand("classify").setTabCompleter(adminCmd);

        // Start charge system
        ChargeSystem.startRegenTask();

        // Initialize persistent storage
        DataPersistence.init(this);

        // Start repeating action bar task
        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        com.pocketlazy.classify.player.PlayerData data =
                                com.pocketlazy.classify.player.PlayerClassManager.getInstance().get(player);
                        if (data != null) data.updateActionBar();
                    }
                },
                0L, 20L
        );

        // Register ProtocolLib armor hider if ProtocolLib is present
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            AssassinArmorHider.register(this);
            getLogger().info("[Classify] Assassin armor hiding enabled via ProtocolLib.");
        } else {
            getLogger().warning("[Classify] ProtocolLib not found! Assassin armor hiding will not work.");
        }

        getLogger().info("§a[Classify] Enabled! All systems online.");
    }

    @Override
    public void onDisable() {
        DataPersistence.saveAll();
        getLogger().info("§c[Classify] Disabled.");
    }

    public static ClassifyPlugin getInstance() {
        return instance;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = Math.max(1, Math.min(maxLives, 20));
    }
}