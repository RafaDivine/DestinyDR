package net.dungeonrealms.game.item.items.functional.ecash;

import net.dungeonrealms.common.game.database.sql.SQLDatabaseAPI;
import net.dungeonrealms.common.util.ChatUtil;
import net.dungeonrealms.database.PlayerWrapper;
import net.dungeonrealms.game.item.ItemType;
import net.dungeonrealms.game.item.ItemUsage;
import net.dungeonrealms.game.item.event.ItemClickEvent;
import net.dungeonrealms.game.item.event.ItemClickEvent.ItemClickListener;
import net.dungeonrealms.game.item.items.functional.FunctionalItem;
import net.dungeonrealms.game.player.chat.Chat;
import net.dungeonrealms.game.player.inventory.menus.guis.webstore.PetSelectionGUI;
import net.dungeonrealms.game.world.entity.type.pet.EnumPets;
import net.dungeonrealms.game.world.entity.util.PetUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemPet extends FunctionalItem implements ItemClickListener {

    public ItemPet() {
        super(ItemType.PET);
        setPermUntradeable(true);
    }

    public ItemPet(ItemStack item) {
        super(item);
    }

    @Override
    public void onClick(ItemClickEvent evt) {
        Player player = evt.getPlayer();

        if (PetUtils.hasActivePet(player)) {
            Entity entity = PetUtils.getPets().get(player);

            if (evt.hasEntity() && evt.getClickedEntity().equals(entity)) {
                renamePet(player, null);
                return;
            }

            // Dismiss Pet
            PetUtils.removePet(player);
            return;
        }

        spawnPet(player);
    }

    public static void spawnPet(Player player) {
        PetUtils.removePet(player);
        PlayerWrapper pw = PlayerWrapper.getWrapper(player);
        EnumPets petType = pw.getActivePet();

        // No Pet.
        if (petType == null) {
            player.sendMessage(ChatColor.RED + "You don't have an active pet, please enter the pets section in your profile to set one.");
            player.closeInventory();
            new PetSelectionGUI(player, null).open(player, null);
            return;
        }

        //Either load pet name or get default.
        EnumPets pet = pw.getActivePet();
        String petName = pw.getPetName(pw.getActivePet());

        // Spawn Pet.
        PetUtils.spawnPet(player, pet, pw.getRank().getChatColor() + petName);
    }

    public static void renamePet(Player player, EnumPets toRename) {
        player.sendMessage(ChatColor.GRAY + "Enter a name for your pet, or type " + ChatColor.RED + ChatColor.UNDERLINE + "cancel" + ChatColor.GRAY + ".");
        Chat.listenForMessage(player, (mess) -> {
            PlayerWrapper pw = PlayerWrapper.getWrapper(player);
            Entity pet = PetUtils.getPets().get(player);

            // Cancel
            String name = mess.getMessage().replaceAll("@", "_").replaceAll(",", ".");
            if (name.equalsIgnoreCase("cancel") || name.equalsIgnoreCase("exit")) {
                player.sendMessage(ChatColor.GRAY + "Pet naming " + ChatColor.RED + ChatColor.UNDERLINE + "CANCELLED.");
                return;
            }

            // Too Long.
            if (name.length() > 20) {
                player.sendMessage(ChatColor.RED + "Your pet name exceeds the maximum length of 20 characters.");
                player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Please remove " + (name.length() - 20) + " characters.");
                return;
            }

            String checkedPetName = SQLDatabaseAPI.filterSQLInjection(Chat.checkForBannedWords(name));

            if (ChatUtil.containsIllegal(checkedPetName)) {
                player.sendMessage(ChatColor.RED + "Your pet name contains illegal characters!");
                return;
            }

            pw.getPetsUnlocked().put(toRename, checkedPetName);
            if (pet != null)
                pet.setCustomName(pw.getRank().getChatColor() + checkedPetName);

            player.sendMessage(ChatColor.GRAY + "Your pet's name has been changed to " + ChatColor.GREEN + ChatColor.UNDERLINE + checkedPetName + ChatColor.GRAY + ".");

        });
    }

    @Override
    protected String getDisplayName() {
        return ChatColor.GREEN + "Pet";
    }

    @Override
    protected String[] getLore() {
        return new String[]{ChatColor.DARK_GRAY + "Summons your active Pet."};
    }

    @Override
    protected ItemUsage[] getUsage() {
        return INTERACT_RIGHT_CLICK;
    }

    @Override
    protected ItemStack getStack() {
        return new ItemStack(Material.NAME_TAG);
    }
}
