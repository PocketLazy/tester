package com.pocketlazy.classify.abilities;

import com.pocketlazy.classify.classes.Archer;
import com.pocketlazy.classify.gui.ArcherQuiverManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.items.AbilityItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ArcherArrowInsertionManager implements Listener {

    // Tracks players whose slot 8 (slot 9) is currently replaced with an arrow
    private static final Map<UUID, ItemStack> replacedAbilityItem = new HashMap<>();
    private static final Set<UUID> pendingRestore = new HashSet<>();

    // Track the arrow actually inserted (type + meta) for correct removal
    private static final Map<UUID, ItemStack> insertedArrowType = new HashMap<>();

    private final Plugin plugin;

    public ArcherArrowInsertionManager(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Listen for right-click with bow/crossbow to insert arrow from quiver into slot 9.
     */
    @EventHandler
    public void onArcherBowUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null) return;
        boolean isBow = held.getType() == Material.BOW;
        boolean isCrossbow = held.getType() == Material.CROSSBOW;
        if (!isBow && !isCrossbow) return;

        // Only trigger on right click (charging a bow/crossbow) and only if slot 9 is the ability item
        boolean rightClick = event.getAction().toString().contains("RIGHT");
        if (!rightClick) return;
        int slot = 8; // Slot 9 is index 8
        ItemStack slotItem = player.getInventory().getItem(slot);
        if (!AbilityItemManager.isAbilityItem(player, slotItem)) return;

        // Already swapped? Don't insert again.
        if (replacedAbilityItem.containsKey(player.getUniqueId())) return;

        // Fetch arrow from quiver (but don't consume yet, just preview)
        ItemStack arrow = ArcherAbilityManager.takeOneArrowFromQuiver(player, false);
        if (arrow == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Remove any display name/lore from arrow
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(null);
            meta.setLore(null);
            arrow.setItemMeta(meta);
        }
        arrow.setAmount(1);

        // Swap slot 9: store ability item, insert arrow with all NBT
        replacedAbilityItem.put(player.getUniqueId(), slotItem == null ? null : slotItem.clone());
        insertedArrowType.put(player.getUniqueId(), arrow.clone());
        player.getInventory().setItem(slot, arrow.clone());
        player.updateInventory();
    }

    /**
     * Listen for shooting bow/crossbow to consume arrow and restore ability item.
     */
    @EventHandler
    public void onArcherShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;

        int slot = 8;
        UUID uuid = player.getUniqueId();
        if (replacedAbilityItem.containsKey(uuid)) {
            // Consume one arrow from quiver (matching the one given earlier)
            ItemStack inserted = insertedArrowType.remove(uuid);
            if (inserted != null) {
                ArcherAbilityManager.removeArrowFromQuiver(player, inserted);
            } else {
                ArcherAbilityManager.takeOneArrowFromQuiver(player, true);
            }

            // Restore ability item after a short delay (1 tick)
            ItemStack abilityItem = replacedAbilityItem.remove(uuid);
            pendingRestore.add(uuid);
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getInventory().setItem(slot, abilityItem);
                    player.updateInventory();
                    pendingRestore.remove(uuid);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Restore ability item if player switches off slot 9 before shooting.
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;

        UUID uuid = player.getUniqueId();
        int slot = 8;
        if (replacedAbilityItem.containsKey(uuid) && event.getPreviousSlot() == slot) {
            ItemStack abilityItem = replacedAbilityItem.remove(uuid);
            player.getInventory().setItem(slot, abilityItem);
            player.updateInventory();
            insertedArrowType.remove(uuid);
        }
    }

    /**
     * Restore ability item if player drops slot 9 item before shooting.
     */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;

        UUID uuid = player.getUniqueId();
        int slot = 8;
        if (replacedAbilityItem.containsKey(uuid)) {
            ItemStack abilityItem = replacedAbilityItem.remove(uuid);
            player.getInventory().setItem(slot, abilityItem);
            player.updateInventory();
            insertedArrowType.remove(uuid);
        }
    }

    /**
     * Clean up on quit (avoid stuck swapped items).
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Archer)) return;

        UUID uuid = player.getUniqueId();
        int slot = 8;
        if (replacedAbilityItem.containsKey(uuid)) {
            ItemStack abilityItem = replacedAbilityItem.remove(uuid);
            player.getInventory().setItem(slot, abilityItem);
            insertedArrowType.remove(uuid);
        }
    }

    /**
     * Utility: Call this on plugin disable to restore ability items for all online archers.
     */
    public static void restoreAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = PlayerClassManager.getInstance().get(player);
            if (data == null || !(data.getPlayerClass() instanceof Archer)) continue;
            UUID uuid = player.getUniqueId();
            int slot = 8;
            if (replacedAbilityItem.containsKey(uuid)) {
                ItemStack abilityItem = replacedAbilityItem.remove(uuid);
                player.getInventory().setItem(slot, abilityItem);
                insertedArrowType.remove(uuid);
            }
        }
    }
}