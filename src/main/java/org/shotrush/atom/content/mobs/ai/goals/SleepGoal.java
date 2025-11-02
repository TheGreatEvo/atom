package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.environment.EnvironmentalContext;
import org.shotrush.atom.content.mobs.ai.needs.NeedsManager;

import java.util.EnumSet;

public class SleepGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final NeedsManager needsManager;
    private int sleepTicks;
    private static final int WAKE_CHECK_INTERVAL = 20;
    private static final double ENERGY_REGEN_PER_TICK = 0.1;
    
    public SleepGoal(Mob mob, Plugin plugin, NeedsManager needsManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.needsManager = needsManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "sleep"));
        this.sleepTicks = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        var needs = needsManager.getNeeds(animal);
        if (needs.isTired()) return true;
        
        if (EnvironmentalContext.isNighttime(mob.getWorld())) {
            return needs.getEnergyPercent() < 0.7;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (sleepTicks < 40 && mob.getHealth() < mob.getMaxHealth()) {
            return false;
        }
        
        var needs = needsManager.getNeeds(animal);
        return needs.getEnergyPercent() < 0.9;
    }
    
    @Override
    public void start() {
        sleepTicks = 0;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void stop() {
        sleepTicks = 0;
    }
    
    @Override
    public void tick() {
        if (!(mob instanceof Animals animal)) return;
        
        sleepTicks++;
        mob.getPathfinder().stopPathfinding();
        
        var needs = needsManager.getNeeds(animal);
        needs.sleep(ENERGY_REGEN_PER_TICK);
        
        if (sleepTicks % 40 == 0) {
            Location loc = mob.getLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().spawnParticle(Particle.ENCHANT, 
                    loc.clone().add(0, mob.getHeight() / 2, 0), 
                    3, 0.3, 0.3, 0.3, 0.01);
            }
        }
        
        if (sleepTicks % WAKE_CHECK_INTERVAL == 0) {
            if (isLoudNoiseNearby()) {
                stop();
            }
        }
    }
    
    private boolean isLoudNoiseNearby() {
        Location loc = mob.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        
        return loc.getWorld().getNearbyEntities(loc, 8, 8, 8).stream()
            .anyMatch(entity -> entity.getVelocity().lengthSquared() > 1.0);
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
