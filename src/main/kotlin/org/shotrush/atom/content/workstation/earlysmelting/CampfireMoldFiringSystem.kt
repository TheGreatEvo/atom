package org.shotrush.atom.content.systems

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.data.Lightable
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.shotrush.atom.Atom
import org.shotrush.atom.content.systems.groundstorage.GroundItemUtils
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.core.api.annotation.RegisterSystem
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds
import java.util.concurrent.ConcurrentHashMap

@RegisterSystem(
    id = "campfire_mold_firing",
    priority = 7,
    toggleable = true,
    description = "Fires clay molds placed on straw when a campfire above is lit",
    enabledByDefault = true
)
class CampfireMoldFiringSystem(private val plugin: Plugin) : Listener {

    companion object {
        const val FIRING_TIME_MS = 10 * 60 * 1000L // 10 minutes
        const val CHECK_INTERVAL_TICKS = 40L // Check every 2 seconds
        val activeFiring = ConcurrentHashMap<Location, FiringJob>()

        data class FiringJob(
            val job: Job,
            val startTime: Long,
            val frameLocation: Location
        )
    }

    private var checkJob: Job? = null

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        // Start periodic checker and resume firing on startup
        val atom = Atom.instance
        atom.launch {
            delay(2000L)
            resumeFiringProcesses()
            startPeriodicChecker()
        }
    }

    private fun startPeriodicChecker() {
        val atom = Atom.instance
        checkJob = atom.launch {
            while (true) {
                delay(CHECK_INTERVAL_TICKS * 50L) // Convert ticks to ms
                checkForNewFiringSetups()
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onCampfirePlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return
        val block = event.blockPlaced
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) return

        // Check if there's a straw block with clay mold below
        val strawBlock = block.location.clone().add(0.0, -1.0, 0.0).block
        val frame = GroundItemUtils.findGroundItemAbove(strawBlock)
        val item = frame?.let { GroundItemUtils.isObstructed(it, customKey = Key.of("atom:straw")) }
        if (item != null && Molds.isEmptyMold(item) && Molds.getMoldType(item) == MoldType.Clay) {
            // Wait a tick to ensure block data is set
            Atom.instance.launch {
                delay(50L)
                val data = block.blockData
                if (data is Lightable && data.isLit) {
                    startFiring(strawBlock.location, frame)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onCampfireInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) return

        val atom = Atom.instance

        Atom.instance.launch(atom.regionDispatcher(block.location)) {
            delay(50L)
            val data = block.blockData
            if (data !is Lightable) return@launch

            val strawBlock = block.location.clone().add(0.0, -1.0, 0.0).block
            val frame = GroundItemUtils.findGroundItemAbove(strawBlock) ?: return@launch
            val item = GroundItemUtils.isObstructed(frame, customKey = Key.of("atom:straw"))
            if (item != null && Molds.isEmptyMold(item) && Molds.getMoldType(item) == MoldType.Clay) {
                if (data.isLit) {
                    // Campfire was lit
                    if (!activeFiring.containsKey(strawBlock.location)) {
                        startFiring(strawBlock.location, frame)
                    }
                } else {
                    // Campfire was extinguished
                    cancelFiring(strawBlock.location)
                }
            }
        }
    }

    private fun checkForNewFiringSetups() {
        // Check all existing workstation data for setups that should be firing
        WorkstationDataManager.getAllWorkstations().forEach { (_, data) ->
            if (data.type == "campfire" && data.curingStartTime != null) {
                val pos = data.position
                val campfireLocation = Location(
                    plugin.server.getWorld("world"),
                    pos.x().toDouble(),
                    pos.y().toDouble(),
                    pos.z().toDouble()
                )
                val strawBlockLocation = campfireLocation.clone().add(0.0, -1.0, 0.0)

                // Launch on region dispatcher for thread safety
                val atom = Atom.instance
                atom.launch(atom.regionDispatcher(strawBlockLocation)) {
                    // Check if this location should still be firing
                    if (!activeFiring.containsKey(strawBlockLocation)) {
                        if (shouldBeFiring(strawBlockLocation)) {
                            val frame = GroundItemUtils.findGroundItemAbove(strawBlockLocation.block)
                            if (frame != null) {
                                val remaining = FIRING_TIME_MS - (System.currentTimeMillis() - data.curingStartTime!!)
                                if (remaining > 0) {
                                    resumeFiring(strawBlockLocation, frame, data.curingStartTime!!)
                                }
                            }
                        } else {
                            // Setup no longer valid, remove data
                            WorkstationDataManager.removeWorkstationData(pos)
                        }
                    }
                }
            }
        }
    }

    private fun shouldBeFiring(strawBlockLocation: Location): Boolean {
        val strawBlock = strawBlockLocation.block
        val campfire = strawBlockLocation.clone().add(0.0, 1.0, 0.0).block

        // Check for lit campfire
        if (campfire.type != Material.CAMPFIRE && campfire.type != Material.SOUL_CAMPFIRE) return false
        val data = campfire.blockData
        if (data !is Lightable || !data.isLit) return false

        // Check for straw block and clay mold
        val frame = GroundItemUtils.findGroundItemAbove(strawBlock) ?: return false
        val item = GroundItemUtils.isObstructed(frame, customKey = Key.of("atom:straw")) ?: return false
        if (!Molds.isEmptyMold(item) || Molds.getMoldType(item) != MoldType.Clay) return false

        return true
    }

    private fun startFiring(strawBlockLocation: Location, frame: ItemFrame) {
        // Cancel any existing firing at this location
        activeFiring[strawBlockLocation]?.job?.cancel()

        val atom = Atom.instance
        val startTime = System.currentTimeMillis()

        // Debug logging
        Atom.instance?.logger?.info("=== STARTING MOLD FIRING ===")
        Atom.instance?.logger?.info("  Straw block: ${strawBlockLocation.blockX}, ${strawBlockLocation.blockY}, ${strawBlockLocation.blockZ}")
        Atom.instance?.logger?.info("  Frame location: ${frame.location.x}, ${frame.location.y}, ${frame.location.z}")

        // Save to campfire workstation data
        val campfireLocation = strawBlockLocation.clone().add(0.0, 1.0, 0.0)
        val pos = net.momirealms.craftengine.core.world.BlockPos(
            campfireLocation.blockX,
            campfireLocation.blockY,
            campfireLocation.blockZ
        )
        val data = WorkstationDataManager.getWorkstationData(pos, "campfire")
        data.curingStartTime = startTime
        WorkstationDataManager.saveData()

        Atom.instance?.logger?.info("  Saved data at: $pos")

        val job = atom.launch(atom.regionDispatcher(strawBlockLocation)) {
            delay(FIRING_TIME_MS)

            completeFiring(strawBlockLocation, frame)
            activeFiring.remove(strawBlockLocation)

            // Remove from campfire workstation data
            val campfireLocation = strawBlockLocation.clone().add(0.0, 1.0, 0.0)
            val campfirePos = net.momirealms.craftengine.core.world.BlockPos(
                campfireLocation.blockX,
                campfireLocation.blockY,
                campfireLocation.blockZ
            )
            WorkstationDataManager.removeWorkstationData(campfirePos)
        }

        activeFiring[strawBlockLocation] = FiringJob(job, startTime, frame.location)

        // Visual feedback
        strawBlockLocation.world?.playSound(strawBlockLocation, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
    }

    private fun resumeFiring(strawBlockLocation: Location, frame: ItemFrame, originalStartTime: Long) {
        val atom = Atom.instance
        val elapsed = System.currentTimeMillis() - originalStartTime
        val remaining = FIRING_TIME_MS - elapsed

        val campfireLocation = strawBlockLocation.clone().add(0.0, 1.0, 0.0)
        val pos = net.momirealms.craftengine.core.world.BlockPos(
            campfireLocation.blockX,
            campfireLocation.blockY,
            campfireLocation.blockZ
        )

        val job = atom.launch(atom.regionDispatcher(strawBlockLocation)) {
            delay(remaining)
            completeFiring(strawBlockLocation, frame)
            activeFiring.remove(strawBlockLocation)
            WorkstationDataManager.removeWorkstationData(pos)
        }

        activeFiring[strawBlockLocation] = FiringJob(job, originalStartTime, frame.location)
        Atom.instance?.logger?.info("  ✓ Resumed firing at ($pos) - ${remaining/1000}s remaining")
    }

    private fun cancelFiring(strawBlockLocation: Location) {
        activeFiring[strawBlockLocation]?.job?.cancel()
        activeFiring.remove(strawBlockLocation)

        val campfireLocation = strawBlockLocation.clone().add(0.0, 1.0, 0.0)
        val pos = net.momirealms.craftengine.core.world.BlockPos(
            campfireLocation.blockX,
            campfireLocation.blockY,
            campfireLocation.blockZ
        )
        WorkstationDataManager.removeWorkstationData(pos)

        Atom.instance?.logger?.info("  Cancelled firing at: $pos")
    }

    private fun completeFiring(strawBlockLocation: Location, frame: ItemFrame) {
        val item = GroundItemUtils.getGroundItem(frame) ?: return
        if (!Molds.isEmptyMold(item)) return

        val shape = Molds.getMoldShape(item)
        val firedMold = Molds.getMold(shape, MoldType.Fired).buildItemStack()

        GroundItemUtils.setGroundItem(frame, firedMold, false)

        // Effects
        strawBlockLocation.world?.playSound(strawBlockLocation, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.2f)
        strawBlockLocation.world?.spawnParticle(
            Particle.FLAME,
            strawBlockLocation,
            20,
            0.3,
            0.3,
            0.3,
            0.02
        )

        Atom.instance?.logger?.info("  ✓ Completed firing at: ${strawBlockLocation.blockX}, ${strawBlockLocation.blockY}, ${strawBlockLocation.blockZ}")
    }

    private fun resumeFiringProcesses() {
        Atom.instance?.logger?.info("=== Resuming mold firing processes ===")
        var resumed = 0
        var expired = 0

        val allData = WorkstationDataManager.getAllWorkstations()
        Atom.instance?.logger?.info("  Total workstations: ${allData.size}")

        allData.forEach { (_, data) ->
            if (data.type == "campfire" && data.curingStartTime != null) {
                val pos = data.position
                val startTime = data.curingStartTime!!
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = FIRING_TIME_MS - elapsed

                val campfireLocation = Location(
                    plugin.server.getWorld("world"),
                    pos.x().toDouble(),
                    pos.y().toDouble(),
                    pos.z().toDouble()
                )
                val strawBlockLocation = campfireLocation.clone().add(0.0, -1.0, 0.0)

                // Check if chunk is loaded before accessing blocks
                val chunk = strawBlockLocation.chunk
                if (!chunk.isLoaded) {
                    Atom.instance?.logger?.info("  ✗ Chunk not loaded at ($pos), skipping")
                    return@forEach
                }

                Atom.instance?.logger?.info("  Checking pos: ${pos.x()}, ${pos.y()}, ${pos.z()}")

                val frame = GroundItemUtils.findGroundItemAbove(strawBlockLocation.block)

                if (frame != null && shouldBeFiring(strawBlockLocation)) {
                    Atom.instance?.logger?.info("  Found ground item - Frame at: ${frame.location.x}, ${frame.location.y}, ${frame.location.z}")

                    if (remaining > 0) {
                        resumeFiring(strawBlockLocation, frame, startTime)
                        resumed++
                    } else {
                        completeFiring(strawBlockLocation, frame)
                        WorkstationDataManager.removeWorkstationData(pos)
                        expired++
                        Atom.instance?.logger?.info("  ✗ Completed expired firing at ($pos)")
                    }
                } else {
                    Atom.instance?.logger?.info("  ✗ Setup no longer valid at ($pos), removing data")
                    WorkstationDataManager.removeWorkstationData(pos)
                }
            }
        }

        Atom.instance?.logger?.info("=== Mold Firing Resume Summary ===")
        Atom.instance?.logger?.info("  Resumed: $resumed processes")
        Atom.instance?.logger?.info("  Expired: $expired molds")
    }

    fun shutdown() {
        checkJob?.cancel()
        activeFiring.values.forEach { it.job.cancel() }
        activeFiring.clear()
    }
}