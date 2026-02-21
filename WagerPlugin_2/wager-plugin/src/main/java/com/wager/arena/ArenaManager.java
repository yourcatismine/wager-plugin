package com.wager.arena;

import com.wager.WagerPlugin;
import com.wager.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final WagerPlugin plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private Location lobbyLocation;
    private File arenaConfigFile;
    private FileConfiguration arenaConfig;

    public ArenaManager(WagerPlugin plugin) {
        this.plugin = plugin;
        this.arenaConfigFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenaConfigFile.exists()) {
            try {
                arenaConfigFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.arenaConfig = YamlConfiguration.loadConfiguration(arenaConfigFile);
    }

    public void loadArenas() {
        // Load lobby
        if (arenaConfig.contains("lobby")) {
            lobbyLocation = Arena.deserializeLocation(arenaConfig.getString("lobby"));
        }

        // Load arenas from config
        if (arenaConfig.contains("arenas")) {
            for (String key : arenaConfig.getConfigurationSection("arenas").getKeys(false)) {
                Arena arena = new Arena(key);
                String path = "arenas." + key;
                arena.setSpawn1(Arena.deserializeLocation(arenaConfig.getString(path + ".spawn1")));
                arena.setSpawn2(Arena.deserializeLocation(arenaConfig.getString(path + ".spawn2")));
                arena.setSchematicName(arenaConfig.getString(path + ".schematic", ""));
                arenas.put(key, arena);
                plugin.getLogger().info("Loaded arena: " + key);
            }
        }

        // Create arena world if it doesn't exist
        String worldName = plugin.getConfig().getString("arena-world", "wager_arenas");
        if (Bukkit.getWorld(worldName) == null) {
            if (com.wager.utils.SchedulerUtil.isFolia()) {
                plugin.getLogger().warning("!!! FOLIA DETECTED !!!");
                plugin.getLogger().warning("Folia does not support dynamic world creation.");
                plugin.getLogger().warning("Please manually create or load the world: " + worldName);
                plugin.getLogger().warning("Arenas using this world will not work until the world is loaded.");
            } else {
                plugin.getLogger().info("Creating arena world: " + worldName);
                WorldCreator creator = new WorldCreator(worldName);
                try {
                    creator.type(org.bukkit.WorldType.FLAT);
                } catch (NoClassDefFoundError ignored) {
                    // WorldType removed in this server version - world will use default type
                }
                creator.generateStructures(false);
                World world = creator.createWorld();
                if (world != null) {
                    world.setAutoSave(false);
                    world.setDifficulty(org.bukkit.Difficulty.NORMAL);
                    world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                    world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
                    world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                    world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, true);
                    world.setTime(6000);
                }
            }
        }

        // Auto-load schematics
        loadSchematics();

        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }

    /**
     * Scans schematics folder and creates arena entries for new schematics
     */
    private void loadSchematics() {
        File schemDir = new File(plugin.getDataFolder(), "schematics");
        if (!schemDir.exists()) {
            schemDir.mkdirs();
            return;
        }

        File[] files = schemDir.listFiles((dir, name) -> name.endsWith(".schem") || name.endsWith(".schematic"));
        if (files == null)
            return;

        for (File file : files) {
            String name = file.getName().replace(".schem", "").replace(".schematic", "");
            if (!arenas.containsKey(name)) {
                Arena arena = new Arena(name);
                arena.setSchematicName(file.getName());
                arenas.put(name, arena);
                plugin.getLogger().info("Found new schematic: " + file.getName() + " - Arena '" + name
                        + "' created. Set spawns with /arena setspawn <1|2> " + name);

                // Try to paste schematic using WorldEdit
                pasteSchematic(file, name);
            }
        }
    }

    /**
     * Paste a schematic into the arena world using WorldEdit API
     */
    private void pasteSchematic(File schematicFile, String arenaName) {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
                plugin.getLogger().warning("WorldEdit not found. Please install WorldEdit to auto-paste schematics.");
                return;
            }

            String worldName = plugin.getConfig().getString("arena-world", "wager_arenas");
            World world = Bukkit.getWorld(worldName);
            if (world == null)
                return;

            // Calculate offset position for each arena (space them 200 blocks apart)
            int arenaIndex = new ArrayList<>(arenas.keySet()).indexOf(arenaName);
            int xOffset = arenaIndex * 200;

            SchedulerUtil.runTaskAsync(plugin, () -> {
                try {
                    com.sk89q.worldedit.extent.clipboard.Clipboard clipboard;
                    com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat format = com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
                            .findByFile(schematicFile);
                    if (format == null) {
                        plugin.getLogger().warning("Unknown schematic format: " + schematicFile.getName());
                        return;
                    }

                    try (com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader = format
                            .getReader(new java.io.FileInputStream(schematicFile))) {
                        clipboard = reader.read();
                    }

                    com.sk89q.worldedit.world.World weWorld = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world);

                    try (com.sk89q.worldedit.EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance()
                            .newEditSession(weWorld)) {
                        com.sk89q.worldedit.function.operation.Operation operation = new com.sk89q.worldedit.session.ClipboardHolder(
                                clipboard)
                                .createPaste(editSession)
                                .to(com.sk89q.worldedit.math.BlockVector3.at(xOffset, 64, 0))
                                .ignoreAirBlocks(false)
                                .build();
                        com.sk89q.worldedit.function.operation.Operations.complete(operation);
                    }

                    plugin.getLogger().info("Pasted schematic '" + schematicFile.getName() + "' at " + xOffset
                            + ", 64, 0 in " + worldName);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to paste schematic: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            plugin.getLogger().warning("Error loading schematic " + schematicFile.getName() + ": " + e.getMessage());
        }
    }

    public void saveArenas() {
        // Save lobby
        if (lobbyLocation != null) {
            arenaConfig.set("lobby", Arena.serializeLocation(lobbyLocation));
        }

        // Save arenas
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            String path = "arenas." + entry.getKey();
            Arena arena = entry.getValue();
            arenaConfig.set(path + ".spawn1", Arena.serializeLocation(arena.getSpawn1()));
            arenaConfig.set(path + ".spawn2", Arena.serializeLocation(arena.getSpawn2()));
            arenaConfig.set(path + ".schematic", arena.getSchematicName());
        }

        try {
            arenaConfig.save(arenaConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        saveArenas();
        return arena;
    }

    public void deleteArena(String name) {
        arenas.remove(name);
        arenaConfig.set("arenas." + name, null);
        saveArenas();
    }

    /**
     * Find an available arena for a wager match
     */
    public Arena findAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isReady() && !arena.isInUse()) {
                return arena;
            }
        }
        return null;
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Map<String, Arena> getArenas() {
        return Collections.unmodifiableMap(arenas);
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
        saveArenas();
    }

    public void reloadSchematics() {
        loadSchematics();
    }
}
