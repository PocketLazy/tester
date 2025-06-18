package com.pocketlazy.classify.abilities;

import com.pocketlazy.classify.gui.ArcherQuiverManager;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import com.pocketlazy.classify.util.AbilitySoundUtil;
import com.pocketlazy.classify.util.ActionBarManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ArcherAbilityManager implements Listener {
    private static final int EXPLOSIVE_COOLDOWN_SECONDS = 120;
    private static final int EXPLOSIVE_COST = 100;
    private static final int SHORTBOW_COST = 15;
    private static final int MARK_COST = 25;
    private static final int TRACK_DURATION = 150;
    private static final int TRACK_COOLDOWN = 180;

    private static final String PLUGIN_NAME = "Classify";
    private static final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);

    private final Map<UUID, Long> explosiveCooldown = new HashMap<>();
    private final Map<UUID, Long> trackCooldown = new HashMap<>();
    private final Map<UUID, UUID> trackedEntity = new HashMap<>();
    private final Map<UUID, BukkitRunnable> trackingTasks = new HashMap<>();
    private final Set<UUID> explosiveClickCooldown = new HashSet<>();

    private static final int ABILITY_SLOT = 8;
    private static final String INJECTED_ARROW_MARK = ChatColor.LIGHT_PURPLE + "§l[Quiver Arrow]";
    private static final Map<UUID, ItemStack> injectedAbilityItems = new HashMap<>();

    private final Map<UUID, Long> shortbowCooldowns = new HashMap<>();
    private final Set<UUID> tempAbilityArrows = Collections.synchronizedSet(new HashSet<>());

    private static ArcherAbilityManager instance;
    public static ArcherAbilityManager getInstance() { return instance; }

    public ArcherAbilityManager() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // --- SHORTBOW ABILITY ---
    @EventHandler
    public void onShortbowAbility(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof com.pocketlazy.classify.classes.Archer)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean isAbilityItem = hand != null && hand.getType() == Material.FEATHER
                && hand.getItemMeta() != null && hand.getItemMeta().hasDisplayName()
                && hand.getItemMeta().getDisplayName().contains("Astral Feather");
        boolean isBow = hand != null && hand.getType() == Material.BOW;
        if (!(isAbilityItem || isBow)) return;

        int level = data.getClassLevel();
        int arrowsToShoot = level >= 3 ? 3 : (level == 2 ? 2 : 1);

        long last = shortbowCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        if (now - last < 500) {
            AbilitySoundUtil.playNoChargeSound(player);
            return;
        }

        if (countArrowsInQuiver(player) < arrowsToShoot) {
            AbilitySoundUtil.playNoChargeSound(player);
            return;
        }
        if (data.getCharge() < SHORTBOW_COST) {
            AbilitySoundUtil.playNoChargeSound(player);
            return;
        }
        data.setCharge(data.getCharge() - SHORTBOW_COST);

        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection().normalize();

        double spreadAngle = 7;
        int[] offsets = getSpreadOffsets(arrowsToShoot);
        if (isAbilityItem) addShortbowDisclaimerAbilityItem(hand);

        for (int i = 0; i < arrowsToShoot; i++) {
            ItemStack arrowUsed = takeOneArrowFromQuiver(player, true);
            if (arrowUsed == null) continue;
            Vector shotDir = dir.clone();
            if (arrowsToShoot > 1) {
                shotDir = rotateVectorY(dir, offsets[i] * spreadAngle);
            }
            Arrow arrow = loc.getWorld().spawnArrow(loc, shotDir, 2.7f, 0.0f);
            arrow.setShooter(player);
            arrow.setCritical(false); // Remove crit particles from shortbow ability
            arrow.setDamage(3.5 / 3.0);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            tempAbilityArrows.add(arrow.getUniqueId());
            scheduleArrowCleanup(arrow, 10 * 20);
            // No custom particle trail for shortbow ability arrows
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.2f, 1.1f);
        shortbowCooldowns.put(player.getUniqueId(), now);
        event.setCancelled(true);
    }

    private static int[] getSpreadOffsets(int count) {
        if (count == 3) return new int[]{-1, 0, 1};
        if (count == 2) return new int[]{-1, 1};
        return new int[]{0};
    }
    private static Vector rotateVectorY(Vector v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z).normalize();
    }
    private void scheduleArrowCleanup(final Arrow arrow, int ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!arrow.isDead() && arrow.isValid() && !arrow.isOnGround() && tempAbilityArrows.contains(arrow.getUniqueId())) {
                arrow.remove();
                tempAbilityArrows.remove(arrow.getUniqueId());
            }
        }, ticks);
    }
    @EventHandler
    public void onAbilityArrowHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            tempAbilityArrows.remove(arrow.getUniqueId());
        }
    }

    // Ender Dragon disclaimer
    public static void addShortbowDisclaimerAbilityItem(ItemStack item) {
        if (item == null || item.getType() != Material.FEATHER) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        boolean found = false;
        for (String l : lore) {
            if (ChatColor.stripColor(l).toLowerCase().contains("Shortbow")) found = true;
        }
        if (!found) {
            lore.add(ChatColor.DARK_GREEN + "§o[Shortbow] Left-click ability:");
            lore.add(ChatColor.GRAY + "§o- Ignores held bow stats/enchantments.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    // --- Arrow logic: any arrow type is consumed, but always shoots normal arrow for ability ---
    public static int countArrowsInQuiver(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof com.pocketlazy.classify.classes.Archer)) return 0;
        int quiverSize = ArcherQuiverManager.getUnlockedSlots(data.getClassLevel());
        ItemStack[] quiverArr = ArcherQuiverManager.getQuiverContents(player);
        if (quiverArr == null) return 0;
        int count = 0;
        for (int i = 0; i < quiverSize; i++) {
            if (quiverArr[i] != null && isArrow(quiverArr[i])) {
                count += quiverArr[i].getAmount();
            }
        }
        return count;
    }

    public static ItemStack takeOneArrowFromQuiver(Player player, boolean consume) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null || !(data.getPlayerClass() instanceof com.pocketlazy.classify.classes.Archer)) return null;
        int quiverSize = ArcherQuiverManager.getUnlockedSlots(data.getClassLevel());
        ItemStack[] quiverArr = ArcherQuiverManager.getQuiverContents(player);
        if (quiverArr == null) return null;
        for (int i = 0; i < quiverSize; i++) {
            ItemStack s = quiverArr[i];
            if (s != null && isArrow(s) && s.getAmount() > 0) {
                ItemStack result = s.clone();
                result.setAmount(1);
                if (consume) {
                    s.setAmount(s.getAmount() - 1);
                    if (s.getAmount() <= 0) quiverArr[i] = null;
                    ArcherQuiverManager.setQuiverContents(player, quiverArr);
                    ArcherQuiverManager.saveQuiver(player);
                }
                return result;
            }
        }
        return null;
    }

    public static boolean isArrow(ItemStack it) {
        if (it == null) return false;
        Material m = it.getType();
        return m == Material.ARROW || m == Material.TIPPED_ARROW || m == Material.SPECTRAL_ARROW;
    }

    // --- Hotbar Arrow Injection ---
    @EventHandler
    public void onPlayerInteractInjectArrow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isArcher(player)) return;

        ItemStack main = player.getInventory().getItemInMainHand();
        if (main == null) return;
        if (main.getType() != Material.BOW && main.getType() != Material.CROSSBOW) return;

        // Already injected, or already has arrow in inventory
        if (injectedAbilityItems.containsKey(player.getUniqueId())) return;
        if (hasArrowInInventory(player)) return;

        // Only inject on right click (for shoot or charge)
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        // Find a real arrow in the quiver (normal > tipped > spectral)
        ItemStack arrow = findBestArrowFromQuiver(player);
        if (arrow == null) return;

        // Save slot 8
        ItemStack prev = player.getInventory().getItem(ABILITY_SLOT);
        injectedAbilityItems.put(player.getUniqueId(), prev == null ? new ItemStack(Material.AIR) : prev.clone());

        // Remove the arrow from quiver here to guarantee consuming for normal shots (match type!)
        removeArrowFromQuiver(player, arrow);

        // Inject a clone of the consumed arrow, with no name/lore
        ItemStack injected = arrow.clone();
        injected.setAmount(1);
        ItemMeta meta = injected.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(null);
            meta.setLore(null);
            injected.setItemMeta(meta);
        }
        player.getInventory().setItem(ABILITY_SLOT, injected);
        player.updateInventory();
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!injectedAbilityItems.containsKey(player.getUniqueId())) return;
        restoreAbilityItem(player);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (injectedAbilityItems.containsKey(player.getUniqueId())) {
            restoreAbilityItem(player);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (injectedAbilityItems.containsKey(player.getUniqueId())) {
            restoreAbilityItem(player);
        }
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (injectedAbilityItems.containsKey(player.getUniqueId())) {
            restoreAbilityItem(player);
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getSlot() == ABILITY_SLOT && injectedAbilityItems.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItemDrop().getItemStack();
        if (stack != null && stack.getItemMeta() != null) {
            ItemMeta meta = stack.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().equals(INJECTED_ARROW_MARK)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (injectedAbilityItems.containsKey(player.getUniqueId())) {
            event.getDrops().removeIf(item -> {
                ItemMeta meta = item.getItemMeta();
                return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(INJECTED_ARROW_MARK);
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> restoreAbilityItem(player), 2L);
        }
    }

    private static boolean isArcher(Player player) {
        PlayerData data = PlayerClassManager.getInstance().get(player);
        return data != null && (data.getPlayerClass() instanceof com.pocketlazy.classify.classes.Archer);
    }
    private static boolean hasArrowInInventory(Player player) {
        for (ItemStack i : player.getInventory().getContents()) {
            if (i != null && i.getType().name().endsWith("_ARROW")) return true;
        }
        return false;
    }
    private static ItemStack findBestArrowFromQuiver(Player player) {
        ItemStack[] quiver = ArcherQuiverManager.getQuiverContents(player);
        PlayerData data = PlayerClassManager.getInstance().get(player);
        int unlocked = ArcherQuiverManager.getUnlockedSlots(data.getClassLevel());
        for (Material m : new Material[]{Material.ARROW, Material.TIPPED_ARROW, Material.SPECTRAL_ARROW}) {
            for (int i = 0; i < unlocked; i++) {
                ItemStack s = quiver[i];
                if (s != null && s.getType() == m && s.getAmount() > 0) {
                    return s;
                }
            }
        }
        return null;
    }
    private static void restoreAbilityItem(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack saved = injectedAbilityItems.remove(player.getUniqueId());
            if (saved != null) {
                player.getInventory().setItem(ABILITY_SLOT, saved);
                player.updateInventory();
            }
        }, 1L);
    }

    // Remove the *exact* arrow (type + meta) from quiver
    public static void removeArrowFromQuiver(Player player, ItemStack type) {
        ItemStack[] quiver = ArcherQuiverManager.getQuiverContents(player);
        PlayerData data = PlayerClassManager.getInstance().get(player);
        int unlocked = ArcherQuiverManager.getUnlockedSlots(data.getClassLevel());
        for (int i = 0; i < unlocked; i++) {
            ItemStack s = quiver[i];
            if (s != null && isArrow(s) && s.getAmount() > 0 && itemsEqualIgnoreAmount(s, type)) {
                s.setAmount(s.getAmount() - 1);
                if (s.getAmount() <= 0) quiver[i] = null;
                ArcherQuiverManager.setQuiverContents(player, quiver);
                break;
            }
        }
    }
    private static boolean itemsEqualIgnoreAmount(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        ItemMeta ma = a.getItemMeta(), mb = b.getItemMeta();
        return Objects.equals(ma, mb);
    }

    // --- Marked shot and tracking END THEME ---
    @EventHandler
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        PlayerData data = PlayerClassManager.getInstance().get(shooter);
        if (data == null || !(data.getPlayerClass() instanceof com.pocketlazy.classify.classes.Archer)) return;
        int level = data.getClassLevel();
        if (level < 2) return;
        if (!shooter.isSneaking()) return;

        // Cooldown
        if (trackCooldown.containsKey(shooter.getUniqueId())) {
            long cdLeft = (trackCooldown.get(shooter.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (cdLeft > 0) return;
        }

        // Charge
        if (data.getCharge() < MARK_COST) {
            shooter.sendMessage(ChatColor.RED + "[Archer] Not enough charge to mark target!");
            AbilitySoundUtil.playNoChargeSound(shooter);
            return;
        }
        data.setCharge(data.getCharge() - MARK_COST);

        LivingEntity target = (LivingEntity) event.getEntity();
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 150, 1, false, false, true));
        startTrackingTask(shooter, target, TRACK_DURATION);

        // Ender Dragon mark burst (End rune/Hypixel style)
        Location tLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        World tWorld = target.getWorld();
        tWorld.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, tLoc, 30, 0.8, 1.2, 0.8, 0.06);
        tWorld.spawnParticle(Particle.CHERRY_LEAVES, tLoc, 18, 0.5, 0.9, 0.5, 0.1);

        shooter.sendMessage(ChatColor.GREEN + "[Archer] Target marked!");
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_GOAT_SCREAMING_AMBIENT, 1.2f, 1.3f);

        trackCooldown.put(shooter.getUniqueId(), System.currentTimeMillis() + TRACK_COOLDOWN * 1000L);
    }

    private void startTrackingTask(Player archer, LivingEntity tracked, int seconds) {
        if (trackingTasks.containsKey(archer.getUniqueId())) trackingTasks.get(archer.getUniqueId()).cancel();

        trackedEntity.put(archer.getUniqueId(), tracked.getUniqueId());
        ActionBarManager.lock(archer);

        BukkitRunnable task = new BukkitRunnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                Player currArcher = Bukkit.getPlayer(archer.getUniqueId());
                LivingEntity currTracked = null;
                for (World world : Bukkit.getWorlds()) {
                    for (LivingEntity entity : world.getLivingEntities()) {
                        if (entity.getUniqueId().equals(tracked.getUniqueId())) {
                            currTracked = entity;
                            break;
                        }
                    }
                    if (currTracked != null) break;
                }
                if (currArcher == null || !currArcher.isOnline() || currTracked == null || currTracked.isDead()) {
                    ActionBarManager.unlock(archer);
                    if (currArcher != null) {
                        currArcher.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                    }
                    trackedEntity.remove(archer.getUniqueId());
                    trackingTasks.remove(archer.getUniqueId());
                    this.cancel();
                    return;
                }
                String entityTypeInfo;
                if (currTracked instanceof Player trackedPlayer) {
                    entityTypeInfo = trackedPlayer.getName();
                } else {
                    entityTypeInfo = currTracked.getType().name();
                }
                Location loc = currTracked.getLocation();
                // Ender Dragon action bar
                String msg =
                        ChatColor.DARK_GREEN + "➤ " +
                                ChatColor.GREEN + "Tracking Arrow: " +
                                ChatColor.WHITE + entityTypeInfo + " " +
                                ChatColor.GREEN + "X:" + loc.getBlockX() +
                                ChatColor.DARK_GREEN + " Y:" + loc.getBlockY() +
                                ChatColor.GREEN + " Z:" + loc.getBlockZ() +
                                ChatColor.DARK_GREEN + " | " +
                                ChatColor.GREEN + timeLeft + "s";
                currArcher.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
                timeLeft--;
                if (timeLeft <= 0) {
                    ActionBarManager.unlock(archer);
                    currArcher.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                    trackedEntity.remove(archer.getUniqueId());
                    trackingTasks.remove(archer.getUniqueId());
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
        trackingTasks.put(archer.getUniqueId(), task);
    }

    // --- Explosive ability: DRAGON THEME ---
    @EventHandler
    public void onExplosiveAbility(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        PlayerData data = PlayerClassManager.getInstance().get(player);
        if (data == null) return;
        int level = data.getClassLevel();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (level != 3) return;

        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean isAbilityItem = hand != null && hand.getType() == Material.FEATHER
                && hand.getItemMeta() != null && hand.getItemMeta().hasDisplayName()
                && hand.getItemMeta().getDisplayName().contains("Astral Feather");

        if (isAbilityItem && rightClick && !player.isSneaking()) {
            if (!explosiveClickCooldown.add(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> explosiveClickCooldown.remove(player.getUniqueId()), 2L);

            int arrowsAvailable = countArrowsInQuiver(player);
            if (arrowsAvailable < 1) {
                player.sendMessage(ChatColor.RED + "[Archer] You need at least 1 arrow in your quiver!");
                AbilitySoundUtil.playNoChargeSound(player);
                event.setCancelled(true);
                return;
            }
            if (!canUseExplosive(player)) {
                int left = getExplosiveCooldownSecondsLeft(player);
                player.sendMessage(ChatColor.RED + "[Archer] WildShot is on cooldown! (" + left + "s)");
                AbilitySoundUtil.playCooldownSound(player);
                event.setCancelled(true);
                return;
            }
            if (data.getCharge() < EXPLOSIVE_COST) {
                player.sendMessage(ChatColor.RED + "[Archer] Not enough charge! Need " + EXPLOSIVE_COST + ".");
                AbilitySoundUtil.playNoChargeSound(player);
                event.setCancelled(true);
                return;
            }
            ItemStack arrowItem = takeOneArrowFromQuiver(player, true);
            if (arrowItem == null) {
                player.sendMessage(ChatColor.RED + "[Archer] You have no arrows in your quiver!");
                AbilitySoundUtil.playNoChargeSound(player);
                event.setCancelled(true);
                return;
            }
            data.setCharge(data.getCharge() - EXPLOSIVE_COST);
            startExplosiveCooldown(player);

            Location loc = player.getEyeLocation();
            Vector dir = loc.getDirection().normalize();
            Arrow arrow = loc.getWorld().spawnArrow(loc, dir, 2.7f, 0.0f);
            arrow.setShooter(player);
            arrow.setCritical(true);
            arrow.setMetadata("archer_explosive_arrow", new FixedMetadataValue(plugin, true));
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            tempAbilityArrows.add(arrow.getUniqueId());
            scheduleArrowCleanup(arrow, 10 * 20);

            // Particle trail (End theme: DRAGON_BREATH, PORTAL, PURPLE REDSTONE, SPELL_WITCH)
            BukkitRunnable particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (arrow.isDead() || arrow.isOnGround() || !arrow.isValid()) {
                        this.cancel();
                        return;
                    }
                    Location aloc = arrow.getLocation();
                    arrow.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, aloc, 3, 0.13, 0.13, 0.13, 0.03);
                }
            };
            particleTask.runTaskTimer(plugin, 0L, 1L);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.2f, 0.7f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.85f);
            player.sendMessage(ChatColor.DARK_GREEN + "[Archer] You unleashed a furious shot!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosiveArrowImpact(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata("archer_explosive_arrow")) return;
        Location loc = arrow.getLocation();
        World world = loc.getWorld();

        double radius = 4.5;
        world.spawnParticle(Particle.CHERRY_LEAVES, loc, 75, 1.2, 1.2, 1.2, 0.10);
        world.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc, 30, 0.7, 1.2, 0.7, 0.13);
        world.spawnParticle(Particle.ASH, loc, 40, 0.7, 1.2, 0.7, new Particle.DustOptions(Color.fromRGB(136, 54, 255), 2.0f));
        world.spawnParticle(Particle.EXPLOSION, loc, 2, 0.3,  0.3, 0.02);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 2f, 0.7f);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
        for (Entity ent : world.getNearbyEntities(loc, radius, radius, radius)) {
            if (ent instanceof LivingEntity le && arrow.getShooter() instanceof Player shooter) {
                double dmg = le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.25;
                le.damage(dmg, shooter);
            }
        }
    }

    public static boolean canUseExplosive(Player player) {
        ArcherAbilityManager inst = getInstance();
        return !inst.explosiveCooldown.containsKey(player.getUniqueId()) ||
                (System.currentTimeMillis() - inst.explosiveCooldown.get(player.getUniqueId())) / 1000 > EXPLOSIVE_COOLDOWN_SECONDS;
    }
    public static void startExplosiveCooldown(Player player) {
        getInstance().explosiveCooldown.put(player.getUniqueId(), System.currentTimeMillis());
    }
    public static int getExplosiveCooldownSecondsLeft(Player player) {
        ArcherAbilityManager inst = getInstance();
        if (!inst.explosiveCooldown.containsKey(player.getUniqueId())) return 0;
        long elapsed = (System.currentTimeMillis() - inst.explosiveCooldown.get(player.getUniqueId())) / 1000;
        int left = EXPLOSIVE_COOLDOWN_SECONDS - (int) elapsed;
        return Math.max(left, 0);
    }
}