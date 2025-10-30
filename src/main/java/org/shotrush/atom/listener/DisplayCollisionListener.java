package org.shotrush.atom.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.shotrush.atom.Atom;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisplayCollisionListener implements Listener {
    
    private final Map<UUID, io.papermc.paper.threadedregions.scheduler.ScheduledTask> playerTasks = new HashMap<>();
    private final Map<UUID, Boolean> frozenPlayers = new HashMap<>();
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startCollisionCheck(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = playerTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        frozenPlayers.remove(playerId);
        restoreMovementSpeed(event.getPlayer());
    }
    
    private void startCollisionCheck(Player player) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = player.getScheduler().runAtFixedRate(
            Atom.getInstance(),
            (scheduledTask) -> checkCollision(player),
            null,
            1,
            1
        );
        playerTasks.put(player.getUniqueId(), task);
    }
    
    private void checkCollision(Player player) {
        if (!player.isOnline()) return;
        
        Location loc = player.getLocation();
        BoundingBox playerBox = player.getBoundingBox();
        boolean colliding = false;
        
        for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (!(entity instanceof Display)) continue;
            Display display = (Display) entity;
            BoundingBox displayBox = getDisplayBoundingBox(display);
            
            if (displayBox != null && playerBox.overlaps(displayBox)) {
                colliding = true;
                freezeMovement(player);
                
                double overlap = getOverlapDepth(playerBox, displayBox);
                if (overlap > 0.01) {
                    Vector pushDirection = calculatePushDirection(player.getLocation(), display.getLocation());
                    Vector pushOut = pushDirection.normalize().multiply(overlap + 0.05);
                    player.setVelocity(pushOut);
                } else {
                    player.setVelocity(new Vector(0, player.getVelocity().getY(), 0));
                }
                
                break; 
            }
        }
        
        if (!colliding) {
            restoreMovementSpeed(player);
        }
    }
    
    private BoundingBox getDisplayBoundingBox(Display display) {
        Location loc = display.getLocation();
        var transform = display.getTransformation();
        org.joml.Vector3f scale = transform.getScale();
        org.joml.Vector3f translation = transform.getTranslation();
        double expansion = 0.01;
        double halfWidth = (scale.x / 2.0) + expansion;
        double halfHeight = (scale.y / 2.0) + expansion;
        double halfDepth = (scale.z / 2.0) + expansion;
        
        return new BoundingBox(
            loc.getX() + translation.x - halfWidth,
            loc.getY() + translation.y - halfHeight,
            loc.getZ() + translation.z - halfDepth,
            loc.getX() + translation.x + halfWidth,
            loc.getY() + translation.y + halfHeight,
            loc.getZ() + translation.z + halfDepth
        );
    }
    
    private Vector calculatePushDirection(Location playerLoc, Location displayLoc) {
        Vector direction = playerLoc.toVector().subtract(displayLoc.toVector());
        if (direction.lengthSquared() < 0.01) {
            return new Vector(0, 1, 0);
        }
        
        return direction.normalize();
    }
    
    private double getOverlapDepth(BoundingBox playerBox, BoundingBox displayBox) {
        double overlapX = Math.min(playerBox.getMaxX(), displayBox.getMaxX()) - Math.max(playerBox.getMinX(), displayBox.getMinX());
        double overlapY = Math.min(playerBox.getMaxY(), displayBox.getMaxY()) - Math.max(playerBox.getMinY(), displayBox.getMinY());
        double overlapZ = Math.min(playerBox.getMaxZ(), displayBox.getMaxZ()) - Math.max(playerBox.getMinZ(), displayBox.getMinZ());
        return Math.min(Math.min(overlapX, overlapY), overlapZ);
    }
    
    private void freezeMovement(Player player) {
        UUID playerId = player.getUniqueId();
        if (!frozenPlayers.getOrDefault(playerId, false)) {
            var attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attribute != null) {
                attribute.setBaseValue(0.0);
                frozenPlayers.put(playerId, true);
            }
        }
    }
    
    private void restoreMovementSpeed(Player player) {
        UUID playerId = player.getUniqueId();
        if (frozenPlayers.getOrDefault(playerId, false)) {
            var attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attribute != null) {
                attribute.setBaseValue(0.1);
                frozenPlayers.put(playerId, false);
            }
        }
    }
}
