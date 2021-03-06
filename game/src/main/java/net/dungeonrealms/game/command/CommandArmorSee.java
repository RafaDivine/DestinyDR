package net.dungeonrealms.game.command;

import net.dungeonrealms.common.game.command.BaseCommand;
import net.dungeonrealms.common.game.database.player.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class CommandArmorSee extends BaseCommand {

    public CommandArmorSee(String command, String usage, String description) {
        super(command, usage, description,"", Arrays.asList("drarmorsee"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;
        if (!Rank.isGM(p))
            return true;

        String ent_name = null;
        Inventory inv = null;
        if (args.length >= 1) {
            ent_name = args[0];
            if (Bukkit.getPlayer(ent_name) != null) {
                inv = Bukkit.createInventory(null, 9, "ARMOR OF " + ent_name);
                Player victim = Bukkit.getPlayer(ent_name);
                for (ItemStack is : victim.getInventory().getArmorContents()) {
                    if (is != null && is.getType() != Material.AIR)
                        inv.addItem(CraftItemStack.asCraftCopy(is));
                }
                if (victim.getInventory().getItemInOffHand() != null && victim.getInventory().getItemInOffHand().getType() != Material.AIR)
                    inv.addItem(victim.getInventory().getItemInOffHand());
            } else {
                p.sendMessage(ChatColor.RED + "The player " + ent_name + "'s armor data is not loaded, and therefore cannot be displayed.");

                p.sendMessage(ChatColor.GRAY + "In a later update, I will make it possible to view offline armor data" +
                        ".");
                return true;
            }
        } else { // get entity the player is looking at
            LivingEntity ent = null;
            for (Entity e : p.getNearbyEntities(20, 20, 20)) {
                if (!(e instanceof LivingEntity)) continue;
                if (p.hasLineOfSight(e)) {
                    ent = (LivingEntity) e;
                    break;
                }
            }
            if (ent == null) return true; // nothing found
            ent_name = ent.getCustomName();
            inv = Bukkit.createInventory(null, 9, "EQUIPMENT OF " + ent.getCustomName());
            for (ItemStack is : ent.getEquipment().getArmorContents()) {
                if (is != null && is.getType() != Material.AIR)
                    inv.addItem(CraftItemStack.asCraftCopy(is));
            }
            if(ent.getEquipment().getItemInOffHand() != null) inv.addItem(CraftItemStack.asCraftCopy(ent.getEquipment().getItemInOffHand()));
            inv.addItem(CraftItemStack.asCraftCopy(ent.getEquipment().getItemInMainHand()));
        }

        if (inv != null) {
            p.openInventory(inv);
            p.sendMessage(ChatColor.GREEN + "Displaying the current armor contents of " + ent_name);
        }

        return true;
    }

}
