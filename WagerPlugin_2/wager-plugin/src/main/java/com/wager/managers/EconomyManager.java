package com.wager.managers;

import com.wager.WagerPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;

public class EconomyManager {

    private final WagerPlugin plugin;

    // Vault (used when available)
    private Economy vaultEconomy;
    private boolean usingVault = false;

    // Built-in file economy (fallback)
    private File economyFile;
    private FileConfiguration economyConfig;

    public EconomyManager(WagerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        // Try Vault first
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) { //
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                    .getRegistration(Economy.class);
            if (rsp != null && rsp.getProvider() != null) {
                vaultEconomy = rsp.getProvider();
                usingVault = true;
                plugin.getLogger().info("Successfully hooked into Vault economy: " + vaultEconomy.getName());

                if (vaultEconomy.getName().equalsIgnoreCase("PrismEconomy")) {
                    //
                }
                return true;
            }
            plugin.getLogger().warning(
                    "Vault found but no economy provider found yet.");
        } else {
            plugin.getLogger()
                    .warning("Vault not found. Wager system will use the built-in local economy");
        }

        // Built-in economy fallback
        setupBuiltInEconomy();
        plugin.getLogger().info("Built-in economy enabled. Players start with $"
                + plugin.getConfig().getDouble("starting-balance", 10000.0));
        return true;
    }

    private void setupBuiltInEconomy() {
        economyFile = new File(plugin.getDataFolder(), "economy.yml");
        if (!economyFile.exists()) {
            try {
                economyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        economyConfig = YamlConfiguration.loadConfiguration(economyFile);
    }

    private void saveEconomy() {
        if (economyConfig != null && economyFile != null) {
            try {
                economyConfig.save(economyFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private double getBuiltInBalance(Player player) {
        String key = player.getUniqueId().toString();
        if (!economyConfig.contains(key)) {
            double start = plugin.getConfig().getDouble("starting-balance", 10000.0);
            economyConfig.set(key, start);
            saveEconomy();
            return start;
        }
        return economyConfig.getDouble(key);
    }

    public double getBalance(Player player) {
        if (usingVault) {
            return vaultEconomy.getBalance(player);
        }
        return getBuiltInBalance(player);
    }

    public boolean hasEnough(Player player, double amount) {
        if (usingVault) {
            return vaultEconomy.has(player, amount);
        }
        return getBuiltInBalance(player) >= amount;
    }

    public void withdraw(Player player, double amount) {
        if (usingVault) {
            vaultEconomy.withdrawPlayer(player, amount);
            return;
        }
        double balance = getBuiltInBalance(player) - amount;
        economyConfig.set(player.getUniqueId().toString(), balance);
        saveEconomy();
    }

    public void deposit(Player player, double amount) {
        if (usingVault) {
            vaultEconomy.depositPlayer(player, amount);
            return;
        }
        double balance = getBuiltInBalance(player) + amount;
        economyConfig.set(player.getUniqueId().toString(), balance);
        saveEconomy();
    }

    public double calculateTax(double amount) {
        double taxPercent = plugin.getConfig().getDouble("tax-percent", 3.0);
        return amount * (taxPercent / 100.0);
    }

    public double calculateWinnings(double wagerAmount) {
        double total = wagerAmount * 2;
        double tax = calculateTax(total);
        return total - tax;
    }

    public boolean isUsingVault() {
        return usingVault;
    }
}
