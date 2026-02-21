package com.wager.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

/**
 * Utility class that transparently routes scheduling calls to either
 * Bukkit's scheduler (Spigot/Paper) or Folia's region-aware schedulers.
 */
public class SchedulerUtil {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Run a task on the main/global-region thread.
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a delayed task on the main/global-region thread.
     */
    public static void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler()
                    .runDelayed(plugin, t -> task.run(), Math.max(1, delayTicks));
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Run a repeating task. The consumer receives a {@link Runnable} that, when called,
     * cancels the task — use it instead of {@code BukkitRunnable#cancel()}.
     */
    public static void runTaskTimer(Plugin plugin, Consumer<Runnable> task,
                                    long initialDelay, long period) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, t -> task.accept(t::cancel),
                            Math.max(1, initialDelay), period);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.accept(this::cancel);
                }
            }.runTaskTimer(plugin, initialDelay, period);
        }
    }

    /**
     * Run a task asynchronously.
     */
    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Run a task on the entity's owning thread (Folia) or the main thread (Bukkit).
     */
    public static void runEntityTask(Plugin plugin, Entity entity, Runnable task) {
        if (FOLIA) {
            entity.getScheduler().run(plugin, t -> task.run(), null);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Teleport a player in a thread-safe way on both Bukkit and Folia.
     */
    public static void teleportPlayer(Plugin plugin, Player player, Location location) {
        if (FOLIA) {
            player.getScheduler().run(plugin, t -> player.teleport(location), null);
        } else {
            player.teleport(location);
        }
    }
}
