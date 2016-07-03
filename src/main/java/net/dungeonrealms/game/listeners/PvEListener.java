package net.dungeonrealms.game.listeners;

import net.dungeonrealms.API;
import net.dungeonrealms.game.handlers.EnergyHandler;
import net.dungeonrealms.game.mechanics.ParticleAPI;
import net.dungeonrealms.game.player.combat.CombatLog;
import net.dungeonrealms.game.world.entities.Entities;
import net.dungeonrealms.game.world.entities.PowerMove;
import net.dungeonrealms.game.world.entities.types.monsters.boss.Boss;
import net.dungeonrealms.game.world.items.Attribute;
import net.dungeonrealms.game.world.items.DamageAPI;
import net.dungeonrealms.game.world.items.Item;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Created by Kieran Quigley (Proxying) on 03-Jul-16.
 */
public class PvEListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void playerMeleeMob(EntityDamageByEntityEvent event) {
        if (!API.isPlayer(event.getDamager())) return;
        if (event.getEntity() instanceof Player) return;
        if (Entities.PLAYER_PETS.containsValue(((CraftEntity) event.getEntity()).getHandle())) return;
        if (Entities.PLAYER_MOUNTS.containsValue(((CraftEntity) event.getEntity()).getHandle())) return;
        if (event.getEntity() instanceof LivingEntity) {
            if (!event.getEntity().hasMetadata("type")) return;
        } else {
            if (event.getEntity().hasMetadata("type")) {
                if (event.getEntity().getMetadata("type").get(0).asString().equals("buff")) return;
            } else {
                return;
            }
        }

        Player damager = (Player) event.getDamager();
        LivingEntity receiver = (LivingEntity) event.getEntity();

        if (API.isInSafeRegion(damager.getLocation()) || API.isInSafeRegion(receiver.getLocation())) {
            event.setDamage(0);
            event.setCancelled(true);
            damager.updateInventory();
            return;
        }

        double finalDamage;

        if (damager.hasPotionEffect(PotionEffectType.SLOW_DIGGING) || EnergyHandler.getPlayerCurrentEnergy(damager) <= 0) {
            event.setCancelled(true);
            event.setDamage(0);
            damager.playSound(damager.getLocation(), Sound.ENTITY_WOLF_PANT, 12F, 1.5F);
            try {
                ParticleAPI.sendParticleToLocation(ParticleAPI.ParticleEffect.CRIT, event.getEntity().getLocation().add(0, 1, 0), new Random().nextFloat(), new Random().nextFloat(), new Random().nextFloat(), 0.75F, 40);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }

        if (CombatLog.isInCombat(damager)) {
            CombatLog.updateCombat(damager);
        } else {
            CombatLog.addToCombat(damager);
        }

        EnergyHandler.removeEnergyFromPlayerAndUpdate(damager.getUniqueId(), EnergyHandler.getWeaponSwingEnergyCost(damager.getEquipment().getItemInMainHand()));

        if (!API.isWeapon(damager.getEquipment().getItemInMainHand())) {
            event.setDamage(1);
            return;
        }

        Item.ItemType weaponType = new Attribute(damager.getInventory().getItemInMainHand()).getItemType();
        Item.ItemTier tier = new Attribute(damager.getInventory().getItemInMainHand()).getItemTier();

        switch (weaponType) {
            case BOW:
                switch (tier) {
                    case TIER_1:
                        DamageAPI.knockbackEntity(damager, receiver, 1.2);
                        break;
                    case TIER_2:
                        DamageAPI.knockbackEntity(damager, receiver, 1.5);
                        break;
                    case TIER_3:
                        DamageAPI.knockbackEntity(damager, receiver, 1.8);
                        break;
                    case TIER_4:
                        DamageAPI.knockbackEntity(damager, receiver, 2.0);
                        break;
                    case TIER_5:
                        DamageAPI.knockbackEntity(damager, receiver, 2.2);
                        break;
                    default:
                        break;
                }
            case STAFF:
                event.setDamage(0);
                event.setCancelled(true);
                return;
            default:
                break;
        }

        finalDamage = DamageAPI.calculateWeaponDamage(damager, receiver);

        event.setDamage(finalDamage);

        if (!receiver.hasMetadata("tier")) return;
        if (PowerMove.chargedMonsters.contains(receiver.getUniqueId()) || PowerMove.chargingMonsters.contains(receiver.getUniqueId())) return;

        int mobTier = receiver.getMetadata("tier").get(0).asInt();
        Random rand = new Random();
        int powerChance = 0;
        if (receiver.hasMetadata("elite")) {
            switch (mobTier) {
                case 1:
                    powerChance = 5;
                    break;
                case 2:
                    powerChance = 7;
                    break;
                case 3:
                    powerChance = 10;
                    break;
                case 4:
                    powerChance = 13;
                    break;
                case 5:
                    powerChance = 20;
                    break;

            }
            if (rand.nextInt(100) <= powerChance) {
                receiver.getWorld().playSound(receiver.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1F, 4.0F);
                PowerMove.doPowerMove("whirlwind", receiver, null);
            }
        } else if (receiver.hasMetadata("boss")) {
            if (receiver instanceof CraftLivingEntity) {
                Boss b = (Boss) ((CraftLivingEntity) receiver).getHandle();
                b.onBossHit(event);
            }
        } else {
            switch (mobTier) {
                case 1:
                    powerChance = 5;
                    break;
                case 2:
                    powerChance = 7;
                    break;
                case 3:
                    powerChance = 10;
                    break;
                case 4:
                    powerChance = 13;
                    break;
                case 5:
                    powerChance = 20;
                    break;

            }
            if (rand.nextInt(100) <= powerChance) {
                PowerMove.doPowerMove("powerstrike", receiver, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void playerRangedMob(EntityDamageByEntityEvent event) {
        if (!DamageAPI.isBowProjectile(event.getDamager()) && !DamageAPI.isStaffProjectile(event.getDamager())) return;
        if (event.getEntity() instanceof Player) return;
        if (Entities.PLAYER_PETS.containsValue(((CraftEntity) event.getEntity()).getHandle())) return;
        if (Entities.PLAYER_MOUNTS.containsValue(((CraftEntity) event.getEntity()).getHandle())) return;
        Projectile projectile = (Projectile) event.getDamager();
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity) {
            if (!event.getEntity().hasMetadata("type")) return;
        } else {
            if (event.getEntity().hasMetadata("type")) {
                if (event.getEntity().getMetadata("type").get(0).asString().equals("buff")) return;
            } else {
                return;
            }
        }
        Player damager = (Player) projectile.getShooter();
        LivingEntity receiver = (LivingEntity) event.getEntity();

        if (API.isInSafeRegion(damager.getLocation()) || API.isInSafeRegion(receiver.getLocation())) {
            event.setDamage(0);
            event.setCancelled(true);
            damager.updateInventory();
            return;
        }

        double finalDamage;

        if (CombatLog.isInCombat(damager)) {
            CombatLog.updateCombat(damager);
        } else {
            CombatLog.addToCombat(damager);
        }

        finalDamage = DamageAPI.calculateProjectileDamage(damager, receiver, projectile);

        event.setDamage(finalDamage);

        if (!receiver.hasMetadata("tier")) return;
        if (PowerMove.chargedMonsters.contains(receiver.getUniqueId()) || PowerMove.chargingMonsters.contains(receiver.getUniqueId())) return;

        int mobTier = receiver.getMetadata("tier").get(0).asInt();
        Random rand = new Random();
        int powerChance = 0;
        if (receiver.hasMetadata("elite")) {
            switch (mobTier) {
                case 1:
                    powerChance = 5;
                    break;
                case 2:
                    powerChance = 7;
                    break;
                case 3:
                    powerChance = 10;
                    break;
                case 4:
                    powerChance = 13;
                    break;
                case 5:
                    powerChance = 20;
                    break;

            }
            if (rand.nextInt(100) <= powerChance) {
                receiver.getWorld().playSound(receiver.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1F, 4.0F);
                PowerMove.doPowerMove("whirlwind", receiver, null);
            }
        } else if (receiver.hasMetadata("boss")) {
            if (receiver instanceof CraftLivingEntity) {
                Boss b = (Boss) ((CraftLivingEntity) receiver).getHandle();
                b.onBossHit(event);
            }
        } else {
            switch (mobTier) {
                case 1:
                    powerChance = 5;
                    break;
                case 2:
                    powerChance = 7;
                    break;
                case 3:
                    powerChance = 10;
                    break;
                case 4:
                    powerChance = 13;
                    break;
                case 5:
                    powerChance = 20;
                    break;

            }
            if (rand.nextInt(100) <= powerChance) {
                PowerMove.doPowerMove("powerstrike", receiver, null);
            }
        }
    }
}