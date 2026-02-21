package com.wager;

import com.wager.arena.ArenaManager;
import com.wager.commands.ArenaCommand;
import com.wager.commands.LeaveCommand;
import com.wager.commands.WagerCommand;
import com.wager.listeners.GUIListener;
import com.wager.listeners.PlayerListener;
import com.wager.managers.EconomyManager;
import com.wager.managers.WagerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class WagerPlugin extends JavaPlugin {

    private static WagerPlugin instance;
    private EconomyManager economyManager;
    private WagerManager wagerManager;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Create schematics folder
        java.io.File schemFolder = new java.io.File(getDataFolder(), "schematics");
        if (!schemFolder.exists()) {
            schemFolder.mkdirs();
        }

        // Initialize managers
        economyManager = new EconomyManager(this);
        economyManager.setupEconomy();

        arenaManager = new ArenaManager(this);
        wagerManager = new WagerManager(this);

        // Register commands
        getCommand("wager").setExecutor(new WagerCommand(this));
        getCommand("arena").setExecutor(new ArenaCommand(this));
        getCommand("leave").setExecutor(new LeaveCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Load arenas
        arenaManager.loadArenas();

        getLogger().info("WagerPlugin enabled! Tax rate: " + getConfig().getDouble("tax-percent") + "%");
    }

    @Override
    public void onDisable() {
        if (wagerManager != null) {
            wagerManager.cancelAllWagers();
        }
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }
        getLogger().info("WagerPlugin disabled!");
    }

    public static WagerPlugin getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public WagerManager getWagerManager() {
        return wagerManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }
}
