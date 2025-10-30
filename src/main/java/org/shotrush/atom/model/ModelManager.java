package org.shotrush.atom.model;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.DataManager;
import org.shotrush.atom.display.DisplayGroup;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ModelManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    @Getter private final Cache<String, DisplayModel> models;
    private final File modelsDir;
    private final java.util.Map<java.util.UUID, PlacedModel> placedModels = new java.util.HashMap<>();
    
    public ModelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = Atom.getInstance().getDataManager();
        this.models = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();
        this.modelsDir = new File(plugin.getDataFolder(), "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
    }
    
    public void loadPlacedModelsDelayed() {
        loadPlacedModels();
    }
    
    public DisplayModel createFromCommand(String command, String modelId, String modelName) {
        DisplayModel model = ModelParser.parseFromCommand(command, modelId, modelName);
        saveModel(model);
        models.put(modelId, model);
        return model;
    }
    
    public void saveModel(DisplayModel model) {
        String fileName = "models/" + model.getId() + ".json";
        dataManager.save(fileName, model);
    }
    
    public Optional<DisplayModel> loadModel(String modelId) {
        DisplayModel cached = models.getIfPresent(modelId);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        String fileName = "models/" + modelId + ".json";
        DisplayModel loaded = dataManager.load(fileName, DisplayModel.class);
        if (loaded != null) {
            models.put(modelId, loaded);
            return Optional.of(loaded);
        }
        
        return Optional.empty();
    }
    
    public DisplayGroup spawnModel(String modelId, Location location) {
        Optional<DisplayModel> modelOpt = loadModel(modelId);
        if (modelOpt.isEmpty()) {
            return null;
        }
        
        DisplayModel model = modelOpt.get();
        return spawnModel(model, location);
    }
    
    public DisplayGroup spawnModel(DisplayModel model, Location location) {
        var dm = Atom.getInstance().getDisplayManager();
        
        DisplayGroup group = dm.createDisplayGroup(location);
        
        Location rootLoc = location.clone();
        
        BlockDisplay root = (BlockDisplay) location.getWorld()
            .spawnEntity(rootLoc, org.bukkit.entity.EntityType.BLOCK_DISPLAY);
        root.setBlock(org.bukkit.Material.AIR.createBlockData());
        
        Location spawnLoc = location.clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);
        
        for (DisplayModel.DisplayPart part : model.getParts()) {
            org.bukkit.entity.Display display;
            
            if ("item_display".equals(part.getType())) {
                org.bukkit.entity.ItemDisplay itemDisplay = (org.bukkit.entity.ItemDisplay) location.getWorld()
                    .spawnEntity(spawnLoc, org.bukkit.entity.EntityType.ITEM_DISPLAY);
                
                org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(part.getMaterial());
                
                if (part.getItemNbt() != null && !part.getItemNbt().isEmpty()) {
                    try {
                        if (part.getMaterial() == org.bukkit.Material.PLAYER_HEAD) {
                            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
                            
                            java.util.regex.Pattern valuePattern = java.util.regex.Pattern.compile("value:\"([^\"]+)\"");
                            java.util.regex.Matcher valueMatcher = valuePattern.matcher(part.getItemNbt());
                            
                            if (valueMatcher.find()) {
                                String textureValue = valueMatcher.group(1);
                                plugin.getLogger().info("Found texture value (first 50 chars): " + textureValue.substring(0, Math.min(50, textureValue.length())));
                                
                                try {
                                    String decoded = new String(java.util.Base64.getDecoder().decode(textureValue));
                                    plugin.getLogger().info("Decoded texture JSON: " + decoded);
                                    
                                    java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
                                    java.util.regex.Matcher urlMatcher = urlPattern.matcher(decoded);
                                    
                                    if (urlMatcher.find()) {
                                        String skinUrl = urlMatcher.group(1);
                                        plugin.getLogger().info("Extracted skin URL: " + skinUrl);
                                        
                                        org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(java.util.UUID.randomUUID(), "CustomHead");
                                        org.bukkit.profile.PlayerTextures textures = profile.getTextures();
                                        textures.setSkin(new java.net.URL(skinUrl));
                                        profile.setTextures(textures);
                                        
                                        skullMeta.setOwnerProfile(profile);
                                        item.setItemMeta(skullMeta);
                                        plugin.getLogger().info("Successfully applied player head texture!");
                                    } else {
                                        plugin.getLogger().warning("Could not find URL in decoded texture JSON");
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to decode/apply texture: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                plugin.getLogger().warning("Could not find texture value in item NBT");
                            }
                        }
                        
                        java.util.regex.Pattern cmdPattern = java.util.regex.Pattern.compile("custom_model_data:(\\d+)");
                        java.util.regex.Matcher cmdMatcher = cmdPattern.matcher(part.getItemNbt());
                        if (cmdMatcher.find()) {
                            int customModelData = Integer.parseInt(cmdMatcher.group(1));
                            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                            meta.setCustomModelData(customModelData);
                            item.setItemMeta(meta);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to parse item components: " + e.getMessage());
                    }
                }
                
                itemDisplay.setItemStack(item);
                display = itemDisplay;
            } else {
                BlockDisplay blockDisplay = (BlockDisplay) location.getWorld()
                    .spawnEntity(spawnLoc, org.bukkit.entity.EntityType.BLOCK_DISPLAY);
                
                org.bukkit.block.data.BlockData blockData;
                if (part.getBlockState() != null && !part.getBlockState().isEmpty()) {
                    try {
                        String fullState = "minecraft:" + part.getMaterial().getKey().getKey() + "[" + part.getBlockState() + "]";
                        blockData = org.bukkit.Bukkit.createBlockData(fullState);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to apply block state for " + part.getMaterial() + ": " + e.getMessage());
                        blockData = part.getMaterial().createBlockData();
                    }
                } else {
                    blockData = part.getMaterial().createBlockData();
                }
                blockDisplay.setBlock(blockData);
                display = blockDisplay;
            }
            
            float[] m = part.getTransform().getMatrix();
            org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                m[0], m[4], m[8], m[12],
                m[1], m[5], m[9], m[13],
                m[2], m[6], m[10], m[14],
                m[3], m[7], m[11], m[15]
            );
            
            display.setTransformationMatrix(matrix);
            display.setInterpolationDuration(1);
            
            if (part.getTags() != null) {
                for (String tag : part.getTags()) {
                    display.addScoreboardTag(tag);
                }
            }
            
            group.addDisplay(display);
            
        }
        
        group.setRoot(root);
        
        
        plugin.getLogger().info("✓ Spawned model " + model.getId() + " with " + group.getDisplays().size() + " displays");
        
        PlacedModel placed = new PlacedModel(model.getId(), location, group.getId());
        placedModels.put(group.getId(), placed);
        savePlacedModels();
        
        return group;
    }
    
    public void deleteModel(String modelId) {
        models.invalidate(modelId);
        String fileName = "models/" + modelId + ".json";
        dataManager.delete(fileName);
    }
    
    public void listModels() {
        File[] files = modelsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String modelId = file.getName().replace(".json", "");
                plugin.getLogger().info("  - " + modelId);
            }
        }
    }
    
    private void savePlacedModels() {
        dataManager.save("placed_models.json", new java.util.ArrayList<>(placedModels.values()));
    }
    
    private void loadPlacedModels() {
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "placed_models.json");
            if (!file.exists()) return;
            
            java.io.Reader reader = new java.io.FileReader(file);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.List<PlacedModel> loaded = gson.fromJson(reader, 
                new com.google.gson.reflect.TypeToken<java.util.List<PlacedModel>>(){}.getType());
            reader.close();
            
            if (loaded != null && !loaded.isEmpty()) {
                plugin.getLogger().info("Loading " + loaded.size() + " placed models...");
                int successCount = 0;
                for (PlacedModel placed : loaded) {
                    try {
                        Location loc = placed.toLocation();
                        if (loc != null && loc.getWorld() != null) {
                            DisplayGroup group = spawnModel(placed.getModelId(), loc);
                            if (group != null) {
                                placedModels.put(group.getId(), placed);
                                successCount++;
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load model " + placed.getModelId() + ": " + e.getMessage());
                    }
                }
                plugin.getLogger().info("Successfully loaded " + successCount + "/" + loaded.size() + " placed models");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load placed models file: " + e.getMessage());
        }
    }
    
    public void removePlacedModel(java.util.UUID groupId) {
        placedModels.remove(groupId);
        savePlacedModels();
    }
    
    public PlacedModel getPlacedModel(java.util.UUID groupId) {
        return placedModels.get(groupId);
    }
}
