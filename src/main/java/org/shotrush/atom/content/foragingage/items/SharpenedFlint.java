package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 3)
public class SharpenedFlint extends CustomItem {
    
    public SharpenedFlint(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "sharpened_flint";
    }
    
    @Override
    public Material getMaterial() {
        return Material.FLINT;
    }
    
    @Override
    public String getDisplayName() {
        return "§fSharpened Flint";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A carefully knapped piece of flint",
            "§7Sharp enough to be useful",
            "§8• Crafting Material",
            "§8[Foraging Age Tool]"
        );
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "sharpened_flint");
    }
}
