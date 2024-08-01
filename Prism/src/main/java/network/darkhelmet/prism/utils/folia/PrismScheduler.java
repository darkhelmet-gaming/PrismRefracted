package network.darkhelmet.prism.utils.folia;

import network.darkhelmet.prism.Prism;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.concurrent.TimeUnit;

public class PrismScheduler {

    private static final Prism PLUGIN = Prism.getInstance();
    private static final boolean FOLIA = Prism.isFolia;

    public static PrismTask runTaskTimerAsynchronously(Runnable runnable, long delay, long period) {
        if (FOLIA) {
            return new PrismTask(Bukkit.getServer().getAsyncScheduler().runAtFixedRate(PLUGIN, val -> runnable.run(), delay * 50, period * 50, TimeUnit.MILLISECONDS));
        } else {
            return new PrismTask(Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, runnable, delay, period));
        }
    }
    public static PrismTask runTaskLaterAsynchronously(Runnable runnable, long delay) {
        if (FOLIA) {
            return new PrismTask(Bukkit.getServer().getAsyncScheduler().runDelayed(PLUGIN, val -> runnable.run(), delay * 50, TimeUnit.MILLISECONDS));
        } else {
            return new PrismTask(Bukkit.getScheduler().runTaskLaterAsynchronously(PLUGIN, runnable, delay));
        }
    }

    public static PrismTask runTaskAsynchronously(Runnable runnable) {
        if (FOLIA) {
            return new PrismTask(Bukkit.getServer().getAsyncScheduler().runNow(PLUGIN, val -> runnable.run()));
        } else {
            return new PrismTask(Bukkit.getScheduler().runTaskAsynchronously(PLUGIN, runnable));
        }
    }

    public static void runTask(Runnable runnable) {
        if (FOLIA) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(PLUGIN, runnable);
        }
    }

    public static void run(Runnable runnable, Location location) {
        if (FOLIA) {
            Bukkit.getServer().getRegionScheduler().execute(PLUGIN, location, runnable);
        } else {
            runnable.run();
        }
    }

    public static void runTaskLater(Runnable runnable, Location location, long delay) {
        if (FOLIA) {
            Bukkit.getServer().getRegionScheduler().runDelayed(PLUGIN, location, val -> runnable.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(PLUGIN, runnable, delay);
        }
    }

    public static PrismTask scheduleSyncRepeatingTask(Runnable runnable, long delay, long period) {
        if (FOLIA) {
            return new PrismTask(Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(PLUGIN, val -> runnable.run(), delay, period));
        } else {
            return new PrismTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, runnable, delay, period));
        }
    }

    public static PrismTask scheduleSyncRepeatingTask(Runnable runnable, Location location, long delay, long period) {
        if (FOLIA) {
            return new PrismTask(Bukkit.getServer().getRegionScheduler().runAtFixedRate(PLUGIN, location, val -> runnable.run(), delay, period));
        } else {
            return new PrismTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, runnable, delay, period));
        }
    }

    public static void cancelTasks() {
        if (FOLIA) {
            Bukkit.getServer().getGlobalRegionScheduler().cancelTasks(PLUGIN);
            Bukkit.getServer().getAsyncScheduler().cancelTasks(PLUGIN);
        } else {
            Bukkit.getScheduler().cancelTasks(PLUGIN);
        }
    }

}
