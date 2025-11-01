package org.shotrush.atom.core.blocks;

import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public abstract class InteractiveSurface extends CustomBlock {
    protected final List<PlacedItem> placedItems = new ArrayList<>();
    
    public InteractiveSurface(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }
    
    public InteractiveSurface(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }
    
    public abstract int getMaxItems();
    public abstract boolean canPlaceItem(ItemStack item);
    public abstract Vector3f calculatePlacement(Player player, int itemCount);
    
    public boolean placeItem(ItemStack item, Vector3f position, float yaw) {
        if (placedItems.size() >= getMaxItems()) return false;
        if (!canPlaceItem(item)) return false;
        
        ItemStack singleItem = item.clone();
        singleItem.setAmount(1);
        PlacedItem placedItem = new PlacedItem(singleItem, position, yaw);
        placedItems.add(placedItem);
        spawnItemDisplay(placedItem);
        return true;
    }
    
    public boolean placeItem(Player player, ItemStack item, Vector3f position, float yaw) {
        if (placeItem(item, position, yaw)) {
            player.swingMainHand();
            return true;
        }
        return false;
    }
    
    public ItemStack removeLastItem() {
        if (placedItems.isEmpty()) return null;
        PlacedItem item = placedItems.remove(placedItems.size() - 1);
        removeItemDisplay(item);
        return item.getItem();
    }
    
    protected void spawnItemDisplay(PlacedItem item) {
        Location itemLoc = spawnLocation.clone().add(item.getPosition().x, item.getPosition().y, item.getPosition().z);
        org.bukkit.Bukkit.getRegionScheduler().run(org.shotrush.atom.Atom.getInstance(), itemLoc, task -> {
            org.bukkit.entity.ItemDisplay display = (org.bukkit.entity.ItemDisplay) itemLoc.getWorld().spawnEntity(itemLoc, org.bukkit.entity.EntityType.ITEM_DISPLAY);
            display.setItemStack(item.getItem());
            
            org.joml.AxisAngle4f rotation = getItemDisplayRotation(item);
            org.joml.Vector3f translation = getItemDisplayTranslation(item);
            org.joml.Vector3f scale = getItemDisplayScale(item);
            
            display.setTransformation(new org.bukkit.util.Transformation(
                translation,
                rotation,
                scale,
                new org.joml.AxisAngle4f()
            ));
            item.setDisplayUUID(display.getUniqueId());
        });
    }
    
    protected org.joml.AxisAngle4f getItemDisplayRotation(PlacedItem item) {
        return new org.joml.AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);
    }
    
    protected org.joml.Vector3f getItemDisplayTranslation(PlacedItem item) {
        return new org.joml.Vector3f(0, 0, 0);
    }
    
    protected org.joml.Vector3f getItemDisplayScale(PlacedItem item) {
        return new org.joml.Vector3f(0.5f, 0.5f, 0.5f);
    }
    
    protected void removeItemDisplay(PlacedItem item) {
        if (item.getDisplayUUID() != null) {
            org.bukkit.entity.Entity entity = org.bukkit.Bukkit.getEntity(item.getDisplayUUID());
            if (entity != null) entity.remove();
        }
    }
    
    public List<PlacedItem> getPlacedItems() {
        return new ArrayList<>(placedItems);
    }
    
    @Override
    protected String serializeAdditionalData() {
        StringBuilder sb = new StringBuilder();
        sb.append(placedItems.size());
        for (PlacedItem item : placedItems) {
            sb.append(";").append(item.getItem().getType().name());
            sb.append(",").append(item.getPosition().x);
            sb.append(",").append(item.getPosition().y);
            sb.append(",").append(item.getPosition().z);
            sb.append(",").append(item.getYaw());
        }
        return sb.toString();
    }
    
    public static class PlacedItem {
        private final ItemStack item;
        private final Vector3f position;
        private final float yaw;
        @Setter
        private java.util.UUID displayUUID;
        
        public PlacedItem(ItemStack item, Vector3f position, float yaw) {
            this.item = item;
            this.position = position;
            this.yaw = yaw;
        }
        
        public ItemStack getItem() { return item; }
        public Vector3f getPosition() { return position; }
        public float getYaw() { return yaw; }
        public java.util.UUID getDisplayUUID() { return displayUUID; }
    }
}
