package com.wager.listeners;

import com.wager.WagerPlugin;
import com.wager.gui.GUIManager;
import com.wager.managers.Wager;
import com.wager.utils.FormatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class GUIListener implements Listener {

    private final WagerPlugin plugin;

    public GUIListener(WagerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // Handle main menu
        if (title.equals(GUIManager.MAIN_MENU_TITLE)) {
            event.setCancelled(true);
            handleMainMenu(player, event);
            return;
        }

        // Handle create wager menu
        if (title.equals(GUIManager.CREATE_WAGER_TITLE)) {
            event.setCancelled(true);
            handleCreateMenu(player, event);
            return;
        }

        // Handle confirm accept menu
        if (title.equals(GUIManager.CONFIRM_ACCEPT_TITLE)) {
            event.setCancelled(true);
            handleConfirmAccept(player, event);
            return;
        }

        // Handle confirm cancel menu
        if (title.equals(GUIManager.CONFIRM_CANCEL_TITLE)) {
            event.setCancelled(true);
            handleConfirmCancel(player, event);
            return;
        }
    }

    private void handleMainMenu(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = event.getRawSlot();

        // Create wager button (slot 4)
        if (slot == 4 && clicked.getType() == Material.EMERALD) {
            player.closeInventory();
            GUIManager.openCreateWagerMenu(player);
            return;
        }

        // Refresh button (slot 50)
        if (slot == 50 && clicked.getType() == Material.COMPASS) {
            GUIManager.openMainMenu(player);
            return;
        }

        // Player head = wager listing
        if (clicked.getType() == Material.PLAYER_HEAD && slot >= 10 && slot <= 43) {
            // Find which wager this is based on position
            List<Wager> waitingWagers = plugin.getWagerManager().getWaitingWagers();
            int index = getWagerIndexFromSlot(slot);
            if (index >= 0 && index < waitingWagers.size()) {
                Wager wager = waitingWagers.get(index);

                if (wager.getCreator().equals(player.getUniqueId())) {
                    // Own wager - open cancel confirm
                    player.closeInventory();
                    GUIManager.openConfirmCancelMenu(player);
                } else {
                    // Someone else's wager - open accept confirm
                    player.closeInventory();
                    GUIManager.openConfirmAcceptMenu(player, wager);
                }
            }
        }
    }

    private void handleCreateMenu(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = event.getRawSlot();

        // Back button
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            player.closeInventory();
            GUIManager.openMainMenu(player);
            return;
        }

        // Custom amount
        if (slot == 31 && clicked.getType() == Material.NAME_TAG) {
            player.closeInventory();
            GUIManager.setPendingCustomAmount(player.getUniqueId());
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.YELLOW + "  Enter your wager amount in chat:");
            player.sendMessage(ChatColor.GRAY + "  Formats: " + ChatColor.WHITE + "1000, 1k, 5.5k, 1m, 1,000");
            player.sendMessage(ChatColor.GRAY + "  Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to go back");
            player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");
            return;
        }

        // Preset amount buttons
        int[] presetSlots = {19, 20, 21, 22, 23, 24, 25};
        List<Integer> presets = plugin.getConfig().getIntegerList("preset-amounts");

        for (int i = 0; i < presetSlots.length; i++) {
            if (slot == presetSlots[i] && i < presets.size()) {
                if (clicked.getType() == Material.GRAY_DYE) {
                    player.sendMessage(ChatColor.RED + "You don't have enough money for this wager!");
                    return;
                }
                double amount = presets.get(i);
                player.closeInventory();
                plugin.getWagerManager().createWager(player, amount);
                return;
            }
        }
    }

    private void handleConfirmAccept(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        // Accept
        if (slot == 11 && clicked.getType() == Material.LIME_WOOL) {
            UUID wagerId = GUIManager.getPendingAccept(player.getUniqueId());
            GUIManager.removePendingAccept(player.getUniqueId());
            player.closeInventory();

            if (wagerId != null) {
                plugin.getWagerManager().acceptWager(player, wagerId);
            }
            return;
        }

        // Decline
        if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            GUIManager.removePendingAccept(player.getUniqueId());
            player.closeInventory();
            GUIManager.openMainMenu(player);
        }
    }

    private void handleConfirmCancel(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        // Confirm cancel
        if (slot == 11 && clicked.getType() == Material.LIME_WOOL) {
            player.closeInventory();
            plugin.getWagerManager().cancelWaitingWager(player);
            return;
        }

        // Keep wager
        if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            player.closeInventory();
            GUIManager.openMainMenu(player);
        }
    }

    /**
     * Map GUI slot to wager index (accounting for border slots)
     */
    private int getWagerIndexFromSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (col == 0 || col == 8) return -1; // border
        if (row < 1 || row > 4) return -1;

        int index = 0;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                int s = r * 9 + c;
                if (s == slot) return index;
                index++;
            }
        }
        return -1;
    }
}
