package com.pocketlazy.classify.commands;

import com.pocketlazy.classify.classes.PlayerClass;
import com.pocketlazy.classify.gui.RecipesGUI;
import com.pocketlazy.classify.gui.ReviveGUI;
import com.pocketlazy.classify.items.CustomItems;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class AdminCommands implements CommandExecutor, TabCompleter {

    private final PlayerClassManager pcm = PlayerClassManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give":
                // /classify give <username> <item> [amount]
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify give <username> <item> [amount]");
                    return true;
                }
                Player targetGive = Bukkit.getPlayerExact(args[1]);
                if (targetGive == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                String itemName = args[2].toLowerCase();
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[3]));
                    } catch (NumberFormatException ignored) {}
                }
                ItemStack item;
                switch (itemName) {
                    case "class_crystal":
                    case "classcrystal":
                        item = CustomItems.getClassCrystal();
                        break;
                    case "upgrader_geode":
                    case "upgradergeode":
                        item = CustomItems.getUpgraderGeode();
                        break;
                    case "healthy_gemstone":
                    case "healthygemstone":
                        item = CustomItems.getHealthyGemstone();
                        break;
                    case "charged_crystal":
                    case "chargedcrystal":
                        item = CustomItems.getChargedCrystal();
                        break;
                    case "revive_stone":
                    case "revivestone":
                        item = CustomItems.getReviveStone();
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Unknown item: " + itemName);
                        return true;
                }
                item.setAmount(amount);
                targetGive.getInventory().addItem(item);
                sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + formatItemName(itemName) + " to " + targetGive.getName());
                targetGive.sendMessage(ChatColor.AQUA + "You received " + amount + " " + formatItemName(itemName) + " from " + sender.getName());
                return true;

            case "change":
                // /classify change <username> <class>
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify change <username> <class>");
                    return true;
                }
                Player targetChange = Bukkit.getPlayerExact(args[1]);
                if (targetChange == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                String className = args[2];
                if (!PlayerClassManager.isValidClass(className)) {
                    sender.sendMessage(ChatColor.RED + "Invalid class name.");
                    return true;
                }
                PlayerClass newClass = PlayerClassManager.classFromString(className);
                pcm.setPlayerClass(targetChange, newClass);
                sender.sendMessage(ChatColor.GREEN + "Changed " + targetChange.getName() + "'s class to " + newClass.getName() + ".");
                targetChange.sendMessage(ChatColor.YELLOW + "Your class was changed to " + newClass.getName() + " by an admin.");
                return true;

            case "level":
                // /classify level set <username> <level>
                if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify level set <username> <level>");
                    return true;
                }
                Player targetLevel = Bukkit.getPlayerExact(args[2]);
                if (targetLevel == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                int level;
                try {
                    level = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Level must be a number (1-3).");
                    return true;
                }
                if (level < 1 || level > 3) {
                    sender.sendMessage(ChatColor.RED + "Level must be 1, 2, or 3.");
                    return true;
                }
                pcm.setClassLevel(targetLevel, level);
                sender.sendMessage(ChatColor.GREEN + "Set " + targetLevel.getName() + "'s class level to " + level + ".");
                targetLevel.sendMessage(ChatColor.YELLOW + "Your class level was set to " + level + " by an admin.");
                return true;

            case "revive":
                // /classify revive [<username>]
                if (!(sender instanceof Player reviver)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                if (args.length < 2) {
                    // Open the revive GUI for all ghosts
                    ReviveGUI.open(reviver);
                    sender.sendMessage(ChatColor.GREEN + "Opened revive menu. Click a ghost to revive them.");
                    return true;
                }
                Player ghostTarget = Bukkit.getPlayerExact(args[1]);
                if (ghostTarget == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                PlayerData ghostData = pcm.get(ghostTarget);
                if (!ghostData.isGhost()) {
                    sender.sendMessage(ChatColor.RED + ghostTarget.getName() + " is not a ghost.");
                    return true;
                }
                // Instantly revive the player
                ghostData.setGhost(false);
                sender.sendMessage(ChatColor.GREEN + ghostTarget.getName() + " has been revived.");
                ghostTarget.sendMessage(ChatColor.LIGHT_PURPLE + "You have been revived by " + sender.getName() + "!");
                return true;

            case "lives":
                // /classify lives <username> <amount>
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify lives <username> <amount>");
                    return true;
                }
                Player targetLives = Bukkit.getPlayerExact(args[1]);
                if (targetLives == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                int lives;
                try {
                    lives = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }
                if (lives < 1 || lives > 10) {
                    sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 10.");
                    return true;
                }
                pcm.setLives(targetLives, lives);
                sender.sendMessage(ChatColor.GREEN + "Set " + targetLives.getName() + "'s lives to " + lives + ".");
                targetLives.sendMessage(ChatColor.YELLOW + "Your lives were set to " + lives + " by an admin.");
                return true;

            case "recipe":
                // /classify recipe edit
                if (args.length < 2 || !args[1].equalsIgnoreCase("edit")) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify recipe edit");
                    return true;
                }
                if (!(sender instanceof Player playerEdit)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                RecipesGUI.openMain(playerEdit, true);
                sender.sendMessage(ChatColor.GREEN + "Opened recipe editor.");
                return true;

            case "charge":
                // /classify charge <username> <amount>
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify charge <username> <amount>");
                    return true;
                }
                Player targetCharge = Bukkit.getPlayerExact(args[1]);
                if (targetCharge == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                int charge;
                try {
                    charge = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }
                PlayerData chargeData = pcm.get(targetCharge);
                chargeData.setChargeCap(charge); // Fix: set max charge, not current charge
                sender.sendMessage(ChatColor.GREEN + "Set " + targetCharge.getName() + "'s max charge to " + charge + ".");
                targetCharge.sendMessage(ChatColor.YELLOW + "Your max charge was set to " + charge + " by an admin.");
                return true;

            case "eliminate":
                // /classify eliminate <username>
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify eliminate <username>");
                    return true;
                }
                Player targetElim = Bukkit.getPlayerExact(args[1]);
                if (targetElim == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                PlayerData elimData = pcm.get(targetElim);
                elimData.setGhost(true);
                sender.sendMessage(ChatColor.GREEN + targetElim.getName() + " is now a ghost.");
                targetElim.sendMessage(ChatColor.RED + "You have been eliminated and are now a ghost!");
                return true;

            case "reset":
                // /classify reset <username>
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classify reset <username>");
                    return true;
                }
                Player targetReset = Bukkit.getPlayerExact(args[1]);
                if (targetReset == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (pcm.has(targetReset)) pcm.remove(targetReset);
                targetReset.getInventory().clear();
                targetReset.setHealth(targetReset.getMaxHealth());
                targetReset.setFoodLevel(20);
                targetReset.setLevel(0);
                targetReset.setExp(0);
                targetReset.sendMessage(ChatColor.YELLOW + "Your stats were reset. Please select a new class.");
                sender.sendMessage(ChatColor.GREEN + "Reset " + targetReset.getName() + "'s stats. They will reselect a class.");
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /classify help");
                return true;
        }
    }

    private String formatItemName(String input) {
        switch (input.toLowerCase()) {
            case "class_crystal":
            case "classcrystal":
                return "Class Crystal";
            case "upgrader_geode":
            case "upgradergeode":
                return "Upgrader Geode";
            case "healthy_gemstone":
            case "healthygemstone":
                return "Healthy Gemstone";
            case "charged_crystal":
            case "chargedcrystal":
                return "Charged Crystal";
            case "revive_stone":
            case "revivestone":
                return "Revive Stone";
            default:
                return input;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "-------[ Classify Admin Commands ]-------");
        sender.sendMessage(ChatColor.YELLOW + "/classify help" + ChatColor.WHITE + " — Lists admin commands and their usage.");
        sender.sendMessage(ChatColor.YELLOW + "/classify change <username> <class>" + ChatColor.WHITE + " — Change a player's class.");
        sender.sendMessage(ChatColor.YELLOW + "/classify level set <username> <level>" + ChatColor.WHITE + " — Set a player's class level.");
        sender.sendMessage(ChatColor.YELLOW + "/classify revive [<username>]" + ChatColor.WHITE + " — Open the revive GUI or instantly revive a player.");
        sender.sendMessage(ChatColor.YELLOW + "/classify lives <username> <amount>" + ChatColor.WHITE + " — Set the number of lives a player has.");
        sender.sendMessage(ChatColor.YELLOW + "/classify recipe edit" + ChatColor.WHITE + " — Opens GUI to edit recipes.");
        sender.sendMessage(ChatColor.YELLOW + "/classify charge <username> <amount>" + ChatColor.WHITE + " — Set a player's max charge.");
        sender.sendMessage(ChatColor.YELLOW + "/classify eliminate <username>" + ChatColor.WHITE + " — Eliminate a player (turn to ghost).");
        sender.sendMessage(ChatColor.YELLOW + "/classify reset <username>" + ChatColor.WHITE + " — Reset all player stats (simulate first join).");
        sender.sendMessage(ChatColor.YELLOW + "/classify give <username> <item> [amount]" + ChatColor.WHITE + " — Give a plugin custom item to a player.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            return Arrays.asList("help", "change", "level", "revive", "lives", "recipe", "charge", "eliminate", "reset", "give");
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "change":
                case "level":
                case "revive":
                case "lives":
                case "charge":
                case "eliminate":
                case "reset":
                case "give":
                    suggestions = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    break;
                case "recipe":
                    suggestions = Collections.singletonList("edit");
                    break;
            }
        }
        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "change":
                    suggestions = PlayerClassManager.getAllClassNames();
                    break;
                case "level":
                    suggestions = Collections.singletonList("set");
                    break;
                case "give":
                    suggestions = Arrays.asList(
                            "class_crystal", "upgrader_geode", "healthy_gemstone", "charged_crystal", "revive_stone"
                    );
                    break;
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("level")) {
            suggestions = Arrays.asList("1", "2", "3");
        }
        return suggestions;
    }
}