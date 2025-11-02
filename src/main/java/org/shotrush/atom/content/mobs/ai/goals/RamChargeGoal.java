package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class RamChargeGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private LivingEntity target;
    private Location chargeStart;
    private boolean isCharging;
    private int chargeCooldown;
    private static final int COOLDOWN_TICKS = 100;
    private static final double CHARGE_DISTANCE = 5.0;
    private static final double CHARGE_SPEED = 2.0;
    private static final double DAMAGE = 8.0;
    private static final double KNOCKBACK = 2.5;
    
    public RamChargeGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "ram_charge"));
        this.isCharging = false;
        this.chargeCooldown = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (chargeCooldown > 0) {
            chargeCooldown--;
            return false;
        }
        
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget == null || !currentTarget.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        Location targetLoc = currentTarget.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        
        if (distance >= CHARGE_DISTANCE && distance <= 15.0) {
            target = currentTarget;
            chargeStart = mobLoc.clone();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!isCharging) return false;
        if (target == null || !target.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null) return false;
        
        if (chargeStart == null) return false;
        
        double distanceTraveled = mobLoc.distance(chargeStart);
        return distanceTraveled < 10.0;
    }
    
    @Override
    public void start() {
        isCharging = true;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc != null && mobLoc.getWorld() != null) {
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 1.5f);
            mobLoc.getWorld().spawnParticle(Particle.CLOUD, mobLoc, 10, 0.3, 0.3, 0.3, 0.05);
        }
    }
    
    @Override
    public void stop() {
        isCharging = false;
        target = null;
        chargeStart = null;
        chargeCooldown = COOLDOWN_TICKS;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        if (target == null || !target.isValid()) {
            stop();
            return;
        }
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) {
            stop();
            return;
        }
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) {
            stop();
            return;
        }
        
        double distance = mobLoc.distance(targetLoc);
        
        if (distance < 2.0) {
            performRamHit();
            stop();
            return;
        }
        
        mob.getPathfinder().moveTo(targetLoc, CHARGE_SPEED);
        
        if (Math.random() < 0.1) {
            mobLoc.getWorld().spawnParticle(Particle.CLOUD, mobLoc, 3, 0.2, 0.2, 0.2, 0.02);
        }
    }
    
    private void performRamHit() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        target.damage(DAMAGE, mob);
        
        Vector knockbackDirection = target.getLocation().toVector().subtract(mobLoc.toVector()).normalize();
        knockbackDirection.setY(0.5);
        target.setVelocity(knockbackDirection.multiply(KNOCKBACK));
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
        mobLoc.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
        mobLoc.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
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
