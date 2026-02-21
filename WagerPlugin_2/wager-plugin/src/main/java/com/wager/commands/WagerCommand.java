package com.wager.commands;

import com.wager.WagerPlugin;
import com.wager.gui.GUIManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WagerCommand implements CommandExecutor {

    private final WagerPlugin plugin;

    public WagerCommand(WagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (plugin.getWagerManager().isInWager(player.getUniqueId())) {
            var wager = plugin.getWagerManager().getPlayerWager(player.getUniqueId());
            if (wager != null && wager.getState() != com.wager.managers.Wager.WagerState.WAITING) {
                player.sendMessage(org.bukkit.ChatColor.RED + "You're currently in an active wager! Use /leave to forfeit.");
                return true;
            }
        }

        GUIManager.openMainMenu(player);
        return true;
    }
}
