package org.shotrush.atom.core.ui;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@AutoRegisterSystem(priority = 0)
public class ActionBarManager {
    
    @Getter
    private static ActionBarManager instance;
    private final Plugin plugin;
    private final Map<UUID, Map<String, String>> playerMessages = new ConcurrentHashMap<>();
    
    public ActionBarManager(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
        startActionBarTick();
    }

    public void setMessage(Player player, String key, String message) {
        playerMessages.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
            .put(key, message);
    }
    
    public void removeMessage(Player player, String key) {
        Map<String, String> messages = playerMessages.get(player.getUniqueId());
        if (messages != null) {
            messages.remove(key);
        }
    }
    
    public void clearMessages(Player player) {
        playerMessages.remove(player.getUniqueId());
    }
    
    public static void send(Player player, String message) {
        send(player, message, 3); 
    }
    
    public static void send(Player player, String message, int durationSeconds) {
        if (instance == null) return;
        
        String tempKey = "temp_" + System.currentTimeMillis() + "_" + message.hashCode();
        instance.setMessage(player, tempKey, message);
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTaskLater(player.getLocation(), () -> {
            instance.removeMessage(player, tempKey);
        }, durationSeconds * 20L);
    }
    
    public static void sendStatus(Player player, String message) {
        if (instance == null) return;
        
        instance.setMessage(player, "status", message);
    }
    
    public static void clearStatus(Player player) {
        if (instance == null) return;
        instance.removeMessage(player, "status");
    }
    
    private void startActionBarTick() {
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runGlobalTaskTimer(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                updateActionBar(player);
            }
        }, 1L, 20L);
    }
    
    private void updateActionBar(Player player) {
        Map<String, String> messages = playerMessages.get(player.getUniqueId());
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        List<String> orderedMessages = new ArrayList<>();

        if (messages.containsKey("status")) {
            orderedMessages.add(messages.get("status"));
        }

        Set<String> uniqueTempMessages = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            if (entry.getKey().startsWith("temp_")) {
                uniqueTempMessages.add(entry.getValue());
            }
        }
        
        
        if (!uniqueTempMessages.isEmpty()) {
            if (uniqueTempMessages.size() == 1) {
                orderedMessages.add(uniqueTempMessages.iterator().next());
            } else {
                
                orderedMessages.add(String.join(" §8• ", uniqueTempMessages));
            }
        }
        
        
        if (messages.containsKey("body_temp")) {
            orderedMessages.add(messages.get("body_temp"));
        }
        if (messages.containsKey("item_heat")) {
            orderedMessages.add(messages.get("item_heat"));
        }
        if (messages.containsKey("thirst")) {
            orderedMessages.add(messages.get("thirst"));
        }
        
        
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("body_temp") && !key.equals("item_heat") && !key.equals("thirst") 
                && !key.equals("status") && !key.startsWith("temp_")) {
                orderedMessages.add(entry.getValue());
            }
        }
        
        if (!orderedMessages.isEmpty()) {
            String combined = String.join(" §8| ", orderedMessages);
            player.sendActionBar(Component.text(combined));
        }
    }
}
