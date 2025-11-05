package org.shotrush.atom.core.api;


public final class AtomAPI {
    
    private AtomAPI() {}

    public static final class Player {
        public static org.shotrush.atom.core.api.player.PlayerDataAPI Data = null;
        public static org.shotrush.atom.core.api.player.AttributeModifierAPI Attributes = null;
    }

    public static final class Item {
        public static org.shotrush.atom.core.api.item.ItemQualityAPI Quality = null;
        public static org.shotrush.atom.core.api.item.QualityInheritanceAPI Inheritance = null;
    }

    public static final class World {
        public static org.shotrush.atom.core.api.world.SimpleBlockBreakAPI BlockBreak = null;
        public static org.shotrush.atom.core.api.world.EnvironmentalFactorAPI Environment = null;
    }

    public static final class Combat {
        public static org.shotrush.atom.core.api.combat.ArmorProtectionAPI Armor = null;
        public static org.shotrush.atom.core.api.combat.TemperatureEffectsAPI Temperature = null;
    }

    public static final class Scheduler {
        
        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTask(org.bukkit.entity.Entity entity, Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(entity, task);
        }
        
        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTaskLater(org.bukkit.entity.Entity entity, Runnable task, long delay) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(entity, task, delay);
        }
        
        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTaskTimer(org.bukkit.entity.Entity entity, Runnable task, long delay, long period) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskTimer(entity, task, delay, period);
        }
        
        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTask(org.bukkit.Location location, Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(location, task);
        }
        
        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runTaskLater(org.bukkit.Location location, Runnable task, long delay) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(location, task, delay);
        }
        
        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runGlobalTask(Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runGlobalTask(task);
        }
        
        public static io.papermc.paper.threadedregions.scheduler.ScheduledTask runAsync(Runnable task) {
            return org.shotrush.atom.core.api.scheduler.SchedulerAPI.runAsync(task);
        }
    }
}
