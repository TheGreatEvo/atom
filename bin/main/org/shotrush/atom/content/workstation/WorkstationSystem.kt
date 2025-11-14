package org.shotrush.atom.content.workstation

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.content.workstation.leatherbed.LeatherBedBlockBehavior
import org.shotrush.atom.content.workstation.leatherbed.resumeCuringProcesses
import org.shotrush.atom.core.api.annotation.RegisterSystem


@RegisterSystem(
    id = "workstation_system",
    priority = 1,
    toggleable = false,
    description = "Manages workstation data persistence and initialization"
)
class WorkstationSystem(private val plugin: Plugin) {
    
    init {
        
        Workstations.init()
        
        
        plugin.logger.info("Initializing Workstation System...")
        WorkstationDataManager.initialize()
        
        
        
        
        Bukkit.getAsyncScheduler().runDelayed(plugin, { _ ->
            LeatherBedBlockBehavior.Companion.resumeCuringProcesses()
        }, 1, java.util.concurrent.TimeUnit.SECONDS)
        
        plugin.logger.info("Workstation System initialized successfully")
    }
}
