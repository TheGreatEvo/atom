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
import org.shotrush.atom.content.mobs.ai.debug.DebugCategory;
import org.shotrush.atom.content.mobs.ai.debug.DebugManager;
import org.shotrush.atom.content.mobs.ai.needs.NeedsManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class GrazingGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final NeedsManager needsManager;
    private Location grassTarget;
    private int eatingTicks;
    private long lastGrazingTime;
    private static final int SEARCH_RADIUS = 16;
    private static final int EATING_DURATION = 60;
    private static final long GRAZING_COOLDOWN = 10000;
    
    public GrazingGoal(Mob mob, Plugin plugin, NeedsManager needsManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.needsManager = needsManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "grazing"));
        this.eatingTicks = 0;
        this.lastGrazingTime = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (System.currentTimeMillis() - lastGrazingTime < GRAZING_COOLDOWN) {
            return false;
        }
        
        var needs = needsManager.getNeeds(animal);
        if (!needs.isHungry()) return false;
        
        grassTarget = findNearestGrass();
        return grassTarget != null;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (eatingTicks > 0) return true;
        
        return needsManager.getNeeds(animal).isHungry() && grassTarget != null;
    }
    
    @Override
    public void start() {
        eatingTicks = 0;
        DebugManager.logGoalActivation(mob, "GrazingGoal", DebugCategory.GOALS);
        if (mob instanceof Animals animal) {
            var needs = needsManager.getNeeds(animal);
            DebugManager.log(String.format("%s#%d started grazing (hunger: %.1f%%)", 
                mob.getType().name(), mob.getEntityId(), needs.getHunger()),
                DebugCategory.NEEDS);
        }
    }
    
    @Override
    public void stop() {
        grassTarget = null;
        eatingTicks = 0;
        mob.getPathfinder().stopPathfinding();
        DebugManager.logGoalDeactivation(mob, "GrazingGoal", DebugCategory.GOALS);
    }
    
    @Override
    public void tick() {
        if (!(mob instanceof Animals animal)) return;
        
        if (grassTarget == null) {
            grassTarget = findNearestGrass();
            if (grassTarget == null) {
                stop();
                return;
            }
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null) return;
        
        if (mobLoc.distanceSquared(grassTarget) > 4.0) {
            mob.getPathfinder().moveTo(grassTarget, 1.0);
            return;
        }
        
        if (eatingTicks == 0) {
            Block grassBlock = grassTarget.getBlock();
            if (grassBlock.getType() == Material.GRASS_BLOCK) {
                grassBlock.setType(Material.DIRT);
                eatingTicks = 1;
                
                if (mobLoc.getWorld() != null) {
                    mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                }
            }
        }
        
        if (eatingTicks > 0) {
            mob.getPathfinder().stopPathfinding();
            eatingTicks++;
            
            if (eatingTicks % 10 == 0) {
                Location effectLoc = mobLoc.clone().add(0, mob.getHeight() / 2, 0);
                if (mobLoc.getWorld() != null) {
                    mobLoc.getWorld().spawnParticle(Particle.ITEM, effectLoc, 
                        5, 0.2, 0.2, 0.2, 0.05, 
                        new org.bukkit.inventory.ItemStack(Material.SHORT_GRASS));
                }
            }
            
            if (eatingTicks >= EATING_DURATION) {
                var needs = needsManager.getNeeds(animal);
                needs.eat(30);
                lastGrazingTime = System.currentTimeMillis();
                DebugManager.log(String.format("%s#%d finished eating (hunger now: %.1f%%)", 
                    mob.getType().name(), mob.getEntityId(), needs.getHunger()),
                    DebugCategory.NEEDS);
                stop();
            }
        }
    }
    
    private Location findNearestGrass() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return null;
        
        List<Location> grassBlocks = new ArrayList<>();
        
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = -3; y <= 3; y++) {
                    Block block = mobLoc.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.GRASS_BLOCK) {
                        grassBlocks.add(block.getLocation());
                    }
                }
            }
        }
        
        if (grassBlocks.isEmpty()) return null;
        
        return grassBlocks.stream()
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
