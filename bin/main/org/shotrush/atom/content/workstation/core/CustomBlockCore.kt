package org.shotrush.atom.content.workstation.core

import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import org.shotrush.atom.Atom
import java.util.UUID


abstract class CustomBlockCore(
    block: CustomBlock
) : AbstractBlockBehavior(block), EntityBlockBehavior {
    
    protected var displayUUID: UUID? = null
    protected var interactionUUID: UUID? = null
    protected var blockPos: BlockPos? = null
    
    
    abstract override fun <T : BlockEntity> blockEntityType(state: ImmutableBlockState): BlockEntityType<T>
    
    
    abstract override fun createBlockEntity(pos: BlockPos, state: ImmutableBlockState): BlockEntity
    
    
    override fun useOnBlock(context: UseOnContext, state: ImmutableBlockState): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val sneaking = player.isSneaking
        
        return if (onInteract(player, sneaking)) {
            InteractionResult.SUCCESS
        } else {
            InteractionResult.PASS
        }
    }
    
    
    open fun onInteract(player: Player, sneaking: Boolean): Boolean = false
    
    
    open fun onWrenchInteract(player: Player, sneaking: Boolean): Boolean = false
    
    
    open fun onPlaced(pos: BlockPos) {
        this.blockPos = pos
    }
    
    
    open fun onRemoved() {
        removeEntities()
    }
    
    
    protected fun removeEntities() {
        displayUUID?.let { uuid ->
            org.bukkit.Bukkit.getEntity(uuid)?.remove()
        }
        interactionUUID?.let { uuid ->
            org.bukkit.Bukkit.getEntity(uuid)?.remove()
        }
        
        
        blockPos?.let { pos ->
            val location = Location(
                org.bukkit.Bukkit.getWorld("world"), 
                pos.x().toDouble() + 0.5,
                pos.y().toDouble() + 0.5,
                pos.z().toDouble() + 0.5
            )
            cleanupNearbyEntities(location)
        }
    }
    
    
    protected fun cleanupNearbyEntities(location: Location, radius: Double = 1.0) {
        location.world?.getNearbyEntities(location, radius, radius, radius)?.forEach { entity ->
            when (entity) {
                is ItemDisplay, is Interaction -> {
                    if (entity.location.distance(location) < radius) {
                        entity.remove()
                    }
                }
            }
        }
    }
    
    
    protected fun spawnEntities(location: Location, itemStack: ItemStack, interactionSize: Vector3f = Vector3f(1f, 1f, 1f)) {
        location.world?.let { world ->
            
            val display = world.spawn(location, ItemDisplay::class.java).apply {
                setItemStack(itemStack)
                itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
                billboard = org.bukkit.entity.Display.Billboard.FIXED
            }
            displayUUID = display.uniqueId
            
            
            val interaction = world.spawn(location, Interaction::class.java).apply {
                interactionWidth = interactionSize.x
                interactionHeight = interactionSize.y
                isResponsive = true
                isInvulnerable = true
            }
            interactionUUID = interaction.uniqueId
        }
    }
    
    
    open fun isValid(): Boolean {
        return displayUUID?.let { org.bukkit.Bukkit.getEntity(it) } != null &&
               interactionUUID?.let { org.bukkit.Bukkit.getEntity(it) } != null
    }
    
    
    protected fun getDisplayEntity(): ItemDisplay? {
        return displayUUID?.let { org.bukkit.Bukkit.getEntity(it) as? ItemDisplay }
    }
    
    
    protected fun getInteractionEntity(): Interaction? {
        return interactionUUID?.let { org.bukkit.Bukkit.getEntity(it) as? Interaction }
    }
    
    
    open fun saveCustomData(tag: CompoundTag) {
        displayUUID?.let { tag.putString("displayUUID", it.toString()) }
        interactionUUID?.let { tag.putString("interactionUUID", it.toString()) }
    }
    
    
    open fun loadCustomData(tag: CompoundTag) {
        tag.getString("displayUUID")?.let { displayUUID = UUID.fromString(it) }
        tag.getString("interactionUUID")?.let { interactionUUID = UUID.fromString(it) }
    }
}
