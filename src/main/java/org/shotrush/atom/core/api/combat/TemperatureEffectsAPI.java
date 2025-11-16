package org.shotrush.atom.core.api.combat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TemperatureEffectsAPI {
    
    public static void applyHeatDamage(Player player, double temperature, boolean hasProtection) {
        if (hasProtection) return;
        
        if (temperature >= 100) {
            player.setFireTicks(40);
            spawnSweatParticles(player, 3);
        } else if (temperature >= 50) {
            player.setFireTicks(20);
            spawnSweatParticles(player, 1);
        }
    }
    
    public static void applyColdDamage(Player player, double temperature, boolean hasProtection) {
        if (hasProtection) return;
        
        if (temperature <= -20) {
            player.damage(2.0);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
            spawnColdParticles(player, 3);
        } else if (temperature <= -10) {
            player.damage(1.0);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
            spawnColdParticles(player, 1);
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
            spawnSweatParticles(player, 5);
        } else if (bodyTemp >= 39.0) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
            spawnSweatParticles(player, 2);
        }
        
        else if (bodyTemp >= 38.0) {
            spawnSweatParticles(player, 1);
        }
        
        else if (bodyTemp <= 33.0) {
            player.damage(0.5);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 2, false, false
            ));
            player.setFreezeTicks(Math.min(player.getFreezeTicks() + 10, 140));
            spawnColdParticles(player, 5);
        } else if (bodyTemp <= 34.5) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
            player.setFreezeTicks(Math.min(player.getFreezeTicks() + 5, 140));
            spawnColdParticles(player, 3);
        } else if (bodyTemp <= 35.5) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
            spawnColdParticles(player, 1);
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
    private static int[] hsv(float h, float s, float v) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, v);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new int[] { r, g, b };
    }
    public static String getBodyTempDisplay(double temp) {
        final double MIN_TEMP = 32.0;
        final double MAX_TEMP = 40.0;
        final double OPTIMAL_MIN = 36.0;
        final double OPTIMAL_MAX = 37.5;

        temp = Math.max(MIN_TEMP, Math.min(MAX_TEMP, temp));

        float hue; // 0..1 (0=red, 1/3=green, 2/3=blue)
        if (temp < OPTIMAL_MIN) {
            // Blue (2/3) -> Green (1/3)
            double t = (temp - MIN_TEMP) / (OPTIMAL_MIN - MIN_TEMP);
            hue = (float)(2.0/3.0 - t * (2.0/3.0 - 1.0/3.0));
        } else if (temp > OPTIMAL_MAX) {
            // Green (1/3) -> Red (0)
            double t = (temp - OPTIMAL_MAX) / (MAX_TEMP - OPTIMAL_MAX);
            hue = (float)(1.0/3.0 - t * (1.0/3.0 - 0.0));
        } else {
            hue = 1f/3f; // green
        }

        // Full saturation, high brightness to keep vivid (avoid brown)
        int[] rgb = hsv(hue, 1.0f, 1.0f);
        String hex = String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
        String text = String.format("%.1f°C", temp);
        return "<" + hex + ">" + text + "</" + hex + ">";
    }
    
    private static void spawnSweatParticles(Player player, int count) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        player.getWorld().spawnParticle(
            Particle.DRIPPING_WATER,
            loc,
            count,
            0.3, 0.3, 0.3,
            0.05
        );
    }
    
    private static void spawnColdParticles(Player player, int count) {
        
        Location loc = player.getLocation().add(0, 1.5, 0);
        
        
        org.bukkit.util.Vector direction = player.getEyeLocation().getDirection();
        
        
        loc.add(direction.multiply(0.4));
        
        
        player.getWorld().spawnParticle(
            Particle.CLOUD,
            loc,
            count,
            0.15, 0.08, 0.15,  
            0.01  
        );
    }
}
