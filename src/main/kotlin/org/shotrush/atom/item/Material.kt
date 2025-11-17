package org.shotrush.atom.item

import org.bukkit.Color

enum class Material(val id: String, val tier: Int, val rgb: Color) {
    Stone("stone", 0, Color.fromRGB(128, 128, 128)),
    Copper("copper", 1, Color.fromRGB(228, 128, 101)),
    Bronze("bronze", 2, Color.fromRGB(200, 100, 50)),
    Iron("iron", 3, Color.fromRGB(210, 210, 210)),
    Steel("steel", 4, Color.fromRGB(128, 128, 128));

    val pastTiers: List<Material> by lazy { MaterialOrderedByTier.take(tier + 1) }

    companion object {
        val MaterialById = Material.entries.associateBy { it.id }
        val MaterialOrderedByTier = Material.entries.sortedBy { it.tier }

        fun byId(id: String) = MaterialById[id] ?: error("No such Material: $id")
    }
}