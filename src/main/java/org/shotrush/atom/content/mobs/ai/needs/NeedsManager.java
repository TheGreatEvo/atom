package org.shotrush.atom.content.mobs.ai.needs;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.mobs.ai.debug.DebugCategory;
import org.shotrush.atom.content.mobs.ai.debug.DebugManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NeedsManager {
    
    private final Atom plugin;
    private final Map<UUID, AnimalNeeds> needs;
    private final Map<UUID, AnimalNeeds.NeedPriority> lastLoggedPriority;
    
    public NeedsManager(Atom plugin) {
        this.plugin = plugin;
        this.needs = new ConcurrentHashMap<>();
        this.lastLoggedPriority = new ConcurrentHashMap<>();
        startNeedsUpdateTask();
    }
    
    public AnimalNeeds getNeeds(Animals animal) {
        return needs.computeIfAbsent(animal.getUniqueId(), id -> new AnimalNeeds());
    }
    
    public void removeNeeds(UUID animalId) {
        needs.remove(animalId);
        lastLoggedPriority.remove(animalId);
    }
    
    private void startNeedsUpdateTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (Map.Entry<UUID, AnimalNeeds> entry : needs.entrySet()) {
                AnimalNeeds need = entry.getValue();
                need.update();
                
                checkAndLogCriticalNeeds(entry.getKey(), need);
            }
        }, 20L, 20L);
    }
    
    private void checkAndLogCriticalNeeds(UUID animalId, AnimalNeeds needs) {
        AnimalNeeds.NeedPriority priority = needs.getMostUrgentNeed();
        AnimalNeeds.NeedPriority lastPriority = lastLoggedPriority.get(animalId);
        
        if (priority.isCritical() && priority != lastPriority) {
            Mob mob = (Mob) plugin.getServer().getEntity(animalId);
            if (mob != null) {
                String needType = priority.name().replace("_CRITICAL", "");
                double value = switch (priority) {
                    case HUNGER_CRITICAL -> needs.getHunger();
                    case THIRST_CRITICAL -> needs.getThirst();
                    case ENERGY_CRITICAL -> needs.getEnergy();
                    default -> 0;
                };
                DebugManager.logCriticalNeed(mob, needType, value);
            }
            lastLoggedPriority.put(animalId, priority);
        } else if (!priority.isCritical() && lastPriority != null && lastPriority.isCritical()) {
            lastLoggedPriority.remove(animalId);
        }
    }
    
    public void drainFromCombat(Animals animal) {
        AnimalNeeds need = getNeeds(animal);
        need.drainFromActivity(0.5, 0.3, 1.0);
        
        if (animal instanceof Mob mob) {
            DebugManager.logNeedsChange(mob, "Combat Drain", 
                need.getHunger(), "H:" + String.format("%.0f", need.getHunger()) + "% " +
                "T:" + String.format("%.0f", need.getThirst()) + "% " +
                "E:" + String.format("%.0f", need.getEnergy()) + "%");
        }
    }
    
    public void drainFromFleeing(Animals animal) {
        AnimalNeeds need = getNeeds(animal);
        need.drainFromActivity(0.2, 0.4, 0.8);
        
        if (animal instanceof Mob mob) {
            DebugManager.logNeedsChange(mob, "Fleeing Drain", 
                need.getHunger(), "H:" + String.format("%.0f", need.getHunger()) + "% " +
                "T:" + String.format("%.0f", need.getThirst()) + "% " +
                "E:" + String.format("%.0f", need.getEnergy()) + "%");
        }
    }
    
    public void drainFromChasing(Animals animal) {
        AnimalNeeds need = getNeeds(animal);
        need.drainFromActivity(0.3, 0.3, 0.6);
        
        if (animal instanceof Mob mob) {
            DebugManager.logNeedsChange(mob, "Chasing Drain", 
                need.getHunger(), "H:" + String.format("%.0f", need.getHunger()) + "% " +
                "T:" + String.format("%.0f", need.getThirst()) + "% " +
                "E:" + String.format("%.0f", need.getEnergy()) + "%");
        }
    }
}
