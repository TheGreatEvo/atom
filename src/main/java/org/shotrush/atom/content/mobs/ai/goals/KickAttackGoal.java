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

public class KickAttackGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private int kickCooldown;
    private static final int COOLDOWN_TICKS = 60;
    private static final double KICK_RANGE = 3.0;
    private static final double DAMAGE = 6.0;
    private static final double KNOCKBACK = 1.8;
    
    public KickAttackGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "kick_attack"));
        this.kickCooldown = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (kickCooldown > 0) {
            kickCooldown--;
            return false;
        }
        
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return false;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return false;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return false;
        
        double distance = mobLoc.distance(targetLoc);
        
        Vector toTarget = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();
        Vector facingDirection = mobLoc.getDirection().normalize();
        double angle = facingDirection.angle(toTarget);
        
        return distance < KICK_RANGE && angle > Math.PI / 2;
    }
    
    @Override
    public boolean shouldStayActive() {
        return false;
    }
    
    @Override
    public void start() {
        performKick();
        kickCooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void tick() {
    }
    
    private void performKick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isValid()) return;
        
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return;
        
        Location targetLoc = target.getLocation();
        if (targetLoc == null) return;
        
        target.damage(DAMAGE, mob);
        
        Vector knockbackDirection = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();
        knockbackDirection.setY(0.4);
        target.setVelocity(knockbackDirection.multiply(KNOCKBACK));
        
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_HORSE_ANGRY, 1.0f, 1.0f);
        mobLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, mobLoc.clone().add(mobLoc.getDirection().multiply(-1)), 1);
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
