package com.someact.somedummy;

import com.someact.somedummy.api.SomeDummyAPI;
import com.someact.somedummy.command.SomeDummyCommand;
import com.someact.somedummy.config.ConfigManager;
import com.someact.somedummy.dummy.DummyEntityManager;
import com.someact.somedummy.listener.ChatInputListener;
import com.someact.somedummy.listener.DummyDamageListener;
import com.someact.somedummy.listener.DummyInteractListener;
import com.someact.somedummy.listener.InventoryClickListener;
import com.someact.somedummy.preset.PresetManager;
import com.someact.somedummy.sound.SoundManager;
import com.someact.somedummy.storage.DummyStorageManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for SomeDummy on PaperMC 26.2.
 */
public class SomeDummyPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SoundManager soundManager;
    private DummyStorageManager storageManager;
    private PresetManager presetManager;
    private DummyEntityManager entityManager;
    private ChatInputListener chatInputListener;

    @Override
    public void onLoad() {
        try {
            getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                SomeDummyCommand cmd = new SomeDummyCommand(this);
                event.registrar().register("somedummy", "SomeDummy main command", List.of("sd", "dummy"), cmd);
            });
        } catch (Exception e) {
            getLogger().warning("Could not register commands via LifecycleEvents: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // 1. Initialize API
        SomeDummyAPI.setPlugin(this);

        // 2. Configuration & Audio
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.soundManager = new SoundManager(this);

        // 3. Storage & Presets
        this.storageManager = new DummyStorageManager(this);
        this.storageManager.init();

        this.presetManager = new PresetManager(this);
        this.presetManager.init();

        // 4. Entity Manager & Visuals
        this.entityManager = new DummyEntityManager(this);
        this.entityManager.start();

        // 5. Listeners
        this.chatInputListener = new ChatInputListener(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new DummyInteractListener(this), this);
        pm.registerEvents(new DummyDamageListener(this), this);
        pm.registerEvents(new InventoryClickListener(), this);
        pm.registerEvents(this.chatInputListener, this);

        if (!configManager.isSilentStartup()) {
            Bukkit.getConsoleSender().sendMessage(com.someact.somedummy.util.MessageUtil.parse(
                    "<dark_gray>[<gradient:#ff7675:#fab1a0><bold>SomeDummy</bold></gradient>]</dark_gray> <gradient:#e67e22:#f39c12>==================================================</gradient>\n" +
                    "  <gradient:#ff7675:#fab1a0><bold>SomeDummy</bold></gradient> <dark_gray>v" + getPluginMeta().getVersion() + "</dark_gray>\n" +
                    "  <gray>Author:</gray> <gold>someact</gold>\n" +
                    "  <gray>Platform:</gray> <yellow>Paper " + Bukkit.getMinecraftVersion() + "</yellow>\n" +
                    "  <gray>Status:</gray> <green>Operational</green> <dark_gray>(" + (System.currentTimeMillis() - startTime) + "ms)</dark_gray>\n" +
                    "<dark_gray>[<gradient:#ff7675:#fab1a0><bold>SomeDummy</bold></gradient>]</dark_gray> <gradient:#e67e22:#f39c12>==================================================</gradient>"
            ));
        } else {
            getLogger().info("SomeDummy plugin enabled successfully in " + (System.currentTimeMillis() - startTime) + "ms!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling SomeDummy plugin...");

        if (entityManager != null) {
            entityManager.stop();
        }

        if (storageManager != null) {
            storageManager.saveAllSync();
        }

        getLogger().info("SomeDummy plugin disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public DummyStorageManager getStorageManager() {
        return storageManager;
    }

    public PresetManager getPresetManager() {
        return presetManager;
    }

    public DummyEntityManager getEntityManager() {
        return entityManager;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }
}
