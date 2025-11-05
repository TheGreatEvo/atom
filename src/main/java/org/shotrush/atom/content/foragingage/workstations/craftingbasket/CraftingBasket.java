package org.shotrush.atom.content.foragingage.workstations.craftingbasket;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.RegionAccessor;
import org.bukkit.Sound;
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
import org.shotrush.atom.core.recipe.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@AutoRegister(priority = 33)
public class CraftingBasket extends InteractiveSurface {

    public CraftingBasket(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public CraftingBasket(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public int getMaxItems() {
        return 4;
    }

    @Override
    public boolean canPlaceItem(ItemStack item) {
        return item != null;
    }

    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        float[][] positions = {
                {-0.3f, 0.3f, -0.3f},
                {0.3f, 0.3f, -0.3f},
                {-0.3f, 0.3f, 0.3f},
                {0.3f, 0.3f, 0.3f}
        };

        if (itemCount < positions.length) {
            return new Vector3f(positions[itemCount][0], positions[itemCount][1], positions[itemCount][2]);
        }
        return new Vector3f(0, 0.2f, 0);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        ItemStack basketItem = createItemWithCustomModel(Material.STONE_BUTTON, "crafting_basket");

        spawnDisplay(display, plugin, basketItem, new Vector3f(0, 0.5f, 0), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), false, 1f, 0.2f);
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

    
    
    protected ItemStack checkRecipe() {
        RecipeManager recipeManager = Atom.getInstance().getRecipeManager();
        if (recipeManager == null) {
            return null;
        }

        List<ItemStack> items = new ArrayList<>();
        for (PlacedItem placedItem : placedItems) {
            items.add(placedItem.getItem());
        }

        ItemStack result = recipeManager.findMatch(items);
        if (result != null) {
            applyQualityInheritance(result);
        }
        return result;
    }
    
    @Override
    protected boolean onCrouchRightClick(Player player) {
        ItemStack result = checkRecipe();
        
        if (result != null) {
            int craftCount = org.shotrush.atom.core.api.player.PlayerDataAPI.getInt(player, "crafting.basket_count", 0);
            double successRate = Math.min(0.95, 0.45 + (craftCount * 0.01));
            
            if (Math.random() < successRate) {
                org.shotrush.atom.core.api.player.PlayerDataAPI.incrementInt(player, "crafting.basket_count", 0);
                
                clearAllItems();
                spawnLocation.getWorld().dropItemNaturally(spawnLocation, result);
                player.playSound(spawnLocation, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, 1.0f, 1.0f);
                
                org.shotrush.atom.core.ui.ActionBarManager.send(player, 
                    String.format("§aSuccessful craft! §7(%.0f%% success rate)", successRate * 100));
                return true;
            } else {
                clearAllItems();
                player.playSound(spawnLocation, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF, 1.0f, 0.5f);
                
                org.shotrush.atom.core.ui.ActionBarManager.send(player, 
                    String.format("§cCrafting failed! §7(%.0f%% success rate)", successRate * 100));
                return true;
            }
        } else {
            player.playSound(spawnLocation, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF, 1.0f, 0.5f);
            return false;
        }
    }

    @Override
    public boolean onInteract(Player player, boolean sneaking) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (sneaking) {
            ItemStack removed = removeLastItem();
            if (removed != null) {
                spawnLocation.getWorld().dropItemNaturally(spawnLocation, removed);
                return true;
            }
            return false;
        }

        if (hand.getType() == Material.AIR) return false;

        if (placeItem(player, hand, calculatePlacement(player, placedItems.size()), 0)) {
            hand.setAmount(hand.getAmount() - 1);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean onWrenchInteract(Player player, boolean sneaking) {
        
        return onInteract(player, sneaking);
    }

    @Override
    public String getIdentifier() {
        return "crafting_basket";
    }

    @Override
    public String getDisplayName() {
        return "§eCrafting Basket";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "§7A basket for crafting items",
                "§8Place up to 4 items"
        };
    }

    @Override
    public org.shotrush.atom.core.blocks.CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) {
            return null;
        }

        CraftingBasket basket = new CraftingBasket((Location) parsed[1], (BlockFace) parsed[2]);
        String[] parts = data.split(";");
        basket.deserializeAdditionalData(parts, 5);

        return basket;
    }
}
