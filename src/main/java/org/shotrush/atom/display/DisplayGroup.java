package org.shotrush.atom.display;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.util.Transformation;
import java.util.*;

@Getter
public class DisplayGroup {
    private final UUID id;
    private final Location origin;
    private final List<Display> displays;
    private Display rootEntity;
    private float currentYaw = 0f;
    private boolean isRotating = false;
    private String rotationAxis = "y";
    private float rotationSpeed = 0f;
    
    public DisplayGroup(Location origin) {
        this.id = UUID.randomUUID();
        this.origin = origin;
        this.displays = new ArrayList<>();

    }
    
    public void addDisplay(Display display) {
        displays.add(display);
        if (rootEntity != null) {
            rootEntity.addPassenger(display);
        }
    }
    
    public void setRoot(Display root) {
        this.rootEntity = root;
        displays.forEach(root::addPassenger);
    }
    
    public void teleport(Location location) {
        if (rootEntity != null) {
            rootEntity.teleport(location);
        } else {
            Location offset = location.clone().subtract(origin);
            displays.forEach(d -> d.teleport(d.getLocation().add(offset)));
        }
    }
    
    public void remove() {
        stopContinuousRotation();
        displays.forEach(Entity::remove);
    }
    
    public void rotate(float yawDelta) {
        if (rootEntity != null) {
            currentYaw += yawDelta;
            org.joml.Quaternionf rotation = new org.joml.Quaternionf().rotateY((float) Math.toRadians(yawDelta));
            
            for (Display display : displays) {
                Transformation current = display.getTransformation();
                
                org.joml.Vector3f translation = new org.joml.Vector3f(current.getTranslation());
                org.joml.Vector3f rotatedTranslation = rotation.transform(translation);
                
                org.joml.Quaternionf newLeftRotation = new org.joml.Quaternionf(rotation).mul(current.getLeftRotation());
                
                Transformation newTransform = new Transformation(
                    rotatedTranslation,
                    newLeftRotation,
                    current.getScale(),
                    current.getRightRotation()
                );
                
                display.setTransformation(newTransform);
                display.setInterpolationDuration(2);
            }
            
            rootEntity.setRotation(currentYaw, rootEntity.getLocation().getPitch());
        }
    }
    
    public void applyRotation(float yaw, float pitch) {
        if (rootEntity != null) {
            currentYaw = yaw;
            
            org.joml.Matrix4f rotationMatrix = new org.joml.Matrix4f()
                .rotateY((float) Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(-pitch));
            
            for (Display display : displays) {
                Transformation current = display.getTransformation();
                
                org.joml.Vector3f translation = new org.joml.Vector3f(current.getTranslation());
                org.joml.Vector3f rotatedTranslation = rotationMatrix.transformPosition(translation);
                
                org.joml.Quaternionf rotation = new org.joml.Quaternionf()
                    .rotateY((float) Math.toRadians(yaw))
                    .rotateX((float) Math.toRadians(-pitch));
                org.joml.Quaternionf newLeftRotation = new org.joml.Quaternionf(rotation).mul(current.getLeftRotation());
                
                Transformation newTransform = new Transformation(
                    rotatedTranslation,
                    newLeftRotation,
                    current.getScale(),
                    current.getRightRotation()
                );
                
                display.setTransformation(newTransform);
                display.setInterpolationDuration(2);
            }
            
            rootEntity.setRotation(yaw, pitch);
        }
    }
    
    public void startContinuousRotation(float degreesPerSecond, String axis) {
        this.isRotating = true;
        this.rotationSpeed = degreesPerSecond;
        this.rotationAxis = axis.toLowerCase();
        
        long intervalTicks = 1;
        float degreesPerTick = degreesPerSecond / 20.0f * intervalTicks;
        
        if (rootEntity == null || rootEntity.getLocation() == null) return;
        
        org.shotrush.atom.Atom plugin = org.shotrush.atom.Atom.getInstance();
        plugin.getSchedulerManager().runAtLocationTimer(
            rootEntity.getLocation(),
            () -> {
                if (!isRotating || rootEntity == null) return;
                
                org.joml.Quaternionf rotation = switch (rotationAxis) {
                    case "x" -> new org.joml.Quaternionf().rotateX((float) Math.toRadians(degreesPerTick));
                    case "z" -> new org.joml.Quaternionf().rotateZ((float) Math.toRadians(degreesPerTick));
                    default -> new org.joml.Quaternionf().rotateY((float) Math.toRadians(degreesPerTick));
                };

                for (Display display : displays) {
                    Transformation current = display.getTransformation();
                    
                    org.joml.Vector3f translation = new org.joml.Vector3f(current.getTranslation());
                    org.joml.Vector3f rotatedTranslation = rotation.transform(translation);
                    
                    org.joml.Quaternionf newLeftRotation = new org.joml.Quaternionf(rotation).mul(current.getLeftRotation());
                    
                    Transformation newTransform = new Transformation(
                        rotatedTranslation,
                        newLeftRotation,
                        current.getScale(),
                        current.getRightRotation()
                    );
                    
                    display.setTransformation(newTransform);
                    display.setInterpolationDuration(2);
                }
            },
            intervalTicks,
            intervalTicks
        );
    }
    
    public void stopContinuousRotation() {
        isRotating = false;
        rotationSpeed = 0f;
    }
    
    public boolean isContinuouslyRotating() {
        return isRotating;
    }

    
    public void scaleAll(float x, float y, float z, int durationTicks) {
        displays.forEach(d -> {
            var manager = org.shotrush.atom.Atom.getInstance().getDisplayManager();
            manager.scale(d, x, y, z, durationTicks);
        });
    }
}
