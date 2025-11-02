package org.shotrush.atom.content.mobs.ai.debug;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.shotrush.atom.content.mobs.ai.memory.AnimalMemory;
import org.shotrush.atom.content.mobs.ai.memory.MemoryManager;
import org.shotrush.atom.content.mobs.ai.needs.AnimalNeeds;
import org.shotrush.atom.content.mobs.ai.needs.NeedsManager;
import org.shotrush.atom.content.mobs.herd.Herd;
import org.shotrush.atom.content.mobs.herd.HerdManager;

import java.util.Optional;

@CommandAlias("mobai")
@Description("Debug commands for mob AI system")
@CommandPermission("atom.debug.mobai")
public class MobAIDebugCommand extends BaseCommand {
    
    private final VisualDebugger visualDebugger;
    private final NeedsManager needsManager;
    private final MemoryManager memoryManager;
    private final HerdManager herdManager;
    
    public MobAIDebugCommand(VisualDebugger visualDebugger, NeedsManager needsManager, 
                            MemoryManager memoryManager, HerdManager herdManager) {
        this.visualDebugger = visualDebugger;
        this.needsManager = needsManager;
        this.memoryManager = memoryManager;
        this.herdManager = herdManager;
    }
    
    @Subcommand("debug")
    @Description("Set global debug level")
    @CommandCompletion("OFF|MINIMAL|NORMAL|VERBOSE")
    public void onDebugGlobal(Player player, String levelStr) {
        try {
            DebugLevel level = DebugLevel.valueOf(levelStr.toUpperCase());
            DebugManager.setGlobalLevel(level);
            player.sendMessage(Component.text("Global debug level set to: ", NamedTextColor.GREEN)
                .append(Component.text(level.name(), NamedTextColor.YELLOW)));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid debug level. Use: OFF, MINIMAL, NORMAL, or VERBOSE", NamedTextColor.RED));
        }
    }
    
    @Subcommand("debug")
    @Description("Set category debug level")
    @CommandCompletion("GOALS|NEEDS|MEMORY|COMBAT|SOCIAL|ENVIRONMENTAL OFF|MINIMAL|NORMAL|VERBOSE")
    public void onDebugCategory(Player player, String categoryStr, String levelStr) {
        try {
            DebugCategory category = DebugCategory.valueOf(categoryStr.toUpperCase());
            DebugLevel level = DebugLevel.valueOf(levelStr.toUpperCase());
            DebugManager.setCategoryLevel(category, level);
            player.sendMessage(Component.text("Debug level for ", NamedTextColor.GREEN)
                .append(Component.text(category.getDisplayName(), category.getColor()))
                .append(Component.text(" set to: ", NamedTextColor.GREEN))
                .append(Component.text(level.name(), NamedTextColor.YELLOW)));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid category or level.", NamedTextColor.RED));
        }
    }
    
    @Subcommand("info")
    @Description("Show detailed info about target entity")
    public void onInfo(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Mob mob)) {
            player.sendMessage(Component.text("You must be looking at a mob!", NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text("=== Mob AI Info ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Type: ", NamedTextColor.GRAY)
            .append(Component.text(mob.getType().name(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("ID: ", NamedTextColor.GRAY)
            .append(Component.text("#" + mob.getEntityId(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY)
            .append(Component.text(mob.getUniqueId().toString().substring(0, 8) + "...", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("Health: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.1f / %.1f", 
                mob.getHealth(), 
                mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()), 
                NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Location: ", NamedTextColor.GRAY)
            .append(Component.text(String.format("%.1f, %.1f, %.1f", 
                mob.getLocation().getX(), 
                mob.getLocation().getY(), 
                mob.getLocation().getZ()), 
                NamedTextColor.WHITE)));
        
        if (mob instanceof Animals animal) {
            displayNeedsInfo(player, animal);
            displayMemoryInfo(player, animal);
            displayHerdInfo(player, animal);
        }
    }
    
    @Subcommand("goals")
    @Description("List active goals for target entity")
    public void onGoals(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Mob mob)) {
            player.sendMessage(Component.text("You must be looking at a mob!", NamedTextColor.RED));
            return;
        }
        
        player.sendMessage(Component.text("=== Active Goals ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Goal inspection requires server-side API access.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Enable VERBOSE debugging to see goal activations in real-time.", NamedTextColor.GRAY));
    }
    
    @Subcommand("needs")
    @Description("Show hunger/thirst/energy for target entity")
    public void onNeeds(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Animals animal)) {
            player.sendMessage(Component.text("You must be looking at an animal!", NamedTextColor.RED));
            return;
        }
        
        displayNeedsInfo(player, animal);
    }
    
    private void displayNeedsInfo(Player player, Animals animal) {
        AnimalNeeds needs = needsManager.getNeeds(animal);
        
        player.sendMessage(Component.text("=== Needs ===", NamedTextColor.GOLD));
        player.sendMessage(getNeedsBar("Hunger", needs.getHunger()));
        player.sendMessage(getNeedsBar("Thirst", needs.getThirst()));
        player.sendMessage(getNeedsBar("Energy", needs.getEnergy()));
        
        if (needs.isStarving()) {
            player.sendMessage(Component.text("⚠ STARVING", NamedTextColor.RED));
        } else if (needs.isHungry()) {
            player.sendMessage(Component.text("○ Hungry", NamedTextColor.YELLOW));
        }
        
        if (needs.isDehydrated()) {
            player.sendMessage(Component.text("⚠ DEHYDRATED", NamedTextColor.RED));
        } else if (needs.isThirsty()) {
            player.sendMessage(Component.text("○ Thirsty", NamedTextColor.YELLOW));
        }
        
        if (needs.isExhausted()) {
            player.sendMessage(Component.text("⚠ EXHAUSTED", NamedTextColor.RED));
        } else if (needs.isTired()) {
            player.sendMessage(Component.text("○ Tired", NamedTextColor.YELLOW));
        }
    }
    
    private Component getNeedsBar(String label, double value) {
        int bars = (int) (value / 10);
        NamedTextColor color;
        if (value < 25) color = NamedTextColor.RED;
        else if (value < 50) color = NamedTextColor.YELLOW;
        else color = NamedTextColor.GREEN;
        
        StringBuilder barStr = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            barStr.append(i < bars ? "█" : "░");
        }
        
        return Component.text(label + ": ", NamedTextColor.GRAY)
            .append(Component.text(barStr.toString(), color))
            .append(Component.text(String.format(" %.1f%%", value), NamedTextColor.WHITE));
    }
    
    @Subcommand("memory")
    @Description("Show danger locations and player memories")
    public void onMemory(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Animals animal)) {
            player.sendMessage(Component.text("You must be looking at an animal!", NamedTextColor.RED));
            return;
        }
        
        displayMemoryInfo(player, animal);
    }
    
    private void displayMemoryInfo(Player player, Animals animal) {
        AnimalMemory memory = memoryManager.getMemory(animal);
        
        player.sendMessage(Component.text("=== Memory ===", NamedTextColor.GOLD));
        
        var recentThreat = memory.getRecentThreat();
        if (recentThreat.isPresent()) {
            var loc = recentThreat.get();
            player.sendMessage(Component.text("Recent Threat: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("(%.0f, %.0f, %.0f)", loc.getX(), loc.getY(), loc.getZ()), NamedTextColor.RED)));
        } else {
            player.sendMessage(Component.text("Recent Threat: ", NamedTextColor.GRAY)
                .append(Component.text("None", NamedTextColor.GREEN)));
        }
        
        var nearbyDangers = memory.getNearbyDangerZones(animal.getLocation(), 50);
        player.sendMessage(Component.text("Nearby Danger Zones: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(nearbyDangers.size()), NamedTextColor.WHITE)));
        
        AnimalMemory.PlayerThreatLevel threatLevel = memory.getThreatLevel(player);
        NamedTextColor threatColor = switch (threatLevel) {
            case FRIENDLY -> NamedTextColor.GREEN;
            case NEUTRAL -> NamedTextColor.YELLOW;
            case CAUTIOUS -> NamedTextColor.GOLD;
            case HOSTILE -> NamedTextColor.RED;
            case MORTAL_ENEMY -> NamedTextColor.DARK_RED;
        };
        
        player.sendMessage(Component.text("Your Threat Level: ", NamedTextColor.GRAY)
            .append(Component.text(threatLevel.name(), threatColor)));
    }
    
    @Subcommand("herd")
    @Description("Show herd info and hierarchy")
    public void onHerd(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Animals animal)) {
            player.sendMessage(Component.text("You must be looking at an animal!", NamedTextColor.RED));
            return;
        }
        
        displayHerdInfo(player, animal);
    }
    
    private void displayHerdInfo(Player player, Animals animal) {
        Optional<Herd> herdOpt = herdManager.getHerd(animal.getUniqueId());
        
        if (herdOpt.isEmpty()) {
            player.sendMessage(Component.text("This animal is not in a herd.", NamedTextColor.YELLOW));
            return;
        }
        
        Herd herd = herdOpt.get();
        boolean isLeader = herd.leader().equals(animal.getUniqueId());
        
        player.sendMessage(Component.text("=== Herd Info ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Herd Size: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(herd.size()), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Role: ", NamedTextColor.GRAY)
            .append(Component.text(isLeader ? "LEADER" : "FOLLOWER", 
                isLeader ? NamedTextColor.YELLOW : NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Panicking: ", NamedTextColor.GRAY)
            .append(Component.text(herd.isPanicking() ? "Yes" : "No", 
                herd.isPanicking() ? NamedTextColor.RED : NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Herd ID: ", NamedTextColor.GRAY)
            .append(Component.text(herd.id().toString().substring(0, 8) + "...", NamedTextColor.DARK_GRAY)));
    }
    
    @Subcommand("track")
    @Description("Toggle visual tracking for target entity")
    public void onTrack(Player player) {
        Entity target = player.getTargetEntity(10, false);
        
        if (!(target instanceof Mob mob)) {
            player.sendMessage(Component.text("You must be looking at a mob!", NamedTextColor.RED));
            return;
        }
        
        visualDebugger.toggleTracking(mob.getUniqueId());
        visualDebugger.enableVisualsForPlayer(player.getUniqueId());
        
        boolean isTracking = visualDebugger.isTracking(mob.getUniqueId());
        player.sendMessage(Component.text(
            isTracking ? "Now tracking " : "Stopped tracking ",
            isTracking ? NamedTextColor.GREEN : NamedTextColor.YELLOW
        ).append(Component.text(mob.getType().name() + " #" + mob.getEntityId(), NamedTextColor.WHITE)));
    }
    
    @Subcommand("performance")
    @Description("Show performance statistics")
    public void onPerformance(Player player) {
        PerformanceMonitor.displayStats(player);
    }
    
    @Subcommand("reset")
    @Description("Reset performance metrics")
    public void onReset(Player player) {
        PerformanceMonitor.reset();
        DebugManager.resetPerformanceMetrics();
        player.sendMessage(Component.text("Performance metrics reset.", NamedTextColor.GREEN));
    }
}
