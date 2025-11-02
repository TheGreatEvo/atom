# Mob AI Debug System

A comprehensive debugging and logging system for the Atom mob AI.

## Components

### 1. DebugManager
Static debug manager with configurable levels and categories.

**Debug Levels:**
- `OFF` - No debug output
- `MINIMAL` - Critical events only (starvation, critical errors)
- `NORMAL` - Standard debugging (goal activations, needs changes)
- `VERBOSE` - Detailed logging (all events, performance metrics)

**Debug Categories:**
- `GOALS` - Goal activation/deactivation
- `NEEDS` - Hunger, thirst, energy changes
- `MEMORY` - Danger locations, player interactions
- `COMBAT` - Combat actions and decisions
- `SOCIAL` - Herd behavior, hierarchy
- `ENVIRONMENTAL` - Weather, time-based behavior

**Features:**
- Color-coded console output
- Per-category debug levels
- Performance tracking
- Automatic critical need detection

### 2. Visual Debugger
Real-time visual feedback using particles and UI elements.

**Particle Colors:**
- 🔴 RED - Aggressive/Combat behavior
- 🟢 GREEN - Peaceful/Grazing
- 🔵 BLUE - Needs-driven (hungry/thirsty)
- 🟡 YELLOW - Environmental (seeking shelter)
- 🟣 PURPLE - Social behavior
- ⚪ WHITE - Idle

**UI Elements:**
- Action bar: Current primary goal
- Boss bar: Hunger/Thirst/Energy levels
- Particle trail: Above tracked mobs

### 3. Performance Monitor
Tracks execution times of goals and systems.

**Warnings:**
- Automatic warning if any operation takes >5ms
- Historical performance data
- Average, min, max execution times
- Recent vs. all-time averages

## Commands

All commands require permission: `atom.debug.mobai`

### Global Debug Level
```
/mobai debug <level>
```
Sets the global debug level for all categories.

**Example:**
```
/mobai debug NORMAL
/mobai debug VERBOSE
/mobai debug OFF
```

### Category Debug Level
```
/mobai debug <category> <level>
```
Sets debug level for a specific category.

**Example:**
```
/mobai debug GOALS VERBOSE
/mobai debug NEEDS NORMAL
/mobai debug COMBAT MINIMAL
```

### Entity Information
```
/mobai info
```
Shows detailed information about the mob you're looking at:
- Type, ID, UUID
- Health and location
- Needs (hunger, thirst, energy)
- Memory (danger zones, player threat level)
- Herd information

**Example Output:**
```
=== Mob AI Info ===
Type: COW
ID: #12345
Health: 10.0 / 10.0
=== Needs ===
Hunger: ██████░░░░ 60.0%
Thirst: ████████░░ 80.0%
Energy: ██████████ 100.0%
=== Memory ===
Recent Threat: None
Nearby Danger Zones: 2
Your Threat Level: NEUTRAL
=== Herd Info ===
Herd Size: 8
Role: FOLLOWER
Panicking: No
```

### Goals
```
/mobai goals
```
Lists active goals for the target entity.

### Needs
```
/mobai needs
```
Shows hunger, thirst, and energy with visual bars.

### Memory
```
/mobai memory
```
Shows danger locations and player memories.

### Herd Info
```
/mobai herd
```
Shows herd size, role, panic status.

### Visual Tracking
```
/mobai track
```
Toggles visual particle tracking for the target mob. Shows:
- Colored particles indicating current behavior state
- Action bar with current goal
- Boss bar with needs levels

**Note:** You must be looking at a mob when using this command.

### Performance Stats
```
/mobai performance
```
Displays performance statistics for all tracked operations.

**Example Output:**
```
=== MobAI Performance Stats ===

GrazingGoal
  Executions: 1,234 | Avg: 0.45ms | Recent: 0.38ms | Min: 0.12ms | Max: 2.34ms
NeedsManager.update
  Executions: 5,678 | Avg: 0.22ms | Recent: 0.19ms | Min: 0.08ms | Max: 1.12ms

Total tracked operations: 15
```

### Reset Metrics
```
/mobai reset
```
Resets all performance metrics.

## Debug Output Examples

### Goal Activation (NORMAL level)
```
[MobAI|Goals] Cow#1234 activated GrazingGoal
[MobAI|Needs] Cow#1234 started grazing (hunger: 45.0%)
```

### Critical Needs (MINIMAL level)
```
[MobAI|Needs] Sheep#5678 is STARVING (HUNGER: 12.0%)
[MobAI|Needs] Wolf#9012 is CRITICAL (THIRST: 8.0%)
```

### Combat (NORMAL level)
```
[MobAI|Combat] Wolf#3456 attacked Rabbit#7890 - damage: 4.0
[MobAI|Needs] Wolf#3456 Combat Drain - H:85% T:78% E:45%
```

### Social Events (NORMAL level)
```
[MobAI|Social] Cow#1111 [SOCIAL] Herd Assignment - Role: LEADER, Herd size: 12, Aggressive: false
[MobAI|Social] Cow#2222 elected as ALPHA (herd size: 8)
```

### Memory (NORMAL level)
```
[MobAI|Memory] Sheep#4444 [Danger Recorded] ATTACKED at (123, 64, -456) severity: 8
[MobAI|Memory] Cow#5555 [Player Threat Update] PlayerName: NEUTRAL -> HOSTILE (interaction: ATTACKED)
```

### Performance Warning (MINIMAL level)
```
[MobAI|Goals] PERFORMANCE WARNING: PackHuntingGoal took 7.23ms
```

## Integration with Existing Code

The debug system is integrated into:

### AnimalBehaviorNew
- Logs animal initialization with stats
- Logs herd assignment and role
- Logs goal registration

### NeedsManager
- Automatically logs critical needs (hunger < 20%, etc.)
- Logs activity drains (combat, fleeing, chasing)
- Tracks need changes over time

### MemoryManager
- Logs danger memory creation
- Logs player threat level changes
- Records location-based memories

### GrazingGoal (Example)
```java
@Override
public void start() {
    eatingTicks = 0;
    DebugManager.logGoalActivation(mob, "GrazingGoal", DebugCategory.GOALS);
    // ... rest of code
}
```

## Adding Debug Logging to New Goals

To add debug logging to a new goal:

1. Import the debug classes:
```java
import org.shotrush.atom.content.mobs.ai.debug.DebugCategory;
import org.shotrush.atom.content.mobs.ai.debug.DebugManager;
```

2. Log activation in `start()`:
```java
@Override
public void start() {
    DebugManager.logGoalActivation(mob, "YourGoalName", DebugCategory.GOALS);
    // your code
}
```

3. Log deactivation in `stop()`:
```java
@Override
public void stop() {
    DebugManager.logGoalDeactivation(mob, "YourGoalName", DebugCategory.GOALS);
    // your code
}
```

4. Log important events:
```java
DebugManager.logCombat(mob, "Attack", "Dealt 5.0 damage to target");
DebugManager.logSocial(mob, "Pack Formation", "Joined pack of 4 wolves");
DebugManager.logEnvironmental(mob, "Shelter Found", "Cave at (100, 65, 200)");
```

## Performance Tracking

To track performance in your code:

```java
long start = PerformanceMonitor.startTracking("OperationName");
// ... your code
PerformanceMonitor.endTracking("OperationName", start);
```

Or use DebugManager:
```java
DebugManager.startPerformanceTracking("MyGoal.tick");
// ... your code
DebugManager.endPerformanceTracking("MyGoal.tick");
```

## Best Practices

1. **Use appropriate debug levels:**
   - `MINIMAL` for critical events only
   - `NORMAL` for standard debugging
   - `VERBOSE` for detailed analysis

2. **Choose the right category:**
   - Use `GOALS` for goal-related events
   - Use `NEEDS` for hunger/thirst/energy
   - Use `COMBAT` for fighting behavior
   - Use `SOCIAL` for herd/pack behavior
   - Use `MEMORY` for learning/memory events
   - Use `ENVIRONMENTAL` for weather/time events

3. **Minimal performance impact:**
   - All debug checks are O(1)
   - No string formatting if debug is OFF
   - Thread-safe concurrent collections

4. **Visual debugging:**
   - Only enable tracking for specific mobs you're studying
   - Use `/mobai track` to toggle tracking on/off
   - Particles visible up to 40 blocks away

## Troubleshooting

**Problem:** No debug output appears
- Check debug level: `/mobai debug NORMAL`
- Verify category level: `/mobai debug GOALS NORMAL`
- Ensure you have permission: `atom.debug.view`

**Problem:** Too much debug output
- Reduce to MINIMAL: `/mobai debug MINIMAL`
- Disable specific categories: `/mobai debug NEEDS OFF`

**Problem:** Performance warnings
- Check `/mobai performance` for slow operations
- Optimize goals that take >5ms
- Consider reducing mob AI complexity

**Problem:** Visual tracking not showing
- Verify you toggled tracking: `/mobai track` while looking at mob
- Check particle settings in client
- Ensure you're within 40 blocks of mob

## Future Enhancements

Planned features:
- Goal priority visualization
- Path visualization (show mob pathfinding)
- Memory visualization (show danger zones in-world)
- Export debug logs to file
- Web-based debug dashboard
- Goal dependency graph
