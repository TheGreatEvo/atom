package org.shotrush.atom.content.mobs;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.shotrush.atom.Atom;

public class AnimalDomestication implements Listener {
    
    private final Atom plugin;
    private static final int MAX_DOMESTICATION_LEVEL = 5;
    
    public AnimalDomestication(Atom plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onAnimalBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Animals baby)) return;
        if (!(event.getMother() instanceof Animals mother)) return;
        if (!(event.getFather() instanceof Animals father)) return;
        
        int motherLevel = getDomesticationLevel(mother);
        int fatherLevel = getDomesticationLevel(father);
        
        int babyLevel = Math.min(MAX_DOMESTICATION_LEVEL, Math.max(motherLevel, fatherLevel) + 1);
        
        baby.setMetadata("domesticationLevel", new FixedMetadataValue(plugin, babyLevel));
        
        plugin.getLogger().info("Baby " + baby.getType() + " born with domestication level: " + babyLevel);
        
        if (babyLevel >= MAX_DOMESTICATION_LEVEL) {
            baby.setMetadata("fullyDomesticated", new FixedMetadataValue(plugin, true));
            plugin.getLogger().info("Baby is fully domesticated!");
        }
    }
    
    private int getDomesticationLevel(Animals animal) {
        if (animal.hasMetadata("domesticationLevel")) {
            return animal.getMetadata("domesticationLevel").getFirst().asInt();
        }
        return 0;
    }
    
    public static double getDomesticationFactor(Animals animal) {
        if (animal.hasMetadata("fullyDomesticated")) {
            return 1.0;
        }
        
        if (animal.hasMetadata("domesticationLevel")) {
            int level = animal.getMetadata("domesticationLevel").getFirst().asInt();
            return level / (double) MAX_DOMESTICATION_LEVEL;
        }
        
        return 0.0;
    }
    
    public static boolean isFullyDomesticated(Animals animal) {
        return animal.hasMetadata("fullyDomesticated");
    }
}
