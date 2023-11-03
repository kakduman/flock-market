package com.birdflop;
import java.sql.SQLException;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FlockMarket extends JavaPlugin {

    public static Economy econ = null; // Declare econ as a public static member

    @Override
    public void onEnable() {

        getLogger().info("FlockMarket has been enabled.");

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DatabaseManager dbManager = new DatabaseManager(this);

        try {
            dbManager.initializeDatabase(); // Initialize the database tables
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to initialize the database.");
        }

        getCommand("deposit_item").setExecutor(new DepositItemCommand(dbManager));
        getCommand("deposit_money").setExecutor(new DepositMoneyCommand(dbManager));
        getCommand("withdraw_item").setExecutor(new WithdrawItemCommand(dbManager));
        getCommand("withdraw_money").setExecutor(new WithdrawMoneyCommand(dbManager));
}


    @Override
    public void onDisable() {
        getLogger().info("FlockMarket has been disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}
