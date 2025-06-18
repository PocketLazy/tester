package com.pocketlazy.classify.classes;

import com.pocketlazy.classify.abilities.AssassinAbilityManager;
import com.pocketlazy.classify.items.AbilityItemManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.util.ClassifyTagUtil;
import com.pocketlazy.classify.util.AbilitySoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Assassin extends PlayerClass implements Listener {

    private static final int DASH_COST = 50, DASH_DISTANCE = 2;
    private static final int CLOAK_COST = 100, CLOAK_DURATION_SEC = 60;
    private static final int WITHERING_SMOKE_COST = 150;
    private static final int WITHERING_SMOKE_COOLDOWN = 20; // seconds
    private static final int WITHERING_SMOKE_WITHER_DURATION = 100; // 5 seconds (ticks)

    private static final double BASE_ATTACK_DAMAGE = 1.0;
    private static final double BASE_ATTACK_SPEED = 4.0; // Vanilla default

    private static final Set<UUID> starParticleRunners = new HashSet<>();
    private static final Map<UUID, Long> witheringSmokeCooldowns = new HashMap<>();

    @Override
    public String getName() { return "Assassin"; }

    @Override
    public String getDescription(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("§7Class: §8Assassin\n");
        sb.append("§bPassive: No fall damage\n");
        sb.append("§bAbility: Dash (§eRight Click§b) - Dash forward with sneaky stealth\n");
        sb.append("§8Cost: §d").append(DASH_COST).append(" §7| Cooldown: §e").append(AssassinAbilityManager.DASH_COOLDOWN).append("s\n");
        if (level >= 2) {
            sb.append("§aPassive: Shadow Boost - At night or darkness, +0.5 damage, +0.5 attack speed, Speed I, starry particles at night\n");
            sb.append("§bAbility: Withering Smoke (§eLeft Click Entity§b) - Withers and clouds vision of hit targets for 5 seconds. Cost: ")
                    .append(WITHERING_SMOKE_COST).append(". Cooldown: ").append(WITHERING_SMOKE_COOLDOWN).append("s.\n");
        }
        if (level >= 3) {
            sb.append("§bAbility: Cloak (§eShift+Right Click§b) – Invisible (including armor) for 60s. Cost: 100+ charge (uses all), 2 min cooldown.\n");
        }
        return sb.toString();
    }

    @Override
    public ItemStack getAbilityItem(int level, Player player, int lives) {
        ItemStack item;
        ItemMeta meta;
        String name;
        int modelData;
        if (level == 1) {
            item = new ItemStack(Material.ENDER_PEARL);
            meta = item.getItemMeta();
            name = "§8Sneaky Shadow";
            modelData = 4001;
        } else if (level == 2) {
            item = new ItemStack(Material.ENDER_PEARL);
            meta = item.getItemMeta();
            name = "§9Nightfall Gemstone";
            modelData = 4002;
        } else {
            item = new ItemStack(Material.ENDER_EYE);
            meta = item.getItemMeta();
            name = "§5Void Seeker Crystal";
            modelData = 4003;
        }
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add("§7Class: §8Assassin");
        lore.add("§aPassive: No fall damage");
        lore.add("§bAbility: Dash " + ChatColor.GRAY + "(Right Click)");
        lore.add("§7Dash forward " + ChatColor.YELLOW + DASH_DISTANCE + " blocks" + ChatColor.GRAY + " with campfire smoke.");
        lore.add("§8Charge Cost: " + ChatColor.AQUA + DASH_COST);
        lore.add("§8Cooldown: " + ChatColor.YELLOW + AssassinAbilityManager.DASH_COOLDOWN + "s");
        if (level >= 2) {
            lore.add("");
            lore.add("§aPassive: Shadow Boost (Night/Light 0): starry particles at night");
            lore.add("§7+0.5 Damage, +0.5 Attack Speed, Speed I");
            lore.add("");
            lore.add("§bAbility: Withering Smoke " + ChatColor.GRAY + "(Left Click Entity)");
            lore.add("§7Withers and clouds vision of hit targets for 5 seconds.");
            lore.add("§8Cost: " + ChatColor.AQUA + WITHERING_SMOKE_COST);
            lore.add("§8Cooldown: " + ChatColor.YELLOW + WITHERING_SMOKE_COOLDOWN + "s");
        }
        if (level == 3) {
            lore.add("");
            lore.add("§bAbility: Cloak " + ChatColor.GRAY + "(Shift+Right Click)");
            lore.add("§7Invisible for " + ChatColor.GREEN + "60s (armor hidden)");
            lore.add("§7Armor is invisible; all charge is consumed.");
            lore.add("§8Charge Cost: " + ChatColor.AQUA + CLOAK_COST + "+ (uses all)");
            lore.add("§8Cooldown: " + ChatColor.YELLOW + AssassinAbilityManager.CLOAK_COOLDOWN + "s");
        }
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setCustomModelData(modelData);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onLevelUp(Player player, int newLevel) {
        setAssassinBaseAttributes(player, newLevel);
        updateTag(player);
        AbilityItemManager.giveAbilityItem(player, this, newLevel, PlayerClassManager.getInstance().get(player).getLives());
        AssassinAbilityManager.reset(player);
        updateStarParticles(player, newLevel);
    }

    @Override
    public void onRemove(Player player, int level) {
        resetAssassinAttributes(player);
        AssassinAbilityManager.reset(player);
        updateTag(player);
        stopStarParticles(player);
    }

    @Override
    public void onAbilityItemUse(Player player, int level) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        // Strict class check, just like all other classes:
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) {
            player.sendMessage(ChatColor.RED + "[Assassin] Only Assassins can use this ability!");
            return;
        }
        if (level < 3) {
            player.sendMessage(ChatColor.RED + "[Assassin] You need to be Assassin level 3 for Cloak!");
            AbilitySoundUtil.playCooldownSound(player);
            return;
        }
        if (AssassinAbilityManager.isCloaked(player)) {
            player.sendMessage(ChatColor.RED + "[Assassin] Cloak is already active!");
            AbilitySoundUtil.playCooldownSound(player);
            return;
        }
        int secondsLeft = AssassinAbilityManager.getCloakCooldownSecondsLeft(player);
        if (secondsLeft > 0) {
            player.sendMessage(ChatColor.RED + "[Assassin] Cloak is on cooldown! " + secondsLeft + "s left.");
            AbilitySoundUtil.playCooldownSound(player);
            return;
        }
        if (data.getCharge() < CLOAK_COST) {
            player.sendMessage(ChatColor.RED + "[Assassin] Not enough charge! Need 100.");
            AbilitySoundUtil.playNoChargeSound(player);
            return;
        }
        data.setCharge(0);
        activateCloak(player);
        AssassinAbilityManager.startCloakCooldown(player);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        // Strict class check at top of all handlers (like all other classes):
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        int level = data.getClassLevel();
        if (!AbilityItemManager.isAbilityItem(player, item)) return;
        if (item.getType() == Material.ENDER_PEARL || item.getType() == Material.ENDER_EYE) {
            e.setCancelled(true);
        }

        // Dash (right click, not sneaking)
        if (e.getAction().toString().contains("RIGHT") && !player.isSneaking()) {
            if (level >= 1) tryDash(player, data);
        }

        // Cloak (shift + right click)
        if (e.getAction().toString().contains("RIGHT") && player.isSneaking() && level >= 3) {
            onAbilityItemUse(player, level);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBackstab(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) return;
        int level = data.getClassLevel();
        if (level < 2) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!AbilityItemManager.isAbilityItem(player, item)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        // Withering Smoke ability logic - REQUIREMENTS ENFORCED
        long now = System.currentTimeMillis();
        long last = witheringSmokeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldown = WITHERING_SMOKE_COOLDOWN * 1000L;
        boolean onCooldown = now - last < cooldown;
        boolean enoughCharge = data.getCharge() >= WITHERING_SMOKE_COST;

        if (onCooldown || !enoughCharge) {
            // Cancel the attack if requirements are not met (NO damage)
            e.setCancelled(true);
            if (onCooldown) {
                long secondsLeft = (cooldown - (now - last)) / 1000L + 1;
                player.sendMessage(ChatColor.RED + "[Assassin] Withering Smoke is on cooldown! " + secondsLeft + "s left.");
                AbilitySoundUtil.playCooldownSound(player);
            } else {
                player.sendMessage(ChatColor.RED + "[Assassin] Not enough charge for Withering Smoke! Need " + WITHERING_SMOKE_COST + ".");
                AbilitySoundUtil.playNoChargeSound(player);
            }
            return;
        }

        // Use up charge and start cooldown
        witheringSmokeCooldowns.put(player.getUniqueId(), now);
        data.setCharge(data.getCharge() - WITHERING_SMOKE_COST);

        LivingEntity target = (LivingEntity) e.getEntity();
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, WITHERING_SMOKE_WITHER_DURATION, 0)); // 5 seconds wither
        player.sendMessage(ChatColor.DARK_GRAY + "[Assassin] Withered " + (target instanceof Player ? target.getName() : "a mob") + " for 5 seconds!");

        // Play a "wither" sound effect to match the ability
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.1f, 0.4f);
        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 0.65f);

        // Block vision with smoke and ink particles for 3 seconds (60 ticks), but NO blindness
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60 || target.isDead() || !target.isValid()) {
                    cancel();
                    return;
                }
                Location eyeLoc = target.getLocation().add(0, target.getHeight() * 0.85, 0);
                target.getWorld().spawnParticle(Particle.LARGE_SMOKE, eyeLoc, 12, 0.2, 0.2, 0.2, 0.04);
                target.getWorld().spawnParticle(Particle.SQUID_INK, eyeLoc, 8, 0.15, 0.15, 0.15, 0.06);
                ticks += 4;
            }
        }.runTaskTimer(com.pocketlazy.classify.ClassifyPlugin.getInstance(), 0L, 4L); // every 0.2s for 3s
    }

    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) return;
        int level = data.getClassLevel();
        if (level >= 1 && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
            player.sendMessage("§a[Classify] No fall damage!");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) {
            resetAssassinAttributes(player);
            stopStarParticles(player);
            return;
        }
        int level = data.getClassLevel();
        setAssassinBaseAttributes(player, level);

        boolean isNightOrDark = false;
        if (level >= 2) {
            long time = player.getWorld().getTime();
            boolean isNight = (time >= 13000 && time < 23000);
            boolean isDark = player.getLocation().getBlock().getLightLevel() == 0;
            isNightOrDark = isNight || isDark;
            setShadowBoost(player, isNightOrDark);
        } else {
            setShadowBoost(player, false);
        }
        updateStarParticles(player, level);
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) return;
        int level = data.getClassLevel();
        if (level < 2) {
            stopStarParticles(player);
            return;
        }
        long time = player.getWorld().getTime();
        boolean isNight = (time >= 13000 && time < 23000);
        boolean isDark = player.getLocation().getBlock().getLightLevel() == 0;
        setShadowBoost(player, isNight || isDark);
        updateStarParticles(player, level);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updateTag(e.getPlayer());
        PlayerData data = PlayerClassManager.getInstance().get(e.getPlayer());
        if (data != null && data.getPlayerClass() instanceof Assassin) {
            updateStarParticles(e.getPlayer(), data.getClassLevel());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        AssassinAbilityManager.reset(e.getPlayer());
        updateTag(e.getPlayer());
        stopStarParticles(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) return;
        int level = data.getClassLevel();
        if (level < 2) {
            stopStarParticles(player);
            return;
        }
        Bukkit.getScheduler().runTaskLater(com.pocketlazy.classify.ClassifyPlugin.getInstance(), () -> {
            long time = player.getWorld().getTime();
            boolean isNight = (time >= 13000 && time < 23000);
            boolean isDark = player.getLocation().getBlock().getLightLevel() == 0;
            setShadowBoost(player, isNight || isDark);
            updateStarParticles(player, level);
        }, 10L);
    }

    // ----- Internal ability logic -----
    private void tryDash(Player player, PlayerData data) {
        // Only allow Assassin class to dash (strict check)
        if (data == null || !(data.getPlayerClass() instanceof Assassin)) {
            player.sendMessage(ChatColor.RED + "[Assassin] Only Assassins can dash!");
            return;
        }
        int secondsLeft = AssassinAbilityManager.getDashCooldownSecondsLeft(player);
        if (secondsLeft > 0) {
            player.sendMessage(ChatColor.RED + "[Assassin] Dash is on cooldown! " + secondsLeft + "s left.");
            AbilitySoundUtil.playCooldownSound(player);
            return;
        }
        if (data.getCharge() < DASH_COST) {
            player.sendMessage(ChatColor.RED + "[Assassin] Not enough charge for Dash!");
            AbilitySoundUtil.playNoChargeSound(player);
            return;
        }
        Vector direction = player.getLocation().getDirection().setY(0).normalize().multiply(DASH_DISTANCE);
        player.setVelocity(direction);
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0,1,0), 18, 0.3, 0.5, 0.3, 0.06);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.1f, 1.4f);
        AssassinAbilityManager.startDashCooldown(player);
        data.setCharge(data.getCharge() - DASH_COST);
        player.sendMessage(ChatColor.DARK_GRAY + "[Assassin] Dashed forward!");
    }

    // Cloak with ProtocolLib-based armor hide (no removal of items)
    public void activateCloak(Player player) {
        AssassinAbilityManager.setCloaked(player, true);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, CLOAK_DURATION_SEC * 20, 1, false, false, false));
        // Emit "arcane" effect: OMINOUS_SPAWNING, SQUID_INK, and LARGE_SMOKE particles in a ring burst (not on head)
        Location base = player.getLocation().add(0, 1.0, 0);
        for (int i = 0; i < 28; i++) {
            double angle = Math.random() * Math.PI * 2;
            double y = 0.3 + Math.random() * 1.0;
            double radius = 0.7 + Math.random() * 0.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = base.clone().add(x, y - 1.0, z);
            player.getWorld().spawnParticle(Particle.OMINOUS_SPAWNING, particleLoc, 1, 0, 0, 0, 0.03);
            player.getWorld().spawnParticle(Particle.SQUID_INK, particleLoc, 2, 0.07, 0.07, 0.07, 0.05);
            player.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, 2, 0.1, 0.1, 0.1, 0.03);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1.2f, 0.6f);

        player.sendMessage(ChatColor.GRAY + "[Assassin] You are now invisible for 60 seconds and your armor is hidden!");
        player.sendMessage(ChatColor.YELLOW + "[Assassin] Note: You can still see your own armor, but other players cannot while Cloak is active.");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p != player)
                p.hidePlayer(com.pocketlazy.classify.ClassifyPlugin.getInstance(), player);
        }
        stopStarParticles(player); // Stop star particles while cloaked!
        new BukkitRunnable() {
            @Override
            public void run() {
                AssassinAbilityManager.removeCloak(player);
                player.sendMessage(ChatColor.RED + "[Assassin] Cloak ended.");
                PlayerData data = PlayerClassManager.getInstance().get(player);
                if (data != null && data.getPlayerClass() instanceof Assassin) updateStarParticles(player, data.getClassLevel());
            }
        }.runTaskLater(com.pocketlazy.classify.ClassifyPlugin.getInstance(), CLOAK_DURATION_SEC * 20L);
    }

    public static void updateTag(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        boolean shouldTag =
                (data != null && (
                        data.isGhost() ||
                                data.getPlayerClass() instanceof Assassin ||
                                player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                ));
        if (shouldTag)
            ClassifyTagUtil.addTag(player);
        else
            ClassifyTagUtil.removeTag(player);
    }

    // --- Attribute logic ---
    private void setAssassinBaseAttributes(Player player, int level) {
        setAttr(player, Attribute.GENERIC_ATTACK_DAMAGE, BASE_ATTACK_DAMAGE);
        setAttr(player, Attribute.GENERIC_ATTACK_SPEED, BASE_ATTACK_SPEED);
    }

    private void setShadowBoost(Player player, boolean boost) {
        if (boost) {
            setAttr(player, Attribute.GENERIC_ATTACK_DAMAGE, BASE_ATTACK_DAMAGE + 0.5);
            setAttr(player, Attribute.GENERIC_ATTACK_SPEED, BASE_ATTACK_SPEED + 0.5);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false, false));
        } else {
            int level = PlayerClassManager.getInstance().get(player).getClassLevel();
            setAssassinBaseAttributes(player, level);
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    public static void resetAssassinAttributes(Player player) {
        setAttr(player, Attribute.GENERIC_ATTACK_DAMAGE, 1.0);
        setAttr(player, Attribute.GENERIC_ATTACK_SPEED, 4.0);
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    private static void setAttr(Player player, Attribute attr, double value) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    private void updateStarParticles(Player player, int level) {
        boolean eligible =
                level >= 2 &&
                        !AssassinAbilityManager.isCloaked(player) &&
                        isNightOrDark(player);

        if (eligible) {
            if (!starParticleRunners.contains(player.getUniqueId())) {
                startStarParticles(player);
                starParticleRunners.add(player.getUniqueId());
            }
        } else {
            stopStarParticles(player);
        }
    }

    private boolean isNightOrDark(Player player) {
        long time = player.getWorld().getTime();
        boolean isNight = (time >= 13000 && time < 23000);
        boolean isDark = player.getLocation().getBlock().getLightLevel() == 0;
        return isNight || isDark;
    }

    private void startStarParticles(Player player) {
        UUID uuid = player.getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerData data = PlayerClassManager.getInstance().get(player);
                if (player.isOnline()
                        && data != null
                        && data.getPlayerClass() instanceof Assassin
                        && !AssassinAbilityManager.isCloaked(player)
                        && isNightOrDark(player)
                        && starParticleRunners.contains(uuid)) {

                    Particle particleType = Particle.OMINOUS_SPAWNING;

                    Location base = player.getLocation().add(0, 1.0, 0); // center of player
                    int count = 10;
                    int spawned = 0;
                    for (int i = 0; i < 16 && spawned < count; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double y = 0.3 + Math.random() * 1.0;
                        double radius = 0.6 + Math.random() * 0.45;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location particleLoc = base.clone().add(x, y - 1.0, z);
                        player.getWorld().spawnParticle(particleType, particleLoc, 1, 0, 0, 0, 0.03);
                        spawned++;
                    }
                } else {
                    cancel();
                    starParticleRunners.remove(uuid);
                }
            }
        }.runTaskTimer(com.pocketlazy.classify.ClassifyPlugin.getInstance(), 0L, 10L + (int) (Math.random() * 11));
    }

    private void stopStarParticles(Player player) {
        starParticleRunners.remove(player.getUniqueId());
    }
}