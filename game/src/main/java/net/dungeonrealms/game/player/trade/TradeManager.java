package net.dungeonrealms.game.player.trade;

import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.GameAPI;
import net.dungeonrealms.game.mechanic.ParticleAPI;
import net.dungeonrealms.game.mechanic.generic.EnumPriority;
import net.dungeonrealms.game.mechanic.generic.GenericMechanic;
import net.dungeonrealms.game.player.combat.CombatLog;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Chase on Nov 16, 2015
 */
public class TradeManager implements GenericMechanic {

    public static ArrayList<Trade> trades = new ArrayList<>();

    @Override
	public void startInitialization() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(DungeonRealms.getInstance(), () -> {
            for(Trade trade : trades){
                if(trade.p1 != null && trade.p1.isOnline())
                	ParticleAPI.spawnParticle(Particle.VILLAGER_HAPPY, trade.p1.getLocation().add(0, 2.05, 0), 5, .001F);
                
                if(trade.p2 != null && trade.p2.isOnline())
                	ParticleAPI.spawnParticle(Particle.VILLAGER_HAPPY, trade.p2.getLocation().add(0, 2.05, 0), 5, .001F);
                
            }
        }, 20, 20);
	}

	@Override
	public void stopInvocation() {
		
	}
    
    /**
     * sender, receiver
     *
     * @param p1
     * @param p2
     */
    public static void openTrade(UUID p1, UUID p2) {
        Player sender = Bukkit.getPlayer(p1);
        Player requested = Bukkit.getPlayer(p2);
        if (sender == null || requested == null) {
            return;
        }
    }

    public static Player getTarget(Player trader) {
        Optional<Entity> tradie =
        trader.getNearbyEntities(2.0D, 2.0D, 2.0D).stream().filter(e -> e instanceof Player && !e.hasMetadata("NPC")
                && canTrade(e.getUniqueId()) && trader.hasLineOfSight(e)).findFirst();
        return tradie.isPresent() ? (Player) tradie.get() : null;
    }

    public static boolean canTrade(UUID uniqueId) {
        Player p = Bukkit.getPlayer(uniqueId);
        if (p == null) {
            return false;
        }

        if (CombatLog.isInCombat(p)) {
            return false;
        }

        if (getTrade(uniqueId) != null) {
            return false;
        }
        
        if(p.getGameMode() == GameMode.SPECTATOR || GameAPI._hiddenPlayers.contains(p)){
        	return false;
        }
        
        if(p.getOpenInventory() != null && !p.getOpenInventory().getTitle().equals("container.crafting")){
        	return false;
        }
        return true;
    }

    public static boolean canTradeItem(ItemStack stack) {
        return true;
    }

    public static void startTrade(Player p1, Player p2) {
        trades.add(new Trade(p1, p2));
    }

    public static Trade getTrade(UUID uuid) {
        for (Trade trade : trades) {
            if (trade.p1.getUniqueId().toString().equalsIgnoreCase(uuid.toString())
                    || trade.p2.getUniqueId().toString().equalsIgnoreCase(uuid.toString()))
                return trade;
        }
        return null;
    }

	@Override
	public EnumPriority startPriority() {
		return EnumPriority.CATHOLICS;
	}
}
