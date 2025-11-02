package org.shotrush.atom.content.mobs.ai.memory;

import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.mobs.ai.debug.DebugCategory;
import org.shotrush.atom.content.mobs.ai.debug.DebugManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryManager {
    
    private final Atom plugin;
    private final Map<UUID, AnimalMemory> memories;
    
    public MemoryManager(Atom plugin) {
        this.plugin = plugin;
        this.memories = new ConcurrentHashMap<>();
    }
    
    public AnimalMemory getMemory(Animals animal) {
        return memories.computeIfAbsent(animal.getUniqueId(), id -> new AnimalMemory(id));
    }
    
    public void recordDanger(Animals animal, Location location, AnimalMemory.DangerType type, int severity) {
        AnimalMemory memory = getMemory(animal);
        memory.rememberDanger(location, type, severity);
        
        if (animal instanceof Mob mob) {
            DebugManager.logMemory(mob, "Danger Recorded", 
                String.format("%s at (%.0f, %.0f, %.0f) severity: %d", 
                    type.name(), location.getX(), location.getY(), location.getZ(), severity));
        }
    }
    
    public void recordPlayerInteraction(Animals animal, Player player, PlayerMemory.PlayerInteraction interaction) {
        AnimalMemory memory = getMemory(animal);
        AnimalMemory.PlayerThreatLevel oldLevel = memory.getThreatLevel(player);
        memory.rememberPlayer(player, interaction);
        AnimalMemory.PlayerThreatLevel newLevel = memory.getThreatLevel(player);
        
        if (animal instanceof Mob mob && oldLevel != newLevel) {
            DebugManager.logMemory(mob, "Player Threat Update", 
                String.format("%s: %s -> %s (interaction: %s)", 
                    player.getName(), oldLevel.name(), newLevel.name(), interaction.name()));
        }
    }
    
    public boolean isDangerous(Animals animal, Location location) {
        AnimalMemory memory = memories.get(animal.getUniqueId());
        if (memory == null) return false;
        return memory.isDangerous(location);
    }
    
    public AnimalMemory.PlayerThreatLevel getThreatLevel(Animals animal, Player player) {
        AnimalMemory memory = memories.get(animal.getUniqueId());
        if (memory == null) return AnimalMemory.PlayerThreatLevel.NEUTRAL;
        return memory.getThreatLevel(player);
    }
    
    public void removeMemory(UUID animalId) {
        memories.remove(animalId);
    }
    
    public void cleanup() {
        plugin.getLogger().info("Cleaning up animal memories...");
        memories.values().forEach(memory -> {
        });
    }
}
