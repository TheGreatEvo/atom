package org.shotrush.atom.item

import org.bukkit.Color

enum class Material(val id: String, val rgb: Color) {
    Stone("stone", Color.fromRGB(128, 128, 128)),
    Copper("copper", Color.fromRGB(228, 128, 101)),
    Bronze("bronze", Color.fromRGB(200, 100, 50)),
    Iron("iron", Color.fromRGB(210, 210, 210)),
    Steel("steel", Color.fromRGB(128, 128, 128));

    companion object {
        val MaterialById = Material.entries.associateBy { it.id }

        fun byId(id: String) = MaterialById[id] ?: error("No such Material: $id")
    }
}