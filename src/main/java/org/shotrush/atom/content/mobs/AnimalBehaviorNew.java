package org.shotrush.atom.content.mobs;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.mobs.ai.combat.FatigueSystem;
import org.shotrush.atom.content.mobs.ai.combat.InjurySystem;
import org.shotrush.atom.content.mobs.ai.combat.MoraleSystem;
import org.shotrush.atom.content.mobs.ai.config.SpeciesBehavior;
import org.shotrush.atom.content.mobs.ai.environment.EnvironmentalContext;
import org.shotrush.atom.content.mobs.ai.goals.*;
import org.bukkit.entity.EntityType;
import org.shotrush.atom.content.mobs.ai.lifecycle.LifeCycleManager;
import org.shotrush.atom.content.mobs.ai.memory.MemoryManager;
import org.shotrush.atom.content.mobs.ai.needs.NeedsManager;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;
import org.shotrush.atom.content.mobs.herd.HerdRole;
import org.shotrush.atom.content.mobs.ai.debug.DebugCategory;
import org.shotrush.atom.content.mobs.ai.debug.DebugLevel;
import org.shotrush.atom.content.mobs.ai.debug.DebugManager;

import java.util.*;

public class AnimalBehaviorNew implements Listener {
    
    private final Atom plugin;
    private final HerdManager herdManager;
    private final InjurySystem injurySystem;
    private final FatigueSystem fatigueSystem;
    private final MoraleSystem moraleSystem;
    private final NeedsManager needsManager;
    private final MemoryManager memoryManager;
    private final LifeCycleManager lifeCycleManager;
    private static final Set<EntityType> COMMON_ANIMALS = new HashSet<>();
    private final Set<UUID> trackedAnimals = new HashSet<>();
    
    static {
        COMMON_ANIMALS.add(EntityType.COW);
        COMMON_ANIMALS.add(EntityType.PIG);
        COMMON_ANIMALS.add(EntityType.SHEEP);
        COMMON_ANIMALS.add(EntityType.CHICKEN);
        COMMON_ANIMALS.add(EntityType.RABBIT);
        COMMON_ANIMALS.add(EntityType.HORSE);
        COMMON_ANIMALS.add(EntityType.DONKEY);
        COMMON_ANIMALS.add(EntityType.MULE);
        COMMON_ANIMALS.add(EntityType.LLAMA);
        COMMON_ANIMALS.add(EntityType.GOAT);
        COMMON_ANIMALS.add(EntityType.CAT);
        COMMON_ANIMALS.add(EntityType.WOLF);
        COMMON_ANIMALS.add(EntityType.FOX);
        COMMON_ANIMALS.add(EntityType.PANDA);
        COMMON_ANIMALS.add(EntityType.POLAR_BEAR);
        COMMON_ANIMALS.add(EntityType.OCELOT);
        COMMON_ANIMALS.add(EntityType.TURTLE);
        COMMON_ANIMALS.add(EntityType.STRIDER);
        COMMON_ANIMALS.add(EntityType.AXOLOTL);
        COMMON_ANIMALS.add(EntityType.FROG);
        COMMON_ANIMALS.add(EntityType.CAMEL);
        COMMON_ANIMALS.add(EntityType.SNIFFER);
        COMMON_ANIMALS.add(EntityType.ARMADILLO);
    }
    
    public AnimalBehaviorNew(Atom plugin) {
        this.plugin = plugin;
        this.herdManager = new HerdManager(plugin);
        this.injurySystem = new InjurySystem(plugin);
        this.fatigueSystem = new FatigueSystem(plugin);
        this.moraleSystem = new MoraleSystem(plugin, herdManager);
        this.needsManager = new NeedsManager(plugin);
        this.memoryManager = new MemoryManager(plugin);
        this.lifeCycleManager = new LifeCycleManager(plugin);
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onAnimalSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        
        if (!COMMON_ANIMALS.contains(entity.getType())) return;
        if (!(entity instanceof Animals animal)) return;
        if (!(entity instanceof Mob mob)) return;
        
        plugin.getLogger().info(">>> Animal spawn detected: " + animal.getType() + " (Reason: " + event.getSpawnReason() + ")");
        
        if (AnimalDomestication.isFullyDomesticated(animal)) {
            plugin.getLogger().info("Fully domesticated - skipping");
            return;
        }
        
        initializeAnimal(animal, mob);
    }
    
    private void enhanceAnimalStats(Animals animal, double domesticationFactor, SpeciesBehavior behavior) {
        double wildFactor = 1.0 - domesticationFactor;
        
        if (animal.getAttribute(Attribute.MAX_HEALTH) != null) {
            double currentMax = Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
            double healthMultiplier = 1.0 + (0.5 * wildFactor);
            Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(currentMax * healthMultiplier);
            animal.setHealth(Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue());
        }
        
        if (animal.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            Objects.requireNonNull(animal.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).setBaseValue(0.5 * wildFactor);
        }
        
        if (animal.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            double currentSpeed = Objects.requireNonNull(animal.getAttribute(Attribute.MOVEMENT_SPEED)).getBaseValue();
            double speedMultiplier = 1.0 + (0.35 * wildFactor);
            Objects.requireNonNull(animal.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(currentSpeed * speedMultiplier);
        }
    }
    
    private void registerGoals(Mob mob, SpeciesBehavior behavior, boolean isAggressive, HerdRole role) {
        com.destroystokyo.paper.entity.ai.MobGoals goalSelector = Bukkit.getMobGoals();
        com.destroystokyo.paper.entity.ai.MobGoals targetSelector = Bukkit.getMobGoals();
        
        goalSelector.addGoal(mob, 0, new RestWhenExhaustedGoal(mob, plugin, needsManager));
        goalSelector.addGoal(mob, 0, new DeathEffectsGoal(mob, plugin, herdManager, moraleSystem));
        goalSelector.addGoal(mob, 0, new HerdPanicGoal(mob, plugin, herdManager, behavior, moraleSystem));
        
        goalSelector.addGoal(mob, 1, new SeekShelterGoal(mob, plugin));
        goalSelector.addGoal(mob, 1, new MotherProtectionGoal(mob, plugin, lifeCycleManager.getFamilyRelationships()));
        goalSelector.addGoal(mob, 1, new AvoidPlayerWhenInjuredGoal(mob, plugin, behavior));
        
        if (isHerbivore(mob.getType())) {
            goalSelector.addGoal(mob, 2, new GrazingGoal(mob, plugin, needsManager));
        }
        goalSelector.addGoal(mob, 2, new SeekWaterGoal(mob, plugin, needsManager));
        
        if (isCarnivore(mob.getType())) {
            goalSelector.addGoal(mob, 2, new HuntPreyGoal(mob, plugin, needsManager, behavior));
            goalSelector.addGoal(mob, 2, new TrackWoundedPreyGoal(mob, plugin, injurySystem));
        }
        
        if (isPackHunter(mob.getType())) {
            goalSelector.addGoal(mob, 2, new FlankAndSurroundGoal(mob, plugin, herdManager));
        }
        
        if (isAggressive) {
            targetSelector.addGoal(mob, 2, new AcquireNearestPlayerTargetGoal(mob, plugin, behavior));
            goalSelector.addGoal(mob, 3, new ChaseAndMeleeAttackGoal(mob, plugin, behavior, fatigueSystem, injurySystem, moraleSystem));
        }
        
        if (isCarnivore(mob.getType())) {
            goalSelector.addGoal(mob, 3, new ScavengeGoal(mob, plugin, needsManager));
            goalSelector.addGoal(mob, 3, new StalkPreyGoal(mob, plugin));
        }
        
        goalSelector.addGoal(mob, 3, new ReunionGoal(mob, plugin, herdManager, behavior));
        goalSelector.addGoal(mob, 3, new SleepGoal(mob, plugin, needsManager));
        
        if (role == HerdRole.FOLLOWER) {
            goalSelector.addGoal(mob, 4, new StayNearHerdGoal(mob, plugin, herdManager, behavior));
        } else {
            goalSelector.addGoal(mob, 4, new SentryBehaviorGoal(mob, plugin, herdManager));
            goalSelector.addGoal(mob, 4, new TerritoryDefenseGoal(mob, plugin, herdManager));
            goalSelector.addGoal(mob, 6, new HerdLeaderWanderGoal(mob, plugin, herdManager));
        }
        
        goalSelector.addGoal(mob, 7, new ShareFoodGoal(mob, plugin, herdManager, lifeCycleManager.getFamilyRelationships(), needsManager));
        
        goalSelector.addGoal(mob, 8, new PlayBehaviorGoal(mob, plugin, lifeCycleManager, lifeCycleManager.getFamilyRelationships()));
        
        goalSelector.addGoal(mob, 10, new TimeBasedActivityGoal(mob, plugin, getActivityPattern(mob.getType())));
        
        registerSpecialGoals(mob, behavior, goalSelector, isAggressive);
        
        plugin.getLogger().info("Registered goals for " + mob.getType() + " (aggressive: " + isAggressive + ", role: " + role + ")");
    }
    
    private void registerSpecialGoals(Mob mob, SpeciesBehavior behavior, com.destroystokyo.paper.entity.ai.MobGoals goalSelector, boolean isAggressive) {
        switch (behavior.specialMechanic()) {
            case RAM_CHARGE:
                if (isAggressive && (mob.getType() == EntityType.SHEEP || mob.getType() == EntityType.GOAT)) {
                    goalSelector.addGoal(mob, 2, new RamChargeGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Ram Charge");
                }
                break;
                
            case KICK_ATTACK:
                if (isAggressive && (mob.getType() == EntityType.HORSE || mob.getType() == EntityType.DONKEY || mob.getType() == EntityType.MULE)) {
                    goalSelector.addGoal(mob, 2, new KickAttackGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Kick Attack");
                }
                break;
                
            case SPIT_ATTACK:
                if (isAggressive && mob.getType() == EntityType.LLAMA) {
                    goalSelector.addGoal(mob, 2, new SpitAttackGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Spit Attack");
                }
                break;
                
            case COUNTER_CHARGE:
                if (isAggressive && mob.getType() == EntityType.PIG) {
                    goalSelector.addGoal(mob, 2, new CounterChargeGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Counter Charge");
                }
                break;
                
            case PACK_HUNTING:
                if (isAggressive && mob.getType() == EntityType.WOLF) {
                    goalSelector.addGoal(mob, 2, new PackHuntingGoal(mob, plugin, herdManager));
                    plugin.getLogger().info("  + Added Pack Hunting");
                }
                break;
                
            case POUNCE_ATTACK:
                if (isAggressive && mob.getType() == EntityType.FOX) {
                    goalSelector.addGoal(mob, 2, new PounceAttackGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Pounce Attack");
                }
                break;
                
            case STAMPEDE:
                if (mob.getType() == EntityType.COW) {
                    goalSelector.addGoal(mob, 1, new StampedeGoal(mob, plugin, herdManager));
                    plugin.getLogger().info("  + Added Stampede");
                }
                break;
                
            case FLIGHT_BURST:
                if (mob.getType() == EntityType.CHICKEN) {
                    goalSelector.addGoal(mob, 1, new FlightBurstGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Flight Burst");
                }
                break;
                
            case CUB_PROTECTION:
                if (mob.getType() == EntityType.POLAR_BEAR) {
                    goalSelector.addGoal(mob, 1, new CubProtectionGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Cub Protection");
                }
                break;
                
            case ROLL_DEFENSE:
                if (mob.getType() == EntityType.ARMADILLO) {
                    goalSelector.addGoal(mob, 0, new RollDefenseGoal(mob, plugin));
                    plugin.getLogger().info("  + Added Roll Defense");
                }
                break;
                
            default:
                break;
        }
    }
    
    private void startStaminaRegeneration(Animals animal) {
        animal.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (animal.isDead() || !animal.isValid()) {
                trackedAnimals.remove(animal.getUniqueId());
                scheduledTask.cancel();
                return;
            }
            
            regenerateStamina(animal);
        }, null, 1L, 40L);
    }
    
    private void regenerateStamina(Animals animal) {
        if (!animal.hasMetadata("stamina")) return;
        
        boolean isFleeing = animal.hasMetadata("fleeing") && animal.getMetadata("fleeing").get(0).asBoolean();
        if (isFleeing) return;
        
        double stamina = animal.getMetadata("stamina").get(0).asDouble();
        double maxStamina = animal.getMetadata("maxStamina").get(0).asDouble();
        
        if (stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + 2.0);
            animal.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        }
    }
    
    @EventHandler
    public void onAnimalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Animals animal)) return;
        if (!COMMON_ANIMALS.contains(animal.getType())) return;
        if (!(animal instanceof Mob mob)) return;
        
        final Player attacker;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            } else {
                return;
            }
        } else {
            return;
        }
        
        injurySystem.applyInjuryEffects(mob);
        
        moraleSystem.checkMorale(mob);
        
        herdManager.getHerd(animal.getUniqueId()).ifPresent(herd -> {
            SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
            herdManager.broadcastPanic(herd, attacker.getLocation(), behavior.panicDurationMs());
        });
        
        double healthPercent = animal.getHealth() / Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue();
        SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
        
        if (healthPercent < behavior.panicHealthThreshold()) {
            animal.setMetadata("fleeing", new FixedMetadataValue(plugin, true));
        }
    }
    
    @EventHandler
    public void onAnimalDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals animal)) return;
        if (!COMMON_ANIMALS.contains(animal.getType())) return;
        
        UUID animalId = animal.getUniqueId();
        
        herdManager.leaveHerd(animalId);
        needsManager.removeNeeds(animalId);
        lifeCycleManager.removeAnimal(animalId);
        trackedAnimals.remove(animalId);
        
        plugin.getLogger().info(">>> Animal died: " + animal.getType() + " - cleaned up all systems");
    }
    
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!COMMON_ANIMALS.contains(entity.getType())) continue;
            if (!(entity instanceof Animals animal)) continue;
            if (!(entity instanceof Mob mob)) continue;
            
            if (trackedAnimals.contains(animal.getUniqueId())) {
                continue;
            }
            
            plugin.getLogger().info(">>> Entity loaded from chunk: " + animal.getType() + " (UUID: " + animal.getUniqueId() + ")");
            
            if (AnimalDomestication.isFullyDomesticated(animal)) {
                plugin.getLogger().info("Fully domesticated - skipping");
                continue;
            }
            
            initializeAnimal(animal, mob);
        }
    }
    
    private void initializeAnimal(Animals animal, Mob mob) {
        double domesticationFactor = AnimalDomestication.getDomesticationFactor(animal);
        SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
        
        DebugManager.log(String.format("Initializing %s#%d (domestication: %.1f%%)", 
            animal.getType().name(), animal.getEntityId(), domesticationFactor * 100),
            DebugCategory.GOALS, DebugLevel.MINIMAL);
        
        enhanceAnimalStats(animal, domesticationFactor, behavior);
        
        Herd herd = herdManager.getOrCreateHerd(animal);
        HerdRole role = herdManager.getRole(animal.getUniqueId());
        
        boolean isAggressive;
        if (animal.hasMetadata("aggressive")) {
            isAggressive = animal.getMetadata("aggressive").get(0).asBoolean();
        } else {
            isAggressive = herdManager.getPersistence().isAggressive(animal);
            if (!isAggressive) {
                double aggressionChance = behavior.getAggressionChance(domesticationFactor);
                isAggressive = Math.random() < aggressionChance;
            }
            animal.setMetadata("aggressive", new FixedMetadataValue(plugin, isAggressive));
        }
        
        plugin.getLogger().info(">>> Initializing: Aggressive=" + isAggressive + ", Role=" + role);
        
        DebugManager.logSocial(mob, "Herd Assignment", 
            String.format("Role: %s, Herd size: %d, Aggressive: %s", 
                role.name(), herd.size(), isAggressive));
        
        double maxStamina = herdManager.getPersistence().getMaxStamina(animal, 100 + (Math.random() * 100));
        double stamina = herdManager.getPersistence().getStamina(animal, maxStamina);
        
        animal.setMetadata("maxStamina", new FixedMetadataValue(plugin, maxStamina));
        animal.setMetadata("stamina", new FixedMetadataValue(plugin, stamina));
        animal.setMetadata("fleeing", new FixedMetadataValue(plugin, false));
        
        herdManager.getPersistence().saveHerdData(animal, herd.id(), role == HerdRole.LEADER, isAggressive, maxStamina, stamina);
        
        needsManager.getNeeds(animal);
        
        lifeCycleManager.registerAnimal(animal);
        
        trackedAnimals.add(animal.getUniqueId());
        
        registerGoals(mob, behavior, isAggressive, role);
        
        if (isAggressive && animal instanceof Wolf wolf) {
            wolf.setAngry(true);
        }
        
        plugin.getLogger().info(">>> Initialization complete!");
        
        startStaminaRegeneration(animal);
    }
    
    public HerdManager getHerdManager() {
        return herdManager;
    }
    
    public NeedsManager getNeedsManager() {
        return needsManager;
    }
    
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
    
    private boolean isHerbivore(EntityType type) {
        return type == EntityType.COW || type == EntityType.SHEEP || 
               type == EntityType.HORSE || type == EntityType.DONKEY || 
               type == EntityType.MULE || type == EntityType.LLAMA || 
               type == EntityType.GOAT || type == EntityType.RABBIT || 
               type == EntityType.CHICKEN || type == EntityType.PIG ||
               type == EntityType.CAMEL || type == EntityType.SNIFFER;
    }
    
    private boolean isCarnivore(EntityType type) {
        return type == EntityType.WOLF || type == EntityType.FOX || 
               type == EntityType.CAT || type == EntityType.OCELOT ||
               type == EntityType.POLAR_BEAR;
    }
    
    private boolean isPackHunter(EntityType type) {
        return type == EntityType.WOLF;
    }
    
    private EnvironmentalContext.ActivityPattern getActivityPattern(EntityType type) {
        return switch (type) {
            case WOLF, FOX, CAT, OCELOT -> EnvironmentalContext.ActivityPattern.NOCTURNAL;
            case RABBIT -> EnvironmentalContext.ActivityPattern.CREPUSCULAR;
            default -> EnvironmentalContext.ActivityPattern.DIURNAL;
        };
    }
}
