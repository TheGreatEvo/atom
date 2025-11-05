package org.shotrush.atom.core.api.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import static org.bukkit.block.Biome.*;

public class EnvironmentalFactorAPI {
    
    public static double getBiomeTemperature(Biome biome) {
        if (biome == SNOWY_PLAINS || biome == SNOWY_TAIGA || biome == SNOWY_BEACH || 
            biome == SNOWY_SLOPES || biome == FROZEN_PEAKS || biome == FROZEN_RIVER || 
            biome == FROZEN_OCEAN || biome == DEEP_FROZEN_OCEAN || biome == ICE_SPIKES) {
            return -8.0;
        }
        
        if (biome == COLD_OCEAN || biome == DEEP_COLD_OCEAN || biome == TAIGA || 
            biome == OLD_GROWTH_PINE_TAIGA || biome == OLD_GROWTH_SPRUCE_TAIGA) {
            return -5.0;
        }
        
        if (biome == DESERT || biome == BADLANDS || biome == ERODED_BADLANDS || 
            biome == WOODED_BADLANDS || biome == SAVANNA || biome == SAVANNA_PLATEAU || 
            biome == WINDSWEPT_SAVANNA) {
            return 12.0;
        }
        
        if (biome == NETHER_WASTES || biome == CRIMSON_FOREST || biome == WARPED_FOREST || 
            biome == SOUL_SAND_VALLEY || biome == BASALT_DELTAS) {
            return 50.0;
        }
        
        return 0.0;
    }
    
    public static double getDayNightModifier(World world) {
        long time = world.getTime();
        boolean isNight = time >= 13000 && time <= 23000;
        return isNight ? -2.0 : 1.0;
    }
    
    public static double getLightLevelModifier(Location location) {
        int lightLevel = location.getBlock().getLightLevel();
        return (lightLevel - 7) * 0.02;
    }
    
    public static double getWaterIceModifier(Player player, Location location) {
        double modifier = 0.0;
        
        if (player.isInWater()) {
            Biome biome = location.getBlock().getBiome();
            
            if (biome == SNOWY_PLAINS || biome == SNOWY_TAIGA || biome == SNOWY_BEACH || 
                biome == SNOWY_SLOPES || biome == FROZEN_PEAKS || biome == FROZEN_RIVER || 
                biome == FROZEN_OCEAN || biome == DEEP_FROZEN_OCEAN || biome == ICE_SPIKES) {
                modifier -= 5.5;
            }
            else if (biome == COLD_OCEAN || biome == DEEP_COLD_OCEAN || biome == TAIGA || 
                     biome == OLD_GROWTH_PINE_TAIGA || biome == OLD_GROWTH_SPRUCE_TAIGA) {
                modifier -= 3.3;
            }
            else if (biome == DESERT || biome == BADLANDS || biome == ERODED_BADLANDS || 
                     biome == WOODED_BADLANDS || biome == SAVANNA || biome == SAVANNA_PLATEAU || 
                     biome == WINDSWEPT_SAVANNA) {
                modifier -= 0.55;
            }
            else if (biome == NETHER_WASTES || biome == CRIMSON_FOREST || biome == WARPED_FOREST || 
                     biome == SOUL_SAND_VALLEY || biome == BASALT_DELTAS) {
                modifier += 10.0;
            }
            else {
                modifier -= 3.0;
            }
        }
        
        Block blockBelow = location.clone().add(0, -1, 0).getBlock();
        if (blockBelow.getType() == Material.ICE || blockBelow.getType() == Material.PACKED_ICE || 
            blockBelow.getType() == Material.BLUE_ICE) {
            modifier -= 0.85;
        } else if (blockBelow.getType() == Material.SNOW || blockBelow.getType() == Material.SNOW_BLOCK) {
            modifier -= 0.55;
        }
        
        return modifier;
    }
    
    public static double getNearbyHeatSources(Location location, int radius) {
        double heatChange = 0.0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = location.clone().add(x, y, z).getBlock();
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    
                    if (distance == 0) continue;
                    
                    double influence = 1.0 / distance;
                    Material type = block.getType();
                    
                    if (type == Material.FIRE || type == Material.SOUL_FIRE) {
                        heatChange += 5.5 * influence;
                    } else if (type == Material.LAVA) {
                        heatChange += 11.0 * influence;
                    } else if (type == Material.MAGMA_BLOCK) {
                        heatChange += 3.3 * influence;
                    } else if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
                        org.bukkit.block.data.Lightable campfire = (org.bukkit.block.data.Lightable) block.getBlockData();
                        if (campfire.isLit()) {
                            heatChange += 4.4 * influence;
                        }
                    }
                    
                    else if (type == Material.WATER || type == Material.BUBBLE_COLUMN) {
                        heatChange -= 0.33 * influence;
                    } else if (type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE) {
                        heatChange -= 0.85 * influence;
                    } else if (type == Material.SNOW || type == Material.SNOW_BLOCK || type == Material.POWDER_SNOW) {
                        heatChange -= 0.55 * influence;
                    }
                }
            }
        }
        
        return heatChange;
    }
    
    public static double calculateEnvironmentalTemperatureChange(Player player, Location location, double multiplier) {
        double tempChange = 0.0;
        
        Biome biome = location.getBlock().getBiome();
        tempChange += getBiomeTemperature(biome);
        tempChange += getDayNightModifier(location.getWorld());
        tempChange += getLightLevelModifier(location);
        tempChange += getWaterIceModifier(player, location);
        tempChange += getNearbyHeatSources(location, 6);
        
        return tempChange * multiplier;
    }
}
