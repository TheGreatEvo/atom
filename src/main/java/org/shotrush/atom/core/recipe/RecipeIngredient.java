package org.shotrush.atom.core.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.ItemQuality;
import org.shotrush.atom.core.api.item.ItemQualityAPI;

import java.util.List;
import java.util.Arrays;

public class RecipeIngredient {
    
    private final Material material;
    private final String customItemId;
    private final String customModelData;
    private final List<RecipeIngredient> alternatives;
    private final ItemQuality requiredQuality;
    
    public RecipeIngredient(Material material) {
        this.material = material;
        this.customItemId = null;
        this.customModelData = null;
        this.alternatives = null;
        this.requiredQuality = null;
    }
    
    public RecipeIngredient(String customItemId) {
        this.material = null;
        this.customItemId = customItemId;
        this.customModelData = null;
        this.alternatives = null;
        this.requiredQuality = null;
    }
    
    public RecipeIngredient(String customItemId, ItemQuality requiredQuality) {
        this.material = null;
        this.customItemId = customItemId;
        this.customModelData = null;
        this.alternatives = null;
        this.requiredQuality = requiredQuality;
    }
    
    public RecipeIngredient(Material material, String customModelData) {
        this.material = material;
        this.customItemId = null;
        this.customModelData = customModelData;
        this.alternatives = null;
        this.requiredQuality = null;
    }
    
    
    private RecipeIngredient(List<RecipeIngredient> alternatives) {
        this.material = null;
        this.customItemId = null;
        this.customModelData = null;
        this.alternatives = alternatives;
        this.requiredQuality = null;
    }
    
    
    public static RecipeIngredient anyOf(RecipeIngredient... ingredients) {
        return new RecipeIngredient(Arrays.asList(ingredients));
    }
    
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        
        if (alternatives != null) {
            for (RecipeIngredient alt : alternatives) {
                if (alt.matches(item)) {
                    return true;
                }
            }
            return false;
        }
        
        if (customItemId != null) {
            CustomItem customItem = Atom.getInstance().getItemRegistry().getItem(customItemId);
            if (customItem == null || !customItem.isCustomItem(item)) {
                return false;
            }
            
            
            if (requiredQuality != null) {
                ItemQuality itemQuality = ItemQualityAPI.getQuality(item);
                return itemQuality == requiredQuality;
            }
            
            return true;
        }
        
        if (material != null && item.getType() != material) {
            return false;
        }
        
        if (customModelData != null) {
            if (!item.hasItemMeta()) {
                return false;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) {
                return false;
            }
            List<String> strings = meta.getCustomModelDataComponent().getStrings();
            return strings.contains(customModelData);
        }
        
        return material != null;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getCustomItemId() {
        return customItemId;
    }
    
    public String getCustomModelData() {
        return customModelData;
    }
}
