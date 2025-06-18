package com.pocketlazy.classify.items;

import com.pocketlazy.classify.classes.PlayerClass;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Unified slot-lock/undroppable manager for ALL class ability items,
 * covering all levels and supporting soulbound logic for healer.
 */
public class AbilityItemManager implements Listener {
    public static final int ABILITY_SLOT = 8; // 9th slot (0-based)
    private final NamespacedKey soulboundKey;

    public AbilityItemManager() {
        // Use a persistent key for soulbound detection (used by Healer, but safe to check for all)
        this.soulboundKey = new NamespacedKey(JavaPlugin.getProvidingPlugin(getClass()), "soulbound");
    }

    public static void updatePlayerAbilityItem(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null) return;
        PlayerClass playerClass = data.getPlayerClass();
        if (playerClass == null) return;
        ItemStack abilityItem = playerClass.getAbilityItem(data.getClassLevel(), player, data.getLives());
        player.getInventory().setItem(ABILITY_SLOT, abilityItem);
    }

    public static void giveAbilityItem(Player player, PlayerClass playerClass, int level, int lives) {
        ItemStack abilityItem = playerClass.getAbilityItem(level, player, lives);
        player.getInventory().setItem(ABILITY_SLOT, abilityItem);
    }

    /** Returns true if the item matches any ability item for this player and class, for any valid level (1-3). */
    public boolean isAbilityItemAnyLevel(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null) return false;
        PlayerClass playerClass = data.getPlayerClass();
        if (playerClass == null) return false;
        for (int lvl = 1; lvl <= 3; lvl++) {
            ItemStack ai = playerClass.getAbilityItem(lvl, player, data.getLives());
            if (ai != null && itemSimilarIgnoreAmount(item, ai)) return true;
        }
        // Soulbound (Healer) support: check for the persistent NBT key
        if (isSoulbound(item)) return true;
        return false;
    }

    /** Returns true if the item matches the player's *current* ability item. */
    public static boolean isAbilityItem(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null) return false;
        PlayerClass playerClass = data.getPlayerClass();
        if (playerClass == null) return false;
        ItemStack expected = playerClass.getAbilityItem(data.getClassLevel(), player, data.getLives());
        if (expected == null) return false;
        return itemSimilarIgnoreAmount(item, expected);
    }

    /** Helper: check similarity by type, name, model data, lore (ignores stack amount). */
    private static boolean itemSimilarIgnoreAmount(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (!a.hasItemMeta() || !b.hasItemMeta()) return false;
        ItemMeta am = a.getItemMeta(), bm = b.getItemMeta();
        if (!Objects.equals(am.getDisplayName(), bm.getDisplayName())) return false;
        if (am.hasCustomModelData() != bm.hasCustomModelData()) return false;
        if (am.hasCustomModelData() && am.getCustomModelData() != bm.getCustomModelData()) return false;
        // Optional: check lore for more robustness
        if (am.hasLore() && bm.hasLore()) {
            List<String> aLore = am.getLore(), bLore = bm.getLore();
            if (!aLore.equals(bLore)) return false;
        }
        return true;
    }

    /** Returns true if the item is "soulbound" (Healer level 2/3 and possibly others in future). */
    private boolean isSoulbound(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(soulboundKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerAbilityItem(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.getPlayer().getInventory().setItem(ABILITY_SLOT, null);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Protect ALL slots: disallow moving/copying the ability item anywhere, and prevent placing it anywhere but slot 8
        if (isAbilityItemAnyLevel(player, clicked) || isAbilityItemAnyLevel(player, cursor)) {
            // Only allow if trying to pick up ability item to slot 8, otherwise block all
            if (event.getSlot() != ABILITY_SLOT) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot move, place, or swap your ability item!");
            }
            // Prevent putting ability item into offhand or any other slot (including armor)
            if (event.getRawSlot() == 40 // Offhand slot
                    || event.getSlot() < 0 || event.getSlot() > 35) { // Out of main inventory/hotbar
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot move, place, or swap your ability item!");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack placed = event.getItemInHand();
        if (!isAbilityItemAnyLevel(player, placed)) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You cannot place your ability item!");
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (!isAbilityItemAnyLevel(player, dropped)) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You cannot drop your ability item!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null) return;
        PlayerClass playerClass = data.getPlayerClass();
        if (playerClass == null) return;

        // Remove ability items of ALL levels (and soulbound) from drops
        List<ItemStack> drops = event.getDrops();
        drops.removeIf(item -> {
            // Remove if matches any level's ability item or is soulbound
            for (int lvl = 1; lvl <= 3; lvl++) {
                ItemStack ai = playerClass.getAbilityItem(lvl, player, data.getLives());
                if (ai != null && itemSimilarIgnoreAmount(item, ai)) return true;
            }
            // Remove if soulbound
            return isSoulbound(item);
        });
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        updatePlayerAbilityItem(player);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isAbilityItemAnyLevel(player, main) || isAbilityItemAnyLevel(player, off)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot move your ability item to or from your offhand!");
        }
    }
}