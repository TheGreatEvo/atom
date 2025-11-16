package org.shotrush.atom.content.systems.groundstorage

import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData
import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemMergeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import org.shotrush.atom.core.api.annotation.RegisterSystem
import org.shotrush.atom.core.data.PersistentData
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Advanced ground item handler using ItemDisplay entities
 * Automatically converts dropped items to persistent ground displays
 */
@RegisterSystem(
    id = "ground_item_display_handler",
    priority = 10,
    description = "Advanced ground items with ItemDisplay entities and auto-conversion",
    toggleable = true,
    enabledByDefault = true
)
class GroundItemDisplayHandler(private val plugin: Plugin) : Listener {

    companion object {
        private const val GROUND_ITEM_KEY = "ground_item_display"
        private const val PICKUP_RANGE = 3.0
        private const val INTERACTION_RANGE = 4.0
        private const val VELOCITY_THRESHOLD = 0.1
        private const val STACKING_RADIUS = 1.0 // Radius to check for stacking
        private const val OCCUPANCY_RADIUS = 0.25 // Minimum distance between different items (reduced from 0.3)
        private const val MAX_PLACEMENT_ATTEMPTS = 6 // Max attempts to find a free spot (reduced from 8)
    }

    private val pendingItems = mutableSetOf<UUID>()
    private val conversionTasks = mutableMapOf<UUID, Int>()

    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity
        if (item.itemStack.type == Material.AIR) return
        
        // Mark item for conversion
        pendingItems.add(item.uniqueId)
        
        // Schedule conversion check
        val taskId = plugin.server.scheduler.scheduleSyncRepeatingTask(
            plugin,
            {
                checkItemForConversion(item)
            },
            5L, // Start checking after 5 ticks
            5L  // Check every 5 ticks
        )
        
        conversionTasks[item.uniqueId] = taskId
    }

    private fun checkItemForConversion(item: Item) {
        if (!item.isValid || item.isDead) {
            cleanupPendingItem(item.uniqueId)
            return
        }

        // Check if item has come to rest
        val velocity = item.velocity
        if (velocity.length() < VELOCITY_THRESHOLD && item.isOnGround) {
            convertItemToDisplay(item)
        }
    }

    private fun convertItemToDisplay(item: Item) {
        val location = item.location
        val itemStack = item.itemStack
        
        // Clean up tracking
        cleanupPendingItem(item.uniqueId)
        
        // Remove original item
        item.remove()
        
        // Try to stack with existing ground items first
        if (!tryStackWithExisting(location, itemStack)) {
            // If not stacked, create new ground item display
            createGroundItemDisplay(location, itemStack)
        }
    }

    private fun tryStackWithExisting(location: Location, itemStack: ItemStack): Boolean {
        // Find existing ground items of the same type within stacking radius
        val existingItems = findGroundItemsInRadius(location, STACKING_RADIUS)
        
        for (display in existingItems) {
            val existingItem = display.itemStack
            
            // Check if items can stack (same type and not at max stack size)
            if (existingItem.isSimilar(itemStack)) {
                val existingAmount = getGroundItemAmount(display)
                val maxStackSize = existingItem.maxStackSize
                
                if (existingAmount < maxStackSize) {
                    val spaceLeft = maxStackSize - existingAmount
                    val amountToAdd = minOf(itemStack.amount, spaceLeft)
                    
                    // Update the existing display with new amount
                    val newAmount = existingAmount + amountToAdd
                    setGroundItemAmount(display, newAmount)
                    
                    // If we couldn't add all items, create a new display for the remainder
                    if (amountToAdd < itemStack.amount) {
                        val remainder = itemStack.clone().apply {
                            amount = itemStack.amount - amountToAdd
                        }
                        createGroundItemDisplay(location, remainder)
                    }
                    
                    return true
                }
            }
        }
        
        return false
    }

    private fun cleanupPendingItem(itemId: UUID) {
        pendingItems.remove(itemId)
        conversionTasks[itemId]?.let { taskId ->
            plugin.server.scheduler.cancelTask(taskId)
            conversionTasks.remove(itemId)
        }
    }

    private fun createGroundItemDisplay(location: Location, itemStack: ItemStack) {
        // Find a free spot near the drop location
        val displayLocation = findFreePosition(location)
        
        // Create ItemDisplay
        val display = location.world?.spawn(displayLocation, ItemDisplay::class.java) ?: return
        
        // Configure display - show single item but store actual amount
        display.setItemStack(itemStack.clone().apply { amount = 1 })
        display.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
        display.billboard = org.bukkit.entity.Display.Billboard.FIXED
        
        // Apply transformation - lay flat on ground
        val transformation = Transformation(
            Vector3f(0f, 0f, 0f), // Translation
            AxisAngle4f(Math.PI.toFloat() / 2, 1f, 0f, 0f), // Rotate 90° around X axis to lay flat
            Vector3f(0.5f, 0.5f, 0.5f), // Scale
            AxisAngle4f(0f, 0f, 0f, 0f) // No additional rotation
        )
        display.transformation = transformation
        
        // Mark as ground item
        PersistentData.flag(display, GROUND_ITEM_KEY)
        
        // Store original item data using Bukkit's PersistentDataContainer
        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "original_item_type"),
            org.bukkit.persistence.PersistentDataType.STRING,
            itemStack.type.name
        )
        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "original_item_amount"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            itemStack.amount
        )
        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "spawn_time"),
            org.bukkit.persistence.PersistentDataType.LONG,
            System.currentTimeMillis()
        )
        
        // Play placement sound
        location.world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f)
    }

    private fun findFreePosition(location: Location): Location {
        val baseLocation = location.clone().apply {
            y = blockY + 0.1 // Slightly above ground
        }
        
        // Check if the exact location is free
        if (isPositionFree(baseLocation)) {
            return baseLocation
        }
        
        // Try to find a nearby free position in a tight spiral pattern
        for (attempt in 1..MAX_PLACEMENT_ATTEMPTS) {
            val angle = (attempt * 60.0) * (Math.PI / 180.0) // 60 degree increments for tighter spiral
            val radius = OCCUPANCY_RADIUS // Constant radius, don't increase with attempts
            val x = baseLocation.x + cos(angle) * radius
            val z = baseLocation.z + sin(angle) * radius
            
            val testLocation = baseLocation.clone().apply {
                this.x = x
                this.z = z
            }
            
            if (isPositionFree(testLocation)) {
                return testLocation
            }
        }
        
        // If no free position found, return the original location (will overlap but rare)
        return baseLocation
    }

    private fun isPositionFree(location: Location): Boolean {
        // Check if there are any ground items within occupancy radius
        val nearbyItems = findGroundItemsInRadius(location, OCCUPANCY_RADIUS)
        
        // Position is free if no ground items are in the way
        return nearbyItems.isEmpty()
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (!event.player.isSneaking) return // Require shift+right-click
        
        val player = event.player
        val eyeLocation = player.eyeLocation
        
        // Raycast to find ground items
        val direction = eyeLocation.direction
        val result = eyeLocation.world.rayTrace(
            eyeLocation,
            direction,
            INTERACTION_RANGE,
            org.bukkit.FluidCollisionMode.NEVER,
            true,
            0.1,
            { entity -> entity is ItemDisplay && isGroundItem(entity) }
        )
        
        val targetDisplay = result?.hitEntity as? ItemDisplay
        if (targetDisplay != null && isGroundItem(targetDisplay)) {
            event.isCancelled = true
            pickupGroundItem(player, targetDisplay)
        }
    }

    private fun pickupGroundItem(player: Player, display: ItemDisplay) {
        val itemStack = display.itemStack
        if (itemStack.type == Material.AIR) {
            display.remove()
            return
        }
        
        // Get the actual amount from persistent storage
        val actualAmount = getGroundItemAmount(display)
        val pickupStack = itemStack.clone().apply { amount = actualAmount }
        
        // Add to player inventory
        val remaining = player.inventory.addItem(pickupStack)
        
        if (remaining.isEmpty()) {
            // Successfully picked up all items
            display.location.world?.playSound(
                display.location,
                Sound.ENTITY_ITEM_PICKUP,
                0.7f,
                1.0f
            )
            display.remove()
        } else {
            // Inventory full, put remaining items back
            val remainingAmount = remaining.values.first().amount
            setGroundItemAmount(display, remainingAmount)
            // Play sound but don't remove display
            display.location.world?.playSound(
                display.location,
                Sound.ENTITY_ITEM_PICKUP,
                0.5f,
                0.8f
            )
        }
    }

    @EventHandler
    fun onItemDespawn(event: ItemDespawnEvent) {
        // Clean up pending items if they despawn before conversion
        cleanupPendingItem(event.entity.uniqueId)
    }

    @EventHandler
    fun onItemMerge(event: ItemMergeEvent) {
        // Prevent merging for items pending conversion
        if (pendingItems.contains(event.entity.uniqueId) || 
            pendingItems.contains(event.target.uniqueId)) {
            event.isCancelled = true
        }
    }

    private fun findGroundItemsInRadius(location: Location, radius: Double): List<ItemDisplay> {
        return location.world?.getNearbyEntities(location, radius, radius, radius)
            ?.filterIsInstance<ItemDisplay>()
            ?.filter { display -> isGroundItem(display) }
            ?: emptyList()
    }

    private fun isGroundItem(display: ItemDisplay): Boolean {
        return PersistentData.isFlagged(display, GROUND_ITEM_KEY)
    }

    private fun getGroundItemAmount(display: ItemDisplay): Int {
        return display.persistentDataContainer.get(
            org.bukkit.NamespacedKey(plugin, "original_item_amount"),
            org.bukkit.persistence.PersistentDataType.INTEGER
        ) ?: 1
    }

    private fun setGroundItemAmount(display: ItemDisplay, amount: Int) {
        display.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "original_item_amount"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            amount
        )
    }

    fun getGroundItemData(display: ItemDisplay): ItemStack? {
        return if (isGroundItem(display)) {
            val container = display.persistentDataContainer
            val typeName = container.get(
                org.bukkit.NamespacedKey(plugin, "original_item_type"),
                org.bukkit.persistence.PersistentDataType.STRING
            ) ?: return null
            val material = Material.valueOf(typeName)
            val amount = container.get(
                org.bukkit.NamespacedKey(plugin, "original_item_amount"),
                org.bukkit.persistence.PersistentDataType.INTEGER
            ) ?: 1
            ItemStack(material, amount)
        } else {
            null
        }
    }

    // Cleanup method for plugin disable/reload
    fun cleanup() {
        // Cancel all pending conversion tasks
        conversionTasks.values.forEach { taskId ->
            plugin.server.scheduler.cancelTask(taskId)
        }
        conversionTasks.clear()
        pendingItems.clear()
    }
}