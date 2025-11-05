package org.shotrush.atom.content.foragingage.workstations.knappingstation;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.api.item.ItemQualityAPI;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.ItemQuality;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;
import org.shotrush.atom.core.ui.ActionBarManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoRegisterSystem(priority = 5)
public class KnappingHandler implements Listener {
    
    private static final Map<UUID, KnappingProgress> activeKnapping = new HashMap<>();
    private static final Map<UUID, Boolean> activeDetectionTasks = new HashMap<>();
    private final Atom plugin;
    
    public KnappingHandler(Plugin plugin) {
        this.plugin = (Atom) plugin;
    }
    
    @EventHandler
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        
        if (newItem == null) {
            return;
        }
        
        CustomItem pebble = Atom.getInstance().getItemRegistry().getItem("pebble");
        CustomItem pressureFlaker = Atom.getInstance().getItemRegistry().getItem("pressure_flaker");
        
        boolean isPebble = pebble != null && pebble.isCustomItem(newItem);
        boolean isPressureFl = pressureFlaker != null && pressureFlaker.isCustomItem(newItem);
        
        if ((isPebble || isPressureFl) && !activeDetectionTasks.containsKey(player.getUniqueId())) {
            startStrikeDetectionForPlayer(player);
        }
    }
    
    private void startStrikeDetectionForPlayer(Player player) {
        activeDetectionTasks.put(player.getUniqueId(), true);
        
        class StrikeDetectionTask implements Runnable {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    activeDetectionTasks.remove(player.getUniqueId());
                    return;
                }
                
                if (!isKnapping(player)) {
                    org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(player, () -> run(), 1L);
                    return;
                }
                
                KnappingProgress progress = activeKnapping.get(player.getUniqueId());
                
                if (player.hasActiveItem()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    CustomItem pebble = Atom.getInstance().getItemRegistry().getItem("pebble");
                    CustomItem pressureFlaker = Atom.getInstance().getItemRegistry().getItem("pressure_flaker");
                    
                    boolean isPebble = pebble != null && pebble.isCustomItem(item);
                    boolean isPressureFl = pressureFlaker != null && pressureFlaker.isCustomItem(item);
                    
                    if ((isPebble || isPressureFl) && player.getActiveItemUsedTime() >= 10) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - progress.lastStrikeTime > 300) {
                            progress.currentStrikes++;
                            progress.lastStrikeTime = currentTime;
                            player.playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 1.0f, 0.8f + (float)(Math.random() * 0.4f));

                            if (progress.stationLocation != null) {
                                spawnStrikeParticles(progress.stationLocation);
                            }
                        }
                    }
                }
                
                org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(player, () -> run(), 1L);
            }
        }
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(player, () -> new StrikeDetectionTask().run());
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        activeDetectionTasks.remove(event.getPlayer().getUniqueId());
    }
    
    private static void spawnStrikeParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location particleLoc = location.clone().add(0, 1, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(
            org.bukkit.Color.fromRGB(128, 128, 128), 
            1.0f
        );
        world.spawnParticle(Particle.DUST, particleLoc, 10, 0.2, 0.2, 0.2, 0, dustOptions);
    }
    
    private static class KnappingProgress {
        long startTime;
        int requiredStrikes;
        int currentStrikes = 0;
        long lastStrikeTime = 0;
        boolean isPressureFlaking = false;
        ItemStack inputFlint = null;
        Location stationLocation;
        
        KnappingProgress(long startTime, boolean isPressureFlaking, ItemStack inputFlint, Location stationLocation) {
            this.startTime = startTime;
            this.isPressureFlaking = isPressureFlaking;
            this.inputFlint = inputFlint;
            this.stationLocation = stationLocation;
            
            if (isPressureFlaking) {
                this.requiredStrikes = 25 + (int)(Math.random() * 11);
            } else {
                this.requiredStrikes = 15 + (int)(Math.random() * 11);
            }
            this.lastStrikeTime = startTime;
        }
    }
    
    public static void startKnapping(Player player, Location dropLocation, Runnable onComplete) {
        startKnapping(player, dropLocation, onComplete, false, null);
    }
    
    public static void startPressureFlaking(Player player, Location dropLocation, ItemStack inputFlint, Runnable onComplete) {
        startKnapping(player, dropLocation, onComplete, true, inputFlint);
    }
    
    private static void startKnapping(Player player, Location dropLocation, Runnable onComplete, boolean isPressureFlaking, ItemStack inputFlint) {
        UUID playerId = player.getUniqueId();
        
        if (activeKnapping.containsKey(playerId)) {
            return;
        }
        
        class KnappingTask implements Runnable {
            @Override
            public void run() {
                if (!player.isOnline() || !activeKnapping.containsKey(playerId)) {
                    activeKnapping.remove(playerId);
                    return;
                }
                
                KnappingProgress progress = activeKnapping.get(playerId);
                
                if (player.getLocation().distance(dropLocation) > 5.0) {
                    player.setLevel(0);
                    player.setExp(0);
                    activeKnapping.remove(playerId);
                    ActionBarManager.send(player, "§cYou moved too far away!");
                    return;
                }
                
                if (progress.currentStrikes >= progress.requiredStrikes) {
                    if (progress.isPressureFlaking) {
                        finishPressureFlaking(player, dropLocation, progress.inputFlint, onComplete);
                    } else {
                        finishKnapping(player, dropLocation, onComplete);
                    }
                    player.setLevel(0);
                    player.setExp(0);
                    activeKnapping.remove(playerId);
                    return;
                }
                
                float progressPercent = (float) progress.currentStrikes / progress.requiredStrikes;
                player.setLevel(progress.currentStrikes);
                player.setExp(progressPercent);
                
                org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(player, () -> run(), 1L);
            }
        }
        
        activeKnapping.put(playerId, new KnappingProgress(System.currentTimeMillis(), isPressureFlaking, inputFlint, dropLocation));
        
        player.setLevel(0);
        player.setExp(0);
        ActionBarManager.sendStatus(player, isPressureFlaking ? 
            "§7Pressure flaking... Carefully work the flint" : 
            "§7Knapping... Strike the flint");
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(player, () -> new KnappingTask().run());
    }
    
    private static void finishKnapping(Player player, Location dropLocation, Runnable onComplete) {
        int knapCount = org.shotrush.atom.core.api.player.PlayerDataAPI.getInt(player, "knapping.count", 0);
        
        double failChance = Math.max(0.1, 0.5 - (knapCount * 0.02));
        
        if (Math.random() < failChance) {
            ActionBarManager.send(player, "§cThe flint broke!");
            ActionBarManager.clearStatus(player);
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            onComplete.run();
        } else {
            ActionBarManager.send(player, "§aSuccessfully sharpened the flint!");
            ActionBarManager.clearStatus(player);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
            onComplete.run();
            
            ItemStack sharpenedFlint = Atom.getInstance().getItemRegistry().createItem("sharpened_flint");
            if (sharpenedFlint != null) {
                double temperature = org.shotrush.atom.content.systems.PlayerTemperatureSystem
                    .getInstance().getPlayerTemperature(player);
                
                org.shotrush.atom.core.items.ItemQuality quality = 
                    org.shotrush.atom.core.items.ItemQuality.fromTemperature(temperature);
                
                org.shotrush.atom.core.api.item.ItemQualityAPI.setQuality(sharpenedFlint, quality);
                
                player.getWorld().dropItemNaturally(dropLocation, sharpenedFlint);
            }
        }
        
        org.shotrush.atom.core.api.player.PlayerDataAPI.incrementInt(player, "knapping.count", 0);
    }
    
    public static boolean isKnapping(Player player) {
        return activeKnapping.containsKey(player.getUniqueId());
    }
    
    private static void finishPressureFlaking(Player player, Location dropLocation, ItemStack inputFlint, Runnable onComplete) {
        int knapCount = org.shotrush.atom.core.api.player.PlayerDataAPI.getInt(player, "pressure_flaking.count", 0);
        
        
        double failChance = Math.max(0.2, 0.7 - (knapCount * 0.03));
        
        
        double itemHeat = org.shotrush.atom.content.systems.ItemHeatSystem.getItemHeat(inputFlint);
        boolean hasHeatBonus = itemHeat >= 50 && itemHeat <= 150;
        if (hasHeatBonus) {
            failChance *= 0.8; 
        }
        
        if (Math.random() < failChance) {
            ActionBarManager.send(player, "§cThe flint shattered during pressure flaking!");
            ActionBarManager.clearStatus(player);
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            onComplete.run();
        } else {
            ActionBarManager.send(player, "§aSuccessfully created high quality sharpened flint!");
            ActionBarManager.clearStatus(player);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.8f);
            onComplete.run();
            
            ItemStack sharpenedFlint = Atom.getInstance().getItemRegistry().createItem("sharpened_flint");
            if (sharpenedFlint != null) {
                
                ItemQualityAPI.setQuality(sharpenedFlint, ItemQuality.HIGH);
                
                
                if (hasHeatBonus) {
                    ActionBarManager.send(player, "§6The heat treatment improved the quality!", 4);
                }
                
                player.getWorld().dropItemNaturally(dropLocation, sharpenedFlint);
            }
        }
        
        org.shotrush.atom.core.api.player.PlayerDataAPI.incrementInt(player, "pressure_flaking.count", 0);
    }
    
    public static void cancelKnapping(Player player) {
        UUID playerId = player.getUniqueId();
        if (activeKnapping.containsKey(playerId)) {
            player.setLevel(0);
            player.setExp(0);
            activeKnapping.remove(playerId);
        }
    }
}
