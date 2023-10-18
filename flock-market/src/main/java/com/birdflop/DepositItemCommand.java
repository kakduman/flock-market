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

        // Assuming that playerId is the same as the player's UUID.
        // You might need to adjust this based on how you manage player IDs.
        String uuid = player.getUniqueId().toString();

        try {
            dbManager.depositItem(uuid, itemInHand);
            player.getInventory().setItemInMainHand(null); // Removing the item from the player's hand
            player.sendMessage("Deposited " + itemInHand.getAmount() + " " + itemInHand.getType().name() + " into your market account.");
        } catch (SQLException e) {
            player.sendMessage("An error occurred while depositing the item. Please try again.");
            e.printStackTrace();
        }

        return true;
    }
}
