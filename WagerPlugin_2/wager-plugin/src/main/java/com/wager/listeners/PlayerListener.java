package com.wager.listeners;

import com.wager.WagerPlugin;
import com.wager.gui.GUIManager;
import com.wager.utils.FormatUtil;
import com.wager.utils.SchedulerUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final WagerPlugin plugin;

    public PlayerListener(WagerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();

        if (plugin.getWagerManager().isInWager(dead.getUniqueId())) {
            // Suppress death message for wager fights
            event.setDeathMessage(null);
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Handle wager outcome
            plugin.getWagerManager().handleDeath(dead);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // If they were in a wager, respawn at lobby
        if (plugin.getArenaManager().getLobbyLocation() != null) {
            // Small delay to check if player was in wager (state already cleaned up by death handler)
            SchedulerUtil.runTaskLater(plugin, () -> {
                if (plugin.getArenaManager().getLobbyLocation() != null && !plugin.getWagerManager().isInWager(player.getUniqueId())) {
                    // Only teleport if not still in a wager somehow
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // If in a wager, count as forfeit
        if (plugin.getWagerManager().isInWager(player.getUniqueId())) {
            plugin.getWagerManager().playerLeave(player);
        }

        // Clean up pending GUI states
        GUIManager.removePendingAccept(player.getUniqueId());
        GUIManager.removePendingCustomAmount(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Check if player is entering custom wager amount
        if (GUIManager.hasPendingCustomAmount(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                GUIManager.removePendingCustomAmount(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Custom amount cancelled.");
                SchedulerUtil.runTask(plugin, () -> GUIManager.openCreateWagerMenu(player));
                return;
            }

            try {
                double amount = FormatUtil.parseFormattedNumber(message);
                GUIManager.removePendingCustomAmount(player.getUniqueId());

                double min = plugin.getConfig().getDouble("min-wager", 100);
                double max = plugin.getConfig().getDouble("max-wager", 1000000);

                if (amount < min) {
                    player.sendMessage(ChatColor.RED + "Minimum wager is " + FormatUtil.formatMoney(min));
                    return;
                }
                if (amount > max) {
                    player.sendMessage(ChatColor.RED + "Maximum wager is " + FormatUtil.formatMoney(max));
                    return;
                }

                // Run on main thread
                final double finalAmount = amount;
                SchedulerUtil.runTask(plugin, () -> {
                    plugin.getWagerManager().createWager(player, finalAmount);
                });

            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount! Use formats like: " + ChatColor.WHITE + "1000, 1k, 5.5k, 1m");
                player.sendMessage(ChatColor.GRAY + "Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to go back");
            }
        }
    }
}
