package com.wager.commands;

import com.wager.WagerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {

    private final WagerPlugin plugin;

    public LeaveCommand(WagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!plugin.getWagerManager().isInWager(player.getUniqueId())) {
            // Not in a wager, just teleport to lobby if it exists
            if (plugin.getArenaManager().getLobbyLocation() != null) {
                player.teleport(plugin.getArenaManager().getLobbyLocation());
                player.sendMessage(ChatColor.GREEN + "Teleported to lobby!");
            } else {
                player.sendMessage(ChatColor.RED + "You're not in a wager.");
            }
            return true;
        }

        var wager = plugin.getWagerManager().getPlayerWager(player.getUniqueId());
        if (wager != null) {
            switch (wager.getState()) {
                case WAITING -> {
                    plugin.getWagerManager().cancelWaitingWager(player);
                    player.sendMessage(ChatColor.GREEN + "You left the wager queue.");
                }
                case IN_PROGRESS, COUNTDOWN -> {
                    player.sendMessage(ChatColor.RED + "⚠ You forfeited the wager!");
                    plugin.getWagerManager().playerLeave(player);
                }
                default -> player.sendMessage(ChatColor.RED + "You can't leave right now.");
            }
        }

        return true;
    }
}
