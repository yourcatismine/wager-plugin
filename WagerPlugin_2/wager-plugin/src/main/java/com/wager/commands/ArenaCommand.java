package com.wager.commands;

import com.wager.WagerPlugin;
import com.wager.arena.Arena;
import com.wager.arena.ArenaManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final WagerPlugin plugin;

    public ArenaCommand(WagerPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("arena").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("wager.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage arenas!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        ArenaManager am = plugin.getArenaManager();

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /arena create <name>");
                    return true;
                }
                String name = args[1];
                if (am.getArena(name) != null) {
                    player.sendMessage(ChatColor.RED + "Arena '" + name + "' already exists!");
                    return true;
                }
                am.createArena(name);
                player.sendMessage(ChatColor.GREEN + "✔ Arena '" + name + "' created! Set spawns with:");
                player.sendMessage(ChatColor.YELLOW + "  /arena setspawn 1 " + name);
                player.sendMessage(ChatColor.YELLOW + "  /arena setspawn 2 " + name);
            }

            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /arena delete <name>");
                    return true;
                }
                String name = args[1];
                if (am.getArena(name) == null) {
                    player.sendMessage(ChatColor.RED + "Arena '" + name + "' not found!");
                    return true;
                }
                am.deleteArena(name);
                player.sendMessage(ChatColor.GREEN + "✔ Arena '" + name + "' deleted!");
            }

            case "setspawn" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /arena setspawn <1|2> <arena>");
                    return true;
                }
                String spawnNum = args[1];
                String arenaName = args[2];

                Arena arena = am.getArena(arenaName);
                if (arena == null) {
                    player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found! Create it first with /arena create " + arenaName);
                    return true;
                }

                if (spawnNum.equals("1")) {
                    arena.setSpawn1(player.getLocation());
                    am.saveArenas();
                    player.sendMessage(ChatColor.GREEN + "✔ Spawn 1 set for arena '" + arenaName + "'!");
                } else if (spawnNum.equals("2")) {
                    arena.setSpawn2(player.getLocation());
                    am.saveArenas();
                    player.sendMessage(ChatColor.GREEN + "✔ Spawn 2 set for arena '" + arenaName + "'!");
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid spawn number! Use 1 or 2.");
                }

                if (arena.isReady()) {
                    player.sendMessage(ChatColor.GREEN + "  ✔ Arena '" + arenaName + "' is now ready for wagers!");
                }
            }

            case "setlobby" -> {
                am.setLobbyLocation(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "✔ Lobby location set!");
            }

            case "list" -> {
                player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage(ChatColor.GOLD + "  ⚔ Arenas");
                player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                if (am.getArenas().isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "  No arenas configured.");
                } else {
                    for (Arena arena : am.getArenas().values()) {
                        String status;
                        if (!arena.isReady()) {
                            status = ChatColor.RED + "NOT READY (missing spawns)";
                        } else if (arena.isInUse()) {
                            status = ChatColor.YELLOW + "IN USE";
                        } else {
                            status = ChatColor.GREEN + "AVAILABLE";
                        }
                        player.sendMessage(ChatColor.WHITE + "  " + arena.getId() + " - " + status);
                        if (arena.getSchematicName() != null && !arena.getSchematicName().isEmpty()) {
                            player.sendMessage(ChatColor.GRAY + "    Schematic: " + arena.getSchematicName());
                        }
                    }
                }
                player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                if (am.getLobbyLocation() != null) {
                    player.sendMessage(ChatColor.GREEN + "  ✔ Lobby is set");
                } else {
                    player.sendMessage(ChatColor.RED + "  ✘ Lobby not set! Use /arena setlobby");
                }
            }

            case "reload" -> {
                am.reloadSchematics();
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "✔ Config and schematics reloaded!");
            }

            case "world" -> {
                String worldName = plugin.getConfig().getString("arena-world", "wager_arenas");
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
                if (world != null) {
                    player.teleport(world.getSpawnLocation());
                    player.sendMessage(ChatColor.GREEN + "Teleported to arena world: " + worldName);
                } else {
                    player.sendMessage(ChatColor.RED + "Arena world not found! It will be created on next restart.");
                }
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.GOLD + "  ⚔ Arena Commands");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.YELLOW + "  /arena create <name>" + ChatColor.GRAY + " - Create an arena");
        player.sendMessage(ChatColor.YELLOW + "  /arena delete <name>" + ChatColor.GRAY + " - Delete an arena");
        player.sendMessage(ChatColor.YELLOW + "  /arena setspawn <1|2> <arena>" + ChatColor.GRAY + " - Set spawn point");
        player.sendMessage(ChatColor.YELLOW + "  /arena setlobby" + ChatColor.GRAY + " - Set lobby location");
        player.sendMessage(ChatColor.YELLOW + "  /arena list" + ChatColor.GRAY + " - List all arenas");
        player.sendMessage(ChatColor.YELLOW + "  /arena world" + ChatColor.GRAY + " - Teleport to arena world");
        player.sendMessage(ChatColor.YELLOW + "  /arena reload" + ChatColor.GRAY + " - Reload config & schematics");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.GRAY + "  Drop .schem files in plugins/WagerPlugin/schematics/");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "delete", "setspawn", "setlobby", "list", "world", "reload"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setspawn")) {
                return filter(Arrays.asList("1", "2"), args[1]);
            }
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("setspawn")) {
                return filter(new ArrayList<>(plugin.getArenaManager().getArenas().keySet()), args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {
            return filter(new ArrayList<>(plugin.getArenaManager().getArenas().keySet()), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
