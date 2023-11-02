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

        String itemName = dbManager.getItemName(itemInHand); // This method should be accessible from dbManager
        if (itemName == null) {
            player.sendMessage("This item cannot be deposited.");
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
