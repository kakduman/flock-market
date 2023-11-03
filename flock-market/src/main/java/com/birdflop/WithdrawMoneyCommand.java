package com.birdflop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.EconomyResponse;

import java.sql.SQLException;

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
            player.sendMessage("Usage: /withdraw_money <amount|'all'>");
            return true;
        }

        double amountToWithdraw;
        if ("all".equalsIgnoreCase(args[0])) {
            // Withdraw all money
            try {
                amountToWithdraw = dbManager.getPlayerBalance(player.getUniqueId().toString());
            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage("An error occurred while checking your market account balance.");
                return true;
            }
        } else {
            // Withdraw specified amount
            try {
                amountToWithdraw = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Please enter a valid number for the amount.");
                return true;
            }
        }

        // Check if the player has enough money in their market account to withdraw
        try {
            double balance = dbManager.getPlayerBalance(player.getUniqueId().toString());
            if (balance < amountToWithdraw) {
                player.sendMessage("You do not have enough money in your market account to withdraw that amount.");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while checking your market account balance.");
            return true;
        }

        // Perform the actual withdrawal from the market account
        try {
            if (dbManager.withdrawMoney(player.getUniqueId().toString(), amountToWithdraw)) {
                // Deposit the money back into the player's in-game balance
                EconomyResponse response = FlockMarket.econ.depositPlayer(player, amountToWithdraw);
                if (response.transactionSuccess()) {
                    player.sendMessage(String.format("You've withdrawn $%.2f from your market account.", amountToWithdraw));
                } else {
                    // In case of failure, refund the money back to the market account
                    dbManager.depositMoney(player.getUniqueId().toString(), player.getName(), amountToWithdraw);
                    player.sendMessage(String.format("An error occurred: %s", response.errorMessage));
                }
            } else {
                player.sendMessage("An error occurred while withdrawing money. (WMC-1)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while withdrawing money. (WMC-2)");
        }

        return true;
    }
}