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
public class LeatherItem extends CustomItem {
    
    public LeatherItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "uncured_leather";
    }
    
    @Override
    public Material getMaterial() {
        return Material.LEATHER;
    }
    
    @Override
    public String getDisplayName() {
        return "§6Uncured Leather with Meat";
    }
    
    @Override
    public List<String> getLore() {
        return new ArrayList<>(List.of(
            "§7Raw animal hide with meat attached",
            "§7Needs processing before use"
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
