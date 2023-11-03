package com.birdflop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class MarketBalanceCommand implements CommandExecutor {

    private DatabaseManager dbManager;

    public MarketBalanceCommand(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        double currentBalance;
        try {
            currentBalance = dbManager.getPlayerBalance(player.getUniqueId().toString());
            player.sendMessage("Market balance: $" + currentBalance);
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while checking your market account balance.");
            return true;
        }
        return true;
    }
}