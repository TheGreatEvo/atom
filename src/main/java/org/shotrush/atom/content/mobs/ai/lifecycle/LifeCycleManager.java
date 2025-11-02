package org.shotrush.atom.content.mobs.ai.lifecycle;

import org.bukkit.entity.Animals;
import org.shotrush.atom.Atom;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LifeCycleManager {
    
    private final Atom plugin;
    private final FamilyRelationships familyRelationships;
    private final Map<UUID, Long> birthTimes;
    
    private static final int BABY_PERCENT = 20;
    private static final int JUVENILE_PERCENT = 40;
    private static final int ADULT_PERCENT = 90;
    
    private static final double BABY_SPEED_MODIFIER = 0.7;
    private static final double JUVENILE_SPEED_MODIFIER = 0.9;
    private static final double ADULT_SPEED_MODIFIER = 1.0;
    private static final double ELDER_SPEED_MODIFIER = 0.85;
    
    private static final double BABY_COMBAT_MODIFIER = 0.0;
    private static final double JUVENILE_COMBAT_MODIFIER = 0.7;
    private static final double ADULT_COMBAT_MODIFIER = 1.0;
    private static final double ELDER_COMBAT_MODIFIER = 0.8;
    
    private static final double ELDER_DOMESTICATION_BONUS = 0.3;
    
    public LifeCycleManager(Atom plugin) {
        this.plugin = plugin;
        this.familyRelationships = new FamilyRelationships();
        this.birthTimes = new ConcurrentHashMap<>();
        startAgeUpdateTask();
    }
    
    public void registerAnimal(Animals animal) {
        birthTimes.putIfAbsent(animal.getUniqueId(), (long) animal.getTicksLived());
    }
    
    public void registerBirth(Animals mother, Animals child) {
        birthTimes.put(child.getUniqueId(), 0L);
        familyRelationships.registerBirth(mother, child);
    }
    
    public AgeStage getStage(Animals animal) {
        int agePercent = getAgePercent(animal);
        
        if (agePercent < BABY_PERCENT) return AgeStage.BABY;
        if (agePercent < JUVENILE_PERCENT) return AgeStage.JUVENILE;
        if (agePercent < ADULT_PERCENT) return AgeStage.ADULT;
        return AgeStage.ELDER;
    }
    
    public boolean isBaby(Animals animal) {
        return getStage(animal) == AgeStage.BABY;
    }
    
    public boolean isJuvenile(Animals animal) {
        return getStage(animal) == AgeStage.JUVENILE;
    }
    
    public boolean isAdult(Animals animal) {
        return getStage(animal) == AgeStage.ADULT;
    }
    
    public boolean isElder(Animals animal) {
        return getStage(animal) == AgeStage.ELDER;
    }
    
    public double getSpeedModifier(Animals animal) {
        return switch (getStage(animal)) {
            case BABY -> BABY_SPEED_MODIFIER;
            case JUVENILE -> JUVENILE_SPEED_MODIFIER;
            case ADULT -> ADULT_SPEED_MODIFIER;
            case ELDER -> ELDER_SPEED_MODIFIER;
        };
    }
    
    public double getCombatModifier(Animals animal) {
        return switch (getStage(animal)) {
            case BABY -> BABY_COMBAT_MODIFIER;
            case JUVENILE -> JUVENILE_COMBAT_MODIFIER;
            case ADULT -> ADULT_COMBAT_MODIFIER;
            case ELDER -> ELDER_COMBAT_MODIFIER;
        };
    }
    
    public double getDomesticationBonus(Animals animal) {
        return isElder(animal) ? ELDER_DOMESTICATION_BONUS : 0.0;
    }
    
    public boolean canAttack(Animals animal) {
        return !isBaby(animal);
    }
    
    public boolean shouldFollowMother(Animals animal) {
        return isBaby(animal) && familyRelationships.getMother(animal).isPresent();
    }
    
    public boolean shouldPlay(Animals animal) {
        return isBaby(animal) || isJuvenile(animal);
    }
    
    public Optional<UUID> getMother(Animals animal) {
        return familyRelationships.getMother(animal);
    }
    
    public List<Animals> findNearbyMother(Animals animal) {
        return familyRelationships.findNearbyMother(animal);
    }
    
    public List<Animals> findNearbyFamily(Animals animal, double range) {
        return familyRelationships.findNearbyFamilyMembers(animal, range);
    }
    
    public boolean hasStrongFamilyBond(Animals animal) {
        return familyRelationships.hasStrongBond(animal);
    }
    
    public double getFamilyBondStrength(Animals animal) {
        return familyRelationships.getBondStrength(animal);
    }
    
    public void removeAnimal(UUID animalId) {
        birthTimes.remove(animalId);
        familyRelationships.removeRelationships(animalId);
    }
    
    public FamilyRelationships getFamilyRelationships() {
        return familyRelationships;
    }
    
    private int getAgePercent(Animals animal) {
        Long birthTime = birthTimes.get(animal.getUniqueId());
        if (birthTime == null) {
            birthTime = 0L;
            birthTimes.put(animal.getUniqueId(), birthTime);
        }
        
        int ticksLived = animal.getTicksLived() - birthTime.intValue();
        int maxAge = getMaxAgeForSpecies(animal);
        
        return (int) ((ticksLived / (double) maxAge) * 100);
    }
    
    private int getMaxAgeForSpecies(Animals animal) {
        return 24000 * 20;
    }
    
    private void startAgeUpdateTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (UUID animalId : birthTimes.keySet()) {
                familyRelationships.updateBondStrength(
                        plugin.getServer().getEntity(animalId) instanceof Animals animal ? animal : null, 
                        20L
                );
            }
        }, 20L, 20L);
    }
    
    public enum AgeStage {
        BABY,
        JUVENILE,
        ADULT,
        ELDER
    }
}
