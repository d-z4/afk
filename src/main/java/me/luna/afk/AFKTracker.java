package me.luna.afk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AFKTracker implements Listener {
    private final Map<UUID, Long> accumulatedTime = new HashMap<>();
    private final Map<UUID, Long> areaEntryTime = new HashMap<>();
    private final Set<UUID> playersInArea = new HashSet<>();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        updateAccumulatedTime(uuid);
        playersInArea.remove(uuid);
        areaEntryTime.remove(uuid);
    }

    private void updateAccumulatedTime(UUID uuid) {
        Long entryTime = areaEntryTime.get(uuid);
        if (entryTime != null) {
            long timeInArea = System.currentTimeMillis() - entryTime;
            accumulatedTime.put(uuid, accumulatedTime.getOrDefault(uuid, 0L) + timeInArea);
        }
    }

    public boolean shouldRunCommand(Player player, long intervalMillis) {
        return getAccumulatedTime(player) >= intervalMillis;
    }

    public void markCommandExecuted(Player player) {
        UUID uuid = player.getUniqueId();
        accumulatedTime.put(uuid, 0L);
        areaEntryTime.put(uuid, System.currentTimeMillis());
    }

    public long getAccumulatedTime(Player player) {
        UUID uuid = player.getUniqueId();
        long accumulated = accumulatedTime.getOrDefault(uuid, 0L);
        Long entryTime = areaEntryTime.get(uuid);
        if (entryTime != null) {
            accumulated += System.currentTimeMillis() - entryTime;
        }
        return accumulated;
    }

    public void sendActionBar(Player player, long remainingMillis, long totalMillis, String titleFormat, 
                               int barLength, String filledChar, String emptyChar, String filledColor, String emptyColor) {
        UUID uuid = player.getUniqueId();
        playersInArea.add(uuid);
        
        if (!areaEntryTime.containsKey(uuid)) {
            areaEntryTime.put(uuid, System.currentTimeMillis());
        }
        
        long seconds = remainingMillis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        String timeFormatted = String.format("%dm %ds", minutes, seconds);
        
        double progress = 1 - Math.max(0.0, Math.min(1.0, (double) remainingMillis / totalMillis));
        int percentage = (int) (progress * 100);
        
        int filledBars = (int) (progress * barLength);
        
        StringBuilder bar = new StringBuilder(filledColor);
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append(filledChar);
            } else {
                bar.append(emptyColor).append(emptyChar);
            }
        }
        bar.append("§r");
        String message = titleFormat
            .replace("{time}", timeFormatted)
            .replace("{bar}", bar.toString())
            .replace("{percentage}", String.valueOf(percentage))
            .replace("&", "§");
        
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void removeFromArea(Player player) {
        UUID uuid = player.getUniqueId();
        updateAccumulatedTime(uuid);
        playersInArea.remove(uuid);
        areaEntryTime.remove(uuid);
    }

    public int getPlayersInArea() {
        return playersInArea.size();
    }

    public Map<Player, Long> getPlayersWithTime() {
        Map<Player, Long> result = new LinkedHashMap<>();
        for (UUID uuid : playersInArea) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                long accumulated = getAccumulatedTime(player);
                result.put(player, accumulated);
            }
        }
        return result;
    }

    public void saveData(File file) {
        for (UUID uuid : new HashSet<>(areaEntryTime.keySet())) {
            updateAccumulatedTime(uuid);
            areaEntryTime.remove(uuid);
        }
        
        Map<String, Long> saveMap = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : accumulatedTime.entrySet()) {
            saveMap.put(entry.getKey().toString(), entry.getValue());
        }
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(saveMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData(File file) {
        if (!file.exists()) return;
        
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Long>>(){}.getType();
            Map<String, Long> loadMap = gson.fromJson(reader, type);
            
            if (loadMap != null) {
                for (Map.Entry<String, Long> entry : loadMap.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    accumulatedTime.put(uuid, entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearData() {
        playersInArea.clear();
        areaEntryTime.clear();
        accumulatedTime.clear();
    }
}
