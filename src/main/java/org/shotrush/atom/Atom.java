package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.shotrush.atom.content.foragingage.throwing.SpearProjectileListener;
import org.shotrush.atom.content.mobs.AnimalBehaviorNew;
import org.shotrush.atom.content.mobs.AnimalDomestication;
import org.shotrush.atom.content.mobs.MobScale;
import org.shotrush.atom.content.mobs.ai.debug.MobAIDebugCommand;
import org.shotrush.atom.content.mobs.ai.debug.VisualDebugger;
import org.shotrush.atom.content.mobs.commands.HerdCommand;
import org.shotrush.atom.core.AutoRegisterManager;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.core.blocks.CustomBlockManager;
import org.shotrush.atom.core.items.CustomItemRegistry;
import org.shotrush.atom.core.recipe.RecipeManager;
import org.shotrush.atom.core.skin.SkinListener;
import org.shotrush.atom.core.storage.DataStorage;
import org.shotrush.atom.core.util.RightClickDetector;
import org.shotrush.atom.world.RockChunkGenerator;

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
    @Getter
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        instance = this;

        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.init(this);
        org.shotrush.atom.core.api.BlockBreakSpeedAPI.initialize(this);
        
        dataStorage = new DataStorage(this);
        ageManager = new AgeManager(this, dataStorage);
        itemRegistry = new CustomItemRegistry(this);
        blockManager = new CustomBlockManager(this);
        recipeManager = new RecipeManager();
        
        AutoRegisterManager.registerAges(this, ageManager);
        AutoRegisterManager.registerItems(this, itemRegistry);
        AutoRegisterManager.registerBlocks(this, blockManager.getRegistry());
        AutoRegisterManager.registerRecipes(this, recipeManager);
        AutoRegisterManager.registerSystems(this);
        
        getServer().getPluginManager().registerEvents(new RightClickDetector(), this);
        getServer().getPluginManager().registerEvents(new SkinListener(), this);
        getServer().getPluginManager().registerEvents(new MobScale(this), this);
        
        AnimalBehaviorNew animalBehavior = new AnimalBehaviorNew(this);
        AnimalDomestication animalDomestication = new AnimalDomestication(this, animalBehavior.getHerdManager());
        getServer().getPluginManager().registerEvents(animalBehavior, this);
        getServer().getPluginManager().registerEvents(animalDomestication, this);
        
        getServer().getPluginManager().registerEvents(new SpearProjectileListener(this), this);
        
        setupCommands(animalBehavior);
        getLogger().info("Atom plugin has been enabled!");
    }
    
    private void setupCommands(AnimalBehaviorNew animalBehavior) {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        AutoRegisterManager.registerCommands(this, commandManager);
        commandManager.registerCommand(new HerdCommand(animalBehavior.getHerdManager()));
        
        VisualDebugger visualDebugger = new VisualDebugger(this);
        commandManager.registerCommand(new MobAIDebugCommand(
            visualDebugger,
            animalBehavior.getHerdManager()
        ));
    }
    public void onDisable() {
        if (blockManager != null) {
            blockManager.stopGlobalUpdate();
            blockManager.cleanupAllDisplays();
            blockManager.saveBlocks();
        }
        
        saveAllPlayerData();
        
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

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return RockChunkGenerator.INSTANCE;
    }
}
