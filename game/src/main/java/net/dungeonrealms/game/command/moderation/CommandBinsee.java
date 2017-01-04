package net.dungeonrealms.game.command.moderation;

import net.dungeonrealms.common.game.command.BaseCommand;
import net.dungeonrealms.common.game.database.DatabaseAPI;
import net.dungeonrealms.common.game.database.data.EnumData;
import net.dungeonrealms.common.game.database.player.rank.Rank;
import net.dungeonrealms.game.mastery.ItemSerialization;
import net.dungeonrealms.game.player.banks.BankMechanics;
import net.dungeonrealms.game.player.banks.Storage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Created by Brad on 25/12/2016.
 */
public class CommandBinsee extends BaseCommand {
    public CommandBinsee(String command, String usage, String description, List<String> aliases) {
        super(command, usage, description, aliases);
    }

    public static Map<UUID, UUID> offline_bin_watchers = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String string, String[] args) {
        if (s instanceof ConsoleCommandSender) return false;
        Player sender = (Player) s;

        if (!Rank.isGM(sender)) return false;

        if (args.length == 0) {
            s.sendMessage(usage);
            return true;
        }

        String playerName = args[0];
        if (Bukkit.getPlayer(playerName) != null) {
            Storage storage = BankMechanics.getInstance().getStorage(Bukkit.getPlayer(playerName).getUniqueId());
            sender.openInventory(storage.collection_bin);
        }
        else {
            if (DatabaseAPI.getInstance().getUUIDFromName(playerName).equals("")) {
                sender.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + playerName + ChatColor.RED + " does not exist in our database.");
                return true;
            }

            UUID p_uuid = UUID.fromString(DatabaseAPI.getInstance().getUUIDFromName(playerName));

            // check if they're logged in on another shard
            if ((Boolean)DatabaseAPI.getInstance().getData(EnumData.IS_PLAYING, p_uuid)) {
                String shard = DatabaseAPI.getInstance().getFormattedShardName(p_uuid);
                sender.sendMessage(ChatColor.RED + "That player is currently playing on shard " + shard + ". " +
                        "Please banksee on that shard to avoid concurrent modification.");
                return false;
            }

            String stringInv = (String) DatabaseAPI.getInstance().getData(EnumData.INVENTORY_COLLECTION_BIN, p_uuid);
            Inventory inv = null;
            if (stringInv.length() > 1) {
                inv = ItemSerialization.fromString(stringInv);
                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() == Material.AIR) {
                        inv.addItem(item);
                    }
                }
                Player p = Bukkit.getPlayer(p_uuid);
                if (p != null)
                    p.sendMessage(ChatColor.RED + "You have items in your collection bin!");
            }
            else {
                sender.sendMessage(ChatColor.RED + "That player's collection bin is empty.");
                return false;
            }

            offline_bin_watchers.put(sender.getUniqueId(), p_uuid);
            sender.openInventory(inv);
        }
        return false;
    }
}