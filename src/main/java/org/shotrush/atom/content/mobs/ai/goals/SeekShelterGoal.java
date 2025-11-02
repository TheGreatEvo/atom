package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.environment.EnvironmentalContext;

import java.util.EnumSet;

public class SeekShelterGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private Location shelterLocation;
    private int repathTimer;
    private static final int REPATH_INTERVAL = 40;
    private static final int SEARCH_RADIUS = 16;
    
    public SeekShelterGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "seek_shelter"));
        this.repathTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!mob.isValid() || mob.isDead()) return false;
        
        Location loc = mob.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        
        return EnvironmentalContext.shouldSeekShelter(loc);
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!mob.isValid() || mob.isDead()) return false;
        
        Location loc = mob.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        
        if (!loc.getWorld().hasStorm()) {
            return false;
        }
        
        return loc.getBlock().getLightFromSky() >= 10;
    }
    
    @Override
    public void start() {
        shelterLocation = null;
        repathTimer = 0;
        findShelter();
    }
    
    @Override
    public void stop() {
        shelterLocation = null;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        repathTimer++;
        
        if (shelterLocation == null || repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            findShelter();
        }
        
        if (shelterLocation != null && shelterLocation.getWorld() != null) {
            mob.getPathfinder().moveTo(shelterLocation, 1.3);
        }
    }
    
    private void findShelter() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) {
            shelterLocation = null;
            return;
        }
        
        Location bestShelter = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                for (int y = -4; y <= 4; y++) {
                    Location checkLoc = mobLoc.clone().add(x, y, z);
                    Block block = checkLoc.getBlock();
                    
                    if (block.getLightFromSky() < 10 && block.isEmpty()) {
                        double distance = mobLoc.distanceSquared(checkLoc);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            bestShelter = checkLoc;
                        }
                    }
                }
            }
        }
        
        shelterLocation = bestShelter;
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
