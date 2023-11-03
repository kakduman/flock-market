package com.birdflop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.EconomyResponse;

public class WithdrawMoneyCommand implements CommandExecutor {

    private DatabaseManager dbManager;

    public WithdrawMoneyCommand(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Usage: /withdraw_money <amount>");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("Please enter a valid number for the amount.");
            return true;
        }

        // Check if the player has enough money in their market account to withdraw
        try {
            double balance = dbManager.getPlayerBalance(player.getUniqueId().toString());
            if (balance < amount) {
                player.sendMessage("You do not have enough money in your market account to withdraw that amount.");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while checking your market account balance.");
            return true;
        }

        // Perform the actual withdrawal from the market account
        try {
            if (dbManager.withdrawMoney(player.getUniqueId().toString(), amount)) {
                // Deposit the money back into the player's in-game balance
                EconomyResponse response = FlockMarket.econ.depositPlayer(player, amount);
                if (response.transactionSuccess()) {
                    player.sendMessage(String.format("You've withdrawn $%s from your market account.", amount));
                } else {
                    // In case of failure, refund the money back to the market account
                    dbManager.depositMoney(player.getUniqueId().toString(), player.getName(), amount);
                    player.sendMessage(String.format("An error occurred: %s", response.errorMessage));
                }
            } else {
                player.sendMessage("An error occurred while withdrawing money.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while withdrawing money.");
        }

        return true;
    }
}
