package com.pocketlazy.classify.abilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class HealerAbilityManager implements Listener {

    // --- CAMPFIRE COST ---
    public static final int CAMPFIRE_COST = 250;

    // These maps are for legacy (non-GUI) campfires, for global cleanup only.
    private static final Map<Location, UUID> campfireToOwner = new HashMap<>();
    private static final Map<Location, Integer> campfireTimers = new HashMap<>();
    private static final Map<Location, ArmorStand> campfireHolograms = new HashMap<>();

    public static void registerListener() {
        Bukkit.getPluginManager().registerEvents(new HealerAbilityManager(), com.pocketlazy.classify.ClassifyPlugin.getInstance());
    }

    /**
     * Returns true if the given potion effect is considered negative and should be blocked for the Healer.
     */
    public static boolean isNegativeEffect(PotionEffectType type) {
        if (type == null) return false;
        String name = type.getName().toUpperCase();
        return name.equals("POISON")
                || name.equals("WITHER")
                || name.equals("WEAKNESS")
                || name.equals("SLOWNESS")
                || name.equals("BLINDNESS")
                || name.equals("HUNGER")
                || name.equals("MINING_FATIGUE")
                || name.equals("LEVITATION")
                || name.equals("UNLUCK")
                || name.equals("NAUSEA")
                || name.equals("BAD_OMEN")
                || name.equals("DARKNESS");
    }

    /**
     * Remove all legacy campfires owned by the given player (for global cleanup).
     * GUI campfires are managed by HealerCampfireGUI.
     */
    public static void removeAllCampfiresFor(Player player) {
        UUID uuid = player.getUniqueId();
        List<Location> toRemove = new ArrayList<>();
        for (Map.Entry<Location, UUID> entry : campfireToOwner.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                toRemove.add(entry.getKey());
            }
        }
        for (Location loc : toRemove) {
            removeCampfire(loc, true);
        }
    }

    // The following methods are legacy and only for global cleanup/compatibility.
    // New campfire behavior (timer, healing, particles, etc.) is handled by HealerCampfireGUI.

    private static void removeCampfire(Location loc, boolean byHealer) {
        if (campfireTimers.containsKey(loc)) {
            Bukkit.getScheduler().cancelTask(campfireTimers.get(loc));
            campfireTimers.remove(loc);
        }
        campfireToOwner.remove(loc);

        // Remove hologram
        if (campfireHolograms.containsKey(loc)) {
            ArmorStand stand = campfireHolograms.get(loc);
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
            campfireHolograms.remove(loc);
        }

        Block block = loc.getBlock();
        if (block.getType() == Material.CAMPFIRE) {
            block.setType(Material.AIR);
            if (byHealer && block.getWorld() != null) {
                block.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 0.8f);
            }
        }
    }

    // Prevent breaking for non-owners (for legacy campfires only)
    @EventHandler
    public void onCampfireBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (campfireToOwner.containsKey(loc)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "[Healer] This campfire can only be removed by its owner or when it expires!");
        }
    }

    // Prevent right-click removal for non-owners (for legacy campfires only)
    @EventHandler
    public void onCampfireInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.CAMPFIRE) return;

        Location loc = block.getLocation();
        UUID owner = campfireToOwner.get(loc);
        if (owner == null) return;
        Player player = event.getPlayer();

        // No longer remove campfire here; handled by GUI in HealerCampfireGUI.
        if (!player.getUniqueId().equals(owner)) {
            player.sendMessage(ChatColor.RED + "[Healer] Only the owner can remove this campfire early.");
            event.setCancelled(true);
        }
    }
}