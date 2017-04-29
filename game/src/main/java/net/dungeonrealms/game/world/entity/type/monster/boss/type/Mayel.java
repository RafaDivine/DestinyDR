package net.dungeonrealms.game.world.entity.type.monster.boss.type;

import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.game.handler.HealthHandler;
import net.dungeonrealms.game.item.items.core.ItemWeaponBow;
import net.dungeonrealms.game.mastery.GamePlayer;
import net.dungeonrealms.game.mechanic.ParticleAPI;
import net.dungeonrealms.game.mechanic.dungeons.BossType;
import net.dungeonrealms.game.mechanic.dungeons.DungeonBoss;
import net.dungeonrealms.game.world.entity.type.monster.type.EnumMonster;
import net.dungeonrealms.game.world.entity.type.monster.type.ranged.RangedWitherSkeleton;
import net.dungeonrealms.game.world.item.DamageAPI;
import net.minecraft.server.v1_9_R2.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Mayel - The Bandit Trove's boss.
 * 
 * Redone on April 28th, 2017.
 * @author Kneesnap
 */
public class Mayel extends RangedWitherSkeleton implements DungeonBoss {

	private boolean canSpawn = false;
	
    public Mayel(World world) {
        super(world);
        this.setSize(0.7F, 2.4F);
        this.fireProof = true;
        createEntity(100);
    }
    
    public String[] getItems(){
    	return new String[] {"mayelbow", "mayelhelmet", "mayelchest", "mayelpants", "mayelboot"};
    }

    /**
     * Called when entity fires a projectile.
     */
    @Override
    public void a(EntityLiving entityliving, float f) {
        DamageAPI.fireBowProjectile((LivingEntity) getBukkitEntity(), new ItemWeaponBow(getHeld()));
    }

    @Override
    public void onBossAttack(EntityDamageByEntityEvent event) {
        LivingEntity livingEntity = (LivingEntity) this.getBukkitEntity();
        if (canSpawnMobs(livingEntity)) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> canSpawn = false, 100L);
            ParticleAPI.sendParticleToLocation(ParticleAPI.ParticleEffect.SPELL, getBukkitEntity().getLocation(), random.nextFloat(), random.nextFloat(), random.nextFloat(), 1F, 100);
            for (int i = 0; i < 4; i++)
                spawnMinion(EnumMonster.MayelPirate, "Mayel's Crew", 1);
            say("Come to my call, brothers!");
        }
    }
    
    public int getXPDrop(){
    	return 5000;
    }
    
    public int getGemDrop(){
    	return random.nextInt(250 - 100) + 100;
    }
    
    private boolean canSpawnMobs(LivingEntity livingEntity) {
        int maxHP = HealthHandler.getMonsterMaxHP(livingEntity);
        int currentHP = HealthHandler.getMonsterHP(livingEntity);
        
        if (currentHP <= maxHP * 0.8 && !canSpawn) {
        	canSpawn = true;
        	return true;
        }
        
        return false;
    }

    @Override
    public BossType getBossType() {
        return BossType.Mayel;
    }

	@Override
	public void addKillStat(GamePlayer gp) {
		gp.getPlayerStatistics().setMayelKills(gp.getPlayerStatistics().getMayelKills() + 1);
	}
}
