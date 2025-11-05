package org.shotrush.atom.core.api.combat;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TemperatureEffectsAPI {
    
    public static void applyHeatDamage(Player player, double temperature, boolean hasProtection) {
        if (hasProtection) return;
        
        if (temperature >= 100) {
            player.setFireTicks(40);
        } else if (temperature >= 50) {
            player.setFireTicks(20);
        }
    }
    
    public static void applyColdDamage(Player player, double temperature, boolean hasProtection) {
        if (hasProtection) return;
        
        if (temperature <= -20) {
            player.damage(2.0);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
        } else if (temperature <= -10) {
            player.damage(1.0);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
        }
    }
    
    public static void applyBodyTemperatureEffects(Player player, double bodyTemp) {
        if (bodyTemp >= 41.0) {
            player.damage(0.5);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.NAUSEA, 100, 0, false, false
            ));
        } else if (bodyTemp >= 39.0) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
        }
        
        else if (bodyTemp <= 33.0) {
            player.damage(0.5);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 2, false, false
            ));
            player.setFreezeTicks(Math.min(player.getFreezeTicks() + 10, 140));
        } else if (bodyTemp <= 34.5) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
            player.setFreezeTicks(Math.min(player.getFreezeTicks() + 5, 140));
        } else if (bodyTemp <= 35.5) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
        }
    }
    
    public static String getTemperatureColor(double temperature, double normalTemp) {
        double deviation = Math.abs(temperature - normalTemp);
        
        if (deviation >= 5.0) return "§c";
        if (deviation >= 2.5) return "§6";
        if (deviation >= 1.5) return "§e";
        return "§a";
    }
    
    public static String getBodyTempColor(double temp) {
        if (temp >= 40.0 || temp <= 32.0) return "§c";
        if (temp >= 38.5 || temp <= 34.0) return "§6";
        if (temp >= 38.0 || temp <= 35.5) return "§e";
        return "§a";
    }
}
