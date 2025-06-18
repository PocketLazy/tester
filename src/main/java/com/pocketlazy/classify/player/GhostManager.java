package com.pocketlazy.classify.player;

import com.pocketlazy.classify.ClassifyPlugin;
import com.pocketlazy.classify.classes.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class GhostManager implements Listener {
    private static final GhostManager INSTANCE = new GhostManager();
    private final Map<UUID, Long> abilityCooldown = new HashMap<>();
    private final Map<UUID, Long> flightCooldown = new HashMap<>();
    private final Map<UUID, Long> visibleUntil = new HashMap<>();
    private final Map<UUID, Long> flightUntil = new HashMap<>();
    private final Set<UUID> isFlying = new HashSet<>();

    public static GhostManager getInstance() { return INSTANCE; }

    public static final int MAX_SPIRIT_CHARGE = 100;

    // Called to set ghost state (universal for online/offline)
    public void setGhost(OfflinePlayer offline, boolean ghost) {
        PlayerData data = PlayerClassManager.getInstance().get(offline.getUniqueId());
        if (data == null) return;
        data.setGhost(ghost);

        // If online, update Bukkit states
        if (offline.isOnline()) {
            Player player = offline.getPlayer();
            setGhost(player, ghost);
        }
    }

    // Called to set ghost state for online players (handles effects)
    public void setGhost(Player player, boolean ghost) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        data.setGhost(ghost);
        PlayerClassManager.updateTabDisplay(player, data);

        if (ghost) {
            player.setInvisible(true);
            player.setGlowing(false);
            player.setGameMode(GameMode.SURVIVAL);
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0);
            player.getInventory().setHelmet(null);
            clearArmor(player);
            data.setPlayerClass(null);
            data.setClassLevel(1);
            data.setSpiritCharge(MAX_SPIRIT_CHARGE);
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            Bukkit.getScheduler().runTaskLater(ClassifyPlugin.getInstance(), () -> {
                player.getInventory().setItem(8, GhostItems.getPhantomAbilityItem());
            }, 2L);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "You are now a ghost! You have Spirit Charge instead of regular charge, and unique abilities.");
        } else {
            player.setInvisible(false);
            player.setGlowing(false);
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1.0);
            player.getInventory().setHelmet(null);
            player.setAllowFlight(false);
            isFlying.remove(player.getUniqueId());
            visibleUntil.remove(player.getUniqueId());
            flightUntil.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "You have been revived!");
        }
    }

    private void clearArmor(Player player) {
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
    }

    public void startSpiritChargeRegen() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = PlayerClassManager.getInstance().get(player);
                    if (data.isGhost()) {
                        int sc = data.getSpiritCharge();
                        if (sc < MAX_SPIRIT_CHARGE) {
                            data.setSpiritCharge(Math.min(MAX_SPIRIT_CHARGE, sc + 5));
                        }
                    }
                }
            }
        }.runTaskTimer(ClassifyPlugin.getInstance(), 40L, 40L); // Every 2 seconds
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data.isGhost()) return;

        int lives = data.getLives();
        lives--;
        data.setLives(lives);

        int newClassLevel = Math.max(1, data.getClassLevel() - 1);
        data.setClassLevel(newClassLevel);

        if (lives <= 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    setGhost(player, true);
                    player.sendMessage(ChatColor.RED + "You have lost all your lives and become a ghost!");
                }
            }.runTaskLater(ClassifyPlugin.getInstance(), 1L);
        } else {
            player.sendMessage("§eYou lost a life! Lives remaining: §c" + lives);
            player.sendMessage(ChatColor.RED + "You lost a class level! New class level: §c" + newClassLevel);
            PlayerClassManager.updateTabDisplay(player, data);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            if (PlayerClassManager.getInstance().get(p).isGhost()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent e) {
        if (e.getEntity() instanceof Player p &&
                PlayerClassManager.getInstance().get(p).isGhost() &&
                e.getAction() == EntityPotionEffectEvent.Action.ADDED) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p &&
                PlayerClassManager.getInstance().get(p).isGhost() &&
                e.getSlotType() == InventoryType.SlotType.ARMOR) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.GRAY + "Ghosts cannot wear armor!");
        }
        if (e.getWhoClicked() instanceof Player p &&
                PlayerClassManager.getInstance().get(p).isGhost() &&
                e.getSlot() == 8) {
            ItemStack item = e.getCurrentItem();
            if (GhostItems.matches(item, GhostItems.getPhantomAbilityItem())) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.GRAY + "This ghost ability item cannot be moved!");
            }
        }
    }

    @EventHandler
    public void onArmorInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (PlayerClassManager.getInstance().get(p).isGhost()) {
            ItemStack item = e.getItem();
            if (item != null && isArmor(item.getType())) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.GRAY + "Ghosts cannot wear armor!");
            }
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (PlayerClassManager.getInstance().get(e.getPlayer()).isGhost()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.GRAY + "Ghosts cannot consume items!");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(p);
        if (data.isGhost()) {
            p.setInvisible(true);
        }
        PlayerClassManager.updateTabDisplay(p, data);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(p);
        if (data.isGhost()) {
            Bukkit.getScheduler().runTaskLater(ClassifyPlugin.getInstance(), () -> p.setInvisible(true), 2L);
        }
        PlayerClassManager.updateTabDisplay(p, data);
    }

    @EventHandler
    public void onGhostPunch(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player ghost)) return;
        if (!PlayerClassManager.getInstance().get(ghost).isGhost()) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        PlayerData data = PlayerClassManager.getInstance().get(ghost);
        if (data.getSpiritCharge() < 25) {
            e.setCancelled(true);
            ghost.sendMessage(ChatColor.DARK_PURPLE + "Not enough Spirit Charge (25 required)!");
            return;
        }
        e.setCancelled(true);
        data.setSpiritCharge(data.getSpiritCharge() - 25);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 1));
        ghost.sendMessage(ChatColor.LIGHT_PURPLE + "You blinded " + (target instanceof Player ? target.getName() : "a mob") + "!");
    }

    @EventHandler
    public void onAbilityUse(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (!data.isGhost()) return;
        ItemStack item = e.getItem();
        if (!GhostItems.matches(item, GhostItems.getPhantomAbilityItem())) return;
        e.setCancelled(true);

        boolean isShift = player.isSneaking();
        long now = System.currentTimeMillis();

        // Spectral Flight (shift right click)
        if (isShift) {
            if (flightCooldown.getOrDefault(player.getUniqueId(), 0L) > now) {
                player.sendMessage(ChatColor.DARK_PURPLE + "Spectral Flight is on cooldown!");
                return;
            }
            if (data.getSpiritCharge() < 100) {
                player.sendMessage(ChatColor.DARK_PURPLE + "Not enough Spirit Charge (100 required)!");
                return;
            }
            data.setSpiritCharge(data.getSpiritCharge() - 100);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setGlowing(true);
            // No helmet during flight
            player.getInventory().setHelmet(null);
            flightUntil.put(player.getUniqueId(), now + 60000);
            flightCooldown.put(player.getUniqueId(), now + 120000);
            isFlying.add(player.getUniqueId());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "You take spectral flight! (1m)");
            Bukkit.getScheduler().runTaskLater(ClassifyPlugin.getInstance(), () -> endFlight(player), 20 * 60);
        } else {
            // Phantom (right click)
            if (abilityCooldown.getOrDefault(player.getUniqueId(), 0L) > now) {
                player.sendMessage(ChatColor.DARK_PURPLE + "Phantom is on cooldown!");
                return;
            }
            if (data.getSpiritCharge() < 50) {
                player.sendMessage(ChatColor.DARK_PURPLE + "Not enough Spirit Charge (50 required)!");
                return;
            }
            data.setSpiritCharge(data.getSpiritCharge() - 50);

            player.setInvisible(false);
            player.setGlowing(true);

            // Set player's own head as helmet
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + player.getName() + "'s Head");
            skull.setItemMeta(meta);
            player.getInventory().setHelmet(skull);

            visibleUntil.put(player.getUniqueId(), now + 60000);
            abilityCooldown.put(player.getUniqueId(), now + 120000);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "You become visible and glow for 1 minute!");
            Bukkit.getScheduler().runTaskLater(ClassifyPlugin.getInstance(), () -> endPhantom(player), 20 * 60);
        }
    }

    private void endFlight(Player player) {
        if (!PlayerClassManager.getInstance().get(player).isGhost()) return;
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGlowing(false);
        isFlying.remove(player.getUniqueId());
        player.getInventory().setHelmet(null);
        player.sendMessage(ChatColor.GRAY + "Spectral flight has ended.");
        player.setInvisible(true);
    }

    private void endPhantom(Player player) {
        if (!PlayerClassManager.getInstance().get(player).isGhost()) return;
        player.setInvisible(true);
        player.setGlowing(false);
        player.getInventory().setHelmet(null);
        player.sendMessage(ChatColor.GRAY + "Phantom form has ended.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        isFlying.remove(p.getUniqueId());
        visibleUntil.remove(p.getUniqueId());
        flightUntil.remove(p.getUniqueId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!PlayerClassManager.getInstance().get(p).isGhost()) return;
        if (e.getRawSlots().contains(8)) {
            ItemStack item = p.getInventory().getItem(8);
            if (GhostItems.matches(item, GhostItems.getPhantomAbilityItem())) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.GRAY + "This ghost ability item cannot be moved!");
            }
        }
    }

    private boolean isArmor(Material m) {
        return m.name().contains("HELMET") || m.name().contains("CHESTPLATE") || m.name().contains("LEGGINGS") || m.name().contains("BOOTS");
    }
}