package com.birdflop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.EconomyResponse;

public class DepositMoneyCommand implements CommandExecutor {

    private DatabaseManager dbManager;

    public DepositMoneyCommand(DatabaseManager dbManager) {
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
            player.sendMessage("Usage: /deposit_money <amount|'all'>");
            return true;
        }
    
        double amount;
        if ("all".equalsIgnoreCase(args[0])) {
            // Deposit all money
            amount = FlockMarket.econ.getBalance(player);
        } else {
            // Deposit specified amount
            try {
                amount = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Please enter a valid number for the amount.");
                return true;
            }
        }
    
        if (FlockMarket.econ.getBalance(player) < amount) {
            player.sendMessage("You do not have enough money to deposit.");
            return true;
        }
    
        EconomyResponse response = FlockMarket.econ.withdrawPlayer(player, amount);
        if (response.transactionSuccess()) {
            try {
                dbManager.depositMoney(player.getUniqueId().toString(), player.getName(), amount);
                player.sendMessage(String.format("You've deposited $%.2f into your market account.", amount));
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage("An error occurred while depositing money.");
            }
        } else {
            player.sendMessage(String.format("An error occurred: %s", response.errorMessage));
        }
    
        return true;
    }
    
}
