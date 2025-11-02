package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 2)
public class WoodSpear extends CustomItem {
    
    public WoodSpear(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "wood_spear";
    }
    
    @Override
    public Material getMaterial() {
        return Material.TRIDENT;
    }
    
    @Override
    public String getDisplayName() {
        return "§6Wooden Spear";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A simple wooden spear",
            "§7for hunting and combat",
            "§8• Throwable weapon",
            "§8[Foraging Age Tool]"
        );
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "wood_spear");
    }
}
