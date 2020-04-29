package codes.zucker.realismdrops;

import org.bukkit.Bukkit;

public class Utils {
    public static void CreateRepeatingTask(Runnable r, long delay, long period, int repeats) {
        int timer = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(Main.class), r, delay, period);
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(Main.class), new Runnable(){
            @Override
            public void run() {
                Bukkit.getScheduler().cancelTask(timer);
            }
        }, delay + (period * repeats));
    }

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}