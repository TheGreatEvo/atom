package org.shotrush.atom.content.systems;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.core.data.PersistentData;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@AutoRegisterSystem(priority = 3)
public class ItemHeatSystem implements Listener {
    @Getter
    private static ItemHeatSystem instance;
    private final Atom plugin;
    private static final NamespacedKey HEAT_MODIFIER_KEY = new NamespacedKey("atom", "heat_modifier");
    
    private static final Map<UUID, Map<Integer, Double>> playerItemHeatCache = new HashMap<>();
    private static final Map<UUID, ItemStack> lastKnownItems = new HashMap<>();
    
    public ItemHeatSystem(org.bukkit.plugin.Plugin plugin) {
        this.plugin = (Atom) plugin;
        instance = this;
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startHeatTickForPlayer(player);
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        
        Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
        if (slotHeatMap != null) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    Double cachedHeat = slotHeatMap.get(slot);
                    if (cachedHeat != null) {
                        setItemHeat(item, cachedHeat);
                    }
                }
            }
        }
        
        
        playerItemHeatCache.remove(playerId);
        lastKnownItems.remove(playerId);
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        
        int previousSlot = event.getPreviousSlot();
        ItemStack previousItem = player.getInventory().getItem(previousSlot);
        if (previousItem != null && previousItem.getType() != Material.AIR) {
            UUID playerId = player.getUniqueId();
            Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
            if (slotHeatMap != null) {
                Double cachedHeat = slotHeatMap.get(previousSlot);
                if (cachedHeat != null) {
                    setItemHeat(previousItem, cachedHeat);
                }
            }
        }
        
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item != null && item.getType() != Material.AIR) {
            applyHeatEffect(player, item);
        } else {
            removeHeatEffect(player);
        }
    }
    
    private void startHeatTickForPlayer(Player player) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(player, task -> {
            if (!player.isOnline()) {
                task.cancel();
                playerItemHeatCache.remove(player.getUniqueId());
                lastKnownItems.remove(player.getUniqueId());
                return;
            }
            
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() != Material.AIR) {
                updateItemHeatInCache(player, heldItem);
                applyHeatEffectFromCache(player, heldItem);
                displayHeatActionBarFromCache(player, heldItem);
            }
        }, 1L, 20L);
    }
    
    private void updateItemHeatInCache(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        
        ItemStack lastItem = lastKnownItems.get(playerId);
        if (lastItem == null || !item.isSimilar(lastItem)) {
            
            double heat = getItemHeat(item);
            playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>()).put(slot, heat);
            lastKnownItems.put(playerId, item.clone());
        }
        
        
        double currentHeat = playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>())
            .computeIfAbsent(slot, k -> getItemHeat(item));
        
        
        double targetTemp = 20.0; 
        org.bukkit.Location loc = player.getLocation();
        
        
        double heatFromSources = org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
            .getNearbyHeatSources(loc, 6);
        targetTemp += heatFromSources * 10;
        
        
        double heatDifference = targetTemp - currentHeat;
        double heatChange = heatDifference * 0.05; 
        double newHeat = currentHeat + heatChange;
        
        
        newHeat = Math.max(-100, Math.min(500, newHeat));
        
        
        playerItemHeatCache.get(playerId).put(slot, newHeat);
    }
    
    private void applyHeatEffectFromCache(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        Double heat = playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>())
            .get(slot);
        if (heat == null) {
            heat = getItemHeat(item);
        }
        
        boolean hasProtection = org.shotrush.atom.core.api.combat.ArmorProtectionAPI.hasLeatherChestplate(player);
        
        if (heat != 0) {
            double speedModifier = -Math.abs(heat) * 0.001;
            org.shotrush.atom.core.api.player.AttributeModifierAPI.applyModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY,
                speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
        } else {
            org.shotrush.atom.core.api.player.AttributeModifierAPI.removeModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
            );
        }
        
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyHeatDamage(player, heat, hasProtection);
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyColdDamage(player, heat, hasProtection);
    }
    
    private void displayHeatActionBarFromCache(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        Double heat = playerItemHeatCache.computeIfAbsent(playerId, k -> new HashMap<>())
            .get(slot);
        if (heat == null) {
            heat = getItemHeat(item);
        }
        
        org.shotrush.atom.core.ui.ActionBarManager manager = org.shotrush.atom.core.ui.ActionBarManager.getInstance();
        if (manager == null) return;
        
        
        if (Math.abs(heat) < 5.0) {
            manager.removeMessage(player, "item_heat");
            return;
        }
        
        String color;
        String descriptor;
        if (heat > 200) {
            color = "§4";
            descriptor = "Burning";
        } else if (heat > 100) {
            color = "§c";
            descriptor = "Very Hot";
        } else if (heat > 50) {
            color = "§6";
            descriptor = "Hot";
        } else if (heat > 20) {
            color = "§e";
            descriptor = "Warm";
        } else if (heat < -50) {
            color = "§b";
            descriptor = "Freezing";
        } else if (heat < -20) {
            color = "§3";
            descriptor = "Cold";
        } else {
            color = "§7";
            descriptor = "Cool";
        }
        
        String message = String.format("§7Item: %s%s §7(%.0f°C)", color, descriptor, heat);
        manager.setMessage(player, "item_heat", message);
    }
    
    private void displayHeatActionBar(Player player, ItemStack item) {
        double heat = getItemHeat(item);
        
        org.shotrush.atom.core.ui.ActionBarManager manager = org.shotrush.atom.core.ui.ActionBarManager.getInstance();
        if (manager == null) return;
        
        
        if (Math.abs(heat) < 5.0) {
            manager.removeMessage(player, "item_heat");
            return;
        }
        
        String color;
        String descriptor;
        if (heat > 200) {
            color = "§4";
            descriptor = "Burning";
        } else if (heat > 100) {
            color = "§c";
            descriptor = "Very Hot";
        } else if (heat > 50) {
            color = "§6";
            descriptor = "Hot";
        } else if (heat > 20) {
            color = "§e";
            descriptor = "Warm";
        } else if (heat < -50) {
            color = "§b";
            descriptor = "Freezing";
        } else if (heat < -20) {
            color = "§3";
            descriptor = "Cold";
        } else {
            color = "§7";
            descriptor = "Cool";
        }
        
        String message = String.format("§7Item: %s%s §7(%.0f°C)", color, descriptor, heat);
        manager.setMessage(player, "item_heat", message);
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItem = event.getItemDrop();
        ItemStack item = droppedItem.getItemStack();
        
        
        saveCachedHeatToItem(player, item);
        droppedItem.setItemStack(item);
        
        double heat = getItemHeat(item);
        
        if (heat >= 50) {
            ItemStack chestplate = player.getInventory().getChestplate();
            boolean hasProtection = chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE;
            
            if (!hasProtection) {
                player.setFireTicks(40); 
            }
        }
        
        startDroppedItemHeatTracking(droppedItem);
    }
    
    private void startDroppedItemHeatTracking(Item droppedItem) {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(droppedItem, task -> {
            if (droppedItem.isDead() || !droppedItem.isValid()) {
                task.cancel();
                return;
            }
            
            ItemStack itemStack = droppedItem.getItemStack();
            double currentHeat = getItemHeat(itemStack);
            org.bukkit.Location loc = droppedItem.getLocation();
            
            double heatChange = org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
                .getNearbyHeatSources(loc, 6);
            
            double ambientTemp = 20.0;
            if (currentHeat > ambientTemp) {
                heatChange -= 0.5;
            }
            
            double newHeat = Math.max(0, currentHeat + heatChange * 0.1);
            setItemHeat(itemStack, newHeat);
            droppedItem.setItemStack(itemStack);
            
            if (newHeat >= 200) {
                double fireChance = Math.min(0.5, (newHeat - 200) / 600);
                
                if (Math.random() < fireChance) {
                    org.bukkit.block.Block below = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                    if (below.getType().isBurnable() || below.getType() == Material.AIR) {
                        loc.getBlock().setType(Material.FIRE);
                    }
                }
            }
        }, 20L, 20L);
    }
    
    private void updateItemHeatFromEnvironment(Player player, ItemStack item) {
        double currentHeat = getItemHeat(item);
        org.bukkit.Location loc = player.getLocation();
        
        
        double environmentalHeat = org.shotrush.atom.core.api.world.EnvironmentalFactorAPI
            .getNearbyHeatSources(loc, 5);
        
        double ambientTemp = 20.0;
        double targetTemp = ambientTemp + (environmentalHeat * 10); 
        
        
        double heatDifference = targetTemp - currentHeat;
        double heatChange = heatDifference * 0.05; 
        
        
        double newHeat = currentHeat + heatChange;
        
        
        newHeat = Math.max(-100, Math.min(500, newHeat));
        
        
        if (Math.abs(newHeat - currentHeat) > 0.5) {
            setItemHeat(item, newHeat);
        }
    }
    
    private void applyHeatEffect(Player player, ItemStack item) {
        double heat = getItemHeat(item);
        boolean hasProtection = org.shotrush.atom.core.api.combat.ArmorProtectionAPI.hasLeatherChestplate(player);
        
        if (heat != 0) {
            double speedModifier = -Math.abs(heat) * 0.001;
            org.shotrush.atom.core.api.player.AttributeModifierAPI.applyModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY,
                speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
        } else {
            org.shotrush.atom.core.api.player.AttributeModifierAPI.removeModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
            );
        }
        
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyHeatDamage(player, heat, hasProtection);
        org.shotrush.atom.core.api.combat.TemperatureEffectsAPI.applyColdDamage(player, heat, hasProtection);
    }
    
    private void removeHeatEffect(Player player) {
        org.shotrush.atom.core.api.player.AttributeModifierAPI.removeModifier(
            player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
        );
    }
    
    public static double getItemHeat(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        
        return PersistentData.getDouble(item.getItemMeta(), "item_heat", 0.0);
    }
    
    public static void setItemHeat(ItemStack item, double heat) {
        if (item == null || item.getType() == Material.AIR) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentData.set(meta, "item_heat", heat);
        item.setItemMeta(meta);
    }
    
    
    private void saveCachedHeatToItem(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int slot = player.getInventory().getHeldItemSlot();
        
        Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
        if (slotHeatMap != null) {
            Double cachedHeat = slotHeatMap.get(slot);
            if (cachedHeat != null) {
                setItemHeat(item, cachedHeat);
            }
        }
    }
    
    public void saveHeatForSlot(Player player, int slot, ItemStack item) {
        UUID playerId = player.getUniqueId();
        Map<Integer, Double> slotHeatMap = playerItemHeatCache.get(playerId);
        if (slotHeatMap != null) {
            Double cachedHeat = slotHeatMap.get(slot);
            if (cachedHeat != null) {
                setItemHeat(item, cachedHeat);
            }
        }
    }
}
