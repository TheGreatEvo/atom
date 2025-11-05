package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.data.PersistentData;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@AutoRegister(priority = 4)
public class BoneItem extends CustomItem {
    
    public BoneItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "bone";
    }
    
    @Override
    public Material getMaterial() {
        return Material.BONE;
    }
    
    @Override
    public String getDisplayName() {
        return "§fBone";
    }
    
    @Override
    public List<String> getLore() {
        return new ArrayList<>(List.of(
            "§7A sturdy bone from an animal",
            "§7Can be used for crafting tools"
        ));
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        
    }
    
    public static void setAnimalSource(ItemMeta meta, String animalType) {
        PersistentData.set(meta, "animal_source", animalType);
        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        lore.add("§8From: " + animalType);
        meta.setLore(lore);
    }
    
    public static String getAnimalSource(ItemMeta meta) {
        return PersistentData.getString(meta, "animal_source", null);
    }
}
