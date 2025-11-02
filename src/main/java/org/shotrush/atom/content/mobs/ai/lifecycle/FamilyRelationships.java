package org.shotrush.atom.content.mobs.ai.lifecycle;

import org.bukkit.Location;
import org.bukkit.entity.Animals;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FamilyRelationships {
    
    private final Map<UUID, UUID> childToMother;
    private final Map<UUID, Set<UUID>> motherToChildren;
    private final Map<UUID, Long> bondStrengths;
    
    private static final long INITIAL_BOND_STRENGTH = 24000;
    private static final long BOND_DECAY_PER_TICK = 1;
    private static final double FAMILY_RECOGNITION_RANGE = 16.0;
    
    public FamilyRelationships() {
        this.childToMother = new ConcurrentHashMap<>();
        this.motherToChildren = new ConcurrentHashMap<>();
        this.bondStrengths = new ConcurrentHashMap<>();
    }
    
    public void registerBirth(Animals mother, Animals child) {
        UUID motherId = mother.getUniqueId();
        UUID childId = child.getUniqueId();
        
        childToMother.put(childId, motherId);
        motherToChildren.computeIfAbsent(motherId, k -> ConcurrentHashMap.newKeySet()).add(childId);
        bondStrengths.put(childId, INITIAL_BOND_STRENGTH);
    }
    
    public Optional<UUID> getMother(Animals animal) {
        return Optional.ofNullable(childToMother.get(animal.getUniqueId()));
    }
    
    public Set<UUID> getChildren(Animals animal) {
        return motherToChildren.getOrDefault(animal.getUniqueId(), Collections.emptySet());
    }
    
    public Set<UUID> getSiblings(Animals animal) {
        return getMother(animal)
                .map(motherToChildren::get)
                .map(children -> children.stream()
                        .filter(id -> !id.equals(animal.getUniqueId()))
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }
    
    public List<Animals> findNearbyFamilyMembers(Animals animal, double range) {
        UUID animalId = animal.getUniqueId();
        Location location = animal.getLocation();
        
        Set<UUID> familyIds = new HashSet<>();
        getMother(animal).ifPresent(familyIds::add);
        familyIds.addAll(getChildren(animal));
        familyIds.addAll(getSiblings(animal));
        
        return animal.getWorld().getNearbyEntities(location, range, range, range).stream()
                .filter(entity -> entity instanceof Animals)
                .map(entity -> (Animals) entity)
                .filter(nearby -> familyIds.contains(nearby.getUniqueId()))
                .collect(Collectors.toList());
    }
    
    public List<Animals> findNearbyMother(Animals animal) {
        return getMother(animal)
                .map(motherId -> animal.getWorld().getNearbyEntities(animal.getLocation(), 
                        FAMILY_RECOGNITION_RANGE, FAMILY_RECOGNITION_RANGE, FAMILY_RECOGNITION_RANGE).stream()
                        .filter(entity -> entity instanceof Animals)
                        .map(entity -> (Animals) entity)
                        .filter(nearby -> nearby.getUniqueId().equals(motherId))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
    
    public double getBondStrength(Animals animal) {
        Long strength = bondStrengths.get(animal.getUniqueId());
        if (strength == null) return 0.0;
        return Math.max(0.0, Math.min(1.0, strength / (double) INITIAL_BOND_STRENGTH));
    }
    
    public void updateBondStrength(Animals animal, long ticksElapsed) {
        if (animal == null || !animal.isValid()) return;
        bondStrengths.computeIfPresent(animal.getUniqueId(), (id, strength) -> 
                Math.max(0, strength - (BOND_DECAY_PER_TICK * ticksElapsed)));
    }
    
    public boolean hasStrongBond(Animals animal) {
        return getBondStrength(animal) > 0.5;
    }
    
    public boolean isMotherOf(Animals potentialMother, Animals potentialChild) {
        return getMother(potentialChild)
                .map(motherId -> motherId.equals(potentialMother.getUniqueId()))
                .orElse(false);
    }
    
    public boolean isSiblingOf(Animals animal1, Animals animal2) {
        return getMother(animal1)
                .flatMap(mother1 -> getMother(animal2)
                        .map(mother2 -> mother1.equals(mother2)))
                .orElse(false);
    }
    
    public void removeRelationships(UUID animalId) {
        childToMother.remove(animalId);
        motherToChildren.remove(animalId);
        bondStrengths.remove(animalId);
        
        motherToChildren.values().forEach(children -> children.remove(animalId));
    }
}
