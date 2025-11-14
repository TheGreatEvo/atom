package org.shotrush.atom.content.workstation.core

import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import net.momirealms.craftengine.libraries.nbt.ListTag
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import org.shotrush.atom.Atom
import org.shotrush.atom.core.util.ActionBarManager
import java.util.UUID
import kotlin.math.PI


data class PlacedItem(
    val item: ItemStack,
    val position: Vector3f,
    val yaw: Float,
    var displayUUID: UUID? = null
)


abstract class InteractiveSurface(
    block: CustomBlock
) : CustomBlockCore(block) {
    
    protected val placedItems = mutableListOf<PlacedItem>()
    private val parentKey = NamespacedKey(Atom.instance!!, "parent_workstation")
    
    
    abstract fun getMaxItems(): Int
    
    
    abstract fun canPlaceItem(item: ItemStack): Boolean
    
    
    abstract fun calculatePlacement(player: Player, itemCount: Int): Vector3f
    
    
    open fun getFullMessage(): String = "§cSurface is full!"
    
    
    open fun getEmptyMessage(): String = "§cNo items on surface!"
    
    
    open fun useGuiMode(): Boolean = false
    
    
    open fun openGui(player: Player) {
        
    }
    
    
    open fun checkRecipe(): ItemStack? = null
    
    
    protected fun applyQualityInheritance(result: ItemStack?) {
        if (result == null || placedItems.isEmpty()) return
        
        val ingredients = placedItems.map { it.item }.toTypedArray()
        org.shotrush.atom.core.api.item.QualityInheritanceAPI.applyInheritedQuality(result, *ingredients)
    }
    
    override fun onInteract(player: Player, sneaking: Boolean): Boolean {
        if (useGuiMode() && !sneaking) {
            openGui(player)
            return true
        }
        
        if (sneaking) {
            return onCrouchRightClick(player)
        }
        
        
        val itemInHand = player.inventory.itemInMainHand
        if (!itemInHand.type.isAir && canPlaceItem(itemInHand)) {
            if (placedItems.size >= getMaxItems()) {
                ActionBarManager.send(player, getFullMessage())
                return false
            }
            
            val position = calculatePlacement(player, placedItems.size)
            if (placeItem(player, itemInHand, position, player.location.yaw)) {
                itemInHand.amount--
                return true
            }
        }
        
        return false
    }
    
    
    protected open fun onCrouchRightClick(player: Player): Boolean {
        val result = checkRecipe()
        
        return if (result != null) {
            applyQualityInheritance(result)
            clearAllItems()
            blockPos?.let { pos ->
                val location = Location(
                    player.world,
                    pos.x().toDouble() + 0.5,
                    pos.y().toDouble() + 0.5,
                    pos.z().toDouble() + 0.5
                )
                player.world.dropItemNaturally(location, result)
            }
            ActionBarManager.send(player, "§aCrafted: ${result.type.name}")
            true
        } else {
            releaseAllItems(player)
            true
        }
    }
    
    
    fun placeItem(player: Player, item: ItemStack, position: Vector3f, yaw: Float): Boolean {
        if (placedItems.size >= getMaxItems()) return false
        if (!canPlaceItem(item)) return false
        
        val singleItem = item.clone().apply { amount = 1 }
        val placedItem = PlacedItem(singleItem, position, yaw)
        placedItems.add(placedItem)
        spawnItemDisplay(placedItem)
        player.swingMainHand()
        return true
    }
    
    
    fun removeLastItem(): ItemStack? {
        if (placedItems.isEmpty()) return null
        val item = placedItems.removeAt(placedItems.size - 1)
        removeItemDisplay(item)
        return item.item
    }
    
    
    protected fun releaseAllItems(player: Player) {
        if (placedItems.isEmpty()) {
            ActionBarManager.send(player, getEmptyMessage())
            return
        }
        
        val items = placedItems.toList()
        placedItems.clear()
        
        items.forEach { placedItem ->
            removeItemDisplay(placedItem)
            blockPos?.let { pos ->
                val location = Location(
                    player.world,
                    pos.x().toDouble() + 0.5,
                    pos.y().toDouble() + 0.5,
                    pos.z().toDouble() + 0.5
                )
                player.world.dropItemNaturally(location, placedItem.item)
            }
        }
    }
    
    
    protected fun clearAllItems() {
        placedItems.forEach { removeItemDisplay(it) }
        placedItems.clear()
    }
    
    
    protected fun spawnItemDisplay(placedItem: PlacedItem) {
        blockPos?.let { pos ->
            val location = Location(
                org.bukkit.Bukkit.getWorld("world"), 
                pos.x().toDouble() + 0.5 + placedItem.position.x,
                pos.y().toDouble() + 0.5 + placedItem.position.y,
                pos.z().toDouble() + 0.5 + placedItem.position.z
            )
            
            location.world?.let { world ->
                val display = world.spawn(location, ItemDisplay::class.java).apply {
                    setItemStack(placedItem.item)
                    itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
                    billboard = org.bukkit.entity.Display.Billboard.FIXED
                    
                    
                    val rotation = getItemDisplayRotation(placedItem)
                    val translation = getItemDisplayTranslation(placedItem)
                    val scale = getItemDisplayScale(placedItem)
                    
                    transformation = Transformation(
                        translation,
                        rotation,
                        scale,
                        AxisAngle4f(0f, 0f, 0f, 1f)
                    )
                    
                    viewRange = 64.0f
                    shadowRadius = 0.0f
                    shadowStrength = 0.0f
                    
                    
                    interactionUUID?.let { parentUUID ->
                        persistentDataContainer.set(
                            parentKey,
                            PersistentDataType.STRING,
                            parentUUID.toString()
                        )
                    }
                }
                
                placedItem.displayUUID = display.uniqueId
                
                
                org.shotrush.atom.content.systems.ItemHeatSystem.startItemDisplayHeatTracking(display)
            }
        }
    }
    
    
    protected fun removeItemDisplay(placedItem: PlacedItem) {
        placedItem.displayUUID?.let { uuid ->
            org.bukkit.Bukkit.getEntity(uuid)?.remove()
        }
    }
    
    
    protected open fun getItemDisplayRotation(item: PlacedItem): AxisAngle4f {
        return AxisAngle4f((PI / 2).toFloat(), 1f, 0f, 0f)
    }
    
    
    protected open fun getItemDisplayTranslation(item: PlacedItem): Vector3f {
        return Vector3f(0.05f, 0f, 0f)
    }
    
    
    protected open fun getItemDisplayScale(item: PlacedItem): Vector3f {
        return Vector3f(0.5f, 0.5f, 0.5f)
    }
    
    
    fun updateItemDisplayUUIDs() {
        
        placedItems.forEach { it.displayUUID = null }
        
        blockPos?.let { pos ->
            val location = Location(
                org.bukkit.Bukkit.getWorld("world"), 
                pos.x().toDouble() + 0.5,
                pos.y().toDouble() + 0.5,
                pos.z().toDouble() + 0.5
            )
            
            
            cleanupNearbyEntities(location, 1.0)
            
            
            placedItems.forEach { spawnItemDisplay(it) }
        }
    }
    
    override fun onPlaced(pos: BlockPos) {
        super.onPlaced(pos)
        
        if (placedItems.isEmpty()) {
            updateItemDisplayUUIDs()
        }
    }
    
    override fun onRemoved() {
        placedItems.forEach { removeItemDisplay(it) }
        super.onRemoved()
    }
    
    override fun saveCustomData(tag: CompoundTag) {
        super.saveCustomData(tag)
        
        
        val itemsList = ListTag()
        placedItems.forEach { placedItem ->
            val itemTag = CompoundTag()
            
            
            itemTag.putString("material", placedItem.item.type.name)
            itemTag.putInt("amount", placedItem.item.amount)
            
            
            itemTag.putFloat("x", placedItem.position.x)
            itemTag.putFloat("y", placedItem.position.y)
            itemTag.putFloat("z", placedItem.position.z)
            itemTag.putFloat("yaw", placedItem.yaw)
            
            itemsList.add(itemTag)
        }
        tag.put("placedItems", itemsList)
    }
    
    override fun loadCustomData(tag: CompoundTag) {
        super.loadCustomData(tag)
        placedItems.clear()
        tag.getList("placedItems")?.let { itemsList ->
            for (i in 0 until itemsList.size) {
                val itemTag = itemsList.getCompound(i)
                
                
                val materialName = itemTag.getString("material")
                val amount = itemTag.getInt("amount")
                val material = Material.getMaterial(materialName) ?: Material.AIR
                val item = ItemStack(material, amount)
                
                
                val position = Vector3f(
                    itemTag.getFloat("x"),
                    itemTag.getFloat("y"),
                    itemTag.getFloat("z")
                )
                val yaw = itemTag.getFloat("yaw")
                
                placedItems.add(PlacedItem(item, position, yaw))
            }
        }
        
        
        
    }
}
