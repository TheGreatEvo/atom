package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.needs.NeedsManager;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class ScavengeGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private final NeedsManager needsManager;
    private Item foodTarget;
    private int eatingTicks;
    private long lastScavengeTime;
    private static final int SEARCH_RADIUS = 16;
    private static final int EATING_DURATION = 30;
    private static final long SCAVENGE_COOLDOWN = 5000;
    
    private static final Map<Material, Integer> FOOD_VALUES = new HashMap<>();
    
    static {
        FOOD_VALUES.put(Material.WHEAT, 15);
        FOOD_VALUES.put(Material.CARROT, 20);
        FOOD_VALUES.put(Material.POTATO, 20);
        FOOD_VALUES.put(Material.BEETROOT, 18);
        FOOD_VALUES.put(Material.APPLE, 25);
        FOOD_VALUES.put(Material.MELON_SLICE, 10);
        FOOD_VALUES.put(Material.SWEET_BERRIES, 12);
        FOOD_VALUES.put(Material.GLOW_BERRIES, 12);
        FOOD_VALUES.put(Material.BEEF, 35);
        FOOD_VALUES.put(Material.PORKCHOP, 35);
        FOOD_VALUES.put(Material.MUTTON, 35);
        FOOD_VALUES.put(Material.CHICKEN, 30);
        FOOD_VALUES.put(Material.RABBIT, 30);
        FOOD_VALUES.put(Material.COD, 25);
        FOOD_VALUES.put(Material.SALMON, 25);
        FOOD_VALUES.put(Material.COOKED_BEEF, 45);
        FOOD_VALUES.put(Material.COOKED_PORKCHOP, 45);
        FOOD_VALUES.put(Material.COOKED_MUTTON, 45);
        FOOD_VALUES.put(Material.COOKED_CHICKEN, 40);
        FOOD_VALUES.put(Material.COOKED_RABBIT, 40);
        FOOD_VALUES.put(Material.BREAD, 30);
    }
    
    public ScavengeGoal(Mob mob, Plugin plugin, NeedsManager needsManager) {
        this.mob = mob;
        this.plugin = plugin;
        this.needsManager = needsManager;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "scavenge"));
        this.eatingTicks = 0;
        this.lastScavengeTime = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (System.currentTimeMillis() - lastScavengeTime < SCAVENGE_COOLDOWN) {
            return false;
        }
        
        var needs = needsManager.getNeeds(animal);
        if (!needs.isHungry()) return false;
        
        foodTarget = findNearestFood();
        return foodTarget != null;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        if (eatingTicks > 0) return true;
        
        if (foodTarget == null || !foodTarget.isValid()) {
            return false;
        }
        
        return needsManager.getNeeds(animal).isHungry();
    }
    
    @Override
    public void start() {
        eatingTicks = 0;
    }
    
    @Override
    public void stop() {
        foodTarget = null;
        eatingTicks = 0;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        if (!(mob instanceof Animals animal)) return;
        
        if (foodTarget == null || !foodTarget.isValid()) {
            foodTarget = findNearestFood();
            if (foodTarget == null) {
                stop();
                return;
            }
        }
        
        Location mobLoc = mob.getLocation();
        Location foodLoc = foodTarget.getLocation();
        if (mobLoc == null || foodLoc == null) return;
        
        if (mobLoc.distanceSquared(foodLoc) > 1.5) {
            mob.getPathfinder().moveTo(foodLoc, 1.2);
            return;
        }
        
        if (eatingTicks == 0) {
            ItemStack itemStack = foodTarget.getItemStack();
            Material foodType = itemStack.getType();
            int foodValue = FOOD_VALUES.getOrDefault(foodType, 10);
            
            foodTarget.remove();
            eatingTicks = 1;
            
            if (mobLoc.getWorld() != null) {
                mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
            }
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (mob.isValid() && !mob.isDead()) {
                    var needs = needsManager.getNeeds(animal);
                    needs.eat(foodValue);
                }
            }, EATING_DURATION);
        }
        
        if (eatingTicks > 0) {
            mob.getPathfinder().stopPathfinding();
            eatingTicks++;
            
            if (eatingTicks % 6 == 0) {
                Location effectLoc = mobLoc.clone().add(0, mob.getHeight() / 2, 0);
                if (mobLoc.getWorld() != null) {
                    mobLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, effectLoc, 
                        2, 0.3, 0.3, 0.3, 0.0);
                }
            }
            
            if (eatingTicks >= EATING_DURATION) {
                lastScavengeTime = System.currentTimeMillis();
                stop();
            }
        }
    }
    
    private Item findNearestFood() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) return null;
        
        return mobLoc.getWorld().getNearbyEntities(mobLoc, 
            SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
            .stream()
            .filter(entity -> entity instanceof Item)
            .map(entity -> (Item) entity)
            .filter(item -> FOOD_VALUES.containsKey(item.getItemStack().getType()))
            .filter(item -> item.isValid() && item.getPickupDelay() == 0)
            .min((a, b) -> Double.compare(
                mobLoc.distanceSquared(a.getLocation()),
                mobLoc.distanceSquared(b.getLocation())
            ))
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
