package com.pocketlazy.classify.persist;

import com.pocketlazy.classify.player.PlayerClassManager;
import com.pocketlazy.classify.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataPersistence {
    private static File dataFile;
    private static YamlConfiguration config;

    public static void init(org.bukkit.plugin.Plugin plugin) {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    public static void saveAll() {
        for (PlayerData data : PlayerClassManager.getInstance().getAll()) {
            String key = data.getUuid().toString();
            config.set(key + ".class", data.getPlayerClass() != null ? data.getPlayerClass().getName() : null);
            config.set(key + ".level", data.getClassLevel());
            config.set(key + ".lives", data.getLives());
            config.set(key + ".charge", data.getCharge());
            config.set(key + ".chargeCap", data.getChargeCap());
            config.set(key + ".healthyGems", data.getHealthyGems());
            config.set(key + ".chargedGems", data.getChargedGems());
            config.set(key + ".ghost", data.isGhost());
            config.set(key + ".lastAbilityUse", data.getLastAbilityUse());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadAll() {
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData data = PlayerClassManager.getInstance().get(uuid);
                String className = config.getString(key + ".class");
                if (className != null) {
                    data.setPlayerClass(PlayerClassManager.classByName(className));
                }
                data.setClassLevel(config.getInt(key + ".level", 1));
                data.setLives(config.getInt(key + ".lives", 3));
                data.setCharge(config.getInt(key + ".charge", 100));
                data.setChargeCap(config.getInt(key + ".chargeCap", 100));
                data.setHealthyGems(config.getInt(key + ".healthyGems", 0));
                data.setChargedGems(config.getInt(key + ".chargedGems", 0));
                data.setGhost(config.getBoolean(key + ".ghost", false));
                data.setLastAbilityUse(config.getLong(key + ".lastAbilityUse", 0));
            } catch (Exception ignore) {}
        }
    }
}