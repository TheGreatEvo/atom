package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.shotrush.atom.content.mobs.ai.debug.MobAIDebugCommand;
import org.shotrush.atom.content.mobs.ai.debug.VisualDebugger;
import org.shotrush.atom.content.mobs.commands.HerdCommand;
import org.shotrush.atom.content.mobs.herd.HerdManager;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.core.blocks.CustomBlockManager;
import org.shotrush.atom.core.items.CustomItemRegistry;
import org.shotrush.atom.core.storage.DataStorage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Atom extends JavaPlugin {

    @Getter
    public static Atom instance;

    @Getter
    public CustomBlockManager blockManager;
    @Getter
    private CustomItemRegistry itemRegistry;
    @Getter
    private DataStorage dataStorage;
    @Getter
    private AgeManager ageManager;

    @Override
    public void onEnable() {
        instance = this;

        
        org.shotrush.atom.core.api.AtomAPI.initialize(this);
        
        
        dataStorage = org.shotrush.atom.core.api.AtomAPI.getDataStorage();
        ageManager = org.shotrush.atom.core.api.AtomAPI.getAgeManager();
        itemRegistry = org.shotrush.atom.core.api.AtomAPI.getItemRegistry();
        blockManager = org.shotrush.atom.core.api.AtomAPI.getBlockManager();
        
        
        org.shotrush.atom.core.api.AtomAPI.registerAges();
        org.shotrush.atom.core.api.AtomAPI.registerItems();
        org.shotrush.atom.core.api.AtomAPI.registerBlocks();
        org.shotrush.atom.core.api.AtomAPI.registerSystems();
        
        setupCommands();
        getLogger().info("Atom plugin has been enabled!");
    }
    
    private void setupCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        
        
        org.shotrush.atom.core.api.AtomAPI.registerCommands(commandManager);
        
        
        HerdManager herdManager = org.shotrush.atom.core.api.AtomAPI.Systems.getService("herd_manager", HerdManager.class);
        if (herdManager != null) {
            commandManager.registerCommand(new HerdCommand(herdManager));
            
            VisualDebugger visualDebugger = new VisualDebugger(this);
            commandManager.registerCommand(new MobAIDebugCommand(visualDebugger, herdManager));
        }
    }
    public void onDisable() {
        
        saveAllPlayerData();
        
        
        org.shotrush.atom.core.api.AtomAPI.shutdown();
        
        getLogger().info("Atom plugin has been disabled!");
    }
    
    private void saveAllPlayerData() {
        getLogger().info("Saving all player data...");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (org.shotrush.atom.content.systems.ThirstSystem.getInstance() != null) {
                int thirst = org.shotrush.atom.content.systems.ThirstSystem.getInstance().getThirst(player);
                org.shotrush.atom.core.api.player.PlayerDataAPI.setInt(player, "thirst.level", thirst);
            }
            
            if (org.shotrush.atom.content.systems.ItemHeatSystem.getInstance() != null) {
                for (int slot = 0; slot < 9; slot++) {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        org.shotrush.atom.content.systems.ItemHeatSystem.getInstance().saveHeatForSlot(player, slot, item);
                    }
                }
            }
            
            if (org.shotrush.atom.content.systems.PlayerTemperatureSystem.getInstance() != null) {
                double temp = org.shotrush.atom.content.systems.PlayerTemperatureSystem.getInstance().getPlayerTemperature(player);
                org.shotrush.atom.core.api.player.PlayerDataAPI.setDouble(player, "temperature.body", temp);
            }
        }
        
        getLogger().info("Player data saved for " + Bukkit.getOnlinePlayers().size() + " players");
    }
}
