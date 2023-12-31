package com.birdflop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class DepositItemCommand implements CommandExecutor {

    private DatabaseManager dbManager;

    public DepositItemCommand(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getAmount() == 0) {
            player.sendMessage("You are not holding any item to deposit.");
            return true;
        }

        String itemName = dbManager.getItemName(itemInHand).toLowerCase(); // This method should be accessible from dbManager

        if (itemName == "repairedItem") {
            player.sendMessage("This item is nonstandard and cannot be deposited (repair cost).");
            return true;
        } else if (itemName == "enchantmentIncomplete") {
            player.sendMessage("This book is nonstandard and cannot be deposited (incomplete enchantment).");
            return true;
        } else if (itemName == "hasExtraTags") {
            player.sendMessage("This item is nonstandard and cannot be deposited (extra tags).");
            return true;
        } else if (itemName == "hasExtraEnchantments") {
            player.sendMessage("This item is nonstandard and cannot be deposited (extra enchantments).");
            return true;
        } else if (itemName == "isNotFullDurability") {
            player.sendMessage("This item is nonstandard and cannot be deposited (damaged).");
            return true;
        }

        String uuid = player.getUniqueId().toString();

        try {
            dbManager.depositItem(uuid, itemName, itemInHand);
            player.getInventory().setItemInMainHand(null); // Only remove the item if depositItem was successful
            player.sendMessage("Deposited " + itemInHand.getAmount() + " " + itemName + " into your market account.");
        } catch (SQLException e) {
            player.sendMessage("An error occurred while depositing the item. Please try again.");
            e.printStackTrace();
        }

        return true;
    }
}
