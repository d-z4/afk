package me.luna.afk;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AFKCommand implements CommandExecutor, TabCompleter {
    private final AFK plugin;
    private final AFKTracker tracker;

    public AFKCommand(AFK plugin, AFKTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: /afk <reload|list>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                plugin.loadConfigValues();
                sender.sendMessage("§aafk config reloaded :3");
                return true;

            case "list":
                Map<Player, Long> playersData = tracker.getPlayersWithTime();
                
                long intervalMillis = plugin.getIntervalMillis();
                sender.sendMessage("§b§lPlayers in AFK area[" + tracker.getPlayersInArea() + "]:");
                
                for (Map.Entry<Player, Long> entry : playersData.entrySet()) {
                    Player p = entry.getKey();
                    long timeSince = entry.getValue();
                    long remaining = intervalMillis - timeSince;
                    
                    long seconds = remaining / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    sender.sendMessage("§b" + p.getName() + "§r: " + minutes + "m " + seconds + "s");
        
                }
                return true;

            default:
                sender.sendMessage("§7Usage: /afk <reload|list>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            
            if ("reload".startsWith(input)) completions.add("reload");
            if ("list".startsWith(input)) completions.add("list");
            
            return completions;
        }
        
        return Collections.emptyList();
    }
}
