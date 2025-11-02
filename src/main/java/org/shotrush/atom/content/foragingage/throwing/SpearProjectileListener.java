package org.shotrush.atom.content.foragingage.throwing;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.projectiles.CustomProjectile;

public class SpearProjectileListener implements Listener {
    
    private final Atom plugin;
    
    public SpearProjectileListener(Atom plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        
        ItemStack item = trident.getItemStack();

        CustomItem spearItem = plugin.getItemRegistry().getItem("wood_spear");
        if (spearItem == null || !spearItem.isCustomItem(item)) return;

        event.setCancelled(true);
        
        Location startLoc = trident.getLocation();
        Vector velocity = trident.getVelocity().multiply(0.5);
        Player shooter = (Player) trident.getShooter();

        assert shooter != null;
        ItemStack itemInHand = shooter.getInventory().getItemInMainHand();
        if (itemInHand.isSimilar(item)) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            ItemStack offHand = shooter.getInventory().getItemInOffHand();
            if (offHand.isSimilar(item)) {
                offHand.setAmount(offHand.getAmount() - 1);
            }
        }
        
        trident.remove();
        
        org.joml.Quaternionf baseRotation = new org.joml.Quaternionf()
            .rotateY((float) Math.toRadians(90))
            .rotateZ((float) Math.toRadians(45));
        
        CustomProjectile.ProjectileConfig config = new CustomProjectile.ProjectileConfig()
            .gravity(0.03)
            .airDrag(0.99)
            .maxLifetime(200)
            .damage(8.0)
            .interpolation(5, -1)
            .baseRotation(baseRotation)
            .scale(1.2f);
        
        CustomProjectile projectile = new CustomProjectile(
            plugin, startLoc, velocity, item, item, shooter, config
        );
        
        projectile.onEntityHit(hitEntity -> {
            hitEntity.damage(8.0, shooter);
            projectile.getDisplay().getLocation().getWorld().dropItemNaturally(
                projectile.getDisplay().getLocation(), item
            );
        });
        
        projectile.onBlockHit(blockHit -> {
            blockHit.getHitPosition().toLocation(startLoc.getWorld())
                .getWorld().dropItemNaturally(blockHit.getHitPosition().toLocation(startLoc.getWorld()), item);
        });
        
        projectile.start();
    }
}
