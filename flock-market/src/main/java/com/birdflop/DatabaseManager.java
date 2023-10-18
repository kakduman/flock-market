package com.birdflop;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;


public class DatabaseManager {

    private Plugin plugin;
    private Connection connection;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                File dbFile = new File(plugin.getDataFolder(), "flockmarket.db");

                if (!dbFile.exists()) {
                    dbFile.getParentFile().mkdirs();
                    dbFile.createNewFile();
                }

                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void initializeDatabase() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            // players table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "id INTEGER PRIMARY KEY, " +
                            "uuid TEXT NOT NULL, " +
                            "username TEXT NOT NULL, " +
                            "balance REAL NOT NULL DEFAULT 0)"
            );
            // items table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS items (" +
                            "id INTEGER PRIMARY KEY, " +
                            "uuid TEXT NOT NULL, " +
                            "item_name TEXT NOT NULL, " +
                            "quantity INTEGER NOT NULL, " +
                            "FOREIGN KEY(uuid) REFERENCES players(uuid))"
            );
            // transactions table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INTEGER PRIMARY KEY, " +
                            "buyer_uuid TEXT, " +
                            "seller_uuid TEXT, " +
                            "item_name TEXT NOT NULL, " +
                            "quantity INTEGER NOT NULL, " +
                            "price_per_unit REAL NOT NULL, " +
                            "transaction_date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                            "FOREIGN KEY(buyer_uuid) REFERENCES players(uuid), " +
                            "FOREIGN KEY(seller_uuid) REFERENCES players(uuid))"
            );
    
            // orders table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS orders (" +
                            "id INTEGER PRIMARY KEY, " +
                            "uuid TEXT NOT NULL, " +
                            "item_name TEXT NOT NULL, " +
                            "order_type TEXT NOT NULL, " +
                            "price REAL, " +
                            "quantity INTEGER NOT NULL, " +
                            "FOREIGN KEY(uuid) REFERENCES players(uuid))"
            );
        }
    }

    public void depositItem(String uuid, ItemStack item) throws SQLException {
        String checkSql = "SELECT quantity FROM items WHERE uuid = ? AND item_name = ?";
        String updateSql = "UPDATE items SET quantity = quantity + ? WHERE uuid = ? AND item_name = ?";
        String insertSql = "INSERT INTO items (uuid, item_name, quantity) VALUES (?, ?, ?)";
        
        try (Connection connection = getConnection();
             PreparedStatement checkStatement = connection.prepareStatement(checkSql);
             PreparedStatement updateStatement = connection.prepareStatement(updateSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
    
            checkStatement.setString(1, uuid);
            checkStatement.setString(2, item.getType().name());
            ResultSet rs = checkStatement.executeQuery();
    
            if (rs.next()) {
                updateStatement.setInt(1, item.getAmount());
                updateStatement.setString(2, uuid);
                updateStatement.setString(3, item.getType().name());
                updateStatement.executeUpdate();
            } else {
                insertStatement.setString(1, uuid);
                insertStatement.setString(2, item.getType().name());
                insertStatement.setInt(3, item.getAmount());
                insertStatement.executeUpdate();
            }
        }
    }
    
    public void depositMoney(String uuid, String username, double amount) throws SQLException {
        String checkPlayerSql = "SELECT * FROM players WHERE uuid = ?";
        try (PreparedStatement checkPlayerStmt = getConnection().prepareStatement(checkPlayerSql)) {
            checkPlayerStmt.setString(1, uuid);
            ResultSet rs = checkPlayerStmt.executeQuery();
            if (!rs.next()) { // If the player doesn't exist, insert a new record
                String insertPlayerSql = "INSERT INTO players (uuid, username, balance) VALUES (?, ?, ?)";
                try (PreparedStatement insertPlayerStmt = getConnection().prepareStatement(insertPlayerSql)) {
                    insertPlayerStmt.setString(1, uuid);
                    insertPlayerStmt.setString(2, username);
                    insertPlayerStmt.setDouble(3, amount);
                    insertPlayerStmt.executeUpdate();
                }
            } else { // If the player exists, update the balance
                String updateBalanceSql = "UPDATE players SET balance = balance + ? WHERE uuid = ?";
                try (PreparedStatement updateBalanceStmt = getConnection().prepareStatement(updateBalanceSql)) {
                    updateBalanceStmt.setDouble(1, amount);
                    updateBalanceStmt.setString(2, uuid);
                    updateBalanceStmt.executeUpdate();
                }
            }
        }
    }

}
