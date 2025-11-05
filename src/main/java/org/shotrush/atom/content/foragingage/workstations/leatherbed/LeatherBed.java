package org.shotrush.atom.content.foragingage.workstations.leatherbed;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.RegionAccessor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.InteractiveSurface;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;
import org.shotrush.atom.core.items.CustomItem;

@AutoRegister(priority = 34)
public class LeatherBed extends InteractiveSurface {

    public LeatherBed(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public LeatherBed(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public int getMaxItems() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(ItemStack item) {
        if (item == null) return false;
        
        CustomItem uncuredLeather = Atom.getInstance().getItemRegistry().getItem("uncured_leather");
        return uncuredLeather != null && uncuredLeather.isCustomItem(item);
    }

    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        return new Vector3f(0f, 0.75f, 0.60f);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        ItemStack stationItem = createItemWithCustomModel(Material.STONE_BUTTON, "leather_bed");

        AxisAngle4f rotation = org.shotrush.atom.core.blocks.util.BlockRotationUtil.getInitialRotationFromFace(blockFace);
        spawnDisplay(display, plugin, stationItem, new Vector3f(0, 0.5f, 0), rotation, new Vector3f(1f, 1f, 1f), true, 0.65f, 0.75f);
    }

    @Override
    protected AxisAngle4f getItemDisplayRotation(PlacedItem item) {
        return new AxisAngle4f((float) Math.toRadians(0), 1, 0, 0);
    }

    @Override
    protected Vector3f getItemDisplayScale(PlacedItem item) {
        return new Vector3f(1.2f, 1.2f, 0.5f);
    }

    @Override
    public void update(float globalAngle) {
    }

    @Override
    protected void removeEntities() {
        
        for (PlacedItem item : placedItems) {
            removeItemDisplay(item);
            if (spawnLocation.getWorld() != null) {
                spawnLocation.getWorld().dropItemNaturally(spawnLocation, item.getItem());
            }
        }
        
        
        super.removeEntities();
    }

    

    @Override
    public boolean onWrenchInteract(Player player, boolean sneaking) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (sneaking) {
            ItemStack removed = removeLastItem();
            if (removed != null) {
                player.getInventory().addItem(removed);
                return true;
            }
            return false;
        }

        if (hand.getType() == Material.WOODEN_HOE || hand.getType() == Material.AIR) return false;
        
        if (!canPlaceItem(hand)) return false;

        if (placeItem(player, hand, calculatePlacement(player, placedItems.size()), 0)) {
            hand.setAmount(hand.getAmount() - 1);
            return true;
        }
        return false;
    }

    @Override
    public String getIdentifier() {
        return "leather_bed";
    }

    @Override
    public String getDisplayName() {
        return "§6Leather Drying Bed";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "§7Place leather to dry",
                "§8Right-click with uncured leather"
        };
    }

    @Override
    public org.shotrush.atom.core.blocks.CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) return null;
        
        LeatherBed bed = new LeatherBed((Location) parsed[1], (BlockFace) parsed[2]);
        String[] parts = data.split(";");
        bed.deserializeAdditionalData(parts, 5);
        
        return bed;
    }
}
