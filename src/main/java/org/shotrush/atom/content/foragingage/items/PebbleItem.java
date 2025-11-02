package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 4)
public class PebbleItem extends CustomItem {
    
    public PebbleItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "pebble";
    }
    
    @Override
    public Material getMaterial() {
        return Material.BRUSH;
    }
    
    @Override
    public String getDisplayName() {
        return "§7Pebble";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A small stone pebble",
            "§7Can be used for knapping",
            "§8• Tool",
            "§8[Foraging Age Tool]"
        );
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "pebble");
    }
}
