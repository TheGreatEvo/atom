package org.shotrush.atom.content.foragingage.systems;

import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.foragingage.items.BoneItem;
import org.shotrush.atom.content.foragingage.items.LeatherItem;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@AutoRegisterSystem(priority = 5)
public class AnimalDropsHandler implements Listener {
    
    private final Atom plugin;
    private final Random random = new Random();
    
    public AnimalDropsHandler(Plugin plugin) {
        this.plugin = (Atom) plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        
        if (!(entity instanceof Animals)) {
            return;
        }
        
        
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        
        List<ItemStack> customDrops = generateCustomDrops(entity);
        event.getDrops().addAll(customDrops);
        
        
        event.setDroppedExp(random.nextInt(3) + 1);
    }
    
    private List<ItemStack> generateCustomDrops(LivingEntity entity) {
        List<ItemStack> drops = new ArrayList<>();
        String animalType = getAnimalDisplayName(entity.getType());
        
        
        int boneCount = getBoneCount(entity.getType());
        if (boneCount > 0) {
            ItemStack bone = plugin.getItemRegistry().createItem("bone");
            if (bone != null) {
                bone.setAmount(boneCount);
                ItemMeta meta = bone.getItemMeta();
                if (meta != null) {
                    BoneItem.setAnimalSource(meta, animalType);
                    bone.setItemMeta(meta);
                }
                drops.add(bone);
            }
        }
        
        
        int leatherCount = getLeatherCount(entity.getType());
        if (leatherCount > 0) {
            ItemStack leather = plugin.getItemRegistry().createItem("uncured_leather");
            if (leather != null) {
                leather.setAmount(leatherCount);
                ItemMeta meta = leather.getItemMeta();
                if (meta != null) {
                    LeatherItem.setAnimalSource(meta, animalType);
                    leather.setItemMeta(meta);
                }
                drops.add(leather);
            }
        }
        
        return drops;
    }
    
    private int getBoneCount(EntityType type) {
        return switch (type) {
            case COW, HORSE, DONKEY, MULE, LLAMA, CAMEL -> random.nextInt(2) + 2; 
            case PIG, SHEEP, GOAT, WOLF, FOX -> random.nextInt(2) + 1; 
            case CHICKEN, RABBIT, CAT, OCELOT -> random.nextInt(2); 
            case POLAR_BEAR, PANDA -> random.nextInt(3) + 2; 
            default -> 1;
        };
    }
    
    private int getLeatherCount(EntityType type) {
        return switch (type) {
            case COW, HORSE, DONKEY, MULE, LLAMA, CAMEL -> random.nextInt(3) + 2; 
            case PIG, SHEEP, GOAT -> random.nextInt(2) + 2; 
            case POLAR_BEAR, PANDA -> random.nextInt(4) + 3; 
            case CHICKEN, RABBIT -> random.nextInt(2) + 1; 
            case WOLF, FOX, CAT, OCELOT -> random.nextInt(2) + 1; 
            default -> 1;
        };
    }
    
    private String getAnimalDisplayName(EntityType type) {
        return switch (type) {
            case COW -> "Cow";
            case PIG -> "Pig";
            case SHEEP -> "Sheep";
            case CHICKEN -> "Chicken";
            case RABBIT -> "Rabbit";
            case HORSE -> "Horse";
            case DONKEY -> "Donkey";
            case MULE -> "Mule";
            case LLAMA -> "Llama";
            case GOAT -> "Goat";
            case CAT -> "Cat";
            case WOLF -> "Wolf";
            case FOX -> "Fox";
            case PANDA -> "Panda";
            case POLAR_BEAR -> "Polar Bear";
            case OCELOT -> "Ocelot";
            case CAMEL -> "Camel";
            default -> type.name();
        };
    }
}
