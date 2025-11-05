package org.shotrush.atom.content.foragingage.workstations.leatherbed;

import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

@AutoRegisterSystem(priority = 5)
public class LeatherBedHandler {
    
    private final Atom plugin;
    
    public LeatherBedHandler(Plugin plugin) {
        this.plugin = (Atom) plugin;
    }

}
