# 🐛 How to Debug Mob AI - Quick Start

## Quick Commands

### Enable Debug Logging
```
/mobai debug VERBOSE          # See everything
/mobai debug NORMAL           # See important stuff
/mobai debug MINIMAL          # Only critical events
/mobai debug OFF              # Disable debug
```

### Debug Specific Categories
```
/mobai debug GOALS VERBOSE    # See all goal activations
/mobai debug NEEDS NORMAL     # See hunger/thirst events
/mobai debug COMBAT VERBOSE   # See all combat events
/mobai debug SOCIAL NORMAL    # See herd/family events
```

### Inspect Specific Mobs
```
# Look at a mob and run:
/mobai info           # Full stats dump
/mobai goals          # What goals are active right now
/mobai needs          # Hunger/thirst/energy levels
/mobai memory         # What does it remember?
/mobai herd           # Herd info and rank
/mobai track          # Toggle visual particles + boss bar
```

### Performance Monitoring
```
/mobai performance    # Show execution times
/mobai reset          # Clear performance data
```

## Visual Debug Features

### When tracking is enabled (`/mobai track`):

**Particle Colors Above Mob:**
- 🔴 **RED** = Combat/Aggressive (attacking, chasing)
- 🟢 **GREEN** = Peaceful (grazing, wandering)
- 🔵 **BLUE** = Needs-driven (hungry, thirsty, tired)
- 🟡 **YELLOW** = Environmental (seeking shelter, sleeping)
- 🟣 **PURPLE** = Social (playing, following herd, protecting)
- ⚪ **WHITE** = Idle (no active goal)

**Boss Bar:**
- Shows hunger/thirst/energy percentages
- Color changes based on urgency

**Action Bar:**
- Shows current active goal name

## Common Debug Scenarios

### "Why won't this cow eat grass?"
```
/mobai debug NEEDS VERBOSE
/mobai track <cow>
# Watch console and particles
# Boss bar shows hunger level
# Look for: "hunger: XX%" in logs
```

### "Why are wolves attacking each other?"
```
/mobai debug COMBAT VERBOSE
/mobai info <wolf>
/mobai herd <wolf>
# Check if they're in rival herds
# Check aggressive metadata
```

### "Are the babies playing?"
```
/mobai debug SOCIAL VERBOSE
/mobai track <baby>
# Purple particles = social behavior
# Watch for "PlayBehaviorGoal activated"
```

### "Why is stampede triggering?"
```
/mobai debug GOALS VERBOSE
/mobai herd <cow>
# Watch for panic broadcasts
# Check herd size (needs ≥4)
```

### "Performance issues?"
```
/mobai performance
# Look for goals taking >5ms
# Check how many animals are tracked
# Watch for red warnings in console
```

## Console Output Examples

### Debug Logging Format:
```
[MobAI|GOALS] §aCow#1234 activated GrazingGoal (hunger: 35%)
[MobAI|COMBAT] §cWolf#5678 activated StalkPreyGoal (target: Rabbit#9012)
[MobAI|NEEDS] §6Sheep#3456 is STARVING (hunger: 12%)
[MobAI|SOCIAL] §dCow#7890 elected as ALPHA (herd size: 8)
[MobAI|MEMORY] §eCow#1234 remembers danger at (100, 64, 200)
```

### Performance Warnings:
```
⚠ [PERFORMANCE] GrazingGoal took 7.3ms (exceeds 5ms threshold)
```

## Tips

1. **Start with NORMAL level** - VERBOSE can be overwhelming
2. **Use category filtering** - Debug only what you need
3. **Track one mob at a time** - Easier to follow
4. **Check boss bars** - Quick visual of needs state
5. **Watch particles** - Instant feedback on behavior state
6. **Performance first** - If laggy, check `/mobai performance`

## Integration in Code

### To add logging to your own goals:
```java
@Override
public void start() {
    DebugManager.logGoalActivation(mob, "MyCustomGoal", DebugCategory.GOALS);
    // your code
}

@Override
public void stop() {
    DebugManager.logGoalDeactivation(mob, "MyCustomGoal", DebugCategory.GOALS);
    // your code
}
```

### To add performance tracking:
```java
long start = System.nanoTime();
// do expensive operation
PerformanceMonitor.recordGoalExecution("MyGoal", System.nanoTime() - start);
```

## Permissions

- `atom.debug.*` - All debug commands
- `atom.debug.use` - Basic debug commands
- `atom.debug.performance` - Performance monitoring

---

**Ready to debug!** Start the server and try `/mobai debug NORMAL` to see the AI in action! 🎮
