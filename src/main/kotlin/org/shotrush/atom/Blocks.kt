package org.shotrush.atom

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.util.Key
import kotlin.reflect.KProperty

object Blocks {
    init {
        println("Loading blocks")
        CraftEngineBlocks.loadedBlocks().forEach { key -> println(key) }
    }
    val Pebble by block("craftengine:custom_0")
}

fun block(key: Key) = CEBlock(key)
fun block(key: String) = block(Key.of(key))

data class CEBlock(val key: Key) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): CustomBlock {
        return CraftEngineBlocks.byId(key) ?: throw IllegalStateException("Block $key not found!")
    }
}