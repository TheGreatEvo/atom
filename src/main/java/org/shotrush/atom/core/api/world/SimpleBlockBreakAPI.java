package org.shotrush.atom.core.api.world;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.shotrush.atom.Atom;

import java.util.HashMap;
import java.util.Map;


public class SimpleBlockBreakAPI {
    
    private static final Map<Material, Double> blockSpeedModifiers = new HashMap<>();
    private static final Map<String, Double> categorySpeedModifiers = new HashMap<>();
    private static final String MODIFIER_NAME = "atom_block_break_modifier";
    
    
    public static void setBlockSpeedModifier(Material material, double modifier) {
        blockSpeedModifiers.put(material, modifier);
    }
    
    
    public static void setCategorySpeedModifier(String category, double modifier) {
        categorySpeedModifiers.put(category.toUpperCase(), modifier);
    }
    
    
    public static void applyBlockBreakSpeed(Player player, Material blockType) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) {
            Atom.getInstance().getLogger().warning("Player " + player.getName() + " has no BLOCK_BREAK_SPEED attribute!");
            return;
        }
        
        
        clearBlockBreakSpeed(player);
        
        
        double modifier = getSpeedModifier(blockType);
        
        Atom.getInstance().getLogger().info("Block " + blockType + " has speed modifier: " + modifier);
        
        
        if (modifier == 1.0) {
            Atom.getInstance().getLogger().info("No modifier needed (1.0 = normal speed)");
            return;
        }
        
        
        
        NamespacedKey key = new NamespacedKey(Atom.getInstance(), MODIFIER_NAME + "_" + blockType.name().toLowerCase());
        double modifierValue = modifier - 1.0;  
        
        AttributeModifier speedMod = new AttributeModifier(
            key,
            modifierValue,
            AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.ANY
        );
        
        attribute.addModifier(speedMod);
        
        Atom.getInstance().getLogger().info("Added modifier: " + modifierValue + " (final speed should be " + modifier + "x)");
        Atom.getInstance().getLogger().info("Attribute value after modifier: " + attribute.getValue());
    }
    
    
    public static void clearBlockBreakSpeed(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) return;
        
        
        attribute.getModifiers().stream()
            .filter(mod -> {
                NamespacedKey key = mod.getKey();
                return key != null && key.getNamespace().equals(Atom.getInstance().getName().toLowerCase());
            })
            .forEach(attribute::removeModifier);
    }
    
    
    private static double getSpeedModifier(Material material) {
        
        if (blockSpeedModifiers.containsKey(material)) {
            return blockSpeedModifiers.get(material);
        }
        
        
        String materialName = material.name();
        for (Map.Entry<String, Double> entry : categorySpeedModifiers.entrySet()) {
            if (materialName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        
        return 1.0;
    }
    
    
    public static void setBaseBlockBreakSpeed(Player player, double speed) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) return;
        
        attribute.setBaseValue(speed);
    }
    
    
    public static double getEffectiveBlockBreakSpeed(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) return 1.0;
        
        return attribute.getValue();
    }
}
