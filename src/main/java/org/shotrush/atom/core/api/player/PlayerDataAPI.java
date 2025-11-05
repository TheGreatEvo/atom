package org.shotrush.atom.core.api.player;

import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;

import java.util.UUID;


public class PlayerDataAPI {
    
    
    public static int getInt(Player player, String key, int defaultValue) {
        return Atom.getInstance().getDataStorage()
            .getPlayerData(player.getUniqueId())
            .getInt(key, defaultValue);
    }
    
    public static void setInt(Player player, String key, int value) {
        UUID playerId = player.getUniqueId();
        var config = Atom.getInstance().getDataStorage().getPlayerData(playerId);
        config.set(key, value);
        Atom.getInstance().getDataStorage().savePlayerData(playerId, config);
    }
    
    public static void incrementInt(Player player, String key, int defaultValue) {
        setInt(player, key, getInt(player, key, defaultValue) + 1);
    }
    
    
    public static double getDouble(Player player, String key, double defaultValue) {
        return Atom.getInstance().getDataStorage()
            .getPlayerData(player.getUniqueId())
            .getDouble(key, defaultValue);
    }
    
    public static void setDouble(Player player, String key, double value) {
        UUID playerId = player.getUniqueId();
        var config = Atom.getInstance().getDataStorage().getPlayerData(playerId);
        config.set(key, value);
        Atom.getInstance().getDataStorage().savePlayerData(playerId, config);
    }
    
    
    public static boolean getBoolean(Player player, String key, boolean defaultValue) {
        return Atom.getInstance().getDataStorage()
            .getPlayerData(player.getUniqueId())
            .getBoolean(key, defaultValue);
    }
    
    public static void setBoolean(Player player, String key, boolean value) {
        UUID playerId = player.getUniqueId();
        var config = Atom.getInstance().getDataStorage().getPlayerData(playerId);
        config.set(key, value);
        Atom.getInstance().getDataStorage().savePlayerData(playerId, config);
    }
    
    
    public static String getString(Player player, String key, String defaultValue) {
        return Atom.getInstance().getDataStorage()
            .getPlayerData(player.getUniqueId())
            .getString(key, defaultValue);
    }
    
    public static void setString(Player player, String key, String value) {
        UUID playerId = player.getUniqueId();
        var config = Atom.getInstance().getDataStorage().getPlayerData(playerId);
        config.set(key, value);
        Atom.getInstance().getDataStorage().savePlayerData(playerId, config);
    }
    
    
    public static class BatchUpdate {
        private final Player player;
        private final org.bukkit.configuration.file.YamlConfiguration config;
        
        private BatchUpdate(Player player) {
            this.player = player;
            this.config = Atom.getInstance().getDataStorage().getPlayerData(player.getUniqueId());
        }
        
        public BatchUpdate set(String key, Object value) {
            config.set(key, value);
            return this;
        }
        
        public BatchUpdate setInt(String key, int value) {
            config.set(key, value);
            return this;
        }
        
        public BatchUpdate setDouble(String key, double value) {
            config.set(key, value);
            return this;
        }
        
        public BatchUpdate setBoolean(String key, boolean value) {
            config.set(key, value);
            return this;
        }
        
        public BatchUpdate setString(String key, String value) {
            config.set(key, value);
            return this;
        }
        
        public BatchUpdate increment(String key, int defaultValue) {
            config.set(key, config.getInt(key, defaultValue) + 1);
            return this;
        }
        
        public void save() {
            Atom.getInstance().getDataStorage().savePlayerData(player.getUniqueId(), config);
        }
    }
    
    public static BatchUpdate batch(Player player) {
        return new BatchUpdate(player);
    }
}
