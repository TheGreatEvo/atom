package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 4)
public class KnifeItem extends CustomItem {
    
    public KnifeItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "knife";
    }
    
    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }
    
    @Override
    public String getDisplayName() {
        return "§fFlint Knife";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A sharp cutting tool",
            "§7Made from high quality flint",
            "§8• Tool",
            "§8[Foraging Age Tool]"
        );
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "flint_knife");
    }
}
