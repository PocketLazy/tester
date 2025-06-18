package com.pocketlazy.classify.gui;

import com.pocketlazy.classify.abilities.HealerAbilityManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class HealerCampfireGUI implements Listener {

    public static final int CAMPFIRE_COST = 250;
    private static final Map<UUID, Location> playerCampfireLocation = new HashMap<>();
    private static final Map<Location, ArmorStand> campfireHolograms = new HashMap<>();
    private static final Set<Location> unbreakableCampfires = new HashSet<>();
    private static final Map<Location, Set<UUID>> campfireRadiusPlayers = new HashMap<>();
    // Track dead players to avoid healing them in the campfire tick
    private static final Set<UUID> recentlyDead = new HashSet<>();

    public HealerCampfireGUI() {}

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, ChatColor.GREEN + "Place Healing Campfire?");
        ItemStack place = new ItemStack(Material.CAMPFIRE);
        ItemMeta meta = place.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Place Healing Campfire");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + CAMPFIRE_COST + " charge");
        lore.add(ChatColor.GRAY + "Places a healing campfire in front of you.");
        meta.setLore(lore);
        place.setItemMeta(meta);

        gui.setItem(4, place);
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
            gui.setItem(i, filler);
        }
        player.openInventory(gui);
    }

    /**
     * Play a warm and cozy campfire placement sound at the given location.
     */
    public static void playCampfirePlaceSound(Location campfireLoc) {
        campfireLoc.getWorld().playSound(campfireLoc, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.5f, 1.0f);
        campfireLoc.getWorld().playSound(campfireLoc, Sound.BLOCK_WOOD_PLACE, 0.7f, 0.85f);
    }

    /**
     * Play a "fire extinguish" and soft crackle when a campfire is removed.
     */
    public static void playCampfireRemoveSound(Location campfireLoc) {
        campfireLoc.getWorld().playSound(campfireLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.3f, 1.15f);
        campfireLoc.getWorld().playSound(campfireLoc, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.7f, 0.6f);
    }

    /**
     * Play a warm healing sound when a player is healed by the campfire.
     */
    public static void playCampfireHealSound(Location campfireLoc) {
        campfireLoc.getWorld().playSound(campfireLoc, Sound.BLOCK_HONEY_BLOCK_FALL, 0.4f, 1.2f);
        campfireLoc.getWorld().playSound(campfireLoc, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1.1f);
        campfireLoc.getWorld().playSound(campfireLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.35f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (
                title.equals(ChatColor.GREEN + "Place Healing Campfire?") ||
                        title.equals(ChatColor.RED + "Remove Healing Campfire?")
        ) {
            e.setCancelled(true);
            if (e.getRawSlot() != 4) return;
            Player player = (Player) e.getWhoClicked();

            if (title.equals(ChatColor.GREEN + "Place Healing Campfire?")) {
                if (playerCampfireLocation.containsKey(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "[Healer] You already have a campfire placed! Remove it before placing another.");
                    player.closeInventory();
                    return;
                }
                PlayerData data = PlayerClassManager.getInstance().get(player);
                if (data.getCharge() < CAMPFIRE_COST) {
                    player.sendMessage(ChatColor.RED + "[Healer] You need " + CAMPFIRE_COST + " charge to place a campfire!");
                    player.closeInventory();
                    return;
                }

                Location playerLoc = player.getLocation();
                BlockFace face = getFacingBlockFace(playerLoc.getYaw());
                Block inFront = player.getLocation().getBlock().getRelative(face);
                Location placeLoc = inFront.getLocation();

                if (inFront.getType() != Material.AIR) {
                    player.sendMessage(ChatColor.RED + "[Healer] Not enough space to place the campfire in front of you!");
                    player.closeInventory();
                    return;
                }

                inFront.setType(Material.CAMPFIRE);
                BlockData blockData = inFront.getBlockData();
                if (blockData instanceof Campfire) {
                    ((Campfire) blockData).setLit(true);
                    inFront.setBlockData(blockData, false);
                }
                playCampfirePlaceSound(placeLoc);
                player.sendMessage(ChatColor.GREEN + "[Healer] Healing campfire placed!");
                data.setCharge(data.getCharge() - CAMPFIRE_COST);
                player.closeInventory();

                playerCampfireLocation.put(player.getUniqueId(), placeLoc);
                unbreakableCampfires.add(placeLoc);

                ArmorStand stand = placeCampfireHologram(placeLoc, 60);
                campfireHolograms.put(placeLoc, stand);

                campfireRadiusPlayers.put(placeLoc, new HashSet<>());

                new BukkitRunnable() {
                    int secondsLeft = 60;
                    int resistanceTick = 0;
                    @Override
                    public void run() {
                        if (placeLoc.getBlock().getType() != Material.CAMPFIRE) {
                            if (campfireHolograms.containsKey(placeLoc)) {
                                campfireHolograms.get(placeLoc).remove();
                                campfireHolograms.remove(placeLoc);
                            }
                            unbreakableCampfires.remove(placeLoc);
                            playerCampfireLocation.remove(player.getUniqueId());
                            campfireRadiusPlayers.remove(placeLoc);
                            this.cancel();
                            return;
                        }

                        World world = placeLoc.getWorld();
                        double cx = placeLoc.getX() + 0.5;
                        double cy = placeLoc.getY();
                        double cz = placeLoc.getZ() + 0.5;
                        double radius = 5.0;

                        Set<UUID> nowInRadius = new HashSet<>();
                        for (Player nearby : world.getPlayers()) {
                            double dx = nearby.getLocation().getX() - cx;
                            double dz = nearby.getLocation().getZ() - cz;
                            if ((dx*dx + dz*dz) <= radius * radius && Math.abs(nearby.getLocation().getY() - cy) <= 2) {
                                nowInRadius.add(nearby.getUniqueId());

                                Set<UUID> prevSet = campfireRadiusPlayers.get(placeLoc);
                                if (prevSet == null || !prevSet.contains(nearby.getUniqueId())) {
                                    nearby.sendMessage(ChatColor.LIGHT_PURPLE + "The campfire's mystical powers are rejuvenating you!");
                                }

                                // ---- FIX: Don't heal dead players ----
                                if (recentlyDead.contains(nearby.getUniqueId()) || nearby.isDead() || nearby.getHealth() <= 0) {
                                    continue;
                                }
                                // ---------------------------------------

                                for (PotionEffect effect : new ArrayList<>(nearby.getActivePotionEffects())) {
                                    if (HealerAbilityManager.isNegativeEffect(effect.getType())) {
                                        nearby.removePotionEffect(effect.getType());
                                    }
                                }
                                double maxHealth = nearby.getMaxHealth();
                                double heal = Math.min(2.0, maxHealth - nearby.getHealth());
                                if (heal > 0) {
                                    nearby.setHealth(nearby.getHealth() + heal);
                                    nearby.sendMessage(ChatColor.GREEN + "[Healer] The campfire heals you!");
                                    playCampfireHealSound(placeLoc);
                                }
                                if (resistanceTick == 0) {
                                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1, true, false), true);
                                }
                                for (int i = 0; i < 12; i++) {
                                    double angle = (2 * Math.PI / 12) * i;
                                    double px = nearby.getLocation().getX() + Math.cos(angle) * 0.6;
                                    double pz = nearby.getLocation().getZ() + Math.sin(angle) * 0.6;
                                    double py = nearby.getLocation().getY() + 0.2;
                                    world.spawnParticle(Particle.COMPOSTER, px, py, pz, 1, 0, 0, 0, 0);
                                }
                            }
                        }

                        campfireRadiusPlayers.put(placeLoc, nowInRadius);

                        int circlePoints = 64;
                        double y = cy + 0.1;
                        for (int i = 0; i < circlePoints; i++) {
                            double angle = 2 * Math.PI * i / circlePoints;
                            double x = cx + radius * Math.cos(angle);
                            double z = cz + radius * Math.sin(angle);
                            world.spawnParticle(Particle.COMPOSTER, x, y, z, 1, 0, 0, 0, 0);
                        }
                        world.spawnParticle(Particle.COMPOSTER, cx, cy + 1.2, cz, 8, 0.3, 0.2, 0.3, 0.01);

                        if (campfireHolograms.containsKey(placeLoc)) {
                            ArmorStand stand = campfireHolograms.get(placeLoc);
                            if (stand != null && !stand.isDead()) {
                                stand.setCustomName(ChatColor.GOLD + "Healing Campfire: " + ChatColor.AQUA + secondsLeft + "s");
                            }
                        }

                        secondsLeft--;
                        resistanceTick = (resistanceTick + 1) % 10;
                        if (secondsLeft <= 0) {
                            playCampfireRemoveSound(placeLoc);
                            placeLoc.getBlock().setType(Material.AIR);
                            if (campfireHolograms.containsKey(placeLoc)) {
                                campfireHolograms.get(placeLoc).remove();
                                campfireHolograms.remove(placeLoc);
                            }
                            unbreakableCampfires.remove(placeLoc);
                            player.sendMessage(ChatColor.YELLOW + "[Healer] Your healing campfire has expired.");
                            playerCampfireLocation.remove(player.getUniqueId());
                            campfireRadiusPlayers.remove(placeLoc);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(com.pocketlazy.classify.ClassifyPlugin.getInstance(), 0L, 20L);
            } else if (title.equals(ChatColor.RED + "Remove Healing Campfire?")) {
                if (player.hasMetadata("remove_campfire")) {
                    Location campfireLoc = (Location) player.getMetadata("remove_campfire").get(0).value();
                    if (campfireLoc.getBlock().getType() == Material.CAMPFIRE) {
                        playCampfireRemoveSound(campfireLoc);
                        campfireLoc.getBlock().setType(Material.AIR);
                        if (campfireHolograms.containsKey(campfireLoc)) {
                            campfireHolograms.get(campfireLoc).remove();
                            campfireHolograms.remove(campfireLoc);
                        }
                        unbreakableCampfires.remove(campfireLoc);
                        campfireRadiusPlayers.remove(campfireLoc);
                        player.sendMessage(ChatColor.YELLOW + "[Healer] Campfire removed.");
                    }
                    playerCampfireLocation.remove(player.getUniqueId());
                    player.removeMetadata("remove_campfire", com.pocketlazy.classify.ClassifyPlugin.getInstance());
                }
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (unbreakableCampfires.contains(loc)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "[Healer] This campfire can only be removed by its owner or when it expires!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        Location campfireLoc = clicked.getLocation();

        if (clicked.getType() == Material.CAMPFIRE && playerCampfireLocation.get(player.getUniqueId()) != null
                && playerCampfireLocation.get(player.getUniqueId()).equals(campfireLoc)) {
            event.setCancelled(true);
            openRemoveCampfireGUI(player, campfireLoc);
        }
    }

    public static void openRemoveCampfireGUI(Player player, Location campfireLoc) {
        Inventory gui = Bukkit.createInventory(player, 9, ChatColor.RED + "Remove Healing Campfire?");
        ItemStack confirm = new ItemStack(Material.BARRIER);
        ItemMeta meta = confirm.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Remove Campfire");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to remove your campfire!"));
        confirm.setItemMeta(meta);

        gui.setItem(4, confirm);
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
            gui.setItem(i, filler);
        }
        player.openInventory(gui);

        player.setMetadata("remove_campfire", new FixedMetadataValue(com.pocketlazy.classify.ClassifyPlugin.getInstance(), campfireLoc));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Location loc = playerCampfireLocation.remove(uuid);
        if (loc != null) {
            if (campfireHolograms.containsKey(loc)) {
                campfireHolograms.get(loc).remove();
                campfireHolograms.remove(loc);
            }
            unbreakableCampfires.remove(loc);
            campfireRadiusPlayers.remove(loc);
            if (loc.getBlock().getType() == Material.CAMPFIRE) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        // Cleanup dead set if any
        recentlyDead.remove(uuid);
    }

    // Mark a player as dead so campfire doesn't heal them post-mortem
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        recentlyDead.add(event.getEntity().getUniqueId());
        // Optionally, schedule cleanup after a short time (to avoid holding dead records forever)
        Bukkit.getScheduler().runTaskLater(com.pocketlazy.classify.ClassifyPlugin.getInstance(),
                () -> recentlyDead.remove(event.getEntity().getUniqueId()), 100L);
    }

    private static ArmorStand placeCampfireHologram(Location loc, int seconds) {
        World world = loc.getWorld();
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc.clone().add(0.5, 1.8, 0.5), EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setCustomName(ChatColor.GOLD + "Healing Campfire: " + ChatColor.AQUA + seconds + "s");
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setCollidable(false);
        stand.setBasePlate(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        return stand;
    }

    public static void removeCampfireFor(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = playerCampfireLocation.remove(uuid);
        if (loc != null) {
            if (campfireHolograms.containsKey(loc)) {
                campfireHolograms.get(loc).remove();
                campfireHolograms.remove(loc);
            }
            unbreakableCampfires.remove(loc);
            campfireRadiusPlayers.remove(loc);
            if (loc.getBlock().getType() == Material.CAMPFIRE) {
                loc.getBlock().setType(Material.AIR);
            }
        }
    }

    public static boolean hasCampfire(Player player) {
        return playerCampfireLocation.containsKey(player.getUniqueId());
    }

    private static BlockFace getFacingBlockFace(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }
}