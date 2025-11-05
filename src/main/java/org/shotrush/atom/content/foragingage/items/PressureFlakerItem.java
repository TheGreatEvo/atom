package org.shotrush.atom.content.foragingage.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 4)
public class PressureFlakerItem extends CustomItem {
    
    public PressureFlakerItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "pressure_flaker";
    }
    
    @Override
    public Material getMaterial() {
        return Material.IRON_NUGGET;
    }
    
    @Override
    public String getDisplayName() {
        return "§fPressure Flaker";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A bone tool for precision knapping",
            "§7Used to create high quality flint",
            "§8• Tool",
            "§8[Foraging Age Tool]"
        );
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "bone");
    }
    
    @Override
    public org.bukkit.inventory.ItemStack create() {
        org.bukkit.inventory.ItemStack item = super.create();
        
        
        Consumable consumable = Consumable.consumable()
            .consumeSeconds(10000.0f)
            .animation(ItemUseAnimation.BOW)
            .hasConsumeParticles(false)
            .build();
        item.setData(DataComponentTypes.CONSUMABLE, consumable);
        
        return item;
    }
}
