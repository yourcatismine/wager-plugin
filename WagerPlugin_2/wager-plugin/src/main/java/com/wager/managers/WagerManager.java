package com.wager.managers;

import com.wager.WagerPlugin;
import com.wager.arena.Arena;
import com.wager.utils.FormatUtil;
import com.wager.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WagerManager {

    private final WagerPlugin plugin;
    private final Map<UUID, Wager> activeWagers = new ConcurrentHashMap<>(); // wagerId -> Wager
    private final Map<UUID, UUID> playerWagerMap = new ConcurrentHashMap<>(); // playerId -> wagerId
    private final Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new ConcurrentHashMap<>();
    private final Map<UUID, Location> savedLocations = new ConcurrentHashMap<>();

    public WagerManager(WagerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a new wager listing
     */
    public Wager createWager(Player creator, double amount) {
        if (isInWager(creator.getUniqueId())) {
            creator.sendMessage(ChatColor.RED + "You are already in a wager!");
            return null;
        }

        if (!plugin.getEconomyManager().hasEnough(creator, amount)) {
            creator.sendMessage(ChatColor.RED + "You don't have enough money! Balance: " + FormatUtil.formatMoney(plugin.getEconomyManager().getBalance(creator)));
            return null;
        }

        double min = plugin.getConfig().getDouble("min-wager", 100);
        double max = plugin.getConfig().getDouble("max-wager", 1000000);
        if (amount < min || amount > max) {
            creator.sendMessage(ChatColor.RED + "Wager amount must be between " + FormatUtil.formatMoney(min) + " and " + FormatUtil.formatMoney(max));
            return null;
        }

        Wager wager = new Wager(creator.getUniqueId(), creator.getName(), amount);
        activeWagers.put(wager.getId(), wager);
        playerWagerMap.put(creator.getUniqueId(), wager.getId());

        // Take money from creator
        plugin.getEconomyManager().withdraw(creator, amount);

        creator.sendMessage(ChatColor.GREEN + "✔ Wager created for " + ChatColor.GOLD + FormatUtil.formatMoney(amount) + ChatColor.GREEN + "! Waiting for opponent...");

        // Broadcast
        Bukkit.broadcastMessage(ChatColor.GOLD + "⚔ " + ChatColor.YELLOW + creator.getName() +
                ChatColor.GOLD + " has created a wager for " + ChatColor.GREEN + FormatUtil.formatMoney(amount) +
                ChatColor.GOLD + "! Use " + ChatColor.YELLOW + "/wager" + ChatColor.GOLD + " to accept!");

        return wager;
    }

    /**
     * Accept a wager
     */
    public boolean acceptWager(Player opponent, UUID wagerId) {
        Wager wager = activeWagers.get(wagerId);
        if (wager == null || wager.getState() != Wager.WagerState.WAITING) {
            opponent.sendMessage(ChatColor.RED + "This wager is no longer available!");
            return false;
        }

        if (wager.getCreator().equals(opponent.getUniqueId())) {
            opponent.sendMessage(ChatColor.RED + "You can't accept your own wager!");
            return false;
        }

        if (isInWager(opponent.getUniqueId())) {
            opponent.sendMessage(ChatColor.RED + "You are already in a wager!");
            return false;
        }

        if (!plugin.getEconomyManager().hasEnough(opponent, wager.getAmount())) {
            opponent.sendMessage(ChatColor.RED + "You don't have enough money! Need: " + FormatUtil.formatMoney(wager.getAmount()));
            return false;
        }

        // Find available arena
        Arena arena = plugin.getArenaManager().findAvailableArena();
        if (arena == null) {
            opponent.sendMessage(ChatColor.RED + "No arenas available! All arenas are currently in use. Please try again shortly.");
            return false;
        }

        // Take money from opponent
        plugin.getEconomyManager().withdraw(opponent, wager.getAmount());

        // Set wager details
        wager.setOpponent(opponent.getUniqueId(), opponent.getName());
        wager.setState(Wager.WagerState.ACCEPTED);
        wager.setArenaId(arena.getId());
        arena.setInUse(true);
        playerWagerMap.put(opponent.getUniqueId(), wagerId);

        // Start the wager
        startWager(wager, arena);
        return true;
    }

    /**
     * Start the wager match with countdown
     */
    private void startWager(Wager wager, Arena arena) {
        Player creator = Bukkit.getPlayer(wager.getCreator());
        Player opponent = Bukkit.getPlayer(wager.getOpponent());

        if (creator == null || opponent == null) {
            cancelWager(wager.getId(), "A player disconnected!");
            return;
        }

        // Save inventories and locations
        savePlayerState(creator);
        savePlayerState(opponent);

        // Prepare players
        preparePlayer(creator);
        preparePlayer(opponent);

        // Teleport to arena
        SchedulerUtil.teleportPlayer(plugin, creator, arena.getSpawn1());
        SchedulerUtil.teleportPlayer(plugin, opponent, arena.getSpawn2());

        wager.setState(Wager.WagerState.COUNTDOWN);

        // Send title: Wager Starting
        String moneyText = FormatUtil.formatMoney(wager.getAmount());
        creator.sendTitle(ChatColor.GOLD + "⚔ WAGER STARTING", ChatColor.YELLOW + "vs " + opponent.getName() + " §7| " + ChatColor.GREEN + moneyText, 10, 40, 10);
        opponent.sendTitle(ChatColor.GOLD + "⚔ WAGER STARTING", ChatColor.YELLOW + "vs " + creator.getName() + " §7| " + ChatColor.GREEN + moneyText, 10, 40, 10);

        // Freeze players during countdown
        creator.setWalkSpeed(0);
        opponent.setWalkSpeed(0);

        int countdownSeconds = plugin.getConfig().getInt("countdown-seconds", 5);

        // Countdown
        int[] count = {countdownSeconds};
        SchedulerUtil.runTaskTimer(plugin, cancel -> {
            if (wager.getState() == Wager.WagerState.FINISHED) {
                cancel.run();
                return;
            }

            Player p1 = Bukkit.getPlayer(wager.getCreator());
            Player p2 = Bukkit.getPlayer(wager.getOpponent());
            if (p1 == null || p2 == null) {
                cancelWager(wager.getId(), "A player disconnected!");
                cancel.run();
                return;
            }

            if (count[0] <= 0) {
                // Start fight
                wager.setState(Wager.WagerState.IN_PROGRESS);
                p1.setWalkSpeed(0.2f);
                p2.setWalkSpeed(0.2f);
                p1.sendTitle(ChatColor.RED + "⚔ FIGHT!", ChatColor.GRAY + "Kill your opponent!", 5, 20, 5);
                p2.sendTitle(ChatColor.RED + "⚔ FIGHT!", ChatColor.GRAY + "Kill your opponent!", 5, 20, 5);
                p1.playSound(p1.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
                p2.playSound(p2.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
                cancel.run();
                return;
            }

            ChatColor countColor = count[0] <= 3 ? ChatColor.RED : ChatColor.YELLOW;
            p1.sendTitle(countColor + "" + count[0], ChatColor.GRAY + "Get ready...", 5, 15, 5);
            p2.sendTitle(countColor + "" + count[0], ChatColor.GRAY + "Get ready...", 5, 15, 5);
            p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            count[0]--;
        }, 20L, 20L);
    }

    /**
     * Handle a player death during a wager
     */
    public void handleDeath(Player dead) {
        UUID wagerId = playerWagerMap.get(dead.getUniqueId());
        if (wagerId == null) return;

        Wager wager = activeWagers.get(wagerId);
        if (wager == null || wager.getState() != Wager.WagerState.IN_PROGRESS) return;

        // Determine winner
        UUID winnerId = wager.getCreator().equals(dead.getUniqueId()) ? wager.getOpponent() : wager.getCreator();
        UUID loserId = dead.getUniqueId();

        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);

        wager.setState(Wager.WagerState.FINISHED);

        // Calculate winnings
        double totalPot = wager.getAmount() * 2;
        double tax = plugin.getEconomyManager().calculateTax(totalPot);
        double winnings = totalPot - tax;

        // Pay winner
        if (winner != null) {
            plugin.getEconomyManager().deposit(winner, winnings);
            winner.sendTitle(ChatColor.GREEN + "✔ YOU WON!",
                    ChatColor.GOLD + "+" + FormatUtil.formatMoney(winnings) + ChatColor.GRAY + " (" + FormatUtil.formatMoney(tax) + " tax)",
                    10, 60, 20);
            winner.sendMessage("");
            winner.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            winner.sendMessage(ChatColor.GREEN + "  ✔ WAGER WON!");
            winner.sendMessage(ChatColor.GRAY + "  Opponent: " + ChatColor.WHITE + (loser != null ? loser.getName() : "Unknown"));
            winner.sendMessage(ChatColor.GRAY + "  Pot: " + ChatColor.GOLD + FormatUtil.formatMoney(totalPot));
            winner.sendMessage(ChatColor.GRAY + "  Tax (" + plugin.getConfig().getDouble("tax-percent") + "%): " + ChatColor.RED + "-" + FormatUtil.formatMoney(tax));
            winner.sendMessage(ChatColor.GRAY + "  Winnings: " + ChatColor.GREEN + "+" + FormatUtil.formatMoney(winnings));
            winner.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            winner.sendMessage("");
            winner.playSound(winner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        if (loser != null) {
            loser.sendTitle(ChatColor.RED + "✘ YOU LOST!",
                    ChatColor.GRAY + "-" + FormatUtil.formatMoney(wager.getAmount()),
                    10, 60, 20);
            loser.sendMessage("");
            loser.sendMessage(ChatColor.RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            loser.sendMessage(ChatColor.RED + "  ✘ WAGER LOST!");
            loser.sendMessage(ChatColor.GRAY + "  Opponent: " + ChatColor.WHITE + (winner != null ? winner.getName() : "Unknown"));
            loser.sendMessage(ChatColor.GRAY + "  Amount Lost: " + ChatColor.RED + "-" + FormatUtil.formatMoney(wager.getAmount()));
            loser.sendMessage(ChatColor.RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            loser.sendMessage("");
            loser.playSound(loser.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);
        }

        // Delayed cleanup - send players back to lobby
        SchedulerUtil.runTaskLater(plugin, () -> endWager(wager), 60L); // 3 second delay
    }

    /**
     * End a wager and clean up
     */
    private void endWager(Wager wager) {
        // Free arena
        if (wager.getArenaId() != null) {
            Arena arena = plugin.getArenaManager().getArena(wager.getArenaId());
            if (arena != null) {
                arena.setInUse(false);
            }
        }

        // Restore and teleport players
        Player creator = Bukkit.getPlayer(wager.getCreator());
        Player opponent = Bukkit.getPlayer(wager.getOpponent());

        if (creator != null) {
            restorePlayerState(creator);
            teleportToLobby(creator);
        }
        if (opponent != null) {
            restorePlayerState(opponent);
            teleportToLobby(opponent);
        }

        // Clean up maps
        activeWagers.remove(wager.getId());
        playerWagerMap.remove(wager.getCreator());
        if (wager.getOpponent() != null) {
            playerWagerMap.remove(wager.getOpponent());
        }
    }

    /**
     * Cancel a wager and refund
     */
    public void cancelWager(UUID wagerId, String reason) {
        Wager wager = activeWagers.get(wagerId);
        if (wager == null) return;

        // Check if players were actually sent to an arena (past WAITING state)
        boolean wasInArena = wager.getArenaId() != null;

        wager.setState(Wager.WagerState.FINISHED);

        // Refund both players
        Player creator = Bukkit.getPlayer(wager.getCreator());
        if (creator != null) {
            plugin.getEconomyManager().deposit(creator, wager.getAmount());
            creator.sendMessage(ChatColor.RED + "Wager cancelled: " + reason + ChatColor.GRAY + " (" + FormatUtil.formatMoney(wager.getAmount()) + " refunded)");
            if (wasInArena) {
                restorePlayerState(creator);
                teleportToLobby(creator);
            }
        }

        if (wager.getOpponent() != null) {
            Player opponent = Bukkit.getPlayer(wager.getOpponent());
            if (opponent != null) {
                plugin.getEconomyManager().deposit(opponent, wager.getAmount());
                opponent.sendMessage(ChatColor.RED + "Wager cancelled: " + reason + ChatColor.GRAY + " (" + FormatUtil.formatMoney(wager.getAmount()) + " refunded)");
                if (wasInArena) {
                    restorePlayerState(opponent);
                    teleportToLobby(opponent);
                }
            }
        }

        // Free arena
        if (wasInArena) {
            Arena arena = plugin.getArenaManager().getArena(wager.getArenaId());
            if (arena != null) arena.setInUse(false);
        }

        activeWagers.remove(wagerId);
        playerWagerMap.remove(wager.getCreator());
        if (wager.getOpponent() != null) {
            playerWagerMap.remove(wager.getOpponent());
        }
    }

    /**
     * Player leaves wager voluntarily (counts as loss if in progress)
     */
    public boolean playerLeave(Player player) {
        UUID wagerId = playerWagerMap.get(player.getUniqueId());
        if (wagerId == null) return false;

        Wager wager = activeWagers.get(wagerId);
        if (wager == null) return false;

        if (wager.getState() == Wager.WagerState.WAITING) {
            // Just cancel and refund creator
            cancelWager(wagerId, "Creator left the queue");
            return true;
        }

        if (wager.getState() == Wager.WagerState.IN_PROGRESS || wager.getState() == Wager.WagerState.COUNTDOWN) {
            // Player forfeits - other player wins
            handleDeath(player);
            return true;
        }

        return false;
    }

    /**
     * Cancel a waiting wager
     */
    public boolean cancelWaitingWager(Player player) {
        UUID wagerId = playerWagerMap.get(player.getUniqueId());
        if (wagerId == null) return false;

        Wager wager = activeWagers.get(wagerId);
        if (wager == null || wager.getState() != Wager.WagerState.WAITING) return false;

        cancelWager(wagerId, "Cancelled by creator");
        return true;
    }

    private void savePlayerState(Player player) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());
        savedLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    private void preparePlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Give basic kit
        player.getInventory().addItem(new ItemStack(org.bukkit.Material.DIAMOND_SWORD));
        player.getInventory().addItem(new ItemStack(org.bukkit.Material.BOW));
        player.getInventory().addItem(new ItemStack(org.bukkit.Material.ARROW, 16));
        player.getInventory().addItem(new ItemStack(org.bukkit.Material.GOLDEN_APPLE, 3));
        player.getInventory().setHelmet(new ItemStack(org.bukkit.Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(org.bukkit.Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(org.bukkit.Material.DIAMOND_BOOTS));
    }

    private void restorePlayerState(Player player) {
        ItemStack[] inv = savedInventories.remove(player.getUniqueId());
        ItemStack[] armor = savedArmor.remove(player.getUniqueId());

        player.getInventory().clear();
        if (inv != null) player.getInventory().setContents(inv);
        if (armor != null) player.getInventory().setArmorContents(armor);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setWalkSpeed(0.2f);
        player.setFireTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void teleportToLobby(Player player) {
        Location lobby = plugin.getArenaManager().getLobbyLocation();
        if (lobby != null) {
            SchedulerUtil.teleportPlayer(plugin, player, lobby);
        }
    }

    public void cancelAllWagers() {
        for (UUID wagerId : new ArrayList<>(activeWagers.keySet())) {
            cancelWager(wagerId, "Server shutting down");
        }
    }

    public boolean isInWager(UUID playerId) {
        return playerWagerMap.containsKey(playerId);
    }

    public Wager getPlayerWager(UUID playerId) {
        UUID wagerId = playerWagerMap.get(playerId);
        if (wagerId == null) return null;
        return activeWagers.get(wagerId);
    }

    public List<Wager> getWaitingWagers() {
        List<Wager> waiting = new ArrayList<>();
        for (Wager wager : activeWagers.values()) {
            if (wager.getState() == Wager.WagerState.WAITING) {
                waiting.add(wager);
            }
        }
        waiting.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return waiting;
    }

    public Collection<Wager> getAllActiveWagers() {
        return Collections.unmodifiableCollection(activeWagers.values());
    }
}
