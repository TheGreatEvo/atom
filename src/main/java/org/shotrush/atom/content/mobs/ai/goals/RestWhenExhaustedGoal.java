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
import org.shotrush.atom.content.mobs.ai.needs.NeedsManager;

import java.util.EnumSet;

public class RestWhenExhaustedGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final NeedsManager needsManager;
    private int restTicks;
    private static final double RAPID_ENERGY_REGEN = 0.5;
    
    public RestWhenExhaustedGoal(Mob mob, Plugin plugin, NeedsManager needsManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.needsManager = needsManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "rest_exhausted"));
        this.restTicks = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        var needs = needsManager.getNeeds(animal);
        return needs.isExhausted();
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        var needs = needsManager.getNeeds(animal);
        return needs.getEnergyPercent() < 0.3;
    }
    
    @Override
    public void start() {
        restTicks = 0;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void stop() {
        restTicks = 0;
    }
    
    @Override
    public void tick() {
        if (!(mob instanceof Animals animal)) return;
        
        restTicks++;
        mob.getPathfinder().stopPathfinding();
        
        var needs = needsManager.getNeeds(animal);
        needs.sleep(RAPID_ENERGY_REGEN);
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            if (restTicks % 20 == 0) {
                mobLoc.getWorld().spawnParticle(Particle.COMPOSTER, 
                    mobLoc.clone().add(0, mob.getHeight() / 2, 0), 
                    5, 0.3, 0.3, 0.3, 0.01);
            }
            
            if (restTicks % 10 == 0) {
                mobLoc.getWorld().spawnParticle(Particle.SOUL, 
                    mobLoc.clone().add(0, 0.2, 0), 
                    2, 0.2, 0.1, 0.2, 0.02);
            }
        }
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.JUMP);
    }
}
