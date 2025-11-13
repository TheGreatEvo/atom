package org.shotrush.atom.content.workstation

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.block.entity.BlockEntityTypes
import net.momirealms.craftengine.core.util.Key
import org.shotrush.atom.content.workstation.knapping.KnappingBlockBehavior
import org.shotrush.atom.content.workstation.knapping.KnappingStationBehavior
import org.shotrush.atom.content.workstation.leatherbed.LeatherBedBlockBehavior
import org.shotrush.atom.content.workstation.craftingbasket.CraftingBasketBlockBehavior
import org.shotrush.atom.content.workstation.knapping.KnappingStationEntity

data class WorkstationDef<T : BlockEntity>(val key: Key, val type: BlockEntityType<T>) {}

object Workstations {
    val KNAPPING_STATION_KEY = Key.of("atom:knapping_station")
    val KNAPPING_STATION_BEHAVIOR = BlockBehaviors.register(KNAPPING_STATION_KEY, KnappingStationBehavior.Companion.Factory)
    val KNAPPING_STATION_ENTITY_TYPE = BlockEntityTypes.register<BlockEntity>(KNAPPING_STATION_KEY)

    val LEATHER_BED_KEY = Key.of("atom:leather_bed")
    val LEATHER_BED_BEHAVIOR = BlockBehaviors.register(LEATHER_BED_KEY, LeatherBedBlockBehavior.Factory)
    val LEATHER_BED_ENTITY_TYPE = BlockEntityTypes.register<BlockEntity>(LEATHER_BED_KEY)

    val CRAFTING_BASKET_KEY = Key.of("atom:crafting_basket")
    val CRAFTING_BASKET_BEHAVIOR = BlockBehaviors.register(CRAFTING_BASKET_KEY, CraftingBasketBlockBehavior.Companion.Factory)
    val CRAFTING_BASKET_ENTITY_TYPE = BlockEntityTypes.register<BlockEntity>(CRAFTING_BASKET_KEY)

    fun init() = Unit

    fun <T : BlockEntity> register(key: String, factory: BlockBehaviorFactory): WorkstationDef<T> {
        val key = Key.of(key)
        BlockBehaviors.register(key, factory)
        return WorkstationDef(key, BlockEntityTypes.register<T>(key))
    }
}