package org.shotrush.atom.model;

import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

@Data
public class PlacedModel {
    private String modelId;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private UUID groupId;
    
    public PlacedModel() {}
    
    public PlacedModel(String modelId, Location location, UUID groupId) {
        this.modelId = modelId;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.groupId = groupId;
    }
    
    public org.bukkit.inventory.ItemStack createItemStack() {
        var modelOpt = org.shotrush.atom.Atom.getInstance().getModelManager().loadModel(modelId);
        String modelName = modelOpt.map(org.shotrush.atom.model.DisplayModel::getName).orElse(modelId);
        
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(modelName, net.kyori.adventure.text.format.NamedTextColor.GOLD)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            
            meta.lore(java.util.List.of(
                net.kyori.adventure.text.Component.text("Model: " + modelId, net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.text.Component.text("Place to spawn this model", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
            ));
            
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(org.shotrush.atom.Atom.getInstance(), "model_id");
            meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, modelId);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public Location toLocation() {
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
