package org.shotrush.atom.content.systems;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

import java.util.*;

@AutoRegisterSystem(priority = 1)
public class GravitySystem implements Listener {
    
    private final Plugin plugin;
    private static final int MAX_HORIZONTAL_DISTANCE = 7;
    
    public GravitySystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        
        if (!placed.getType().isSolid()) return;
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(placed.getLocation(), () -> {
            if (!placed.getType().isSolid()) return;
            
            int horizontalDistance = getHorizontalDistanceFromSupport(placed);
            if (horizontalDistance >= MAX_HORIZONTAL_DISTANCE) {
                Set<Block> fallingBlocks = new HashSet<>();
                collectConnectedFloatingBlocks(placed, fallingBlocks);
                
                if (!fallingBlocks.isEmpty()) {
                    Player nearestPlayer = getNearestPlayer(placed.getLocation());
                    makeBlocksFallWithToppling(fallingBlocks, nearestPlayer);
                }
            }
        }, 2L);
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (!broken.getType().isSolid()) return;
        
        event.setDropItems(false);
        Location loc = broken.getLocation();
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(loc, () -> {
            Set<Block> affectedBlocks = new HashSet<>();
            
            Block above = loc.getBlock().getRelative(BlockFace.UP);
            if (above.getType().isSolid()) {
                collectAllAboveBlocks(above, affectedBlocks);
            }
            
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block neighbor = loc.getBlock().getRelative(face);
                if (!neighbor.getType().isSolid()) continue;
                
                int horizontalDistance = getHorizontalDistanceFromSupport(neighbor);
                if (horizontalDistance >= MAX_HORIZONTAL_DISTANCE) {
                    collectConnectedFloatingBlocks(neighbor, affectedBlocks);
                }
            }
            
            if (!affectedBlocks.isEmpty()) {
                Player nearestPlayer = getNearestPlayer(loc);
                makeBlocksFallWithToppling(affectedBlocks, nearestPlayer);
            }
        }, 1L);
    }
    
    private void collectAllAboveBlocks(Block start, Set<Block> collected) {
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        
        while (!queue.isEmpty() && collected.size() < 200) {
            Block current = queue.poll();
            if (collected.contains(current)) continue;
            if (!current.getType().isSolid()) continue;
            
            collected.add(current);
            
            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.SELF || face == BlockFace.DOWN) continue;
                Block adjacent = current.getRelative(face);
                if (adjacent.getType().isSolid() && !collected.contains(adjacent)) {
                    queue.add(adjacent);
                }
            }
        }
    }
    
    private int getHorizontalDistanceFromSupport(Block block) {
        Queue<Block> queue = new LinkedList<>();
        Map<Block, Integer> distances = new HashMap<>();
        Set<Location> visited = new HashSet<>();
        
        queue.add(block);
        distances.put(block, 0);
        
        while (!queue.isEmpty()) {
            Block current = queue.poll();
            if (visited.contains(current.getLocation())) continue;
            visited.add(current.getLocation());
            
            int currentDistance = distances.get(current);
            if (currentDistance > MAX_HORIZONTAL_DISTANCE) break;
            
            Block below = current.getRelative(BlockFace.DOWN);
            if (below.getType().isSolid()) {
                return currentDistance;
            }
            
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adjacent = current.getRelative(face);
                if (adjacent.getType().isSolid() && !visited.contains(adjacent.getLocation())) {
                    distances.put(adjacent, currentDistance + 1);
                    queue.add(adjacent);
                }
            }
        }
        
        return Integer.MAX_VALUE;
    }
    
    private void collectConnectedFloatingBlocks(Block start, Set<Block> collected) {
        if (collected.size() > 200) return;
        
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        
        while (!queue.isEmpty() && collected.size() < 200) {
            Block current = queue.poll();
            if (collected.contains(current)) continue;
            if (!current.getType().isSolid()) continue;
            
            int distance = getHorizontalDistanceFromSupport(current);
            if (distance < MAX_HORIZONTAL_DISTANCE) continue;
            
            collected.add(current);
            
            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.SELF) continue;
                Block adjacent = current.getRelative(face);
                if (adjacent.getType().isSolid() && !collected.contains(adjacent)) {
                    queue.add(adjacent);
                }
            }
        }
    }
    
    private void makeBlocksFallWithToppling(Set<Block> blocks, Player nearestPlayer) {
        if (blocks.isEmpty()) return;
        
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        double avgX = 0, avgY = 0, avgZ = 0;
        
        for (Block block : blocks) {
            avgX += block.getX();
            avgY += block.getY();
            avgZ += block.getZ();
            minY = Math.min(minY, block.getY());
            maxY = Math.max(maxY, block.getY());
        }
        
        avgX /= blocks.size();
        avgY /= blocks.size();
        avgZ /= blocks.size();
        
        double structureHeight = maxY - minY;
        Vector playerDirection = nearestPlayer != null ? 
            nearestPlayer.getEyeLocation().getDirection().normalize() : 
            new Vector(0, 0, 0);
        
        for (Block block : blocks) {
            double heightAboveBase = block.getY() - minY;
            double heightFactor = structureHeight > 0 ? heightAboveBase / structureHeight : 0;
            double fallSpeed = BlockWeight.getFallSpeed(block.getType());
            Vector velocity = new Vector(0, -fallSpeed, 0);
            
            if (nearestPlayer != null && heightFactor > 0.2) {
                Vector horizontalPush = playerDirection.clone().setY(0).normalize();
                double pushStrength = 0.15 + (heightFactor * 0.35);
                velocity.add(horizontalPush.multiply(pushStrength));
            }
            
            Location spawnLoc = block.getLocation().add(0.5, 0, 0.5);
            org.bukkit.block.data.BlockData blockData = block.getBlockData().clone();
            block.setType(Material.AIR);
            
            block.getWorld().spawn(spawnLoc, FallingBlock.class, fb -> {
                fb.setBlockData(blockData);
                fb.setDropItem(false);
                fb.setHurtEntities(false);
                fb.setVelocity(velocity);
            });
        }
    }
    
    private Player getNearestPlayer(Location location) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        
        return nearest;
    }
}
