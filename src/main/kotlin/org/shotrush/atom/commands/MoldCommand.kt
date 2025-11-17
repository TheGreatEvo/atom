package org.shotrush.atom.commands

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.shotrush.atom.item.Material
import org.shotrush.atom.item.ToolShape
import org.shotrush.atom.item.MoldType
import org.shotrush.atom.item.Molds

object MoldCommand {
    fun register() {
        commandTree("filled-mold") {
            aliases = arrayOf("fm")
            multiLiteralArgument("type", literals = arrayOf("wax", "fired"), false) {
                multiLiteralArgument("shape", literals = ToolShape.entries.map { it.id }.toTypedArray(), false) {
                    multiLiteralArgument("material", literals = Material.entries.map { it.id }.toTypedArray(), false) {
                        playerExecutor { player, args ->
                            val type = MoldType.byId(args["type"] as String)
                            val shape = ToolShape.byId(args["shape"] as String)
                            val material = Material.byId(args["material"] as String)
                            val mold = Molds.getFilledMold(shape, type, material)
                            player.inventory.addItem(mold)
                        }
                    }
                }
            }
        }
    }
}