package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.data.PersistentData;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.ArrayList;
import java.util.List;

@AutoRegister(priority = 4)
public class MeatItem extends CustomItem {
    
    public MeatItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "raw_meat";
    }
    
    @Override
    public Material getMaterial() {
        return Material.BEEF;
    }
    
    @Override
    public String getDisplayName() {
        return "§cRaw Meat";
    }
    
    @Override
    public List<String> getLore() {
        return new ArrayList<>(List.of(
            "§7Fresh meat from an animal",
            "§7Should be cooked before eating"
        ));
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        
    }
    
    public static void setAnimalSource(ItemMeta meta, String animalType) {
        PersistentData.set(meta, "animal_source", animalType);
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§8From: " + animalType);
        meta.setLore(lore);
    }
    
    public static String getAnimalSource(ItemMeta meta) {
        return PersistentData.getString(meta, "animal_source", null);
    }
}
