package org.shotrush.atom.display;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DisplayEntityManager {
    private final JavaPlugin plugin;
    @Getter private final Cache<UUID, DisplayGroup> displayGroups;
    @Getter private final Cache<UUID, AnimationState> animations;
    
    public DisplayEntityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.displayGroups = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();
        this.animations = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();
    }
    
    public void initialize() {
        Atom.getInstance().getSchedulerManager().runGlobalTimer(() -> {
            animations.asMap().forEach((uuid, state) -> {
                if (state.isActive()) {
                    state.tick();
                }
            });
        }, 1, 1);
    }
    
    public DisplayGroup createDisplayGroup(Location location) {
        DisplayGroup group = new DisplayGroup(location);
        displayGroups.put(group.getId(), group);
        return group;
    }
    
    public Collection<DisplayGroup> getAllGroups() {
        return displayGroups.asMap().values();
    }
    
    public DisplayGroup getGroupByEntity(Entity entity) {
        for (DisplayGroup group : displayGroups.asMap().values()) {
            if (group.getRootEntity() != null && group.getRootEntity().equals(entity)) {
                return group;
            }
            for (Display display : group.getDisplays()) {
                if (display.equals(entity)) {
                    return group;
                }
            }
        }
        return null;
    }
    
    public BlockDisplay createBlockDisplay(Location location, org.bukkit.Material material) {
        BlockDisplay display = (BlockDisplay) location.getWorld().spawnEntity(location, EntityType.BLOCK_DISPLAY);
        display.setBlock(material.createBlockData());
        return display;
    }
    
    public ItemDisplay createItemDisplay(Location location, org.bukkit.inventory.ItemStack item) {
        ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        display.setItemStack(item);
        return display;
    }
    
    public TextDisplay createTextDisplay(Location location, String text) {
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.setText(text);
        return display;
    }
    
    public AnimationBuilder animate(Display display) {
        return new AnimationBuilder(display, this);
    }
    
    public void interpolate(Display display, Transformation target, int durationTicks) {
        display.setInterpolationDuration(durationTicks);
        display.setInterpolationDelay(0);
        display.setTransformation(target);
    }
    
    public void rotate(Display display, float pitch, float yaw, float roll, int durationTicks) {
        Transformation current = display.getTransformation();
        Quaternionf rotation = new Quaternionf().rotateXYZ(
            (float) Math.toRadians(pitch),
            (float) Math.toRadians(yaw),
            (float) Math.toRadians(roll)
        );
        Transformation target = new Transformation(
            current.getTranslation(),
            rotation,
            current.getScale(),
            current.getRightRotation()
        );
        interpolate(display, target, durationTicks);
    }
    
    public void scale(Display display, float x, float y, float z, int durationTicks) {
        Transformation current = display.getTransformation();
        Transformation target = new Transformation(
            current.getTranslation(),
            current.getLeftRotation(),
            new Vector3f(x, y, z),
            current.getRightRotation()
        );
        interpolate(display, target, durationTicks);
    }
    
    public void translate(Display display, float x, float y, float z, int durationTicks) {
        Transformation current = display.getTransformation();
        Transformation target = new Transformation(
            new Vector3f(x, y, z),
            current.getLeftRotation(),
            current.getScale(),
            current.getRightRotation()
        );
        interpolate(display, target, durationTicks);
    }
    
    public void stretch(Display display, float factor, int durationTicks) {
        scale(display, factor, factor, factor, durationTicks);
    }
    
    public void shutdown() {
        displayGroups.asMap().values().forEach(group -> {
            if (group.getRootEntity() != null && group.getRootEntity().isValid()) {
                Atom.getInstance().getSchedulerManager().runForEntity(group.getRootEntity(), group::remove);
            }
        });
        displayGroups.invalidateAll();
        animations.invalidateAll();
    }
}
