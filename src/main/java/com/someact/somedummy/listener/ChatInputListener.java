package com.someact.somedummy.listener;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.util.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Captures player chat input for interactive Dummy editing.
 */
public class ChatInputListener implements Listener {

    private final SomeDummyPlugin plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputListener(SomeDummyPlugin plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
    }

    public void cancelInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());

        if (callback != null) {
            event.setCancelled(true);
            String serialized = MessageUtil.miniMessage().serialize(event.message());
            String rawText = MessageUtil.miniMessage().stripTags(serialized).trim();

            player.getScheduler().run(plugin, task -> {
                callback.accept(rawText);
            }, null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }
}
