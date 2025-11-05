package org.shotrush.atom.content.foragingage.workstations.knappingstation;

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
import org.shotrush.atom.core.ui.ActionBarManager;

@AutoRegister(priority = 32)
public class KnappingStation extends InteractiveSurface {

    public KnappingStation(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public KnappingStation(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public int getMaxItems() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(ItemStack item) {
        if (item == null) return false;
        
        
        if (item.getType() == Material.FLINT) return true;
        
        CustomItem sharpenedFlint = Atom.getInstance().getItemRegistry().getItem("sharpened_flint");
        return sharpenedFlint != null && sharpenedFlint.isCustomItem(item);
    }

    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        return new Vector3f(-0.2f, 1f, 0.2f);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        ItemStack stationItem = createItemWithCustomModel(Material.STONE_BUTTON, "knapping_station");

        spawnDisplay(display, plugin, stationItem, new Vector3f(0, 0.5f, 0), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), true, 0.65f, 0.75f);

    }


    @Override
    protected AxisAngle4f getItemDisplayRotation(PlacedItem item) {
        float randomYaw = (float) (Math.random() * Math.PI * 2);
        AxisAngle4f yawRotation = new AxisAngle4f(randomYaw, 0, 1, 0);

            AxisAngle4f flatRotation = new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);

            return org.shotrush.atom.core.blocks.util.BlockRotationUtil.combineRotations(yawRotation,
                    flatRotation);
    }

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

        CustomItem pebbleItem = Atom.getInstance().getItemRegistry().getItem("pebble");
        if (pebbleItem != null && pebbleItem.isCustomItem(hand)) {
            if (placedItems.isEmpty()) {
                ActionBarManager.send(player, "§cPlace flint first!");
                return true;
            }
            
            PlacedItem placedFlint = placedItems.get(0);
            if (placedFlint.getItem().getType() != Material.FLINT) {
                ActionBarManager.send(player, "§cYou can only knap regular flint with a pebble!");
                return true;
            }
            
            if (!KnappingHandler.isKnapping(player)) {
                KnappingHandler.startKnapping(player, spawnLocation, () -> removeLastItem());
            }
            
            return false;
        }
        
        CustomItem pressureFlakerItem = Atom.getInstance().getItemRegistry().getItem("pressure_flaker");
        if (pressureFlakerItem != null && pressureFlakerItem.isCustomItem(hand)) {
            if (placedItems.isEmpty()) {
                ActionBarManager.send(player, "§cPlace sharpened flint first!");
                return true;
            }
            
            PlacedItem placedFlint = placedItems.get(0);
            CustomItem sharpenedFlint = Atom.getInstance().getItemRegistry().getItem("sharpened_flint");
            
            if (sharpenedFlint == null || !sharpenedFlint.isCustomItem(placedFlint.getItem())) {
                ActionBarManager.send(player, "§cYou need sharpened flint for pressure flaking!");
                return true;
            }
            
            if (!KnappingHandler.isKnapping(player)) {
                ItemStack inputFlint = placedFlint.getItem().clone();
                KnappingHandler.startPressureFlaking(player, spawnLocation, inputFlint, () -> removeLastItem());
            }
            
            return false;
        }

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
        return "knapping_station";
    }

    @Override
    public String getDisplayName() {
        return "§6Knapping Station";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "§7Knap flint into tools",
                "§8Use brush to knap"
        };
    }

    @Override
    public org.shotrush.atom.core.blocks.CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) return null;
        
        KnappingStation station = new KnappingStation((Location) parsed[1], (BlockFace) parsed[2]);
        String[] parts = data.split(";");
        station.deserializeAdditionalData(parts, 5);
        
        return station;
    }


}
