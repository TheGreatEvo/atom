package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.environment.EnvironmentalContext;

import java.util.EnumSet;

public class TimeBasedActivityGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final EnvironmentalContext.ActivityPattern activityPattern;
    private double originalSpeed;
    private boolean speedModified;
    
    public TimeBasedActivityGoal(Mob mob, Plugin plugin, EnvironmentalContext.ActivityPattern activityPattern) {
        this.mob = mob;
        this.plugin = plugin;
        this.activityPattern = activityPattern;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "time_based_activity"));
        this.speedModified = false;
    }
    
    @Override
    public boolean shouldActivate() {
        return mob.isValid() && !mob.isDead();
    }
    
    @Override
    public boolean shouldStayActive() {
        return mob.isValid() && !mob.isDead();
    }
    
    @Override
    public void start() {
        AttributeInstance speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            originalSpeed = speedAttr.getBaseValue();
            speedModified = false;
        }
    }
    
    @Override
    public void stop() {
        if (speedModified) {
            restoreOriginalSpeed();
        }
    }
    
    @Override
    public void tick() {
        if (mob.getWorld() == null) return;
        
        double activityModifier = EnvironmentalContext.getActivityModifier(mob.getWorld(), activityPattern);
        
        AttributeInstance speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double modifiedSpeed = originalSpeed * activityModifier;
            speedAttr.setBaseValue(modifiedSpeed);
            speedModified = true;
        }
    }
    
    private void restoreOriginalSpeed() {
        AttributeInstance speedAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(originalSpeed);
        }
        speedModified = false;
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
