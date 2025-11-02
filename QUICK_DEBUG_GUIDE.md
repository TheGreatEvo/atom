# Quick Debug Guide - Mob AI

## Quick Start

1. **Enable debugging:**
   ```
   /mobai debug NORMAL
   ```

2. **Look at a mob and get info:**
   ```
   /mobai info
   ```

3. **Track a specific mob visually:**
   ```
   /mobai track
   ```

## Common Use Cases

### "Why isn't this cow eating?"
```
/mobai needs          # Check hunger level
/mobai debug NEEDS VERBOSE
/mobai track          # Watch for GrazingGoal activation
```

### "Why is this herd panicking?"
```
/mobai herd           # Check panic status
/mobai memory         # Check for danger zones
/mobai debug SOCIAL NORMAL
```

### "Is the AI slow?"
```
/mobai performance    # Check execution times
```

### "What goals are active?"
```
/mobai debug GOALS VERBOSE
# Watch console for goal activations
```

## Debug Levels

- **OFF** - No output
- **MINIMAL** - Critical only (starvation, errors)
- **NORMAL** - Standard debugging ⭐ (recommended)
- **VERBOSE** - Everything (very chatty)

## Particle Colors

When tracking a mob with `/mobai track`:

- 🔴 **Red** = Fighting/Aggressive
- 🟢 **Green** = Peaceful/Grazing
- 🔵 **Blue** = Hungry/Thirsty/Tired
- 🟡 **Yellow** = Seeking shelter/sleep
- 🟣 **Purple** = Social behavior (herd)
- ⚪ **White** = Idle/Wandering

## Reading the Console

Example output:
```
[MobAI|Goals] Cow#1234 activated GrazingGoal
[MobAI|Needs] Cow#1234 started grazing (hunger: 45.0%)
[MobAI|Needs] Cow#1234 finished eating (hunger now: 75.0%)
```

Format: `[MobAI|Category] EntityType#ID message`

## Performance Warnings

If you see this:
```
[MobAI|Goals] PERFORMANCE WARNING: PackHuntingGoal took 7.23ms
```

The goal is taking too long. Use `/mobai performance` to investigate.

## Tips

1. **Start with NORMAL level** - VERBOSE is overwhelming
2. **Use categories** - Only enable what you need
3. **Track one mob at a time** - Too many particles = lag
4. **Check performance regularly** - Keep goals under 5ms
5. **Look at info first** - `/mobai info` shows current state

## Troubleshooting

**No output?**
- Check: `/mobai debug NORMAL`
- Verify permission: `atom.debug.mobai`

**Too much spam?**
- Reduce: `/mobai debug MINIMAL`
- Disable category: `/mobai debug GOALS OFF`

**Can't see particles?**
- Enable tracking: `/mobai track` (while looking at mob)
- Check distance: Must be within 40 blocks
- Client settings: Particles enabled?

## Example Debugging Session

```bash
# 1. Enable debug
/mobai debug NORMAL
/mobai debug NEEDS VERBOSE

# 2. Find problematic mob
/mobai info

# 3. Track it visually
/mobai track

# 4. Watch console and particles
# - Console shows goal activations
# - Particles show current behavior
# - Boss bar shows needs

# 5. Check performance
/mobai performance

# 6. Disable when done
/mobai debug OFF
```

## Adding Debug to Your Goals

```java
import org.shotrush.atom.content.mobs.ai.debug.*;

public class MyGoal implements Goal<Mob> {
    @Override
    public void start() {
        DebugManager.logGoalActivation(mob, "MyGoal", DebugCategory.GOALS);
    }
    
    @Override
    public void stop() {
        DebugManager.logGoalDeactivation(mob, "MyGoal", DebugCategory.GOALS);
    }
}
```

## Permission

All commands require: `atom.debug.mobai`

Grant to yourself:
```
/lp user YourName permission set atom.debug.mobai true
```
