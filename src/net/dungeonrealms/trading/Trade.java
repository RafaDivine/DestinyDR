package net.dungeonrealms.trading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import net.dungeonrealms.mastery.Utils;
import net.dungeonrealms.mechanics.ItemManager;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

/**
 * Created by Chase on Oct 10, 2015
 */
public class Trade {

	public static class TradeManager {
		public static ArrayList<Trade> trades = new ArrayList<>();

		/**
		 * returns an instance of the trade object for the specified players
		 * uuid
		 * 
		 * @param uuid
		 * @return
		 */
		public static Trade getTrade(UUID uuid) {
			for (int i = 0; i < trades.size(); i++) {
				UUID uuid1 = trades.get(i).p1.getUniqueId();
				UUID uuid2 = trades.get(i).p2.getUniqueId();
				if (uuid == uuid1 || uuid == uuid2)
					return trades.get(i);
			}
			return null;
		}
	}

	public Player p1;
	/**
	 * Player 1's Personal Inventory before and after the trade.
	 */
	Inventory p1Before;
	Inventory p1After;
	public Player p2;
	/**
	 * Player 2's Personal Inventory Before and after the trade.
	 */
	Inventory p2Before;
	Inventory p2After;
	/**
	 * Shared Inventory of players.
	 */
	public Inventory inv;
	public boolean completed = false;

	public Trade(Player p1, Player p2) {
		this.p1 = p1;
		this.p2 = p2;
		p1Before = p1.getInventory();
		p2Before = p1.getInventory();
		inv = Bukkit.createInventory(null, 36, p1.getName() + "  Trade " + p2.getName());
		TradeManager.trades.add(this);
	}

	/**
	 * checks if the UUID is the same as the UUID of the player who is on the
	 * left side.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isLeft(UUID id) {
		return (id == p1.getUniqueId());
	}

	/**
	 * Opens the trade window for both players.
	 */
	public void launchTradeWindow() {
		p1.closeInventory();
		inv = Bukkit.createInventory(null, 36, p1.getName() + "  Trade with " + p2.getName());
		ItemStack separator = ItemManager.createItem(Material.BONE, " ", null);
		ItemStack item = ItemManager.createItemWithData(Material.INK_SACK, ChatColor.YELLOW.toString() + "Ready", null,
		        DyeColor.GRAY.getDyeData());
		net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("state", "notready");
		nms.setTag(nbt);
		nms.c(ChatColor.YELLOW + "Ready Up");
		inv.setItem(0, CraftItemStack.asBukkitCopy(nms));
		inv.setItem(8, CraftItemStack.asBukkitCopy(nms));
		inv.setItem(4, separator);
		inv.setItem(13, separator);
		inv.setItem(22, separator);
		inv.setItem(31, separator);
		inv.setItem(4, separator);
		p1.openInventory(inv);
		p2.openInventory(inv);
	}

	/**
	 * Closes inv for both players and deletes Trade.
	 */
	public void handleClose() {
		if (!completed)
			giveItemsBack();
		TradeManager.trades.remove(this);
		p1.closeInventory();
		p2.closeInventory();
	}

	/**
	 * Gives both players their items back that were in the trade window when
	 * the inventory was closed.
	 */
	public void giveItemsBack() {
		InventoryView inv = p1.getOpenInventory();
		int[] left = new int[] { 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 30, 28, 29 };
		int[] right = new int[] { 23, 24, 25, 26, 5, 6, 7, 14, 15, 16, 17, 32, 33, 34, 35 };
		for (int aLeft : left) {
			ItemStack current = inv.getItem(aLeft);
			if (current != null && current.getType() != Material.AIR) {
				p1.getInventory().addItem(current);
			}
		}
		for (int aRight : right) {
			ItemStack current = inv.getItem(aRight);
			if (current != null && current.getType() != Material.AIR) {
				p2.getInventory().addItem(current);
			}
		}
	}
	// 0, 8 Confirm
	// 4, 13, 22, 27, 31 separator
	// LEFT ITEMS 1,2,3 9, 10, 11, 12, 18, 19, 20, 21
	// RIGHT ITEMS 23,24,25,26 , 5,6,7, 14,15,16,17

	/**
	 * Checks if specified slot is owned the the player on the left side.
	 * 
	 * @param slot
	 * @return
	 */
	public boolean isLeftSlot(int slot) {
		int[] left = new int[] { 0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 30, 28, 29 };
		for (int aLeft : left)
			if (aLeft == slot)
				return true;
		return false;
	}

	/**
	 * Checks if the slot clicked is a no no zone.
	 * 
	 * @param slot
	 * @return
	 */
	public boolean isSeperator(int slot) {
		int[] num = new int[] { 4, 13, 22, 27, 31 };
		for (int aLeft : num)
			if (aLeft == slot)
				return true;
		return false;

	}

	/**
	 * Finalizes the Trade between both players.
	 */
	public void accept() {
		InventoryView inv = p1.getOpenInventory();
		int[] left = new int[] { 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 30, 28, 29 };
		int[] right = new int[] { 23, 24, 25, 26, 5, 6, 7, 14, 15, 16, 17, 32, 33, 34, 35 };
		completed = true;
		for (int aLeft : left) {
			ItemStack current = inv.getItem(aLeft);
			if (current != null && current.getType() != Material.AIR) {
				p2.getInventory().addItem(current);
			}
		}
		for (int aRight : right) {
			ItemStack current = inv.getItem(aRight);
			if (current != null && current.getType() != Material.AIR) {
				p1.getInventory().addItem(current);
			}
		}
		p1After = p1.getInventory();
		p2After = p2.getInventory();

		checkForDupes();

		p2.closeInventory();
		p1.closeInventory();

		p1.sendMessage(ChatColor.GREEN.toString() + " Trade Completed!");
		p2.sendMessage(ChatColor.GREEN.toString() + " Trade Completed!");
	}

	/**
	 * Checks players for duplicated items from a trade.
	 */
	private void checkForDupes() {
		for (ItemStack stack : p1After.getContents()) {
			if (p1Before.contains(stack)) {
				for (ItemStack oldStack : p1Before.getContents()) {
					if (oldStack == stack) {
						break;
					}
				}
			}

		}
		for (int i = 0; i < p2After.getSize(); i++) {

		}
	}

}
