package com.birdflop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.HashMap;

public class WithdrawItemCommand implements CommandExecutor {

    private DatabaseManager dbManager;

    public WithdrawItemCommand(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /withdraw_item [item-name] [quantity|'all']");
            return true;
        }

        Player player = (Player) sender;
        String itemName = args[0].toLowerCase();
        String quantityStr = args[1];
        int quantity;

        // Try to get the quantity or set to maximum if "all" is specified
        if ("all".equalsIgnoreCase(quantityStr)) {
            try {
                quantity = dbManager.getItemQuantity(player.getUniqueId().toString(), itemName);
            } catch (SQLException e) {
                player.sendMessage("An error occurred. Please try again.");
                e.printStackTrace();
                return true;
            }
            if (quantity <= 0) {
                player.sendMessage("You do not have any " + itemName + " to withdraw.");
                return true;
            }
        } else {
            try {
                quantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid quantity. Please enter a number or 'all'.");
                return true;
            }
        }

        try {
            ItemStack item = dbManager.withdrawItem(player.getUniqueId().toString(), itemName, quantity);
            if (item == null) {
                player.sendMessage("You do not have enough " + itemName + " to withdraw.");
                return true;
            }
            
            // Give items to the player, respecting the max stack size
            while (quantity > 0) {
                int amountToGive = Math.min(quantity, item.getMaxStackSize());
                item.setAmount(amountToGive);
                HashMap<Integer, ItemStack> couldNotStore = player.getInventory().addItem(item);
                if (!couldNotStore.isEmpty()) {
                    // Drop on the ground if inventory is full
                    couldNotStore.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                }
                quantity -= amountToGive;
                if (quantity > 0) {
                    item = item.clone(); // Clone the ItemStack for the next iteration if there's more to give
                }
            }

            player.sendMessage("Withdrew " + quantityStr + " " + itemName + " from your market account.");
        } catch (SQLException e) {
            player.sendMessage("An error occurred while withdrawing the item. Please try again.");
            e.printStackTrace();
        }

        return true;
    }
}
