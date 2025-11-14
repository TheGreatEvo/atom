package org.shotrush.atom.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DyedItemColor
import io.papermc.paper.datacomponent.item.TooltipDisplay
import io.papermc.paper.persistence.PersistentDataContainerView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.item.CustomItem
import net.momirealms.craftengine.core.util.Key
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.shotrush.atom.getNamespacedKey

object Molds {
    fun getMold(tool: MoldShape, variant: MoldType): CustomItem<ItemStack> {
        return CraftEngineItems.byId(Key.of("atom", "${variant.id}_mold_${tool.mold}"))!!
    }

    fun getToolHead(tool: MoldShape, material: Material): CustomItem<ItemStack> {
        val key = "${material.id}_${tool.id}_head"
        return CraftEngineItems.byId(Key.of("atom", key)) ?: error("No tool head found for $key")
    }

    fun getFilledMold(shape: MoldShape, variant: MoldType, material: Material): ItemStack {
        if (variant != MoldType.Wax && variant != MoldType.Fired) throw IllegalArgumentException("Only Wax and Fired molds can be filled!")
        val item = CraftEngineItems.byId(Key.of("atom", "filled_${variant.id}_mold_${shape.mold}"))!!
        val stack = item.buildItemStack()
        val lore = stack.lore() ?: mutableListOf()
        val loreCopy = lore.toMutableList()
        
        loreCopy[0] = Component.text("Filled with: ").style {
            it.decoration(TextDecoration.ITALIC, false).color(
                NamedTextColor.GRAY
            )
        }.append(
            Component.translatable(
                "material.${material.id}.name",
                TextColor.color(material.rgb.asRGB())
            )
        )
        stack.lore(loreCopy)
        stack.setData(
            DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(
                material.rgb
            )
        )
        stack.setData(
            DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .hiddenComponents(setOf(DataComponentTypes.DYED_COLOR))
                .build()
        )
        stack.editPersistentDataContainer {
            it.set(NamespacedKey("atom", "mold_type"), PersistentDataType.STRING, variant.id)
            it.set(NamespacedKey("atom", "mold_shape"), PersistentDataType.STRING, shape.id)
            it.set(NamespacedKey("atom", "mold_fill"), PersistentDataType.STRING, material.id)
        }
        return stack
    }

    val FilledRegex = Regex("atom:filled_(.+)_mold_(.+)")

    fun isFilledMold(stack: ItemStack): Boolean {
        if(!stack.getNamespacedKey().matches(FilledRegex)) return false
        if(!stack.persistentDataContainer.has("mold_type")) return false
        if(!stack.persistentDataContainer.has("mold_shape")) return false
        if(!stack.persistentDataContainer.has("mold_fill")) return false
        return true
    }

    fun emptyMold(stack: ItemStack): Pair<ItemStack, ItemStack> {
        if(!isFilledMold(stack)) throw IllegalArgumentException("Item is not a filled mold!")
        val moldTypeId = stack.persistentDataContainer.getString("mold_type") ?: error("No mold type found!")
        val moldShapeId = stack.persistentDataContainer.getString("mold_shape") ?: error("No mold shape found!")
        val materialId = stack.persistentDataContainer.getString("mold_fill") ?: error("No material found!")

        val moldType = MoldType.byId(moldTypeId)
        val moldShape = MoldShape.byId(moldShapeId)
        val material = Material.byId(materialId)

        val emptyMold = getMold(moldShape, moldType).buildItemStack()
        val toolHead = getToolHead(moldShape, material).buildItemStack()
        return Pair(emptyMold, toolHead)
    }
}

fun PersistentDataContainerView.has(key: String) =
    this.has(NamespacedKey("atom", key))

fun PersistentDataContainerView.getString(key: String) =
    this.get(NamespacedKey("atom", key), PersistentDataType.STRING)

fun PersistentDataContainerView.getString(key: NamespacedKey) =
    this.get(key, PersistentDataType.STRING)