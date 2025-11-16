package org.shotrush.atom.content.workstation.campfire.features

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
import org.bukkit.entity.ItemDisplay
import org.shotrush.atom.Atom
import org.shotrush.atom.content.systems.groundstorage.GroundItemUtils
import org.shotrush.atom.content.systems.groundstorage.GroundItemDisplayUtils
import org.shotrush.atom.content.workstation.campfire.CampfireRegistry
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

class MoldFiringFeature(
    private val firingMs: Long = 5 * 60 * 1000L,
) : CampfireRegistry.Listener {

    data class FiringJob(val job: Job, val startedAt: Long, val items: List<Location>)

    private val active = mutableMapOf<Location, FiringJob>()
    private val atom get() = Atom.instance

    override fun onCampfireLit(state: CampfireRegistry.CampfireState) {
        // If valid setup below, start or resume firing for all valid molds
        val strawLoc = state.location.clone().add(0.0, -1.0, 0.0)
        atom.launch(atom.regionDispatcher(strawLoc)) {
            val items = findAllValidClayMoldItems(strawLoc)
            if (items.isEmpty()) return@launch
            
            val start = state.startTime ?: System.currentTimeMillis()
            val remaining = firingMs - (System.currentTimeMillis() - start)
            if (remaining > 0) {
                scheduleFiring(strawLoc, items, remaining, start)
            } else {
                completeFiring(strawLoc, items)
            }
        }
    }

    override fun onCampfireExtinguished(state: CampfireRegistry.CampfireState, reason: String) {
        // Extinguish cancels firing, but does not complete
        val strawLoc = state.location.clone().add(0.0, -1.0, 0.0)
        active.remove(strawLoc)?.job?.cancel()
    }

    override fun onCampfireBroken(state: CampfireRegistry.CampfireState) {
        val strawLoc = state.location.clone().add(0.0, -1.0, 0.0)
        active.remove(strawLoc)?.job?.cancel()
    }

    override fun onResumeTimerScheduled(state: CampfireRegistry.CampfireState, remainingMs: Long) {
        // On startup when we resume a lit campfire, check mold and resume firing
        onCampfireLit(state)
    }

    override fun onResumeTimerExpired(state: CampfireRegistry.CampfireState) {
        // If registry decided it’s expired and unlit now, treat as extinguished
        onCampfireExtinguished(state, "expired-on-resume")
    }

    private fun scheduleFiring(strawLoc: Location, items: List<Any>, remaining: Long, startedAt: Long) {
        // Cancel existing
        active.remove(strawLoc)?.job?.cancel()

        val job = atom.launch(atom.regionDispatcher(strawLoc)) {
            delay(remaining)
            // Verify still valid and campfire lit
            val camp = strawLoc.clone().add(0.0, 1.0, 0.0).block
            val data = camp.blockData as? Lightable
            if (camp.type == Material.CAMPFIRE || camp.type == Material.SOUL_CAMPFIRE) {
                if (data != null && data.isLit && findAllValidClayMoldItems(strawLoc).isNotEmpty()) {
                    completeFiring(strawLoc, items)
                }
            }
            active.remove(strawLoc)
        }
        val itemLocs = items.map { item ->
            when (item) {
                is ItemFrame -> item.location
                is ItemDisplay -> item.location
                else -> strawLoc
            }
        }
        active[strawLoc] = FiringJob(job, startedAt, itemLocs)
        Atom.instance.logger.info("Mold firing scheduled for ${items.size} molds at ${strawLoc.blockX},${strawLoc.blockY},${strawLoc.blockZ} ${remaining / 1000}s")
    }

    private fun completeFiring(strawLoc: Location, items: List<Any>) {
        val world = strawLoc.world ?: return
        
        for (item in items) {
            val moldItem = when (item) {
                is ItemFrame -> GroundItemUtils.getGroundItem(item)
                is ItemDisplay -> GroundItemDisplayUtils.getGroundItem(item)
                else -> null
            } ?: continue
            
            if (!Molds.isMold(moldItem)) continue
            val shape = Molds.getMoldShape(moldItem)
            val fired = Molds.getMold(shape, MoldType.Fired).buildItemStack()
            
            when (item) {
                is ItemFrame -> GroundItemUtils.setGroundItem(item, fired, false)
                is ItemDisplay -> GroundItemDisplayUtils.setGroundItem(item, fired, false)
            }
        }

        world.playSound(strawLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.2f)
        world.spawnParticle(Particle.FLAME, strawLoc, 20, 0.3, 0.3, 0.3, 0.02)
        Atom.instance.logger.info("Mold firing completed for ${items.size} molds at ${strawLoc.blockX},${strawLoc.blockY},${strawLoc.blockZ}")
    }

    private fun findValidClayMoldItem(strawLoc: Location): Any? {
        // Try ItemDisplay first (new system)
        val display = GroundItemDisplayUtils.findClosestGroundItem(strawLoc)
        if (display != null) {
            val item = GroundItemDisplayUtils.isObstructed(display, customKey = Key.of("atom:straw"))
            if (item != null && Molds.isMold(item) && Molds.getMoldType(item) == MoldType.Clay) {
                // Verify campfire above is lit
                val camp = strawLoc.clone().add(0.0, 1.0, 0.0).block
                if (camp.type != Material.CAMPFIRE && camp.type != Material.SOUL_CAMPFIRE) return null
                val data = camp.blockData as? Lightable ?: return null
                if (!data.isLit) return null
                return display
            }
        }
        
        // Fallback to ItemFrame (old system)
        val frame = GroundItemUtils.findClosestGroundItem(strawLoc) ?: return null
        val item = GroundItemUtils.isObstructed(frame, customKey = Key.of("atom:straw")) ?: return null
        if (!Molds.isMold(item) || Molds.getMoldType(item) != MoldType.Clay) return null

        // Campfire above must be lit
        val camp = strawLoc.clone().add(0.0, 1.0, 0.0).block
        if (camp.type != Material.CAMPFIRE && camp.type != Material.SOUL_CAMPFIRE) return null
        val data = camp.blockData as? Lightable ?: return null
        if (!data.isLit) return null

        return frame
    }

    private fun findAllValidClayMoldItems(strawLoc: Location): List<Any> {
        val validItems = mutableListOf<Any>()
        
        // Try ItemDisplay first (new system)
        val displays = GroundItemDisplayUtils.findAllGroundItems(strawLoc)
        for (display in displays) {
            val item = GroundItemDisplayUtils.isObstructed(display, customKey = Key.of("atom:straw"))
            if (item != null && Molds.isMold(item) && Molds.getMoldType(item) == MoldType.Clay) {
                // Verify campfire above is lit
                val camp = strawLoc.clone().add(0.0, 1.0, 0.0).block
                if (camp.type != Material.CAMPFIRE && camp.type != Material.SOUL_CAMPFIRE) continue
                val data = camp.blockData as? Lightable ?: continue
                if (!data.isLit) continue
                validItems.add(display)
            }
        }
        
        // If we found items with new system, don't check old system
        if (validItems.isNotEmpty()) return validItems
        
        // Fallback to ItemFrame (old system)
        val frames = GroundItemUtils.findAllGroundItems(strawLoc)
        for (frame in frames) {
            val item = GroundItemUtils.isObstructed(frame, customKey = Key.of("atom:straw")) ?: continue
            if (!Molds.isMold(item) || Molds.getMoldType(item) != MoldType.Clay) continue

            // Campfire above must be lit
            val camp = strawLoc.clone().add(0.0, 1.0, 0.0).block
            if (camp.type != Material.CAMPFIRE && camp.type != Material.SOUL_CAMPFIRE) continue
            val data = camp.blockData as? Lightable ?: continue
            if (!data.isLit) continue
            
            validItems.add(frame)
        }

        return validItems
    }
}