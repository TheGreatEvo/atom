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
import org.shotrush.atom.Atom
import org.shotrush.atom.content.systems.groundstorage.GroundItemUtils
import org.shotrush.atom.content.workstation.campfire.CampfireRegistry
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

class MoldFiringFeature(
    private val firingMs: Long = 5 * 60 * 1000L,
) : CampfireRegistry.Listener {

    data class FiringJob(val job: Job, val startedAt: Long, val frameLoc: Location)

    private val active = mutableMapOf<Location, FiringJob>()
    private val atom get() = Atom.instance

    override fun onCampfireLit(state: CampfireRegistry.CampfireState) {
        // If valid setup below, start or resume a firing tied to the same startTime
        val strawLoc = state.location.clone().add(0.0, -1.0, 0.0)
        atom.launch(atom.regionDispatcher(strawLoc)) {
            val frame = findValidClayMoldFrame(strawLoc) ?: return@launch
            val start = state.startTime ?: System.currentTimeMillis()
            val remaining = firingMs - (System.currentTimeMillis() - start)
            if (remaining > 0) {
                scheduleFiring(strawLoc, frame, remaining, start)
            } else {
                completeFiring(strawLoc, frame)
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

    private fun scheduleFiring(strawLoc: Location, frame: ItemFrame, remaining: Long, startedAt: Long) {
        // Cancel existing
        active.remove(strawLoc)?.job?.cancel()

        val job = atom.launch(atom.regionDispatcher(strawLoc)) {
            delay(remaining)
            // Verify still valid and campfire lit
            val camp = strawLoc.clone().add(0.0, 1.0, 0.0).block
            val data = camp.blockData as? Lightable
            if (camp.type == Material.CAMPFIRE || camp.type == Material.SOUL_CAMPFIRE) {
                if (data != null && data.isLit && findValidClayMoldFrame(strawLoc) != null) {
                    completeFiring(strawLoc, frame)
                }
            }
            active.remove(strawLoc)
        }
        active[strawLoc] = FiringJob(job, startedAt, frame.location)
        Atom.instance.logger.info("Mold firing scheduled at ${strawLoc.blockX},${strawLoc.blockY},${strawLoc.blockZ} ${remaining / 1000}s")
    }

    private fun completeFiring(strawLoc: Location, frame: ItemFrame) {
        val item = GroundItemUtils.getGroundItem(frame) ?: return
        if (!Molds.isMold(item)) return
        val shape = Molds.getMoldShape(item)
        val fired = Molds.getMold(shape, MoldType.Fired).buildItemStack()
        GroundItemUtils.setGroundItem(frame, fired, false)

        val w = strawLoc.world ?: return
        w.playSound(strawLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.2f)
        w.spawnParticle(Particle.FLAME, strawLoc, 20, 0.3, 0.3, 0.3, 0.02)
        Atom.instance.logger.info("Mold firing completed at ${strawLoc.blockX},${strawLoc.blockY},${strawLoc.blockZ}")
    }

    private fun findValidClayMoldFrame(strawLoc: Location): ItemFrame? {
        // Straw: custom "atom:straw" under campfire area (using your obstructed semantic)
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
}