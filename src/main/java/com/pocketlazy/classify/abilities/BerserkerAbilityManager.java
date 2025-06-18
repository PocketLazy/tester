package com.pocketlazy.classify.abilities;

import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.util.ActionBarManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BerserkerAbilityManager {
    // Internal cooldowns and state
    private static final Map<UUID, Long> syphonCooldown = new HashMap<>();
    private static final Map<UUID, Long> unyieldingRageEnd = new HashMap<>();
    private static final Map<UUID, Long> unyieldingRageCooldown = new HashMap<>();
    private static final Map<UUID, Long> rageCooldown = new HashMap<>();
    private static final Map<UUID, Long> bladeCooldown = new HashMap<>();
    private static final Map<UUID, Long> bladeEnd = new HashMap<>();
    private static final Map<UUID, Long> flamingSlashCooldown = new HashMap<>();
    private static final Map<UUID, Long> witherRageCooldown = new HashMap<>();
    private static final Set<UUID> inBladeMode = new HashSet<>();
    private static final Set<UUID> witherReady = new HashSet<>();
    private static final Map<UUID, BukkitRunnable> bladeActionBars = new HashMap<>();

    // ------ BLOODLUST (PASSIVE) ------
    public static void applyBloodlust(Player player) {
        double maxHp = player.getMaxHealth();
        double currHp = Math.max(1, player.getHealth());
        double missingFrac = 1.0 - (currHp / maxHp);
        double bonus = Math.min(0.06, Math.floor(missingFrac * 10) * 0.02); // +2% per 10%, max +6%
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attr == null) return;
        // Remove any previous bloodlust modifiers (by name)
        attr.getModifiers().removeIf(mod -> mod.getName().equals("berserker-bloodlust"));
        if (bonus > 0) {
            AttributeModifier mod = new AttributeModifier(
                    UUID.nameUUIDFromBytes(("berserker-bloodlust-" + player.getUniqueId()).getBytes()),
                    "berserker-bloodlust", bonus, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
            attr.addModifier(mod);
        }
    }

    // ------ SYPHON (PASSIVE LIFESTEAL) ------
    public static void handleSyphon(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (inBladeMode.contains(player.getUniqueId())) return; // Disabled in blade mode
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (syphonCooldown.getOrDefault(uuid, 0L) > now) return;
        if (Math.random() > 0.15) return;
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2.0));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        syphonCooldown.put(uuid, now + 1500);
    }

    // ------ BLOODLUST (REFRESH ON HIT) ------
    public static void handleBloodlust(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (inBladeMode.contains(player.getUniqueId())) return; // Disabled in blade mode
        applyBloodlust(player);
    }

    // ------ UNYIELDING RAGE (ON KILL BUFF) ------
    public static void handleKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null) return;
        int level = data.getClassLevel();
        if (level < 2) return;
        if (inBladeMode.contains(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (unyieldingRageCooldown.getOrDefault(uuid, 0L) > now) {
            player.sendMessage("§c[Unyielding Rage] On cooldown!");
            return;
        }
        if (unyieldingRageEnd.getOrDefault(uuid, 0L) > now) {
            player.sendMessage("§c[Unyielding Rage] Already active!");
            return;
        }
        if (data.getCharge() < 80) {
            player.sendMessage("§c[Unyielding Rage] Not enough charge!");
            return;
        }
        data.setCharge(data.getCharge() - 80);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 2)); // Strength III
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1)); // Speed II
        player.sendMessage("§6[Unyielding Rage] Strength III & Speed II for 60s!");
        unyieldingRageEnd.put(uuid, now + 60000);
        unyieldingRageCooldown.put(uuid, now + 60000);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removePotionEffect(PotionEffectType.STRENGTH); // Strength
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }.runTaskLater(com.pocketlazy.classify.ClassifyPlugin.getInstance(), 20 * 60);
    }

    // ------ RAGE (WEAKENED/EMPOWERED), INFERNO BLADE, FLAMING SLASH, WITHERING RAGE ------
    public static void handleInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null) return;
        int level = data.getClassLevel();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // INFERNO BLADE MODE
        if (inBladeMode.contains(uuid)) {
            // Flaming Slash or Withering Rage
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    // Withering Rage
                    if (witherRageCooldown.getOrDefault(uuid, 0L) > now) {
                        player.sendMessage("§c[Withering Rage] On cooldown!");
                        return;
                    }
                    if (data.getCharge() < 120) {
                        player.sendMessage("§c[Withering Rage] Not enough charge!");
                        return;
                    }
                    data.setCharge(data.getCharge() - 120);
                    witherReady.add(uuid);
                    witherRageCooldown.put(uuid, now + 25000);
                    player.sendMessage("§c[Withering Rage] Skull ready! Left click to launch.");
                    // (Optional: swirling particles around player)
                } else {
                    // Flaming Slash
                    if (flamingSlashCooldown.getOrDefault(uuid, 0L) > now) {
                        player.sendMessage("§c[Flaming Slash] On cooldown!");
                        return;
                    }
                    if (data.getCharge() < 100) {
                        player.sendMessage("§c[Flaming Slash] Not enough charge!");
                        return;
                    }
                    LivingEntity target = getTarget(player, 10);
                    if (target == null) {
                        player.sendMessage("§c[Flaming Slash] No target in range!");
                        return;
                    }
                    data.setCharge(data.getCharge() - 100);
                    flamingSlashCooldown.put(uuid, now + 10000);

                    double percent = 0.25;
                    if (target.getType() == EntityType.WITHER || target.getType() == EntityType.ENDER_DRAGON)
                        percent = 0.05;
                    double dmg = Math.max(1.0, target.getHealth() * percent);
                    target.damage(dmg, player);
                    target.setFireTicks(60);
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0,1,0), 25, 0.6, 0.6, 0.6, 0.03);
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.7f);
                    player.sendMessage("§c[Flaming Slash] Dealt " + String.format("%.1f", dmg) + " damage!");
                }
            }
            // Withering Rage: left click to fire skull
            if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) && witherReady.contains(uuid)) {
                witherReady.remove(uuid);
                LivingEntity target = getTarget(player, 20);
                if (target != null) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 5, 1));
                    target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1.2, 0), 40, 0.4, 0.4, 0.4);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 1);
                    player.sendMessage("§c[Withering Rage] Target afflicted with Wither II!");
                } else {
                    player.sendMessage("§c[Withering Rage] No target in sight!");
                }
            }
            event.setCancelled(true);
            return;
        }

        // --- Not in blade mode: Rage/Inferno Blade ---
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                // Rage (Weakened or Empowered)
                if (rageCooldown.getOrDefault(uuid, 0L) > now) {
                    player.sendMessage("§c[Rage] On cooldown!");
                    return;
                }
                double percent = 0.10;
                double atk = 0.5;
                double ms = 0.005;
                int cost = 50;
                int cooldown = 20000;
                if (level >= 3) {
                    atk = 1.0;
                    ms = 0.01;
                    cost = 80;
                    cooldown = 15000;
                }
                if (data.getCharge() < cost) {
                    player.sendMessage("§c[Rage] Not enough charge!");
                    return;
                }
                data.setCharge(data.getCharge() - cost);
                rageCooldown.put(uuid, now + cooldown);
                // Lose 10% max HP
                double lose = player.getMaxHealth() * percent;
                player.damage(lose);
                // Apply stat buffs
                AttributeInstance ad = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                AttributeInstance sp = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                AttributeModifier adMod = new AttributeModifier(UUID.randomUUID(), "berserker-rage-ad", atk, AttributeModifier.Operation.ADD_NUMBER);
                AttributeModifier spMod = new AttributeModifier(UUID.randomUUID(), "berserker-rage-ms", ms, AttributeModifier.Operation.ADD_NUMBER);
                ad.addModifier(adMod);
                sp.addModifier(spMod);
                player.sendMessage("§c[Rage] Fury unleashed!");
                // Remove buffs after 10s (200 ticks)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ad.removeModifier(adMod);
                        sp.removeModifier(spMod);
                    }
                }.runTaskLater(com.pocketlazy.classify.ClassifyPlugin.getInstance(), 200);
                event.setCancelled(true);
                return;
            } else if (level >= 3) {
                // Inferno Blade
                if (bladeCooldown.getOrDefault(uuid, 0L) > now) {
                    player.sendMessage("§c[Inferno Blade] On cooldown!");
                    return;
                }
                if (data.getCharge() < 250) {
                    player.sendMessage("§c[Inferno Blade] Not enough charge!");
                    return;
                }
                data.setCharge(data.getCharge() - 250);
                bladeCooldown.put(uuid, now + 600_000);
                bladeEnd.put(uuid, now + 180_000);
                inBladeMode.add(uuid);
                player.sendMessage("§c[Inferno Blade] You are consumed by rage!");

                // Action Bar lock + display (only during Inferno Blade)
                ActionBarManager.lock(player);
                BukkitRunnable bar = new BukkitRunnable() {
                    int timeLeft = 180;
                    @Override
                    public void run() {
                        if (!inBladeMode.contains(uuid) || !player.isOnline()) {
                            player.sendActionBar("");
                            this.cancel();
                            ActionBarManager.unlock(player);
                            bladeActionBars.remove(uuid);
                            return;
                        }
                        int charge = PlayerClassManager.getInstance().get(player).getCharge();
                        // Format: Inferno timer (left)                  Charge (right)
                        String actionBar = String.format("§cInferno: §f%3ds%s%36s§eCharge: §f%d", timeLeft, "", "", charge);
                        // ^ pad for visual separation, you can adjust the spacing as needed
                        player.sendActionBar(actionBar);
                        timeLeft--;
                        if (timeLeft < 0) {
                            player.sendActionBar("");
                            this.cancel();
                            ActionBarManager.unlock(player);
                            bladeActionBars.remove(uuid);
                        }
                    }
                };
                bar.runTaskTimer(com.pocketlazy.classify.ClassifyPlugin.getInstance(), 0L, 20L);
                bladeActionBars.put(uuid, bar);

                // Apply buffs
                AttributeInstance ad = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                AttributeInstance as = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
                ad.getModifiers().removeIf(m -> m.getName().equals("berserker-blade-ad"));
                as.getModifiers().removeIf(m -> m.getName().equals("berserker-blade-as"));
                AttributeModifier adMod = new AttributeModifier(UUID.nameUUIDFromBytes(("berserker-blade-ad-" + uuid).getBytes()), "berserker-blade-ad", 15, AttributeModifier.Operation.ADD_NUMBER);
                AttributeModifier asMod = new AttributeModifier(UUID.nameUUIDFromBytes(("berserker-blade-as-" + uuid).getBytes()), "berserker-blade-as", 1.6, AttributeModifier.Operation.ADD_NUMBER);
                ad.addModifier(adMod);
                as.addModifier(asMod);

                // Remove after 180s (3600 ticks)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        inBladeMode.remove(uuid);
                        ad.removeModifier(adMod);
                        as.removeModifier(asMod);
                        player.sendMessage("§c[Inferno Blade] Power fades.");
                        // Cancel the action bar runnable & unlock
                        BukkitRunnable bar = bladeActionBars.remove(uuid);
                        if (bar != null) bar.cancel();
                        player.sendActionBar("");
                        ActionBarManager.unlock(player);
                    }
                }.runTaskLater(com.pocketlazy.classify.ClassifyPlugin.getInstance(), 20 * 180);
                event.setCancelled(true);
                return;
            }
        }
    }

    // --- Helper: Get target in line of sight ---
    private static LivingEntity getTarget(Player player, double range) {
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Location loc = player.getEyeLocation().clone();
        for (int i = 0; i < range; i++) {
            loc.add(dir);
            for (Entity e : player.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
                if (e instanceof LivingEntity le && !le.equals(player)) return le;
            }
        }
        return null;
    }

    // --- RESET ALL (on death/quit/class change) ---
    public static void resetAll(Player player) {
        UUID uuid = player.getUniqueId();
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attr != null) {
            attr.getModifiers().removeIf(mod -> mod.getName().startsWith("berserker-"));
        }
        AttributeInstance as = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (as != null) {
            as.getModifiers().removeIf(mod -> mod.getName().startsWith("berserker-"));
        }
        AttributeInstance sp = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (sp != null) {
            sp.getModifiers().removeIf(mod -> mod.getName().startsWith("berserker-"));
        }
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.WITHER);
        syphonCooldown.remove(uuid);
        unyieldingRageEnd.remove(uuid);
        unyieldingRageCooldown.remove(uuid);
        rageCooldown.remove(uuid);
        bladeCooldown.remove(uuid);
        bladeEnd.remove(uuid);
        flamingSlashCooldown.remove(uuid);
        witherRageCooldown.remove(uuid);
        inBladeMode.remove(uuid);
        witherReady.remove(uuid);

        // Cancel action bar if running
        BukkitRunnable bar = bladeActionBars.remove(uuid);
        if (bar != null) bar.cancel();
        ActionBarManager.unlock(player);
        player.sendActionBar("");
    }
}