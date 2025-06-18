package com.pocketlazy.classify.commands;

import com.pocketlazy.classify.gui.ClassifyGUI;
import com.pocketlazy.classify.gui.RecipesGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ClassCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Server server = Bukkit.getServer();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Players only!");
                return true;
            }
            ClassifyGUI.openClassInfo(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help": {
                sendFancyHelp(sender);
                return true;
            }
            case "recipes": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Players only!");
                    return true;
                }
                RecipesGUI.openMain(player, false);
                return true;
            }
            default:
                sendFancyHelp(sender);
                return true;
        }
    }

    private void sendFancyHelp(CommandSender sender) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§lClassify Player Commands");
        lines.add("");
        lines.add("§e/class §7- Show your current class info");
        lines.add("§e/class help §7- Show this help menu");
        lines.add("§e/class recipes §7- Open the custom recipes GUI");
        lines.add("");
        lines.add("§bTip: §7Use §e/class recipes §7to view and learn about custom items!");
        sender.sendMessage(lines.toArray(new String[0]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(Arrays.asList("help", "recipes"));
            return filter(out, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String start) {
        List<String> out = new ArrayList<>();
        for (String opt : options)
            if (opt.toLowerCase().startsWith(start.toLowerCase())) out.add(opt);
        return out;
    }
}