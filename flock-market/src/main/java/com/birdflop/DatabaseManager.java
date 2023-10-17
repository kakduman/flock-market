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

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "id INTEGER PRIMARY KEY, " +
                            "uuid TEXT NOT NULL, " +
                            "username TEXT NOT NULL, " +
                            "balance REAL NOT NULL DEFAULT 0)"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS items (" +
                            "id INTEGER PRIMARY KEY, " +
                            "player_id INTEGER NOT NULL, " +
                            "item_name TEXT NOT NULL, " +
                            "quantity INTEGER NOT NULL, " +
                            "FOREIGN KEY(player_id) REFERENCES players(id))"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INTEGER PRIMARY KEY, " +
                            "buyer_id INTEGER, " +
                            "seller_id INTEGER, " +
                            "item_id INTEGER, " +
                            "quantity INTEGER NOT NULL, " +
                            "price_per_unit REAL NOT NULL, " +
                            "transaction_date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                            "FOREIGN KEY(buyer_id) REFERENCES players(id), " +
                            "FOREIGN KEY(seller_id) REFERENCES players(id), " +
                            "FOREIGN KEY(item_id) REFERENCES items(id))"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS orders (" +
                            "id INTEGER PRIMARY KEY, " +
                            "player_id INTEGER NOT NULL, " +
                            "item_id INTEGER NOT NULL, " +
                            "order_type TEXT NOT NULL, " +
                            "price REAL, " +
                            "quantity INTEGER NOT NULL, " +
                            "FOREIGN KEY(player_id) REFERENCES players(id), " +
                            "FOREIGN KEY(item_id) REFERENCES items(id))"
            );
        }
    }
    public void addItem(String playerId, ItemStack item) throws SQLException {
        String checkSql = "SELECT quantity FROM items WHERE player_id = ? AND item_name = ?";
        String updateSql = "UPDATE items SET quantity = quantity + ? WHERE player_id = ? AND item_name = ?";
        String insertSql = "INSERT INTO items (player_id, item_name, quantity) VALUES (?, ?, ?)";
        
        try (Connection connection = getConnection();
            PreparedStatement checkStatement = connection.prepareStatement(checkSql);
            PreparedStatement updateStatement = connection.prepareStatement(updateSql);
            PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {

            checkStatement.setString(1, playerId);
            checkStatement.setString(2, item.getType().name());
            ResultSet rs = checkStatement.executeQuery();

            if (rs.next()) {
                updateStatement.setInt(1, item.getAmount());
                updateStatement.setString(2, playerId);
                updateStatement.setString(3, item.getType().name());
                updateStatement.executeUpdate();
            } else {
                insertStatement.setString(1, playerId);
                insertStatement.setString(2, item.getType().name());
                insertStatement.setInt(3, item.getAmount());
                insertStatement.executeUpdate();
            }
        }
    }

    public void depositMoney(String playerId, double amount) throws SQLException {
        String sqlUpdateBalance = "UPDATE players SET balance = balance + ? WHERE uuid = ?";

        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sqlUpdateBalance)) {

            statement.setDouble(1, amount);
            statement.setString(2, playerId);

            int rowsAffected = statement.executeUpdate();

            // If no rows were affected, it means the player is not in the database yet.
            if (rowsAffected == 0) {
                String sqlInsertPlayer = "INSERT INTO players (uuid, balance) VALUES (?, ?)";
                try (PreparedStatement insertStatement = connection.prepareStatement(sqlInsertPlayer)) {
                    insertStatement.setString(1, playerId);
                    insertStatement.setDouble(2, amount);

                    insertStatement.executeUpdate();
                }
            }
        }
    }
}
