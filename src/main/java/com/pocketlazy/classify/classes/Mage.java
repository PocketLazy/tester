package com.pocketlazy.classify.classes;

import com.pocketlazy.classify.abilities.MageAbilityManager;
import com.pocketlazy.classify.items.AbilityItemManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.util.AbilitySoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.util.Vector;

import java.util.*;

public class Mage extends PlayerClass implements Listener {
    private static final int ARCANE_CHARGE_COOLDOWN = 30; // seconds
    private static final int OVERFLOW_COOLDOWN = 15; // seconds

    // For twinkling arcane particles: maps player UUID to the running particle task
    private final Map<UUID, Integer> arcaneParticleTasks = new HashMap<>();

    public Mage() {}

    @Override
    public String getName() { return "Mage"; }

    @Override
    public String getDescription(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.AQUA + "Mage\n");
        sb.append("§7Class: §bMage\n");
        sb.append("§7Charge Cap: " + ChatColor.LIGHT_PURPLE + getMysticChargeCap(level) + "\n");
        sb.append(ChatColor.DARK_AQUA + "Passive: Mystic Charge\n");
        if (level == 1) {
            sb.append(ChatColor.GRAY + "Increase your charge cap by §d100§7.\n");
        } else if (level == 2) {
            sb.append(ChatColor.GRAY + "Increase your charge cap by §d200§7 (doesn't stack with lower levels).\n");
        } else {
            sb.append(ChatColor.GRAY + "Increase your charge cap by §d500§7 (doesn't stack with lower levels).\n");
        }
        sb.append(ChatColor.DARK_AQUA + "Ability: Mage Beam (Left Click)\n");
        if (level == 1) sb.append("§7Fire a beam (5 blocks, §c1❤§7)\n");
        else sb.append("§7Fire a beam (10 blocks, §c1❤§7)\n");
        sb.append("§8Charge Cost: §b10\n");
        if (level == 3) {
            sb.append(ChatColor.DARK_AQUA + "Ability: Arcane Charge (Right Click)\n");
            sb.append("§7Empower Mage Beam for 25s (2❤, 15 blocks)\n");
            sb.append("§8Cooldown: §e" + ARCANE_CHARGE_COOLDOWN + "s\n");
        }
        sb.append(ChatColor.DARK_AQUA + "Ability: Overflow (Shift+Right Click)\n");
        if (level == 1) sb.append("§7Sacrifice 50% HP, restore 10% charge.\n");
        else if (level == 2) sb.append("§7Sacrifice 50% HP, restore 20% charge.\n");
        else sb.append("§7Sacrifice 25% HP, restore 25% charge.\n");
        sb.append("§8Cooldown: §e" + OVERFLOW_COOLDOWN + "s\n");
        return sb.toString();
    }

    @Override
    public ItemStack getAbilityItem(int level, Player player, int lives) {
        Material mat = (level == 3) ? Material.ENCHANTED_BOOK : Material.BOOK;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String name = level == 1 ? "§bMystic Spellbook" : (level == 2 ? "§9Runic Grimoire" : "§5Arcane Grimoire");
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Class: §bMage");
        // REMOVED CHARGE CAP LINE FROM LORE
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Passive: Mystic Charge");
        if (level == 1) {
            lore.add(ChatColor.GRAY + "Increase your charge cap by §d100§7.");
        } else if (level == 2) {
            lore.add(ChatColor.GRAY + "Increase your charge cap by §d200§7 (doesn't stack with lower levels).");
        } else {
            lore.add(ChatColor.GRAY + "Increase your charge cap by §d500§7 (doesn't stack with lower levels).");
        }
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Ability: " + ChatColor.AQUA + "Mage Beam " + ChatColor.GRAY + "(Left Click)");
        if (level == 1) {
            lore.add(ChatColor.GRAY + "Fire a beam " + ChatColor.YELLOW + "5 blocks" + ChatColor.GRAY + " (" + ChatColor.RED + "1❤" + ChatColor.GRAY + " damage)");
        } else {
            lore.add(ChatColor.GRAY + "Fire a beam " + ChatColor.YELLOW + "10 blocks" + ChatColor.GRAY + " (" + ChatColor.RED + "1❤" + ChatColor.GRAY + " damage)");
        }
        lore.add(ChatColor.DARK_GRAY + "Charge Cost: " + ChatColor.AQUA + "10");
        if (level == 3) {
            lore.add("");
            lore.add(ChatColor.DARK_AQUA + "Ability: " + ChatColor.BLUE + "Arcane Charge " + ChatColor.GRAY + "(Right Click)");
            lore.add(ChatColor.GRAY + "Empower Mage Beam for " + ChatColor.GREEN + "25s");
            lore.add(ChatColor.GRAY + "Beam deals " + ChatColor.RED + "2❤" + ChatColor.GRAY + ", 15 blocks");
            lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + ARCANE_CHARGE_COOLDOWN + "s");
        }
        lore.add("");
        lore.add(ChatColor.DARK_AQUA + "Ability: " + ChatColor.GOLD + "Overflow " + ChatColor.GRAY + "(Shift+Right Click)");
        if (level == 1) {
            lore.add(ChatColor.GRAY + "Sacrifice 50% HP, restore 10% charge.");
        } else if (level == 2) {
            lore.add(ChatColor.GRAY + "Sacrifice 50% HP, restore 20% charge.");
        } else {
            lore.add(ChatColor.GRAY + "Sacrifice 25% HP, restore 25% charge.");
        }
        lore.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.YELLOW + OVERFLOW_COOLDOWN + "s");

        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.setCustomModelData(level == 1 ? 2001 : level == 2 ? 2002 : 2003);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);

        item.setItemMeta(meta); // Ensure set after all meta changes
        return item;
    }

    /**
     * Returns the *total* charge cap for display, for this Mage level.
     * This is for GUI and lore only, not for code logic.
     */
    private int getMysticChargeCap(int level) {
        // For display: base (assume 100) + bonus
        int base = 100;
        if (level == 1) return base + 100;
        if (level == 2) return base + 200;
        if (level == 3) return base + 500;
        return base;
    }

    /**
     * Sets the mage charge bonus correctly for the player's level.
     * This is called on assign and level up. This is the ONLY place it should be set for Mage!
     */
    private void setMageChargeBonus(PlayerData data, int level) {
        if (level == 1) data.setMageChargeBonus(100);
        else if (level == 2) data.setMageChargeBonus(200);
        else if (level >= 3) data.setMageChargeBonus(500);
        else data.setMageChargeBonus(0);
        // Clamp charge if above new cap
        if (data.getCharge() > data.getChargeCap()) {
            data.setCharge(data.getChargeCap());
        }
    }

    @Override
    public void onLevelUp(Player player, int newLevel) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data != null) {
            setMageChargeBonus(data, newLevel);
        }
        player.sendMessage("§aYou leveled up to Mage level " + newLevel + "!");
        onClassAssigned(player, newLevel);
    }

    @Override
    public void onClassAssigned(Player player, int level) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data != null) {
            setMageChargeBonus(data, level);
            AbilityItemManager.updatePlayerAbilityItem(player);
            MageAbilityManager.reset(player);
        }
        cancelArcaneParticleTask(player.getUniqueId());
    }

    @Override
    public void onRemove(Player player, int level) {
        // Reset bonus when switching off Mage
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data != null) {
            data.setMageChargeBonus(0);
        }
        MageAbilityManager.reset(player);
        player.getInventory().setItem(AbilityItemManager.ABILITY_SLOT, null);
        cancelArcaneParticleTask(player.getUniqueId());
    }

    @Override
    public void onAbilityItemUse(Player player, int level) {
        PlayerInteractEvent fakeEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, player.getInventory().getItemInMainHand(), null, null);
        onRightClick(fakeEvent);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Remove arcane particle tasks, AbilityItemManager will handle drops
        cancelArcaneParticleTask(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data != null && data.getPlayerClass() instanceof Mage) {
            int level = data.getClassLevel();
            setMageChargeBonus(data, level);
        }
        cancelArcaneParticleTask(player.getUniqueId());
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Mage)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isSword(item) && !AbilityItemManager.isAbilityItem(player, item)) return;

        int level = data.getClassLevel();
        boolean arcane = MageAbilityManager.isArcaneChargeActive(player.getUniqueId());
        int range = arcane ? 15 : (level == 1 ? 5 : 10);
        double damage = arcane ? 4.0 : 2.0;

        int cost = 10;
        if (data.getCharge() < cost) {
            player.sendMessage("§c[Classify] Not enough charge!");
            AbilitySoundUtil.playNoChargeSound(player);
            return;
        }
        // Deduct charge ONCE, here only!
        data.setCharge(data.getCharge() - cost);

        fireMageBeam(player, range, damage);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Mage)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!AbilityItemManager.isAbilityItem(player, item)) return;

        if (player.isSneaking()) {
            int secondsLeft = MageAbilityManager.getOverflowCooldownSecondsLeft(player);
            if (secondsLeft > 0) {
                player.sendMessage("§cOverflow is on cooldown! " + secondsLeft + "s left.");
                AbilitySoundUtil.playCooldownSound(player);
                event.setCancelled(true);
                return;
            }
            if (data.getCharge() >= data.getChargeCap()) {
                player.sendMessage("§aYour charge is already full!");
                event.setCancelled(true);
                return;
            }

            int level = data.getClassLevel();
            double hpToLose;
            double regenFrac;
            if (level == 1) {
                hpToLose = player.getMaxHealth() * 0.5;
                regenFrac = 0.10;
            } else if (level == 2) {
                hpToLose = player.getMaxHealth() * 0.5;
                regenFrac = 0.20;
            } else {
                hpToLose = player.getMaxHealth() * 0.25;
                regenFrac = 0.25;
            }
            player.damage(hpToLose);
            int regen = (int)(data.getChargeCap() * regenFrac);
            data.setCharge(Math.min(data.getCharge() + regen, data.getChargeCap()));
            player.sendMessage("§eOverflow! §7Restored §b" + regen + "§7 charge.");
            // Play gloomlock grimoire-like sound: ominous, magical, deep
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.7f);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.9f);
            MageAbilityManager.startOverflowCooldown(player, OVERFLOW_COOLDOWN);
            event.setCancelled(true);
            return;
        }

        if (data.getClassLevel() == 3) {
            int secondsLeft = MageAbilityManager.getArcaneChargeCooldownSecondsLeft(player);
            if (secondsLeft > 0) {
                player.sendMessage("§cArcane Charge is on cooldown! " + secondsLeft + "s left.");
                AbilitySoundUtil.playCooldownSound(player);
                event.setCancelled(true);
                return;
            }
            if (data.getCharge() < data.getChargeCap() * 0.25) {
                player.sendMessage("§c[Classify] You need at least 25% charge to activate Arcane Charge!");
                AbilitySoundUtil.playNoChargeSound(player);
                event.setCancelled(true);
                return;
            }
            MageAbilityManager.startArcaneCharge(player, ARCANE_CHARGE_COOLDOWN);
            data.setCharge(0);
            player.sendMessage("§b§lArcane Charge! §7Mage Beam empowered for 25 seconds.");
            // Play enchanting table sound!
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 1.1f);
            startArcaneParticleTask(player);
            Bukkit.getScheduler().runTaskLater(com.pocketlazy.classify.ClassifyPlugin.getInstance(), () -> {
                cancelArcaneParticleTask(player.getUniqueId());
            }, 25 * 20L);
            event.setCancelled(true);
        }
    }

    // No more onMageItemMove event - now handled centrally!

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        Material mat = item.getType();
        return mat.name().endsWith("_SWORD");
    }

    // Only ever damages ONE entity per click and never deducts charge again!
    private void fireMageBeam(Player player, int range, double damage) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        World world = player.getWorld();
        boolean hitEntity = false;
        for (int i = 0; i < range; i++) {
            Location step = eye.clone().add(direction.clone().multiply(i));
            Particle particleType;
            try {
                particleType = Particle.valueOf("ENCHANTMENT_TABLE");
            } catch (IllegalArgumentException ignored) {
                particleType = Particle.valueOf("SPELL_WITCH");
            }
            world.spawnParticle(particleType, step, 5, 0.1, 0.1, 0.1, 0.01);
            for (LivingEntity target : world.getNearbyLivingEntities(step, 0.5, 0.5, 0.5)) {
                if (target == player) continue;
                target.damage(damage, player);
                world.spawnParticle(particleType, target.getLocation().add(0, 1, 0), 20, 0.1, 0.1, 0.1, 0.01);
                player.sendMessage("§d[Classify] Mage beam hit for " + damage/2 + "❤!");
                hitEntity = true;
                break; // Only hit one entity!
            }
            if (hitEntity) break; // Stop scanning blocks after first entity is hit
            if (step.getBlock().getType().isSolid()) break;
        }
        if (!hitEntity) {
            player.sendMessage("§d[Classify] Mage beam fired!");
        }
    }

    // --- Arcane Charge Particle Effect Management ---

    private void startArcaneParticleTask(Player player) {
        UUID uuid = player.getUniqueId();
        cancelArcaneParticleTask(uuid);
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                com.pocketlazy.classify.ClassifyPlugin.getInstance(),
                () -> {
                    if (!player.isOnline()) {
                        cancelArcaneParticleTask(uuid);
                        return;
                    }
                    Location base = player.getLocation().add(0, 1.1, 0); // slightly above head
                    Random r = new Random();
                    for (int i = 0; i < 8; i++) {
                        double angle = r.nextDouble() * Math.PI * 2;
                        double radius = 0.7 + r.nextDouble() * 0.5;
                        double x = base.getX() + Math.cos(angle) * radius;
                        double z = base.getZ() + Math.sin(angle) * radius;
                        double y = base.getY() + r.nextDouble() * 0.5;
                        Location pLoc = new Location(base.getWorld(), x, y, z);
                        base.getWorld().spawnParticle(Particle.ENCHANT, pLoc, 2, 0, 0, 0, 0.01);
                    }
                },
                0L, 8L
        );
        arcaneParticleTasks.put(uuid, taskId);
    }

    private void cancelArcaneParticleTask(UUID playerUUID) {
        Integer id = arcaneParticleTasks.remove(playerUUID);
        if (id != null) {
            Bukkit.getScheduler().cancelTask(id);
        }
    }
}