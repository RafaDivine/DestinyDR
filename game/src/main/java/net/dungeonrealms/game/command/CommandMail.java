package net.dungeonrealms.game.command;

import net.dungeonrealms.GameAPI;
import net.dungeonrealms.common.game.command.BaseCommand;
import net.dungeonrealms.common.game.database.player.rank.Rank;
import net.dungeonrealms.game.handler.MailHandler;
import net.dungeonrealms.game.player.banks.BankMechanics;
import net.dungeonrealms.game.player.inventory.PlayerMenus;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Nick on 10/15/2015.
 */
public class CommandMail extends BaseCommand {

    public CommandMail(String command, String usage, String description) {
        super(command, usage, description);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String string, String[] args) {
        if (s instanceof ConsoleCommandSender) return false;

        Player player = (Player) s;

        if (Rank.isDev(player)) { // @todo: We can make this public at a later date.
            if (args.length == 2) {
                if (args[0].equals("send")) {
                    if (player.getEquipment().getItemInMainHand() != null && player.getEquipment().getItemInMainHand().getType() != Material.AIR) {
                        if (!player.getName().equals(args[1])) {
                            if (BankMechanics.getInstance().takeGemsFromInventory(5, player)) {
                                if (GameAPI.isItemTradeable(player.getEquipment().getItemInMainHand())) {
                                    if (MailHandler.getInstance().sendMail(player, args[1], player.getEquipment().getItemInMainHand())) {
                                        player.getEquipment().setItemInMainHand(null);
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "This item cannot be sent via mail.");
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "There is a " + ChatColor.UNDERLINE + "5 GEM" + ChatColor.RESET + ChatColor.RED + " fee to send mail.");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You cannot send mail to yourself.");
                        }
                    } else {
                        return true;
                    }
                }
                return true;
            }
        }

        PlayerMenus.openMailInventory(player);

        return true;

    }
}