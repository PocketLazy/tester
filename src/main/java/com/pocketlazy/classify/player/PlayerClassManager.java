package com.pocketlazy.classify.player;

import com.pocketlazy.classify.ClassifyPlugin;
import com.pocketlazy.classify.classes.*;
import com.pocketlazy.classify.items.AbilityItemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class PlayerClassManager implements Listener {
    private static final PlayerClassManager INSTANCE = new PlayerClassManager();
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Random random = new Random();

    public static PlayerClassManager getInstance() {
        return INSTANCE;
    }

    public PlayerData get(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), PlayerData::new);
    }

    public PlayerData get(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public Collection<PlayerData> getAll() {
        return playerDataMap.values();
    }

    public void assignRandomClass(Player player) {
        PlayerData data = get(player);
        if (data.getPlayerClass() != null) return;
        PlayerClass[] classes = new PlayerClass[] {
                new Berserker(), new Archer(), new Assassin(), new Healer(), new Mage()
        };
        PlayerClass chosen = classes[random.nextInt(classes.length)];
        data.setPlayerClass(chosen);
        data.setMageChargeBonus(chosen instanceof Mage ? mageBonusForLevel(1) : 0);
        player.sendMessage("§aYou have been assigned the class: §e" + chosen.getName() + "§a!");
        AbilityItemManager.updatePlayerAbilityItem(player);
        updateTabDisplay(player, data);
    }

    // Set player's class and reset to level 1
    public void setPlayerClass(Player player, PlayerClass clazz) {
        PlayerData data = get(player);
        if (data.getPlayerClass() != null) {
            data.getPlayerClass().onRemove(player, data.getClassLevel());
        }
        data.setPlayerClass(clazz);
        data.setClassLevel(1);

        // Ensure mageChargeBonus is only set when class is Mage
        if (clazz instanceof Mage) {
            data.setMageChargeBonus(mageBonusForLevel(1));
        } else {
            data.setMageChargeBonus(0);
        }

        player.sendMessage("§aYour class is now: §e" + clazz.getName() + "§a!");
        clazz.onLevelUp(player, 1);
        AbilityItemManager.updatePlayerAbilityItem(player);
        updateTabDisplay(player, data);
    }

    // Set player's class level (does NOT change class)
    public void setClassLevel(Player player, int level) {
        PlayerData data = get(player);
        data.setClassLevel(level);
        data.getPlayerClass().onLevelUp(player, level);

        // Ensure mageChargeBonus is only set when class is Mage
        if (data.getPlayerClass() instanceof Mage) {
            data.setMageChargeBonus(mageBonusForLevel(level));
        } else {
            data.setMageChargeBonus(0);
        }

        AbilityItemManager.updatePlayerAbilityItem(player);
        updateTabDisplay(player, data);
    }

    private static int mageBonusForLevel(int level) {
        return switch (level) {
            case 1 -> 100;
            case 2 -> 200;
            case 3 -> 500;
            default -> 0;
        };
    }

    // Set player's lives
    public void setLives(Player player, int lives) {
        PlayerData data = get(player);
        data.setLives(lives);
        AbilityItemManager.updatePlayerAbilityItem(player);
        updateTabDisplay(player, data);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlayerData data = get(player);
        if (data.getPlayerClass() == null) {
            assignRandomClass(player);
        } else {
            // Ensure mageChargeBonus is applied/revoked on join
            if (data.getPlayerClass() instanceof Mage) {
                data.setMageChargeBonus(mageBonusForLevel(data.getClassLevel()));
            } else {
                data.setMageChargeBonus(0);
            }
            AbilityItemManager.updatePlayerAbilityItem(player);
        }
        updateTabDisplay(player, data);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Save on quit
        com.pocketlazy.classify.persist.DataPersistence.saveAll();
    }

    // -- Required static utils for command system --

    // Returns true if name matches any class
    public static boolean isValidClass(String className) {
        return getAllClassNames().contains(className.toLowerCase());
    }

    // Returns all valid class names (lowercase)
    public static List<String> getAllClassNames() {
        return Arrays.asList("berserker", "archer", "assassin", "healer", "mage");
    }

    // Returns all registered PlayerClass instances
    public static List<PlayerClass> getAllRegisteredClasses() {
        return List.of(
                new Berserker(),
                new Archer(),
                new Assassin(),
                new Healer(),
                new Mage()
        );
    }

    // Returns PlayerClass by name, null if not found
    public static PlayerClass classFromString(String className) {
        return switch (className.toLowerCase()) {
            case "berserker" -> new Berserker();
            case "archer" -> new Archer();
            case "assassin" -> new Assassin();
            case "healer" -> new Healer();
            case "mage" -> new Mage();
            default -> null;
        };
    }

    // Legacy alias for backwards compatibility
    public static PlayerClass classByName(String name) {
        return classFromString(name);
    }

    // --------- ADDED FOR ADMIN COMMANDS SUPPORT ---------

    // Returns true if this player has data in the manager
    public boolean has(Player player) {
        return playerDataMap.containsKey(player.getUniqueId());
    }

    // Removes a player's data from the manager
    public void remove(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    // --------- TAB DISPLAY UTILITY ---------

    public static void updateTabDisplay(Player player, PlayerData data) {
        // You can adjust the heart/emote to your preference
        if (data.isGhost()) {
            player.setPlayerListName(ChatColor.GRAY + player.getName());
        } else {
            player.setPlayerListName(ChatColor.GREEN + player.getName() +
                    ChatColor.DARK_GRAY + " [" +
                    ChatColor.RED + data.getLives() + "❤" +
                    ChatColor.DARK_GRAY + "]");
        }
    }

    // Update all online players (e.g. after a reload or admin command)
    public static void updateAllTabDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = PlayerClassManager.getInstance().get(player);
            updateTabDisplay(player, data);
        }
    }
}