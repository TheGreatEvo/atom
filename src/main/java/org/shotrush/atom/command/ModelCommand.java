package org.shotrush.atom.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.display.DisplayGroup;
import org.shotrush.atom.model.DisplayModel;
import org.shotrush.atom.model.ModelManager;

import java.util.Optional;

@CommandAlias("model|models")
@Description("Manage display entity models")
public class ModelCommand extends BaseCommand {
    
    @Subcommand("create")
    @CommandPermission("atom.model.create")
    @Syntax("<id> <name> <command...>")
    @Description("Create a model from a summon command")
    public void onCreate(Player player, String id, String name, String[] commandParts) {
        String command = String.join(" ", commandParts);
        
        if (!command.startsWith("/summon")) {
            command = "/summon " + command;
        }
        
        try {
            ModelManager mm = Atom.getInstance().getModelManager();
            DisplayModel model = mm.createFromCommand(command, id, name);
            
            player.sendMessage(Component.text("✓ Model created: ", NamedTextColor.GREEN)
                .append(Component.text(model.getName(), NamedTextColor.GOLD))
                .append(Component.text(" (" + model.getParts().size() + " parts)", NamedTextColor.GRAY)));
        } catch (Exception e) {
            player.sendMessage(Component.text("✗ Failed to parse command: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    @Subcommand("import")
    @CommandPermission("atom.model.create")
    @Syntax("<id> <name> <filename>")
    @Description("Import a model from a file in plugins/Atom/import/")
    public void onImport(Player player, String id, String name, String filename) {
        try {
            ModelManager mm = Atom.getInstance().getModelManager();
            java.io.File importDir = new java.io.File(Atom.getInstance().getDataFolder(), "import");
            if (!importDir.exists()) {
                importDir.mkdirs();
            }
            
            java.io.File file = new java.io.File(importDir, filename + (filename.endsWith(".txt") ? "" : ".txt"));
            if (!file.exists()) {
                player.sendMessage(Component.text("✗ File not found: " + file.getName(), NamedTextColor.RED));
                player.sendMessage(Component.text("Expected path: " + file.getAbsolutePath(), NamedTextColor.GRAY));
                player.sendMessage(Component.text("Available files:", NamedTextColor.GRAY));
                java.io.File[] files = importDir.listFiles();
                if (files != null && files.length > 0) {
                    for (java.io.File f : files) {
                        player.sendMessage(Component.text("  - " + f.getName(), NamedTextColor.GRAY));
                    }
                } else {
                    player.sendMessage(Component.text("  (no files found)", NamedTextColor.GRAY));
                }
                return;
            }
            
            String command = java.nio.file.Files.readString(file.toPath()).trim();
            DisplayModel model = mm.createFromCommand(command, id, name);
            
            player.sendMessage(Component.text("✓ Model imported: ", NamedTextColor.GREEN)
                .append(Component.text(model.getName(), NamedTextColor.GOLD))
                .append(Component.text(" (" + model.getParts().size() + " parts)", NamedTextColor.GRAY)));
            
            for (int i = 0; i < Math.min(3, model.getParts().size()); i++) {
                DisplayModel.DisplayPart part = model.getParts().get(i);
                player.sendMessage(Component.text("Part " + i + ": " + part.getMaterial() + 
                    " [" + part.getBlockState() + "]", NamedTextColor.GRAY));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("✗ Failed to import: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    @Subcommand("spawn")
    @CommandPermission("atom.model.spawn")
    @Syntax("<id>")
    @Description("Spawn a saved model at your location and rotation")
    @CommandCompletion("@models")
    public void onSpawn(Player player, String id) {
        try {
            ModelManager mm = Atom.getInstance().getModelManager();
            Location spawnLoc = player.getLocation().clone();
            DisplayGroup group = mm.spawnModel(id, spawnLoc);
            
            player.sendMessage(Component.text("✓ Model spawned: ", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.GOLD)));
        } catch (Exception e) {
            player.sendMessage(Component.text("✗ Failed to spawn: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    @Subcommand("give")
    @CommandPermission("atom.model.give")
    @Syntax("<id> [amount]")
    @Description("Get a placeable item for a model")
    @CommandCompletion("@models")
    public void onGive(Player player, String id, @Default("1") int amount) {
        try {
            ModelManager mm = Atom.getInstance().getModelManager();
            var modelOpt = mm.loadModel(id);
            
            if (modelOpt.isEmpty()) {
                player.sendMessage(Component.text("✗ Model not found: " + id, NamedTextColor.RED));
                return;
            }
            
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD, amount);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            
            meta.displayName(Component.text(modelOpt.get().getName(), NamedTextColor.GOLD)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            
            meta.lore(java.util.List.of(
                Component.text("Model: " + id, NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Place to spawn this model", NamedTextColor.YELLOW)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
            ));
            
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(
                new org.bukkit.NamespacedKey(Atom.getInstance(), "model_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                id
            );
            
            item.setItemMeta(meta);
            player.getInventory().addItem(item);
            
            player.sendMessage(Component.text("✓ Given ", NamedTextColor.GREEN)
                .append(Component.text(amount + "x ", NamedTextColor.GOLD))
                .append(Component.text(modelOpt.get().getName(), NamedTextColor.GOLD)));
        } catch (Exception e) {
            player.sendMessage(Component.text("✗ Failed to give item: " + e.getMessage(), NamedTextColor.RED));
        }
    }
    
    @Subcommand("rotate")
    @CommandPermission("atom.model.edit")
    @Syntax("<degrees>")
    @Description("Rotate the nearest model")
    public void onRotate(Player player, float degrees) {
        try {
            var dm = Atom.getInstance().getDisplayManager();
            DisplayGroup nearest = null;
            double minDist = Double.MAX_VALUE;
            
            int totalGroups = dm.getAllGroups().size();
            player.sendMessage(Component.text("Searching " + totalGroups + " groups...", NamedTextColor.GRAY));
            
            for (DisplayGroup group : dm.getAllGroups()) {
                if (group.getRootEntity() != null && group.getRootEntity().isValid()) {
                    double dist = group.getRootEntity().getLocation().distance(player.getLocation());
                    player.sendMessage(Component.text("Found group at distance: " + String.format("%.2f", dist), NamedTextColor.GRAY));
                    
                    if (dist < minDist && dist < 10) {
                        minDist = dist;
                        nearest = group;
                    }
                }
            }
            
            if (nearest != null) {
                nearest.rotate(degrees);
                player.sendMessage(Component.text("✓ Rotated model by ", NamedTextColor.GREEN)
                    .append(Component.text(degrees + "°", NamedTextColor.GOLD)));
            } else {
                player.sendMessage(Component.text("✗ No model found within 10 blocks", NamedTextColor.RED));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("✗ Failed to rotate: " + e.getMessage(), NamedTextColor.RED));
            e.printStackTrace();
        }
    }
    
    @Subcommand("list")
    @CommandPermission("atom.model.list")
    @Description("List all saved models")
    public void onList(Player player) {
        ModelManager mm = Atom.getInstance().getModelManager();
        mm.listModels();
        
        player.sendMessage(Component.text("=== Saved Models ===", NamedTextColor.GOLD));
        
        mm.getModels().asMap().forEach((id, model) -> {
            player.sendMessage(Component.text("• ", NamedTextColor.GRAY)
                .append(Component.text(id, NamedTextColor.AQUA))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(model.getName(), NamedTextColor.WHITE))
                .append(Component.text(" (" + model.getParts().size() + " parts)", NamedTextColor.GRAY)));
        });
    }
    
    @Subcommand("info")
    @CommandPermission("atom.model.info")
    @Syntax("<id>")
    @Description("Show model information")
    @CommandCompletion("@models")
    public void onInfo(Player player, String id) {
        ModelManager mm = Atom.getInstance().getModelManager();
        Optional<DisplayModel> modelOpt = mm.loadModel(id);
        
        if (modelOpt.isEmpty()) {
            player.sendMessage(Component.text("✗ Model not found: " + id, NamedTextColor.RED));
            return;
        }
        
        DisplayModel model = modelOpt.get();
        player.sendMessage(Component.text("=== Model Info ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("ID: ", NamedTextColor.GRAY)
            .append(Component.text(model.getId(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Name: ", NamedTextColor.GRAY)
            .append(Component.text(model.getName(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Parts: ", NamedTextColor.GRAY)
            .append(Component.text(model.getParts().size(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Animated: ", NamedTextColor.GRAY)
            .append(Component.text(model.getMetadata().isAnimated() ? "Yes" : "No", 
                model.getMetadata().isAnimated() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        
        if (model.getMetadata().getDescription() != null) {
            player.sendMessage(Component.text("Description: ", NamedTextColor.GRAY)
                .append(Component.text(model.getMetadata().getDescription(), NamedTextColor.WHITE)));
        }
    }
    
    @Subcommand("delete")
    @CommandPermission("atom.model.delete")
    @Syntax("<id>")
    @Description("Delete a saved model")
    @CommandCompletion("@models")
    public void onDelete(Player player, String id) {
        ModelManager mm = Atom.getInstance().getModelManager();
        Optional<DisplayModel> modelOpt = mm.loadModel(id);
        
        if (modelOpt.isEmpty()) {
            player.sendMessage(Component.text("✗ Model not found: " + id, NamedTextColor.RED));
            return;
        }
        
        mm.deleteModel(id);
        player.sendMessage(Component.text("✓ Deleted model: ", NamedTextColor.GREEN)
            .append(Component.text(id, NamedTextColor.GOLD)));
    }
    
    @Subcommand("rotate continuous")
    @CommandPermission("atom.model.rotate")
    @Syntax("<speed> [axis]")
    @Description("Start continuous rotation")
    @CommandCompletion("45 x|y|z")
    public void onRotateContinuous(Player player, float speed, @Default("y") String axis) {
        DisplayGroup group = findTargetModel(player);
        if (group == null) {
            player.sendMessage(Component.text("✗ No model found. Look at a model!", NamedTextColor.RED));
            return;
        }
        
        if (!axis.matches("[xyzXYZ]")) {
            player.sendMessage(Component.text("✗ Invalid axis! Use x, y, or z", NamedTextColor.RED));
            return;
        }
        
        group.startContinuousRotation(speed, axis);
        player.sendMessage(Component.text("✓ Started continuous rotation: ", NamedTextColor.GREEN)
            .append(Component.text(speed + "°/s on " + axis.toUpperCase() + " axis", NamedTextColor.GOLD)));
    }
    
    @Subcommand("rotate stop")
    @CommandPermission("atom.model.rotate")
    @Description("Stop continuous rotation")
    public void onRotateStop(Player player) {
        DisplayGroup group = findTargetModel(player);
        if (group == null) {
            player.sendMessage(Component.text("✗ No model found. Look at a model!", NamedTextColor.RED));
            return;
        }
        
        group.stopContinuousRotation();
        player.sendMessage(Component.text("✓ Stopped continuous rotation", NamedTextColor.GREEN));
    }
    
    @Subcommand("rotate set")
    @CommandPermission("atom.model.rotate")
    @Syntax("<degrees> [axis]")
    @Description("Set rotation by degrees")
    @CommandCompletion("90 x|y|z")
    public void onRotateSet(Player player, float degrees, @Default("y") String axis) {
        DisplayGroup group = findTargetModel(player);
        if (group == null) {
            player.sendMessage(Component.text("✗ No model found. Look at a model!", NamedTextColor.RED));
            return;
        }
        
        if (!axis.matches("[xyzXYZ]")) {
            player.sendMessage(Component.text("✗ Invalid axis! Use x, y, or z", NamedTextColor.RED));
            return;
        }
        
        if (axis.equalsIgnoreCase("y")) {
            group.rotate(degrees);
        } else {
            group.stopContinuousRotation();
            group.startContinuousRotation(degrees * 20, axis);
            org.bukkit.Bukkit.getScheduler().runTaskLater(Atom.getInstance(), () -> {
                group.stopContinuousRotation();
            }, 1);
        }
        
        player.sendMessage(Component.text("✓ Rotated ", NamedTextColor.GREEN)
            .append(Component.text(degrees + "° on " + axis.toUpperCase() + " axis", NamedTextColor.GOLD)));
    }
    
    
    private DisplayGroup findTargetModel(Player player) {
        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            10,
            0.5,
            entity -> entity instanceof Display
        );
        
        if (result != null && result.getHitEntity() != null) {
            return Atom.getInstance().getDisplayManager().getGroupByEntity(result.getHitEntity());
        }
        
        return null;
    }
    
}
