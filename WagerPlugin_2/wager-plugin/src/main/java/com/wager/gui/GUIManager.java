package com.wager.gui;

import com.wager.WagerPlugin;
import com.wager.managers.Wager;
import com.wager.utils.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GUIManager {

    // GUI identifiers stored in title
    public static final String MAIN_MENU_TITLE = ChatColor.DARK_GRAY + "⚔ " + ChatColor.GOLD + "" + ChatColor.BOLD + "Wager Menu";
    public static final String CREATE_WAGER_TITLE = ChatColor.DARK_GRAY + "⚔ " + ChatColor.GREEN + "" + ChatColor.BOLD + "Create Wager";
    public static final String CONFIRM_ACCEPT_TITLE = ChatColor.DARK_GRAY + "⚔ " + ChatColor.YELLOW + "" + ChatColor.BOLD + "Confirm Wager";
    public static final String CONFIRM_CANCEL_TITLE = ChatColor.DARK_GRAY + "⚔ " + ChatColor.RED + "" + ChatColor.BOLD + "Cancel Wager?";

    // Store pending data
    private static final Map<UUID, UUID> pendingAccepts = new HashMap<>(); // player -> wagerId
    private static final Map<UUID, Double> pendingCustomAmounts = new HashMap<>();

    /**
     * Open main wager menu
     */
    public static void openMainMenu(Player player) {
        WagerPlugin plugin = WagerPlugin.getInstance();
        Inventory gui = Bukkit.createInventory(null, 54, MAIN_MENU_TITLE);

        // Fill borders with dark glass
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) gui.setItem(i, border);
        for (int i = 45; i < 54; i++) gui.setItem(i, border);
        for (int i = 9; i < 45; i += 9) gui.setItem(i, border);
        for (int i = 17; i < 54; i += 9) gui.setItem(i, border);

        // Create Wager button (top center)
        gui.setItem(4, createItem(Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "Create Wager",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Click to create a new wager",
                        ChatColor.GRAY + "and challenge other players!",
                        "",
                        ChatColor.YELLOW + "▶ Click to create"
                )));

        // Player balance info
        double balance = plugin.getEconomyManager().getBalance(player);
        gui.setItem(49, createItem(Material.GOLD_INGOT, ChatColor.GOLD + "" + ChatColor.BOLD + "Your Balance",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Balance: " + ChatColor.GREEN + FormatUtil.formatMoney(balance),
                        ""
                )));

        // Tax info
        gui.setItem(48, createItem(Material.PAPER, ChatColor.YELLOW + "ℹ Tax Info",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Winners are taxed " + ChatColor.YELLOW + plugin.getConfig().getDouble("tax-percent") + "%" + ChatColor.GRAY + " of the pot",
                        ChatColor.GRAY + "Example: " + ChatColor.WHITE + "$1,000" + ChatColor.GRAY + " wager each = ",
                        ChatColor.GRAY + "$2,000 pot - " + ChatColor.RED + "$60 tax" + ChatColor.GRAY + " = " + ChatColor.GREEN + "$1,940 winnings",
                        ""
                )));

        // Refresh button
        gui.setItem(50, createItem(Material.COMPASS, ChatColor.AQUA + "↻ Refresh", Arrays.asList("", ChatColor.GRAY + "Click to refresh the list")));

        // Active wager listings
        List<Wager> waitingWagers = plugin.getWagerManager().getWaitingWagers();
        int slot = 10;
        int count = 0;

        if (waitingWagers.isEmpty()) {
            gui.setItem(22, createItem(Material.BARRIER, ChatColor.GRAY + "No Active Wagers",
                    Arrays.asList("", ChatColor.GRAY + "No one is wagering right now.", ChatColor.GRAY + "Be the first to create one!", "")));
        } else {
            for (Wager wager : waitingWagers) {
                if (count >= 21) break; // Max 21 slots (3 rows x 7)
                if (slot % 9 == 0) slot++; // Skip left border
                if (slot % 9 == 8) slot += 2; // Skip right border

                boolean isOwn = wager.getCreator().equals(player.getUniqueId());

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(wager.getCreator()));

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Wager: " + ChatColor.GREEN + "" + ChatColor.BOLD + FormatUtil.formatMoney(wager.getAmount()));
                lore.add(ChatColor.GRAY + "Potential Win: " + ChatColor.GOLD + FormatUtil.formatMoney(plugin.getEconomyManager().calculateWinnings(wager.getAmount())));
                lore.add("");

                if (isOwn) {
                    skullMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + wager.getCreatorName() + ChatColor.GRAY + " (You)");
                    lore.add(ChatColor.RED + "▶ Click to cancel");
                } else {
                    skullMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + wager.getCreatorName());
                    lore.add(ChatColor.GREEN + "▶ Click to accept wager!");
                }

                lore.add("");
                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);

                // Store wager ID in NBT via display name encoding (hacky but no NMS needed)
                gui.setItem(slot, head);

                slot++;
                count++;
            }
        }

        player.openInventory(gui);
    }

    /**
     * Open create wager menu
     */
    public static void openCreateWagerMenu(Player player) {
        WagerPlugin plugin = WagerPlugin.getInstance();
        Inventory gui = Bukkit.createInventory(null, 54, CREATE_WAGER_TITLE);

        // Fill borders
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) gui.setItem(i, border);
        for (int i = 45; i < 54; i++) gui.setItem(i, border);
        for (int i = 9; i < 45; i += 9) gui.setItem(i, border);
        for (int i = 17; i < 54; i += 9) gui.setItem(i, border);

        // Title
        gui.setItem(4, createItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "" + ChatColor.BOLD + "Select Wager Amount",
                Arrays.asList("", ChatColor.GRAY + "Choose a preset or enter custom", "")));

        // Balance
        double balance = plugin.getEconomyManager().getBalance(player);
        gui.setItem(49, createItem(Material.GOLD_INGOT, ChatColor.GOLD + "Balance: " + FormatUtil.formatMoney(balance), null));

        // Preset amounts
        List<Integer> presets = plugin.getConfig().getIntegerList("preset-amounts");
        Material[] materials = {
                Material.IRON_NUGGET, Material.IRON_INGOT, Material.GOLD_NUGGET,
                Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT
        };

        int[] presetSlots = {19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(presets.size(), presetSlots.length); i++) {
            int amount = presets.get(i);
            boolean canAfford = balance >= amount;
            Material mat = canAfford ? materials[Math.min(i, materials.length - 1)] : Material.GRAY_DYE;

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Wager: " + ChatColor.GREEN + FormatUtil.formatMoney(amount));
            lore.add(ChatColor.GRAY + "Potential Win: " + ChatColor.GOLD + FormatUtil.formatMoney(plugin.getEconomyManager().calculateWinnings(amount)));
            lore.add("");
            if (canAfford) {
                lore.add(ChatColor.GREEN + "▶ Click to create wager");
            } else {
                lore.add(ChatColor.RED + "✘ Not enough money!");
            }
            lore.add("");

            gui.setItem(presetSlots[i], createItem(mat,
                    (canAfford ? ChatColor.GREEN : ChatColor.RED) + "" + ChatColor.BOLD + FormatUtil.formatShort(amount),
                    lore));
        }

        // Custom amount button
        gui.setItem(31, createItem(Material.NAME_TAG, ChatColor.YELLOW + "" + ChatColor.BOLD + "Custom Amount",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Type a custom amount in chat!",
                        ChatColor.GRAY + "Supports: " + ChatColor.WHITE + "1k, 5.5k, 1m, 1,000",
                        "",
                        ChatColor.YELLOW + "▶ Click to enter custom amount",
                        ""
                )));

        // Back button
        gui.setItem(45, createItem(Material.ARROW, ChatColor.RED + "← Back", Arrays.asList("", ChatColor.GRAY + "Return to wager menu")));

        player.openInventory(gui);
    }

    /**
     * Open confirm accept menu
     */
    public static void openConfirmAcceptMenu(Player player, Wager wager) {
        WagerPlugin plugin = WagerPlugin.getInstance();
        Inventory gui = Bukkit.createInventory(null, 27, CONFIRM_ACCEPT_TITLE);

        pendingAccepts.put(player.getUniqueId(), wager.getId());

        // Fill with gray glass
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        // Green side
        ItemStack greenGlass = createItem(Material.LIME_STAINED_GLASS_PANE, " ", null);
        for (int i : new int[]{0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21}) gui.setItem(i, greenGlass);

        // Red side
        ItemStack redGlass = createItem(Material.RED_STAINED_GLASS_PANE, " ", null);
        for (int i : new int[]{5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26}) gui.setItem(i, redGlass);

        // Info in middle
        double winnings = plugin.getEconomyManager().calculateWinnings(wager.getAmount());
        double tax = plugin.getEconomyManager().calculateTax(wager.getAmount() * 2);
        gui.setItem(4, createItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "" + ChatColor.BOLD + "Wager Details",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Opponent: " + ChatColor.AQUA + wager.getCreatorName(),
                        ChatColor.GRAY + "Amount: " + ChatColor.GREEN + FormatUtil.formatMoney(wager.getAmount()),
                        ChatColor.GRAY + "Pot: " + ChatColor.GOLD + FormatUtil.formatMoney(wager.getAmount() * 2),
                        ChatColor.GRAY + "Tax: " + ChatColor.RED + FormatUtil.formatMoney(tax),
                        ChatColor.GRAY + "Win: " + ChatColor.GREEN + FormatUtil.formatMoney(winnings),
                        ""
                )));

        // Confirm button
        gui.setItem(11, createItem(Material.LIME_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "✔ ACCEPT WAGER",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Click to accept and fight!",
                        ChatColor.YELLOW + FormatUtil.formatMoney(wager.getAmount()) + ChatColor.GRAY + " will be deducted",
                        ""
                )));

        // Cancel button
        gui.setItem(15, createItem(Material.RED_WOOL, ChatColor.RED + "" + ChatColor.BOLD + "✘ DECLINE",
                Arrays.asList(
                        "",
                        ChatColor.GRAY + "Click to go back",
                        ""
                )));

        player.openInventory(gui);
    }

    /**
     * Open cancel wager confirmation
     */
    public static void openConfirmCancelMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, CONFIRM_CANCEL_TITLE);

        Wager wager = WagerPlugin.getInstance().getWagerManager().getPlayerWager(player.getUniqueId());
        if (wager == null) return;

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        // Confirm cancel
        gui.setItem(11, createItem(Material.LIME_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "✔ YES, CANCEL",
                Arrays.asList("", ChatColor.GRAY + "Cancel wager and get refund", ChatColor.GREEN + "+" + FormatUtil.formatMoney(wager.getAmount()), "")));

        // Keep wager
        gui.setItem(15, createItem(Material.RED_WOOL, ChatColor.RED + "" + ChatColor.BOLD + "✘ NO, KEEP IT",
                Arrays.asList("", ChatColor.GRAY + "Keep your wager listed", "")));

        // Info
        gui.setItem(4, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Cancel Wager?",
                Arrays.asList("", ChatColor.GRAY + "Amount: " + ChatColor.GOLD + FormatUtil.formatMoney(wager.getAmount()), "")));

        player.openInventory(gui);
    }

    // --- Utility ---

    public static UUID getPendingAccept(UUID playerId) {
        return pendingAccepts.get(playerId);
    }

    public static void removePendingAccept(UUID playerId) {
        pendingAccepts.remove(playerId);
    }

    public static void setPendingCustomAmount(UUID playerId) {
        pendingCustomAmounts.put(playerId, 0.0);
    }

    public static boolean hasPendingCustomAmount(UUID playerId) {
        return pendingCustomAmounts.containsKey(playerId);
    }

    public static void removePendingCustomAmount(UUID playerId) {
        pendingCustomAmounts.remove(playerId);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
