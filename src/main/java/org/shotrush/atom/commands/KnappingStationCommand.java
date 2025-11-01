package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 32)
@CommandAlias("knapping|knappingstation")
@Description("Get a knapping station")
public class KnappingStationCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.knapping")
    public void onKnapping(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "knapping_station");
    }
}
