package net.dungeonrealms.game.mechanic;

import com.google.common.collect.Lists;
import net.dungeonrealms.database.PlayerWrapper;
import net.dungeonrealms.game.handler.HealthHandler;
import net.dungeonrealms.game.handler.KarmaHandler;
import net.dungeonrealms.game.handler.KarmaHandler.EnumPlayerAlignments;
import net.dungeonrealms.game.item.PersistentItem;
import net.dungeonrealms.game.item.items.core.*;
import net.dungeonrealms.game.item.items.functional.PotionItem;
import net.dungeonrealms.game.mastery.MetadataUtils.Metadata;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.mechanic.data.PotionTier;
import net.dungeonrealms.game.mechanic.data.ShardTier;
import net.dungeonrealms.game.miscellaneous.ItemBuilder;
import net.dungeonrealms.game.quests.Quest;
import net.dungeonrealms.game.quests.QuestPlayerData;
import net.dungeonrealms.game.quests.QuestPlayerData.QuestProgress;
import net.dungeonrealms.game.quests.Quests;
import net.dungeonrealms.game.world.item.Item.ArmorAttributeType;
import net.dungeonrealms.game.world.item.itemgenerator.ItemGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ItemManager - Contains basic item utils.
 * <p>
 * Redone by Kneesnap in early April 2017.
 */
public class ItemManager {

    /**
     * Adds a starter kit to the player.
     *
     * @param player
     */
    public static void giveStarter(Player player) {
        giveStarter(player, false);
    }

    /**
     * Adds a starter kit to the player.
     * TODO: Make this less bad.
     *
     * @param player
     * @param isNew
     */
    public static void giveStarter(Player player, boolean isNew) {

        // Give T1 potions.
        for (int i = 0; i < 3; i++)
            player.getInventory().addItem(new PotionItem(PotionTier.TIER_1).setUntradeable(true).generateItem());

        if (isNew)
            player.getInventory().addItem(new VanillaItem(new ItemStack(Material.BREAD, 3)).setUntradeable(true).generateItem());

        if (isNew)
            player.getInventory().addItem(new ItemBuilder().setItem(new ItemStack(Material.BREAD, 3)).setNBTString
                    ("subtype", "starter").addLore(ChatColor.GRAY + "Untradeable").build());

        if (Utils.randInt(0, 1) == 1) {
            ItemStack fixedSword = ItemGenerator.getNamedItem("training_sword");
            player.getInventory().addItem(new ItemBuilder().setItem(fixedSword).setNBTString("dataType", "starterSet").build());
        } else {
            ItemStack fixedAxe = ItemGenerator.getNamedItem("training_axe");
            player.getInventory().addItem(new ItemBuilder().setItem(fixedAxe).setNBTString("dataType", "starterSet").build());
        }

        EntityEquipment e = player.getEquipment();
        ItemStack fixedHelmet = ItemGenerator.getNamedItem("traininghelm");
        if (e.getHelmet() == null || e.getHelmet().getType() == Material.AIR)
            player.getInventory().setHelmet(new ItemBuilder().setItem(fixedHelmet).setNBTString("dataType", "starterSet").build());

        ItemStack fixedChestplate = ItemGenerator.getNamedItem("trainingchest");
        if (e.getChestplate() == null || e.getChestplate().getType() == Material.AIR)
            player.getInventory().setChestplate(new ItemBuilder().setItem(fixedChestplate).setNBTString("dataType", "starterSet").build());

        ItemStack fixedLeggings = ItemGenerator.getNamedItem("traininglegs");
        if (e.getLeggings() == null || e.getLeggings().getType() == Material.AIR)
            player.getInventory().setLeggings(new ItemBuilder().setItem(fixedLeggings).setNBTString("dataType", "starterSet").build());

        ItemStack fixedBoots = ItemGenerator.getNamedItem("trainingboots");
        if (e.getBoots() == null || e.getBoots().getType() == Material.AIR)
            player.getInventory().setBoots(new ItemBuilder().setItem(fixedBoots).setNBTString("dataType", "starterSet").build());

        PlayerWrapper.getWrapper(player).calculateAllAttributes();
    }


    /**
     * Creates a character journal for a player.
     * This should ONLY be called when force opening this book for the player.
     * In other words, use new ItemPlayerJournal(Player).generateItem() instead of this.
     * We can save space / cpu power by only generating this when the player opens it.
     */
    public static ItemStack createCharacterJournal(Player p) {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bm = (BookMeta) stack.getItemMeta();
        List<String> pages = new ArrayList<>();
        String page1_string;
        String questPage_string;
        String page2_string;
        String page3_string;
        String page4_string;
        String new_line = "\n" + ChatColor.BLACK.toString() + " " + "\n";
        PlayerWrapper pw = PlayerWrapper.getWrapper(p);

        EnumPlayerAlignments playerAlignment = pw.getAlignment();
        String pretty_align = (playerAlignment == KarmaHandler.EnumPlayerAlignments.LAWFUL ? ChatColor.DARK_GREEN.toString() :
                playerAlignment.getAlignmentColor()) + ChatColor.UNDERLINE.toString() + playerAlignment.name();

        if (playerAlignment != EnumPlayerAlignments.LAWFUL) {
            String time = String.valueOf(pw.getAlignmentTime());
            page1_string = ChatColor.BLACK.toString() + "" + ChatColor.BOLD.toString() + ChatColor.UNDERLINE.toString() + "  Your Character  \n\n"
                    + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "Alignment: " + pretty_align + "\n" + playerAlignment.getAlignmentColor().toString() + ChatColor.BOLD + time + "s.." + new_line;
        } else {
            page1_string = ChatColor.BLACK.toString() + "" + ChatColor.BOLD.toString() + ChatColor.UNDERLINE.toString() + "  Your Character  " + "\n\n"
                    + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "Alignment: " + pretty_align + new_line;
        }

        page1_string += ""
                + ChatColor.BLACK.toString() + playerAlignment.getDescription() + new_line + ChatColor.BLACK + "   " + HealthHandler.getHP(p)
                + " / " + HealthHandler.getMaxHP(p) + "" + ChatColor.BOLD + " HP" + "\n" + ChatColor.BLACK
                + "   " + pw.getPlayerStats().getDPS() + "% " + ChatColor.BOLD + "DPS" + "\n" + ChatColor.BLACK
                + "   " + (HealthHandler.getRegen(p) + pw.getPlayerStats().getHPRegen()) + " " + ChatColor.BOLD + "HP/s" + "\n" + ChatColor.BLACK
                + "   " + pw.getAttributes().getAttribute(ArmorAttributeType.ENERGY_REGEN).toString() + "% " + ChatColor.BOLD.toString() + "Energy/s" + "\n" + ChatColor.BLACK
                + "   " + pw.getEcash() + ChatColor.BOLD + " E-CASH" + "\n" + ChatColor.BLACK
                + "   " + pw.getAttributes().getAttribute(ArmorAttributeType.GEM_FIND).getValue() + ChatColor.BOLD + " GEM FIND" + "\n" + ChatColor.BLACK
                + "   " + pw.getAttributes().getAttribute(ArmorAttributeType.ITEM_FIND).getValue() + ChatColor.BOLD + " ITEM FIND";

        questPage_string = ChatColor.BLACK + "" + ChatColor.BOLD + ChatColor.UNDERLINE + "  Quest Progress  \n\n";
        int quests = 0;

        if (Quests.isEnabled()) {
            QuestPlayerData data = Quests.getInstance().playerDataMap.get(p);
            if (data != null) {
                //TODO: Multi page support. (Does vanilla do this automatically?)
                for (Quest doing : data.getCurrentQuests()) {
                    quests++;
                    QuestProgress qp = data.getQuestProgress(doing);

                    questPage_string += ChatColor.BLACK + doing.getQuestName() + "> " + ChatColor.GREEN;
                    if (qp.getCurrentStage().getPrevious() == null) {
                        questPage_string += "Start by talking to " + qp.getCurrentStage().getNPC().getName();
                    } else {
                        questPage_string += qp.getCurrentStage().getPrevious().getObjective().getTaskDescription(p, qp.getCurrentStage());
                    }
                    questPage_string += "\n\n";
                }
            }
        }

        page2_string = ChatColor.DARK_AQUA.toString() + ChatColor.BOLD + "  ** LEVEL/EXP **\n\n" + ChatColor.BLACK + ChatColor.BOLD
                + "       LEVEL\n" + "          " + ChatColor.BLACK + pw.getLevel() + "\n\n" + ChatColor.BLACK + ChatColor.BOLD
                + "          XP" + "\n" + ChatColor.BLACK + "       " + pw.getExperience() + "/" + pw.getEXPNeeded();


        //  PORTAL SHARD PAGE  //
        String portalShardPage = ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "Portal Key Shards" + "\n" + ChatColor.BLACK.toString()
                + ChatColor.ITALIC.toString() + "A sharded fragment from the great portal of Maltai that may be exchanged at the Dungeoneer for epic equipment.\n";

        for (ShardTier tier : ShardTier.values())
            portalShardPage += "\n" + (tier.getColor() != ChatColor.WHITE ? tier.getColor() : ChatColor.DARK_GRAY) + "Portal Shards: " + ChatColor.BLACK + pw.getPortalShards(tier);

        //  COMMAND PAGE  //
        page3_string = (ChatColor.BLACK.toString() + "" + ChatColor.BOLD.toString() + ChatColor.UNDERLINE.toString() + "   Command Guide  " + new_line
                + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "/msg" + "\n" + ChatColor.BLACK.toString() + "Sends a PM." + new_line
                + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "/ask" + "\n" + ChatColor.BLACK.toString() + "Ask any questions." + new_line
                + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "/shard" + "\n" + ChatColor.BLACK.toString() + "Switch your current session." + new_line
                + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "/pinvite"
                + "\n"
                + ChatColor.BLACK.toString()
                + "Invite to party");


        page4_string = (ChatColor.BLACK + ChatColor.BOLD.toString() + "/premove " + "\n" + ChatColor.BLACK.toString()
                + "Kick player from party" + new_line + ChatColor.BLACK + ChatColor.BOLD.toString() + "/pleave " + "\n"
                + ChatColor.BLACK.toString() + "Leave your party"
                + new_line + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "/roll "
                + "\n" + ChatColor.BLACK.toString() + "Rolls a random number."
        );


        String page5_string = (ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "/stats" + "\n" + ChatColor.BLACK.toString() + "Set Attributes"
                + new_line + ChatColor.BLACK.toString() + ChatColor.BOLD.toString() + "/toggles" + "\n" + ChatColor.BLACK.toString() + "Open Toggles Menu");

        bm.setAuthor("King Bulwar");
        pages.add(page1_string);
        if (quests > 0)
            pages.add(questPage_string);
        pages.add(page2_string);
        pages.add(portalShardPage);
        pages.add(page3_string);
        pages.add(page4_string);
        pages.add(page5_string);

        bm.setPages(pages);
        stack.setItemMeta(bm);
        return stack;
    }

    public static void whitelistItemDrop(Player player, Location drop, ItemStack item) {
        whitelistItemDrop(player, drop.getWorld().dropItem(drop.clone().add(0, 1, 0), item));
    }

    public static void whitelistItemDrop(Player player, org.bukkit.entity.Item item) {
        if (player != null)
            Metadata.WHITELIST.set(item, player.getName());
    }

    /**
     * Is this item marked as droppable, and tradeable?
     */
    public static boolean isItemDroppable(ItemStack item) {
        return !get(item).isUndroppable() && isItemTradeable(item);
    }

    /**
     * Make an item undroppable.
     */
    public static ItemStack makeItemUndroppable(ItemStack item) {
        VanillaItem vanilla = new VanillaItem(item);
        vanilla.setUndroppable(true);
        return vanilla.generateItem();
    }

    /**
     * Is this item marked as tradeable and not soulbound or permanently untradeable?
     */
    public static boolean isItemTradeable(ItemStack item) {
        ItemGeneric ig = get(item);
        if (ig.isUntradeable()) {
            System.out.println("Returning tradable debug 1");
            return false;
        }
        if (ig.isSoulbound()) {
            System.out.println("Returning tradable debug 2");
            return false;
        }
        if (ig.isPermanentUntradeable()) {
            System.out.println("Returning tradable debug 3");
            return false;
        }
        System.out.println("Returning tradable debug 4");
        return true;
    }

    /**
     * Make an item untradeable.
     */
    public static ItemStack makeItemUntradeable(ItemStack item) {
        VanillaItem vanilla = new VanillaItem(item);
        vanilla.setUntradeable(true);
        return vanilla.generateItem();
    }

    public static boolean isDungeonItem(ItemStack item) {
        return get(item).isDungeon();
    }

    /**
     * Is this item soulbound?
     */
    public static boolean isItemSoulbound(ItemStack item) {
        return get(item).isSoulbound();
    }

    /**
     * Make an item soulbound.
     */
    public static ItemStack makeItemSoulbound(ItemStack item) {
        VanillaItem vanilla = new VanillaItem(item);
        vanilla.setSoulbound(true);
        return vanilla.generateItem();
    }

    /**
     * Is this item permanently untradeable?
     */
    public static boolean isItemPermanentlyUntradeable(ItemStack item) {
        return get(item).isPermanentUntradeable();
    }

    /**
     * Make an item permanent untradeable.
     */
    public static ItemStack makeItemPermenantUntradeable(ItemStack item) {
        VanillaItem vanilla = new VanillaItem(item);
        vanilla.setPermUntradeable(true);
        return vanilla.generateItem();
    }

    private static ItemGeneric get(ItemStack item) {
        return (ItemGeneric) PersistentItem.constructItem(item);
    }

    public static CombatItem createRandomCombatItem() {
        return new Random().nextBoolean() ? new ItemWeapon() : new ItemArmor();
    }

    public static ItemStack createItem(Material mat, short data, String name) {
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        List<String> l = new ArrayList<>();
        for (String s : lore)
            l.add(ChatColor.GRAY + s);
        meta.setLore(l);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack createItem(Material mat, String name, short data, String... lore) {
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && lore.length > 0)
            meta.setLore(Lists.newArrayList(lore));
        stack.setItemMeta(meta);
        return stack;
    }
}
