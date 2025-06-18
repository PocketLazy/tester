package com.pocketlazy.classify.util;

import com.pocketlazy.classify.ClassifyPlugin;
import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChargeSystem {
    // Counter to track ticks for charge and spirit regen
    private static int tickCounter = 0;

    public static void startRegenTask() {
        Bukkit.getScheduler().runTaskTimer(ClassifyPlugin.getInstance(), () -> {
            tickCounter++;

            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = PlayerClassManager.getInstance().get(player);

                if (data.isGhost()) {
                    // Ghosts regen spirit charge: +5 every 100 ticks (5 seconds)
                    if (tickCounter % 100 == 0) {
                        int newSpirit = Math.min(data.getSpiritCharge() + 5, PlayerData.getMaxSpiritCharge());
                        data.setSpiritCharge(newSpirit);
                    }
                    continue;
                }

                // Living players: charge regen +5 every 10 ticks (0.5 second)
                if (tickCounter % 10 == 0) {
                    int regen = 5;
                    int newCharge = Math.min(data.getCharge() + regen, data.getChargeCap());
                    data.setCharge(newCharge);
                    // Action bar removed!
                }
            }

            // Reset the counter every 100 ticks to avoid int overflow
            if (tickCounter >= 100) tickCounter = 0;
        }, 0L, 1L); // run every tick for accurate timing
    }
}