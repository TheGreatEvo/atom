package org.shotrush.atom.content.workstation.leatherbed

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.*
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.core.world.ChunkPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.AxisAngle4f
import org.joml.Vector3f
import org.shotrush.atom.Atom
import org.shotrush.atom.content.AnimalProduct
import org.shotrush.atom.content.foraging.items.SharpenedFlint
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.content.workstation.core.InteractiveSurface
import org.shotrush.atom.content.workstation.core.PlacedItem
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.content.workstation.leatherbed.LeatherBedBlockBehavior.Companion.activeProcessing
import org.shotrush.atom.core.api.player.PlayerDataAPI
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.getItemStack
import org.shotrush.atom.item.Items
import org.shotrush.atom.matches
import org.shotrush.atom.putItemStack
import kotlin.random.Random


class LeatherBedBlockBehavior(
    block: CustomBlock,
) : InteractiveSurface(block) {

    companion object {
        private val activeProcessing = mutableMapOf<Player, Job>()
        internal val stabilizingLeather = mutableMapOf<BlockPos, Job>()
        internal val curingStartTimes = mutableMapOf<BlockPos, Long>()
        internal var CURING_TIME_MS = 10 * 60 * 1000L

        fun isScrapingTool(item: ItemStack): Boolean {
            return item.matches("atom:sharpened_flint") || item.matches("atom:knife")
        }
    }

    object Factory : BlockBehaviorFactory {
        override fun create(
            block: CustomBlock,
            arguments: Map<String?, Any?>,
        ): BlockBehavior = LeatherBedBlockBehavior(block)
    }

    override fun <T : BlockEntity> blockEntityType(state: ImmutableBlockState): BlockEntityType<T> =
        @Suppress("UNCHECKED_CAST")
        Workstations.LEATHER_BED_ENTITY_TYPE as BlockEntityType<T>

    override fun createBlockEntity(
        pos: BlockPos,
        state: ImmutableBlockState,
    ): BlockEntity = LeatherBedBlockEntity(pos, state)

    override fun getMaxItems(): Int = 1

    override fun canPlaceItem(item: ItemStack): Boolean {
        val itemId = CraftEngineItems.getCustomItemId(item)
        val isRawLeather = itemId != null && itemId.value().startsWith("animal_leather_raw_")
        val isCuredLeather = itemId != null && itemId.value().startsWith("animal_leather_cured_")

        return isRawLeather || isCuredLeather || item.type == Material.LEATHER
    }

    override fun calculatePlacement(player: Player, itemCount: Int): Vector3f {
        return Vector3f(-0.05f, 0.75f, 0.60f)
    }

    override fun getFullMessage(): String = "§cLeather bed is full!"

    override fun getEmptyMessage(): String = "§cPlace leather first!"

    override fun getItemDisplayRotation(item: PlacedItem): AxisAngle4f {

        return AxisAngle4f((Math.PI / 2).toFloat(), 0f, 0f, 0f)
    }

    override fun getItemDisplayScale(item: PlacedItem): Vector3f {

        return Vector3f(1f, 1f, 1f)
    }

    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val item = context.item.item as? ItemStack ?: return InteractionResult.PASS
        val pos = context.clickedPos

        val blockEntity = context.level.storageWorld().getBlockEntityAtIfLoaded(pos)

        if (blockEntity !is LeatherBedBlockEntity) return InteractionResult.PASS

        if (isScrapingTool(item)) {
            if (!blockEntity.hasItem()) {
                ActionBarManager.send(player, getEmptyMessage())
                return InteractionResult.SUCCESS
            } else {
                blockEntity.startScraping(player, item)
            }
            return InteractionResult.SUCCESS
        } else {
            return if (player.isSneaking) {
                blockEntity.tryEmptyItems(player, item)
            } else {
                blockEntity.tryPlaceItem(player, item)
            }
        }


//        val result = super.useOnBlock(context, state)
//
//
//        if (result == InteractionResult.SUCCESS && blockPos != null) {
//            WorkstationDataManager.updatePlacedItems(blockPos!!, placedItems)
//            WorkstationDataManager.saveData()
//
//            val placedItem = placedItems.lastOrNull()?.item
//
//            if (placedItem?.type == Material.LEATHER) {
//                startStabilization(pos)
//            }
//        }
//
//        return result
    }

    override fun onRemoved() {

        activeProcessing.values.forEach { it.cancel() }
        activeProcessing.clear()


        blockPos?.let { pos ->
            stabilizingLeather[pos]?.cancel()
            stabilizingLeather.remove(pos)
            curingStartTimes.remove(pos)


        }

        super.onRemoved()
    }

    private fun startStabilization(pos: BlockPos) {

        stabilizingLeather[pos]?.cancel()


        val workstationData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
        val currentItem = workstationData.placedItems.lastOrNull()


        if (currentItem == null || currentItem.item.type != Material.LEATHER) {
            Atom.instance?.logger?.info("StartStabilization: No vanilla leather found at $pos")
            return
        }

        Atom.instance?.logger?.info("Starting leather stabilization at $pos")


        val startTime = System.currentTimeMillis()
        curingStartTimes[pos] = startTime
        workstationData.curingStartTime = startTime
        WorkstationDataManager.saveData()


        val job = GlobalScope.launch {
            delay(CURING_TIME_MS)


            val updatedData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
            val oldLeatherItem = updatedData.placedItems.lastOrNull()

            if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {
                Atom.instance?.logger?.info("Completing leather stabilization at $pos")


                val animals = listOf(
                    "cow",
                    "pig",
                    "sheep",
                    "chicken",
                    "rabbit",
                    "horse",
                    "donkey",
                    "mule",
                    "llama",
                    "goat",
                    "cat",
                    "wolf",
                    "fox",
                    "panda",
                    "polar_bear",
                    "ocelot",
                    "camel"
                )
                val randomAnimal = animals.random()
                val curedLeatherId = "atom:animal_leather_cured_$randomAnimal"

                CraftEngineItems.byId(Key.of(curedLeatherId))?.let { curedItem ->

                    val curedLeather = curedItem.buildItemStack()


                    updatedData.placedItems.clear()
                    val newPlacedItem = PlacedItem(
                        item = curedLeather,
                        position = oldLeatherItem.position,
                        yaw = oldLeatherItem.yaw,
                        displayUUID = oldLeatherItem.displayUUID
                    )
                    updatedData.placedItems.add(newPlacedItem)


                    newPlacedItem.displayUUID?.let { uuid ->
                        (Bukkit.getEntity(uuid) as? org.bukkit.entity.ItemDisplay)?.let { display ->

                            Bukkit.getScheduler().runTask(Atom.instance!!, Runnable {
                                display.setItemStack(curedLeather)

                                display.location.world?.playSound(display.location, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f)
                                display.location.world?.spawnParticle(
                                    Particle.HAPPY_VILLAGER,
                                    display.location,
                                    10,
                                    0.3,
                                    0.3,
                                    0.3,
                                    0.0
                                )
                            })
                        }
                    }


                    WorkstationDataManager.updatePlacedItems(pos, updatedData.placedItems)
                    WorkstationDataManager.saveData()

                    Atom.instance?.logger?.info("Leather cured successfully at $pos to $curedLeatherId")
                } ?: Atom.instance?.logger?.warning("Failed to find cured leather item: $curedLeatherId")
            } else {
                Atom.instance?.logger?.info("Leather no longer present at $pos, cancelling stabilization")
            }

            stabilizingLeather.remove(pos)
            curingStartTimes.remove(pos)


            val data = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
            data.curingStartTime = null
            WorkstationDataManager.saveData()
        }

        stabilizingLeather[pos] = job
    }
}


fun LeatherBedBlockBehavior.Companion.accelerateCuring(pos: BlockPos): Boolean {
    val job = stabilizingLeather[pos]
    if (job == null) {
        Atom.instance?.logger?.info("No stabilization job found for $pos")
        return false
    }


    job.cancel()
    stabilizingLeather.remove(pos)
    curingStartTimes.remove(pos)


    val workstationData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
    val oldLeatherItem = workstationData.placedItems.lastOrNull()

    Atom.instance?.logger?.info("Accelerating cure at $pos, item type: ${oldLeatherItem?.item?.type}")

    if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {

        val animals = listOf(
            "cow",
            "pig",
            "sheep",
            "chicken",
            "rabbit",
            "horse",
            "donkey",
            "mule",
            "llama",
            "goat",
            "cat",
            "wolf",
            "fox",
            "panda",
            "polar_bear",
            "ocelot",
            "camel"
        )
        val randomAnimal = animals.random()
        val curedLeatherId = "atom:animal_leather_cured_$randomAnimal"

        CraftEngineItems.byId(Key.of(curedLeatherId))?.let { curedItem ->
            val curedLeather = curedItem.buildItemStack()


            workstationData.placedItems.clear()
            val newPlacedItem = PlacedItem(
                item = curedLeather,
                position = oldLeatherItem.position,
                yaw = oldLeatherItem.yaw,
                displayUUID = oldLeatherItem.displayUUID
            )
            workstationData.placedItems.add(newPlacedItem)


            newPlacedItem.displayUUID?.let { uuid ->
                (Bukkit.getEntity(uuid) as? org.bukkit.entity.ItemDisplay)?.let { display ->
                    display.setItemStack(curedLeather)

                    display.location.world?.playSound(display.location, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f)
                    display.location.world?.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        display.location,
                        10,
                        0.3,
                        0.3,
                        0.3,
                        0.0
                    )
                }
            }


            WorkstationDataManager.updatePlacedItems(pos, workstationData.placedItems)
            WorkstationDataManager.saveData()
            return true
        }
    }
    return false
}

fun LeatherBedBlockBehavior.Companion.getCuringTimeRemaining(pos: BlockPos): Long? {
    if (!stabilizingLeather.containsKey(pos)) return null
    val startTime = curingStartTimes[pos] ?: return null
    val elapsed = System.currentTimeMillis() - startTime
    val remaining = CURING_TIME_MS - elapsed
    return if (remaining > 0) remaining else 0
}

fun LeatherBedBlockBehavior.Companion.setCuringTime(timeMs: Long) {
    CURING_TIME_MS = timeMs
}


fun LeatherBedBlockBehavior.Companion.resumeCuringProcesses() {
    Atom.instance?.logger?.info("Resuming leather curing processes...")


    WorkstationDataManager.getAllWorkstations().forEach { (_, data) ->
        if (data.type == "leather_bed" && data.curingStartTime != null) {
            val pos = data.position
            val elapsedTime = System.currentTimeMillis() - data.curingStartTime!!
            val remainingTime = CURING_TIME_MS - elapsedTime

            if (remainingTime > 0) {

                Atom.instance?.logger?.info("Resuming curing at $pos with ${remainingTime / 1000}s remaining")
                curingStartTimes[pos] = data.curingStartTime!!

                val job = GlobalScope.launch {
                    delay(remainingTime)


                    val updatedData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
                    val oldLeatherItem = updatedData.placedItems.lastOrNull()

                    if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {
                        completeCuring(pos, updatedData, oldLeatherItem)
                    }

                    stabilizingLeather.remove(pos)
                    curingStartTimes.remove(pos)
                    updatedData.curingStartTime = null
                    WorkstationDataManager.saveData()
                }

                stabilizingLeather[pos] = job
            } else {

                Atom.instance?.logger?.info("Completing overdue curing at $pos")
                val oldLeatherItem = data.placedItems.lastOrNull()
                if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {
                    completeCuring(pos, data, oldLeatherItem)
                }
                data.curingStartTime = null
                WorkstationDataManager.saveData()
            }
        }
    }
}

private fun LeatherBedBlockBehavior.Companion.completeCuring(
    pos: BlockPos,
    workstationData: WorkstationDataManager.WorkstationData,
    oldLeatherItem: PlacedItem,
) {
    val animals = listOf(
        "cow",
        "pig",
        "sheep",
        "chicken",
        "rabbit",
        "horse",
        "donkey",
        "mule",
        "llama",
        "goat",
        "cat",
        "wolf",
        "fox",
        "panda",
        "polar_bear",
        "ocelot",
        "camel"
    )
    val randomAnimal = animals.random()
    val curedLeatherId = "atom:animal_leather_cured_$randomAnimal"

    CraftEngineItems.byId(Key.of(curedLeatherId))?.let { curedItem ->
        val curedLeather = curedItem.buildItemStack()


        workstationData.placedItems.clear()
        val newPlacedItem = PlacedItem(
            item = curedLeather,
            position = oldLeatherItem.position,
            yaw = oldLeatherItem.yaw,
            displayUUID = oldLeatherItem.displayUUID
        )
        workstationData.placedItems.add(newPlacedItem)


        newPlacedItem.displayUUID?.let { uuid ->
            (Bukkit.getEntity(uuid) as? org.bukkit.entity.ItemDisplay)?.let { display ->

                Bukkit.getScheduler().runTask(Atom.instance!!, Runnable {
                    display.setItemStack(curedLeather)

                    display.location.world?.playSound(display.location, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f)
                    display.location.world?.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        display.location,
                        10,
                        0.3,
                        0.3,
                        0.3,
                        0.0
                    )
                })
            }
        }


        WorkstationDataManager.updatePlacedItems(pos, workstationData.placedItems)
        WorkstationDataManager.saveData()

        Atom.instance?.logger?.info("Leather cured successfully at $pos to $curedLeatherId")
    }
}


class LeatherBedBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : BlockEntity(Workstations.LEATHER_BED_ENTITY_TYPE, pos, blockState) {
    private var storedItem: ItemStack = ItemStack.empty()
        set(value) {
            field = value
            updateRender()
        }

    init {
        blockEntityRenderer = LeatherBedBlockDynamicRenderer(this)
    }

    override fun loadCustomData(tag: CompoundTag) {
        storedItem = tag.getItemStack("storedItem")
    }

    override fun saveCustomData(tag: CompoundTag) {
        tag.putItemStack("storedItem", storedItem)
    }

    fun updateRender() {
        val render = blockEntityRenderer as? LeatherBedBlockDynamicRenderer ?: return
        val chunk = world.getChunkAtIfLoaded(ChunkPos(pos)) ?: return
        val tracking = chunk.trackedBy
        tracking.forEach(render::update)
    }

    val location: Location
        get() = Location(
            world.world.platformWorld() as World,
            pos.x().toDouble(),
            pos.y().toDouble(),
            pos.z().toDouble()
        )

    fun hasItem(): Boolean = !storedItem.isEmpty
    fun startScraping(player: Player, item: ItemStack) {
        val atom = Atom.instance
        atom.launch(atom.regionDispatcher(location)) {
            val strokeCount = 20 + Random.nextInt(11)
            var currentStroke = 0

            ActionBarManager.sendStatus(player, "§7Scraping leather... Use the tool carefully")

            while (currentStroke < strokeCount && isActive) {
                delay(250)
                if (!player.isHandRaised || !LeatherBedBlockBehavior.isScrapingTool(player.activeItem)) {
                    ActionBarManager.sendStatus(player, "§cScraping cancelled - tool lowered")
                    delay(1000)
                    break
                }

                if (!player.isOnline || player.location.distance(location) > 5.0) break

                withContext(atom.entityDispatcher(player)) {
                    playScrapingEffects(player)
                }

                currentStroke++

                val prog = (currentStroke.toFloat() / strokeCount * 100).toInt()
                ActionBarManager.sendStatus(player, "§7Scraping leather... §e$prog%")
            }


            if (currentStroke >= strokeCount) {
                finishScraping(player, item)
            }

            ActionBarManager.clearStatus(player)
        }

    }

    val raw_leather_pattern = Regex("atom:animal_leather_raw_(.*)")
    val cured_leather_pattern = Regex("atom:animal_leather_cured_(.*)")

    private suspend fun finishScraping(player: Player, tool: ItemStack) {
        val center = location.add(0.5, 0.5, 0.5)

        if (storedItem.matches(raw_leather_pattern)) {
            val animalType = Items.getAnimalFromProduct(storedItem)

            center.world.dropItemNaturally(
                center,
                Items.getAnimalProduct(animalType, AnimalProduct.RawMeat).buildItemStack()
            )
            center.world.dropItemNaturally(
                center,
                Items.getAnimalProduct(animalType, AnimalProduct.Leather).buildItemStack()
            )
        } else {
            center.world.dropItemNaturally(center, storedItem)
        }

        storedItem = ItemStack.empty()

        if (SharpenedFlint.isSharpenedFlint(tool)) {
            SharpenedFlint.damageItem(tool, player, 0.3)
        }


        player.playSound(player.location, Sound.BLOCK_WOOL_BREAK, 1.0f, 1.0f)
        ActionBarManager.send(player, "§aScraped the leather successfully!")

        PlayerDataAPI.incrementInt(player, "leather_scraping.count", 0)
    }

    private fun playScrapingEffects(player: Player) {
        val location = location.add(0.5, 1.0, 0.5)
        location.world?.playSound(location, Sound.ITEM_BRUSH_BRUSHING_GENERIC, 1.0f, 1.0f)
        val dustOptions = Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.0f)
        location.world?.spawnParticle(
            Particle.DUST,
            location,
            10,
            0.2,
            0.2,
            0.2,
            0.0,
            dustOptions
        )
    }

    fun tryPlaceItem(player: Player, item: ItemStack): InteractionResult {
        if (!storedItem.isEmpty) {
            return InteractionResult.FAIL
        }

        val clone = item.clone()
        item.subtract(1)
        clone.amount = 1
        storedItem = clone
        return InteractionResult.SUCCESS
    }

    fun tryEmptyItems(player: Player, item: ItemStack): InteractionResult {
        if (storedItem.isEmpty) return InteractionResult.FAIL
        location.world.dropItemNaturally(location, storedItem)
        storedItem = ItemStack.empty()
        return InteractionResult.SUCCESS
    }
}

class LeatherBedBlockDynamicRenderer(entity: LeatherBedBlockEntity) : DynamicBlockEntityRenderer {
    override fun show(player: net.momirealms.craftengine.core.entity.player.Player) {
    }

    override fun hide(player: net.momirealms.craftengine.core.entity.player.Player) {
    }

    override fun update(player: net.momirealms.craftengine.core.entity.player.Player) {
        hide(player)
        show(player)
    }

}