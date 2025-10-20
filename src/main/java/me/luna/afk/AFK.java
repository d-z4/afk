package me.luna.afk;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public final class AFK extends JavaPlugin {
    private AFKTracker tracker;
    private BukkitTask updateTask;
    
    private long intervalMillis;
    private String actionBarTitle;
    private int barLength;
    private String filledChar;
    private String emptyChar;
    private String filledColor;
    private String emptyColor;
    private String worldName;
    private double x1, y1, z1, x2, y2, z2;
    private String command;

    public long getIntervalMillis() {
        return intervalMillis;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        
        tracker = new AFKTracker();
        tracker.loadData(new File(getDataFolder(), "data.json"));
        getServer().getPluginManager().registerEvents(tracker, this);
        
        AFKCommand afkCommand = new AFKCommand(this, tracker);
        getCommand("afk").setExecutor(afkCommand);
        getCommand("afk").setTabCompleter(afkCommand);
        
        updateTask = Bukkit.getScheduler().runTaskTimer(this, this::updatePlayers, 20L, 20L);
    }
    
    public void loadConfigValues() {
        intervalMillis = getConfig().getLong("interval-minutes") * 60 * 1000;
        actionBarTitle = getConfig().getString("actionbar.title");
        barLength = getConfig().getInt("bar.length");
        filledChar = getConfig().getString("bar.filled-character");
        emptyChar = getConfig().getString("bar.empty-character");
        filledColor = getConfig().getString("bar.filled-color");
        emptyColor = getConfig().getString("bar.empty-color");
        worldName = getConfig().getString("area.world");
        x1 = getConfig().getDouble("area.pos1.x");
        y1 = getConfig().getDouble("area.pos1.y");
        z1 = getConfig().getDouble("area.pos1.z");
        x2 = getConfig().getDouble("area.pos2.x");
        y2 = getConfig().getDouble("area.pos2.y");
        z2 = getConfig().getDouble("area.pos2.z");
        command = getConfig().getString("command");
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (tracker != null) {
            tracker.saveData(new File(getDataFolder(), "data.json"));
            tracker.clearData();
        }
    }

    
    private void updatePlayers() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInArea(player.getLocation(), world, x1, y1, z1, x2, y2, z2)) {
                long accumulated = tracker.getAccumulatedTime(player);
                long remainingMillis = intervalMillis - accumulated;
                
                if (remainingMillis > 0) {
                    tracker.sendActionBar(player, remainingMillis, intervalMillis, actionBarTitle, 
                                        barLength, filledChar, emptyChar, filledColor, emptyColor);
                } else if (tracker.shouldRunCommand(player, intervalMillis)) {
                    String finalCommand = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    tracker.markCommandExecuted(player);
                    tracker.removeFromArea(player);
                }
            } else {
                tracker.removeFromArea(player);
            }
        }
    }
    
    private boolean isInArea(Location loc, World world, double x1, double y1, double z1, double x2, double y2, double z2) {
        if (!loc.getWorld().equals(world)) {
            return false;
        }
        
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxZ = Math.max(z1, z2);
        
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
