# Animal Behavior System - Quick Reference

## 🎯 Goal Priority Guide

```
Priority 0 (CRITICAL - Always Check First)
├── RestWhenExhaustedGoal - Forces rest when energy depleted
├── DeathEffectsGoal - Mourning & fleeing from corpses
└── HerdPanicGoal - Mass panic response

Priority 1 (SAFETY)
├── SeekShelterGoal - Storm shelter seeking
├── MotherProtectionGoal - Protect babies
└── AvoidPlayerWhenInjuredGoal - Flee when wounded

Priority 2 (NEEDS & HUNTING)
├── GrazingGoal - Eat grass (herbivores only)
├── SeekWaterGoal - Find water (all animals)
├── HuntPreyGoal - Hunt prey (carnivores only)
├── TrackWoundedPreyGoal - Track injured (carnivores only)
└── FlankAndSurroundGoal - Pack tactics (wolves only)

Priority 3 (ADVANCED)
├── ScavengeGoal - Scavenge food (carnivores only)
├── StalkPreyGoal - Stealth hunting (carnivores only)
├── ReunionGoal - Return to herd (all animals)
└── SleepGoal - Sleep when tired (all animals)

Priority 4 (ROLE-BASED)
├── StayNearHerdGoal - Followers stay close
├── SentryBehaviorGoal - Leaders watch for threats
└── TerritoryDefenseGoal - Alphas defend territory

Priority 6 (NAVIGATION)
└── HerdLeaderWanderGoal - Leader pathfinding

Priority 7 (SOCIAL)
└── ShareFoodGoal - Share with family

Priority 8 (DEVELOPMENT)
└── PlayBehaviorGoal - Baby/juvenile play

Priority 10 (ENVIRONMENTAL)
└── TimeBasedActivityGoal - Day/night cycle effects
```

---

## 🦁 Species Quick Reference

### Herbivores (Grazing Enabled)
```java
COW, SHEEP, HORSE, DONKEY, MULE, LLAMA, GOAT, 
RABBIT, CHICKEN, PIG, CAMEL, SNIFFER
```
**Goals:** Grazing, SeekWater, Rest, Shelter, Sleep, Social

### Carnivores (Hunting Enabled)
```java
WOLF, FOX, CAT, OCELOT, POLAR_BEAR
```
**Goals:** Hunt, Track, Stalk, Scavenge, SeekWater, Rest, Sleep

### Pack Hunters (Coordination Enabled)
```java
WOLF
```
**Extra Goals:** FlankAndSurround, Sentry, TerritoryDefense

---

## 🌍 Activity Patterns

| Pattern | Species | Active Time |
|---------|---------|-------------|
| **DIURNAL** | Most animals | 6am - 6pm |
| **NOCTURNAL** | Wolf, Fox, Cat, Ocelot | 6pm - 6am |
| **CREPUSCULAR** | Rabbit | Dawn & Dusk |
| **ALWAYS_ACTIVE** | None (default fallback) | 24/7 |

### Speed Modifiers by Time
- **Peak Activity:** 1.2x speed
- **Active Time:** 1.0x speed
- **Neutral Time:** 0.8x speed
- **Inactive Time:** 0.3x speed

---

## 📊 Manager Systems

### NeedsManager
```java
// Get animal's needs
var needs = needsManager.getNeeds(animal);

// Check status
needs.isHungry()    // Food < 40%
needs.isThirsty()   // Water < 40%
needs.isTired()     // Energy < 30%
needs.isExhausted() // Energy < 10%

// Modify needs
needs.eat(amount)    // Restore hunger
needs.drink(amount)  // Restore thirst
needs.sleep(amount)  // Restore energy

// Drain from activity
needsManager.drainFromCombat(animal)
needsManager.drainFromFleeing(animal)
needsManager.drainFromChasing(animal)
```

### LifeCycleManager
```java
// Register animal
lifeCycleManager.registerAnimal(animal);

// Check age stage
AgeStage stage = lifeCycleManager.getStage(animal);
// Returns: BABY, JUVENILE, ADULT, ELDER

// Get modifiers
double speedMod = lifeCycleManager.getSpeedModifier(animal);
double combatMod = lifeCycleManager.getCombatModifier(animal);

// Family relationships
Optional<UUID> mother = lifeCycleManager.getMother(animal);
List<Animals> family = lifeCycleManager.findNearbyFamily(animal, range);
boolean hasFamily = lifeCycleManager.hasStrongFamilyBond(animal);

// Age checks
lifeCycleManager.isBaby(animal)
lifeCycleManager.isJuvenile(animal)
lifeCycleManager.isAdult(animal)
lifeCycleManager.isElder(animal)

// Cleanup
lifeCycleManager.removeAnimal(animalId);
```

### FamilyRelationships
```java
// Access through lifecycle
FamilyRelationships family = lifeCycleManager.getFamilyRelationships();

// Register birth
family.registerBirth(mother, child);

// Get relationships
Optional<UUID> mother = family.getMother(animal);
Set<UUID> children = family.getChildren(animal);
Set<UUID> siblings = family.getSiblings(animal);

// Find nearby family
List<Animals> nearbyFamily = family.findNearbyFamilyMembers(animal, range);
List<Animals> nearbyMother = family.findNearbyMother(animal);

// Bond strength
double bond = family.getBondStrength(animal); // 0.0 - 1.0
boolean strongBond = family.hasStrongBond(animal); // > 0.5

// Relationship checks
family.isMotherOf(mother, child)
family.isSiblingOf(animal1, animal2)
```

---

## 🎮 Goal Activation Conditions

### RestWhenExhaustedGoal
- Energy < 10%
- Overrides all other goals

### GrazingGoal  
- Is herbivore
- Hunger < 60%
- Grass block within 16 blocks
- Cooldown: 10 seconds

### SeekWaterGoal
- Thirst < 60%
- Water within 24 blocks
- Cooldown: 8 seconds

### HuntPreyGoal
- Is carnivore (Wolf/Fox)
- Hunger < 60%
- Prey within aggro radius
- Not targeting player

### SleepGoal
- Energy < 70% OR
- Nighttime AND energy < 70%
- No nearby threats

### MotherProtectionGoal
- Has children
- Child damaged within 12 blocks
- Enrages and gets 2x damage

### PlayBehaviorGoal
- Is baby
- Has siblings nearby (< 10 blocks)
- 5% chance per tick
- Duration: 400 ticks (20 seconds)

### ShareFoodGoal
- Rank is Alpha or Beta
- Has food
- Family member hungry (< 40%)
- Within 8 blocks

---

## 🔧 Adding New Goals

```java
// In registerGoals() method

// 1. Determine priority (0-10)
int priority = 3;

// 2. Check if species-specific
if (isCarnivore(mob.getType())) {
    goalSelector.addGoal(mob, priority, new MyCustomGoal(mob, plugin, ...));
}

// 3. Pass required managers
goalSelector.addGoal(mob, priority, 
    new MyGoal(mob, plugin, needsManager, lifeCycleManager));
```

---

## 🐛 Debugging Tips

### Enable Debug Logging
Check console for:
```
>>> Animal spawn detected: COW (Reason: NATURAL)
>>> Initializing: Aggressive=false, Role=FOLLOWER
>>> Registered goals for COW (aggressive: false, role: FOLLOWER)
>>> Initialization complete!
```

### Death Cleanup
```
>>> Animal died: COW - cleaned up all systems
```

### Common Issues

**Goal not activating?**
- Check priority conflicts
- Verify species classification
- Check cooldown timers
- Verify needs thresholds

**Animal stuck/frozen?**
- Check for exhaustion (energy = 0)
- Check for morale break
- Check for injury effects

**Memory leak?**
- Verify cleanup on death
- Check removal from all managers
- Monitor trackedAnimals set size

---

## 📈 Performance Metrics

### Per-Animal Overhead
- **Managers:** 3 (Needs, Lifecycle, Herd)
- **Goals:** 10-20 (species-dependent)
- **Metadata Keys:** 5-8
- **Update Tasks:** 2 (Needs @ 1s, Lifecycle @ 1s)

### Recommended Limits
- **Entities per Chunk:** < 20
- **Total Tracked Animals:** < 500
- **Herd Size:** 2-20 (varies by species)

### Memory Usage
- **Per Animal:** ~2KB
- **100 Animals:** ~200KB
- **500 Animals:** ~1MB

---

## 🎨 Visual Effects Reference

### Goal Particles
- **Sleep:** ENCHANT (purple sparkles)
- **Rest:** COMPOSTER + SOUL (green/blue)
- **Grazing:** ITEM (grass particles)
- **Drinking:** SPLASH + DRIPPING_WATER
- **Mourning:** SOUL + ASH (grey/white)
- **Reunion:** NOTE (musical notes)
- **Play:** HAPPY_VILLAGER (green hearts)
- **Share Food:** HEART + ITEM

### Goal Sounds
- **Grazing:** ENTITY_GENERIC_EAT
- **Drinking:** ENTITY_GENERIC_DRINK
- **Mourning:** Species-specific ambient (low pitch)
- **Reunion:** Species-specific ambient (high pitch)
- **Alert:** ENTITY_GOAT_SCREAMING_AMBIENT
- **Territory:** ENTITY_RAVAGER_ROAR

---

## 🔗 Quick Links

- Main Class: `AnimalBehaviorNew.java`
- Goals Folder: `ai/goals/`
- Managers: `ai/needs/`, `ai/lifecycle/`
- Config: `ai/config/SpeciesBehavior.java`
- Environment: `ai/environment/EnvironmentalContext.java`

---

**Last Updated:** November 2, 2025
