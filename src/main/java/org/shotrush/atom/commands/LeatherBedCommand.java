package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 34)
@CommandAlias("leatherbed|leatherrack")
@Description("Get a leather drying bed")
public class LeatherBedCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.leatherbed")
    public void onLeatherBed(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "leather_bed");
    }
}
