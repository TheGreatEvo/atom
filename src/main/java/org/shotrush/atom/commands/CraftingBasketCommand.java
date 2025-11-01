package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;

@AutoRegister(priority = 33)
@CommandAlias("craftingbasket|basket")
@Description("Get a crafting basket")
public class CraftingBasketCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.craftingbasket")
    public void onCraftingBasket(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "crafting_basket");
    }
}
