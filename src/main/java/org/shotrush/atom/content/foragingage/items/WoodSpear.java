package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;
import org.shotrush.atom.core.ui.ActionBarManager;

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
        meta.setItemModel(NamespacedKey.minecraft("stick"));
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "wood_spear");
    }
    
    public void damageItem(ItemStack item, Player player) {
        if (item == null || item.getAmount() <= 0) return;
        
        int currentAmount = item.getAmount();
        
        if (Math.random() < 0.5) {
            item.setAmount(currentAmount - 1);
            
            if (currentAmount - 1 <= 0) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                ActionBarManager.send(player,"§cYour Wooden Spear broke!");
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.5f, 1.2f);
            }
        }
    }
}
