package org.shotrush.atom.content.systems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

@AutoRegisterSystem(priority = 2)
public class ThirstSystem implements Listener {
    
    private final Plugin plugin;
    private final Map<UUID, Integer> thirstLevels = new HashMap<>();
    private final Map<UUID, Long> thirstAccelerationEnd = new HashMap<>();
    
    private static final int MAX_THIRST = 20;
    private static final int THIRST_DECREASE_INTERVAL = 600;
    
    public ThirstSystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        org.bukkit.configuration.file.YamlConfiguration config = 
            org.shotrush.atom.Atom.getInstance().getDataStorage().getPlayerData(playerId);
        int savedThirst = config.getInt("thirst.level", MAX_THIRST);
        
        thirstLevels.put(playerId, savedThirst);
        updateThirstDisplay(player);
        startThirstTickForPlayer(player);
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        int thirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        
        org.bukkit.configuration.file.YamlConfiguration config = 
            org.shotrush.atom.Atom.getInstance().getDataStorage().getPlayerData(playerId);
        config.set("thirst.level", thirst);
        org.shotrush.atom.Atom.getInstance().getDataStorage().savePlayerData(playerId, config);
        
        thirstLevels.remove(playerId);
        thirstAccelerationEnd.remove(playerId);
    }
    
    private void startThirstTickForPlayer(Player player) {
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            
            UUID playerId = player.getUniqueId();
            int currentThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
            
            int decreaseAmount = 1;
            
            Long accelerationEnd = thirstAccelerationEnd.get(playerId);
            if (accelerationEnd != null && System.currentTimeMillis() < accelerationEnd) {
                decreaseAmount = 2;
            } else {
                thirstAccelerationEnd.remove(playerId);
            }
            
            if (currentThirst > 0) {
                currentThirst -= decreaseAmount;
                thirstLevels.put(playerId, Math.max(0, currentThirst));
            }
            
            if (currentThirst <= 0) {
                player.damage(1.0);
            } else if (currentThirst <= 5) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 0, false, false
                ));
            }
            
            checkWaterPurification(player);
            
            updateThirstDisplay(player);
        }, null, THIRST_DECREASE_INTERVAL, THIRST_DECREASE_INTERVAL);
    }
    
    @EventHandler
    public void onEntityAirChange(org.bukkit.event.entity.EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        int thirst = thirstLevels.getOrDefault(player.getUniqueId(), MAX_THIRST);
        int targetAir = (thirst * 300) / MAX_THIRST;
        
        if (player.isInWater() || player.getEyeLocation().getBlock().getType() == Material.WATER) {
            if (event.getAmount() > targetAir) {
                event.setAmount(targetAir);
            }
        } else {
            int currentAir = player.getRemainingAir();
            if (event.getAmount() != targetAir && currentAir != targetAir) {
                event.setAmount(Math.min(299, targetAir));
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() == Material.AIR || item.getType() == Material.BUCKET) {
            org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock != null && targetBlock.getType() == Material.WATER) {
                drinkRawWater(player);
                event.setCancelled(true);
                return;
            }
            
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.WATER) {
                drinkRawWater(player);
                event.setCancelled(true);
                return;
            }
        }
        
        if (item.getType() == Material.POTION) {
            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            if (meta != null && meta.hasCustomEffect(PotionEffectType.REGENERATION)) {
                drinkPurifiedWater(player);
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            }
        }
    }
    
    private void drinkRawWater(Player player) {
        UUID playerId = player.getUniqueId();
        
        addThirst(player, 5);
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0, false, false));
        
        thirstAccelerationEnd.put(playerId, System.currentTimeMillis() + 30000);
    }
    
    private void drinkPurifiedWater(Player player) {
        addThirst(player, 10);
    }
    
    private void checkWaterPurification(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.POTION) return;
        
        double heat = ItemHeatSystem.getItemHeat(item);
        if (heat < 100.0) return;
        
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null && !meta.hasCustomEffect(PotionEffectType.REGENERATION)) {
            org.shotrush.atom.core.items.CustomItem purifiedWater = 
                org.shotrush.atom.Atom.getInstance().getItemRegistry().getItem("purified_water");
            if (purifiedWater != null) {
                ItemStack newItem = purifiedWater.create();
                ItemHeatSystem.setItemHeat(newItem, heat);
                player.getInventory().setItemInMainHand(newItem);
            }
        }
    }
    
    private void updateThirstDisplay(Player player) {
        int thirst = thirstLevels.getOrDefault(player.getUniqueId(), MAX_THIRST);
        
        player.setMaximumAir(300);
        
        if (player.isInWater() || player.getEyeLocation().getBlock().getType() == Material.WATER) {
            player.setRemainingAir(Math.min(300, (thirst * 300) / MAX_THIRST));
        } else {
            int airValue = (thirst * 300) / MAX_THIRST;
            player.setRemainingAir(Math.min(299, airValue));
        }
    }
    
    public void addThirst(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        int currentThirst = thirstLevels.getOrDefault(playerId, MAX_THIRST);
        int newThirst = Math.min(currentThirst + amount, MAX_THIRST);
        thirstLevels.put(playerId, newThirst);
        updateThirstDisplay(player);
    }
    
    public int getThirst(Player player) {
        return thirstLevels.getOrDefault(player.getUniqueId(), MAX_THIRST);
    }
}
