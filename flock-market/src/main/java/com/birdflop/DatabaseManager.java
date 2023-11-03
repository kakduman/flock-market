package com.birdflop;
import de.tr7zw.changeme.nbtapi.NBTItem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

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
                            "FOREIGN KEY(uuid) REFERENCES players(uuid)" +
                            "FOREIGN KEY(item_name) REFERENCES itemtodata(item_name))"
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
                            "FOREIGN KEY(seller_uuid) REFERENCES players(uuid), " +
                            "FOREIGN KEY(item_name) REFERENCES itemtodata(item_name))"
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
                            "FOREIGN KEY(uuid) REFERENCES players(uuid), " +
                            "FOREIGN KEY(item_name) REFERENCES itemtodata(item_name))"
            );
            // itemtodata table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS itemtodata (" +
                        "id INTEGER PRIMARY KEY, " +
                        "item_name TEXT NOT NULL, " +
                        "item_data TEXT NOT NULL)"
            );
        }
    }

    public void depositItem(String uuid, String itemName, ItemStack item) throws SQLException {
    
        // Check if the item name and data already exist in the itemtodata table
        addItemToDataMapping(itemName, item); // This method is defined as earlier, but may now skip if item exists
    
        // Now proceed with depositing the item, as you now have a valid item name
        // and the item data is either already in itemtodata table or has just been added
        String checkSql = "SELECT quantity FROM items WHERE uuid = ? AND item_name = ?";
        String updateSql = "UPDATE items SET quantity = quantity + ? WHERE uuid = ? AND item_name = ?";
        String insertSql = "INSERT INTO items (uuid, item_name, quantity) VALUES (?, ?, ?)";
    
        try (Connection connection = getConnection();
             PreparedStatement checkStatement = connection.prepareStatement(checkSql);
             PreparedStatement updateStatement = connection.prepareStatement(updateSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            
            checkStatement.setString(1, uuid);
            checkStatement.setString(2, itemName);
            ResultSet rs = checkStatement.executeQuery();
    
            if (rs.next()) {
                updateStatement.setInt(1, item.getAmount());
                updateStatement.setString(2, uuid);
                updateStatement.setString(3, itemName);
                updateStatement.executeUpdate();
            } else {
                insertStatement.setString(1, uuid);
                insertStatement.setString(2, itemName);
                insertStatement.setInt(3, item.getAmount());
                insertStatement.executeUpdate();
            }
        }
    }

    public void addItemToDataMapping(String itemName, ItemStack item) throws SQLException {
        String checkSql = "SELECT item_data FROM itemtodata WHERE item_name = ?";
        String insertSql = "INSERT INTO itemtodata (item_name, item_data) VALUES (?, ?)";
        String itemData = itemStackToBase64(item);
    
        try (Connection connection = getConnection();
             PreparedStatement checkStatement = connection.prepareStatement(checkSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            
            checkStatement.setString(1, itemName);
            ResultSet rs = checkStatement.executeQuery();
    
            // If the item name is not already present in the table, insert it
            if (!rs.next()) {
                insertStatement.setString(1, itemName);
                insertStatement.setString(2, itemData);
                insertStatement.executeUpdate();
            }
        }
    }
    

    public String getItemName(ItemStack item) {
        // Check for item meta (renaming, special tags, etc.)
        if (!hasNoExtraNbtTags(item)){
            return "repairedItem";
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            
            // Check if the item is an enchanted book
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) meta;
                
                // Check if the book has exactly one enchantment
                if (bookMeta.getStoredEnchants().size() == 1) {
                    for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
                        Enchantment enchantment = entry.getKey();
                        int level = entry.getValue();
                        
                        // Check if the enchantment is at max level
                        if (level < enchantment.getMaxLevel()) {
                            return "enchantmentIncomplete";
                        }
                    }
                    
                    // Check for extra NBT tags excluding the enchantment
                    if (meta.hasLore() || meta.hasDisplayName() || meta.hasCustomModelData() || !meta.getItemFlags().isEmpty()) {
                        return "hasExtraTags";
                    }

                    // If all checks pass, return the enchantment name
                    return bookMeta.getStoredEnchants().entrySet().iterator().next().getKey().getKey().getKey();
                }
                
                // If the book has more than one enchantment, it's not valid
                return "hasExtraEnchantments";
            } else {
                // For other items, check if they are renamed, have lore, or other tags
                if (meta.hasDisplayName() || meta.hasLore() || meta.hasCustomModelData() || meta.hasAttributeModifiers()) {
                    return "hasExtraTags";
                }
            }
        }

        // Check for durability (assuming it’s a tool, weapon, or armor)
        if (item.getType().getMaxDurability() != 0 && item.getDamage() != 0) {
            return "isNotFullDurability";
        }

        // return item name
        return item.getType().name();
    }

    public String itemStackToBase64(ItemStack item) throws IllegalStateException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
             
            dataOutput.writeObject(item); // Serialize the ItemStack
            // Convert the output stream to a byte array, then encode to Base64 and return as a string
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            // IllegalStateException is thrown to indicate that the serialization failed
            throw new IllegalStateException("Unable to save item stack.", e);
        }
    }    

    public boolean hasNoExtraNbtTags(ItemStack item) {
        NBTItem nbtItem = new NBTItem(item);

        // Check if the item has ever been repaired / put in an anvil
        if (nbtItem.hasKey("RepairCost")) {
            return false;
        }
        return true; // Item is clean of unwanted NBT tags
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

    public int getItemQuantity(String uuid, String itemName) throws SQLException {
        String sql = "SELECT quantity FROM items WHERE uuid = ? AND item_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setString(2, itemName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("quantity");
            }
        }
        return 0; // No items found for that name or an error occurred
    }
    

    public ItemStack withdrawItem(String uuid, String itemName, int quantity) throws SQLException {
        // Check the quantity of the item the player has
        int playerQuantity = getItemQuantity(uuid, itemName);
        if (playerQuantity < quantity) {
            return null; // Not enough of the item to withdraw
        }
    
        // Get the serialized item data from the itemtodata table
        String itemDataSql = "SELECT item_data FROM itemtodata WHERE item_name = ?";
        String itemData = null;
        try (PreparedStatement itemDataStmt = getConnection().prepareStatement(itemDataSql)) {
            itemDataStmt.setString(1, itemName);
            ResultSet rs = itemDataStmt.executeQuery();
            if (rs.next()) {
                itemData = rs.getString("item_data");
            }
        }
        if (itemData == null) {
            return null; // No item data found
        }
    
        ItemStack item = base64ToItemStack(itemData);
        
        // Update the items table
        String updateSql = "UPDATE items SET quantity = quantity - ? WHERE uuid = ? AND item_name = ?";
        String deleteSql = "DELETE FROM items WHERE uuid = ? AND item_name = ? AND quantity <= 0";
        try (PreparedStatement updateStmt = getConnection().prepareStatement(updateSql);
            PreparedStatement deleteStmt = getConnection().prepareStatement(deleteSql)) {
            updateStmt.setInt(1, quantity);
            updateStmt.setString(2, uuid);
            updateStmt.setString(3, itemName);
            updateStmt.executeUpdate();
    
            // Only execute the delete statement if the quantity after update is 0 or less
            if (playerQuantity - quantity <= 0) {
                deleteStmt.setString(1, uuid);
                deleteStmt.setString(2, itemName);
                deleteStmt.executeUpdate();
            }
        }
    
        // Adjust the quantity of the item to withdraw
        item.setAmount(quantity > item.getMaxStackSize() ? item.getMaxStackSize() : quantity);
        
        return item;
    }
    
    public ItemStack base64ToItemStack(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                return (ItemStack) dataInput.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to read item stack from base64.", e);
        }
    }

    public double getPlayerBalance(String uuid) throws SQLException {
        String sql = "SELECT balance FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        }
        return 0.0; // Player not found or no balance
    }

    public boolean withdrawMoney(String uuid, double amount) throws SQLException {
        // First, check if the player has enough balance
        double balance = getPlayerBalance(uuid);
        if (balance < amount) {
            return false; // Not enough balance to withdraw
        }

        // Proceed to withdraw the amount
        String sql = "UPDATE players SET balance = balance - ? WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, uuid);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0; // Return true if the withdrawal was successful
        }
    }

}
