package net.dungeonrealms.game.item.items.functional;

import lombok.Getter;
import lombok.Setter;
import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.GameAPI;
import net.dungeonrealms.database.PlayerWrapper;
import net.dungeonrealms.game.handler.HealthHandler;
import net.dungeonrealms.game.handler.KarmaHandler.EnumPlayerAlignments;
import net.dungeonrealms.game.item.ItemType;
import net.dungeonrealms.game.item.ItemUsage;
import net.dungeonrealms.game.item.event.ItemClickEvent;
import net.dungeonrealms.game.item.event.ItemClickEvent.ItemClickListener;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.mechanic.data.EnumTier;
import net.dungeonrealms.game.mechanic.data.PotionTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;

public class PotionItem extends FunctionalItem implements ItemClickListener {

    @Getter
    private PotionTier tier;
    @Getter
    private boolean splash;
    @Setter
    private int healAmount;
    @Getter
    @Setter
    private boolean fromShop;

    public PotionItem(int potionTier) {
        this(PotionTier.getById(potionTier));
    }

    public PotionItem(PotionTier tier) {
        super(ItemType.POTION);
        setTier(tier);
    }

    public PotionItem(ItemStack item) {
        super(item);
        setTier(PotionTier.getById(getTagInt(TIER)));
        setSplash(getItem().getType() == Material.SPLASH_POTION);
        setHealAmount(getTagInt("healAmount"));
    }

    public void setTier(PotionTier tier) {
        this.tier = tier;
        int healAmount = isFromShop() ? Utils.randInt(tier.getShopHealthMin(), tier.getShopHealthMax()) : tier.getDefaultHealth();
        healAmount = (((healAmount + 5) / 10) * 10);
        setHealAmount(healAmount);
    }

    public PotionItem setSplash(boolean splash) {
        this.splash = splash;
        return this;
    }

    public int getHealAmount() {
        return (int) (this.healAmount * (isSplash() ? 0.65D : 1D));
    }

    @Override
    public void updateItem() {
        setTagInt("healAmount", this.healAmount);
        setTagInt(TIER, getTier().getId());
        super.updateItem();
    }

    @Override
    protected ItemStack getStack() {
        ItemStack potion = new ItemStack(isSplash() ? Material.SPLASH_POTION : Material.POTION);
        PotionMeta pm = (PotionMeta) potion.getItemMeta();
        pm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS);
        pm.setBasePotionData(new PotionData(getTier().getPotionType()));
        potion.setItemMeta(pm);
        return potion;
    }

    @Override
    public void onClick(ItemClickEvent evt) {

//        if (evt.getWrapper().getAlignment() == EnumPlayerAlignments.CHAOTIC) {
//            evt.getPlayer().sendMessage(ChatColor.RED + "You may not use potions whilst chaotic.");
//            return;
//        }

        if (!isSplash() && HealthHandler.getHP(evt.getPlayer()) >= HealthHandler.getMaxHP(evt.getPlayer())) {
            evt.getPlayer().sendMessage(ChatColor.RED + "You are already at full HP!");
            return;
        }

        if (isSplash()) {
            evt.setCancelled(false);
        } else {
            evt.setUsed(true);
            HealthHandler.heal(evt.getPlayer(), getHealAmount(), true);
            PlayerWrapper wrapper = PlayerWrapper.getPlayerWrapper(evt.getPlayer());
            if (wrapper != null && (wrapper.getAlignment() == EnumPlayerAlignments.CHAOTIC || wrapper.getAlignment() == EnumPlayerAlignments.NEUTRAL)) {
                HealthHandler.trackHeal(evt.getPlayer());
            }
        }
        Bukkit.getScheduler().runTask(DungeonRealms.getInstance(), () -> findNextPotion(evt.getPlayer(), evt.getHand()));
    }

    private void findNextPotion(Player player, EquipmentSlot slot) {
        int slotCount = -1;
        if(player.getEquipment().getItemInMainHand() != null && player.getEquipment().getItemInMainHand().getType() != Material.AIR) return;
        for (ItemStack stack : player.getInventory().getContents()) {
            slotCount++;
            if (!isPotion(stack))
                continue;
            GameAPI.setHandItem(player, stack, slot);
            player.getInventory().setItem(slotCount, new ItemStack(Material.AIR));
            return;
        }
    }

    @Override
    protected ItemUsage[] getUsage() {
        return INTERACT_RIGHT_CLICK;
    }

    public static boolean isPotion(ItemStack item) {
        return isType(item, ItemType.POTION);
    }

    @Override
    protected String[] getLore() {
        return new String[]{"An Elixir that heals for " + ChatColor.RED.toString() + ChatColor.BOLD + getHealAmount() + ChatColor.GRAY + "HP" + (isSplash() ? " in a " + ChatColor.RED + ChatColor.BOLD + "4x4" + ChatColor.GRAY + " area." : ".")};
    }

    @Override
    protected String getDisplayName() {
        return EnumTier.getTier(getTier()).getColor() + getTier().getName() + " Elixir of " + (isSplash() ? "Splash" : "Singular") + " Healing";
    }
}
