package com.pocketlazy.classify.classes;

import com.pocketlazy.classify.abilities.HealerAbilityManager;
import com.pocketlazy.classify.gui.HealerCampfireGUI;
import com.pocketlazy.classify.items.AbilityItemManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.util.AbilitySoundUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Healer extends PlayerClass implements Listener {

    private final Map<UUID, Long> lastSelfHeal = new HashMap<>();
    private final Map<UUID, Long> lastReverseHeal = new HashMap<>();
    private final NamespacedKey soulboundKey;

    public Healer() {
        this.soulboundKey = new NamespacedKey(JavaPlugin.getProvidingPlugin(getClass()), "soulbound");
    }

    @Override
    public String getName() { return "Healer"; }

    @Override
    public String getDescription(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.LIGHT_PURPLE + "Healer\n");
        sb.append(ChatColor.GRAY + "Class: " + ChatColor.LIGHT_PURPLE + "Healer\n");
        sb.append(ChatColor.AQUA + "Ability: Self Heal " + ChatColor.GRAY + "(Right Click)\n");
        sb.append(ChatColor.GRAY + "Heal yourself for " + (level == 1 ? "10%" : "15%") + " of max health\n");
        sb.append(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "50\n");
        if (level >= 2) {
            sb.append(ChatColor.AQUA + "Ability: Reverse Heal " + ChatColor.GRAY + "(Left Click Entity)\n");
            sb.append(ChatColor.GRAY + "Deal Instant Damage II to target\n");
            sb.append(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "150\n");
            sb.append(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + "60s\n");
        }
        if (level == 3) {
            sb.append(ChatColor.AQUA + "Ability: Healing Campfire (Shift+Right Click)\n");
            sb.append(ChatColor.GRAY + "Place a healing campfire in front of you (see item for details)\n");
            sb.append(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + HealerCampfireGUI.CAMPFIRE_COST + "\n");
        }
        sb.append(ChatColor.AQUA + "Passive: Immunity to negative effects.");
        return sb.toString();
    }

    @Override
    public ItemStack getAbilityItem(int level, Player player, int lives) {
        ItemStack item = (level == 3) ? new ItemStack(Material.NETHER_STAR) : new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        String name = level == 1 ? "§eGlistering Stardust" : (level == 2 ? "§fGraceful Starlight" : "§dDivine Healer's Star");
        meta.setDisplayName(name);

        // Mark item as soulbound for level 2 and 3
        if (level >= 2) {
            meta.getPersistentDataContainer().set(soulboundKey, PersistentDataType.BYTE, (byte)1);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Class: §dHealer");
        lore.add("§7Ability: §bSelf Heal (Right Click)");
        lore.add("§7Heal yourself for " + (level == 1 ? "10%" : "15%") + " of max health");
        lore.add("§8Charge Cost: §b50");
        if (level >= 2) {
            lore.add("");
            lore.add("§7Ability: §dReverse Heal (Left Click Entity)");
            lore.add("§7Deal §5Instant Damage II §7to target");
            lore.add("§8Charge Cost: §b150");
            lore.add("§8Cooldown: §e60s");
        }
        if (level == 3) {
            lore.add("");
            lore.add("§7Ability: §6Healing Campfire (Shift+Right Click)");
            lore.add("§7Place a healing campfire in front of you.");
            lore.add("§8Charge Cost: §b" + HealerCampfireGUI.CAMPFIRE_COST);
        }
        lore.add("");
        lore.add("§bPassive: Immunity to negative effects.");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onLevelUp(Player player, int newLevel) {
        player.sendMessage("§aYou leveled up to Healer level " + newLevel + "!");
        onClassAssigned(player, newLevel);
    }

    @Override
    public void onClassAssigned(Player player, int level) {
        AbilityItemManager.updatePlayerAbilityItem(player);
    }

    @Override
    public void onRemove(Player player, int level) {
        HealerAbilityManager.removeAllCampfiresFor(player);
        player.getInventory().setItem(AbilityItemManager.ABILITY_SLOT, null);
    }

    @Override
    public void onAbilityItemUse(Player player, int level) {
        // Ability use handled in events.
    }

    @EventHandler
    public void onHealerImmunity(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Healer)) return;
        if (event.getCause() == Cause.EXPIRATION) return;
        PotionEffectType effect = event.getModifiedType();
        if (effect == null) return;
        if (HealerAbilityManager.isNegativeEffect(effect)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.AQUA + "[Healer] " + ChatColor.GRAY + "Your immunity blocked " + effect.getName().toLowerCase().replace('_', ' ') + "!");
        }
    }

    // Ability 1: Self Heal (Right Click, not sneaking)
    @EventHandler
    public void onHealerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Healer)) return;
        int level = data.getClassLevel();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!AbilityItemManager.isAbilityItem(player, item)) return;

        boolean isShift = player.isSneaking();
        boolean isRight = event.getAction().toString().contains("RIGHT");

        // Ability 3: Healing Campfire (Shift + Right Click)
        if (level == 3 && isRight && isShift) {
            HealerCampfireGUI.open(player);
            event.setCancelled(true);
            return;
        }

        // Ability 1: Self Heal (Right Click only, not sneaking)
        if (isRight && !isShift) {
            int healPercent = (level == 1) ? 10 : 15;
            int cost = 50; // Fixed cost for all levels
            int cooldown = (level == 1) ? 30 : 15;
            long now = System.currentTimeMillis();
            long last = lastSelfHeal.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < cooldown * 1000) {
                int secondsLeft = (int) ((cooldown * 1000 - (now - last)) / 1000) + 1;
                player.sendMessage(ChatColor.RED + "[Healer] Heal is on cooldown! " + secondsLeft + "s left.");
                AbilitySoundUtil.playCooldownSound(player);
                event.setCancelled(true);
                return;
            }
            if (data.getCharge() < cost) {
                player.sendMessage(ChatColor.RED + "[Healer] Not enough charge! Need " + cost + ".");
                AbilitySoundUtil.playNoChargeSound(player);
                event.setCancelled(true);
                return;
            }
            double maxHealth = player.getMaxHealth();
            double heal = Math.max(1, Math.floor(maxHealth * (healPercent / 100.0)));
            double newHealth = Math.min(player.getHealth() + heal, maxHealth);
            player.setHealth(newHealth);
            player.sendMessage(ChatColor.GREEN + "[Healer] You healed yourself for " + heal + " health!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
            data.setCharge(data.getCharge() - cost);
            lastSelfHeal.put(player.getUniqueId(), now);
            event.setCancelled(true);
        }
    }

    // Ability 2: Reverse Heal (Left Click attack entity)
    @EventHandler
    public void onHealerAttackEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Healer)) return;
        int level = data.getClassLevel();
        if (level < 2) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!AbilityItemManager.isAbilityItem(player, item)) return;
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity living)) return;

        // Only allow if not sneaking
        if (player.isSneaking()) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        long last = lastReverseHeal.getOrDefault(player.getUniqueId(), 0L);
        int cooldown = 60;
        if (now - last < cooldown * 1000) {
            int secondsLeft = (int) ((cooldown * 1000 - (now - last)) / 1000) + 1;
            player.sendMessage(ChatColor.RED + "[Healer] Reverse Heal is on cooldown! " + secondsLeft + "s left.");
            AbilitySoundUtil.playCooldownSound(player);
            event.setCancelled(true);
            return;
        }
        int cost = 150;
        if (data.getCharge() < cost) {
            player.sendMessage(ChatColor.RED + "[Healer] Not enough charge! Need " + cost + ".");
            AbilitySoundUtil.playNoChargeSound(player);
            event.setCancelled(true);
            return;
        }

        // Cancel normal attack, apply Instant Damage II
        event.setCancelled(true);
        living.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1));
        player.sendMessage(ChatColor.LIGHT_PURPLE + "[Healer] Reverse Heal used on " + target.getName() + "!");
        // Spell sound for reverse heal (evoker cast spell)
        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.2f, 1.0f);
        data.setCharge(data.getCharge() - cost);
        lastReverseHeal.put(player.getUniqueId(), now);
    }

    @EventHandler
    public void onHealerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        int level = (data != null) ? data.getClassLevel() : 1;
        if (!AbilityItemManager.isAbilityItem(player, event.getItem())) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "[Healer] You cannot consume your Healer item!");
    }

    // No more custom drop or slot-move logic needed (handled by AbilityItemManager)

    @EventHandler
    public void onHealerDeath(PlayerDeathEvent event) {
        // Soulbound and level 1 items are removed from drops by AbilityItemManager!
        // This event can be kept if you have other healer-specific cleanup.
    }

    private boolean isSoulboundHealerItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(soulboundKey, PersistentDataType.BYTE);
    }
}