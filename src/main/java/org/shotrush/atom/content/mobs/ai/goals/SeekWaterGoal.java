package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.needs.NeedsManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SeekWaterGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final NeedsManager needsManager;
    private Location waterTarget;
    private int drinkingTicks;
    private long lastDrinkTime;
    private static final int SEARCH_RADIUS = 24;
    private static final int DRINKING_DURATION = 40;
    private static final long DRINKING_COOLDOWN = 8000;
    
    public SeekWaterGoal(Mob mob, Plugin plugin, NeedsManager needsManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.needsManager = needsManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "seek_water"));
        this.drinkingTicks = 0;
        this.lastDrinkTime = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (System.currentTimeMillis() - lastDrinkTime < DRINKING_COOLDOWN) {
            return false;
        }
        
        var needs = needsManager.getNeeds(animal);
        if (!needs.isThirsty()) return false;
        
        waterTarget = findNearestWater();
        return waterTarget != null;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (drinkingTicks > 0) return true;
        
        return needsManager.getNeeds(animal).isThirsty() && waterTarget != null;
    }
    
    @Override
    public void start() {
        drinkingTicks = 0;
    }
    
    @Override
    public void stop() {
        waterTarget = null;
        drinkingTicks = 0;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        if (!(mob instanceof Animals animal)) return;
        
        if (waterTarget == null) {
            waterTarget = findNearestWater();
            if (waterTarget == null) {
                stop();
                return;
            }
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null) return;
        
        if (mobLoc.distanceSquared(waterTarget) > 4.0) {
            mob.getPathfinder().moveTo(waterTarget, 1.1);
            return;
        }
        
        if (drinkingTicks == 0) {
            drinkingTicks = 1;
            if (mobLoc.getWorld() != null) {
                mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
            }
        }
        
        if (drinkingTicks > 0) {
            mob.getPathfinder().stopPathfinding();
            drinkingTicks++;
            
            if (drinkingTicks % 8 == 0) {
                Location effectLoc = mobLoc.clone().add(
                    mobLoc.getDirection().multiply(0.5).setY(mob.getHeight() / 3)
                );
                if (mobLoc.getWorld() != null) {
                    mobLoc.getWorld().spawnParticle(Particle.SPLASH, effectLoc, 
                        8, 0.2, 0.1, 0.2, 0.1);
                    mobLoc.getWorld().spawnParticle(Particle.DRIPPING_WATER, effectLoc, 
                        3, 0.1, 0.1, 0.1, 0.0);
                }
            }
            
            if (drinkingTicks >= DRINKING_DURATION) {
                var needs = needsManager.getNeeds(animal);
                needs.drink(40);
                lastDrinkTime = System.currentTimeMillis();
                stop();
            }
        }
    }
    
    private Location findNearestWater() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return null;
        
        List<Location> waterBlocks = new ArrayList<>();
        
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = -3; y <= 3; y++) {
                    Block block = mobLoc.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.WATER) {
                        Block above = block.getRelative(0, 1, 0);
                        if (above.isEmpty() || above.isPassable()) {
                            waterBlocks.add(above.getLocation());
                        }
                    }
                }
            }
        }
        
        if (waterBlocks.isEmpty()) return null;
        
        return waterBlocks.stream()
            .min((a, b) -> Double.compare(
                mobLoc.distanceSquared(a), 
                mobLoc.distanceSquared(b)))
            .orElse(null);
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
