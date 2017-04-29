package net.dungeonrealms.game.command.dungeon;

import net.dungeonrealms.GameAPI;
import net.dungeonrealms.common.game.command.BaseCommand;
import net.dungeonrealms.game.mastery.MetadataUtils;
import net.dungeonrealms.game.mechanic.dungeons.DungeonManager;
import net.dungeonrealms.game.world.entity.EnumEntityType;
import net.dungeonrealms.game.world.entity.type.monster.boss.type.Burick;
import net.dungeonrealms.game.world.entity.type.monster.boss.type.InfernalAbyss;
import net.dungeonrealms.game.world.entity.type.monster.boss.type.Mayel;
import net.dungeonrealms.game.world.entity.util.EntityStats;
import net.minecraft.server.v1_9_R2.Entity;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Created by Kieran Quigley (Proxying) on 17-Jun-16.
 */
public class BossTeleport extends BaseCommand {
    public BossTeleport(String command, String usage, String description) {
        super(command, usage, description);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 3) return true;
        if (!(sender instanceof BlockCommandSender)) return true;

        BlockCommandSender bcs = (BlockCommandSender) sender;
        if (!bcs.getBlock().getWorld().getName().contains("DUNGEON")) return true;
        DungeonManager.DungeonObject dungeonObject = DungeonManager.getInstance().getDungeon(bcs.getBlock().getWorld());
        if (!dungeonObject.canSpawnBoss) {
            if (!DungeonManager.getInstance().isAllOppedPlayers(bcs.getBlock().getWorld())) {
                int percentToKill = (int) (dungeonObject.maxAlive * 0.80);
                int killed = dungeonObject.killed;
                for (Player p : bcs.getBlock().getWorld().getPlayers()) {
                    p.sendMessage(ChatColor.RED + "You need to kill " + ChatColor.UNDERLINE + (percentToKill - killed) + ChatColor.RED + " monsters to spawn the boss.");
                }
                return true;
            }
        }
        if (dungeonObject.hasBossSpawned || dungeonObject.beingRemoved) {
            return true;
        }

        double x = Double.parseDouble(args[0]);
        double y = Double.parseDouble(args[1]);
        double z = Double.parseDouble(args[2]);

        for (Player player : bcs.getBlock().getWorld().getPlayers()) {
            player.teleport(new Location(player.getWorld(), x, y + 2, z));
            player.setFallDistance(0.0F);
        }

        dungeonObject.getType().spawnBoss(bcs.getBlock().getLocation());
        
        return false;
    }
}
