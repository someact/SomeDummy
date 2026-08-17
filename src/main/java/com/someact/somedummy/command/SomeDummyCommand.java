package com.someact.somedummy.command;

import com.someact.somedummy.SomeDummyPlugin;
import com.someact.somedummy.config.ConfigManager;
import com.someact.somedummy.gui.AdminConfigGUI;
import com.someact.somedummy.gui.AdminDummyManagerGUI;
import com.someact.somedummy.gui.PlayerDummyListGUI;
import com.someact.somedummy.gui.PresetLibraryGUI;
import com.someact.somedummy.model.DummyData;
import com.someact.somedummy.util.MessageUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Modern Paper BasicCommand implementation for /somedummy, /sd, and /dummy.
 */
public class SomeDummyCommand implements BasicCommand, CommandExecutor, TabCompleter {

    private final SomeDummyPlugin plugin;

    public SomeDummyCommand(SomeDummyPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigManager config() {
        return plugin.getConfigManager();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        processCommand(stack.getSender(), args);
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        return processTabComplete(stack.getSender(), args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        processCommand(sender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return processTabComplete(sender, args);
    }

    private void processCommand(CommandSender sender, String[] args) {
        ConfigManager cfg = config();
        if (cfg == null) {
            sender.sendMessage("SomeDummy plugin is initializing...");
            return;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                new PlayerDummyListGUI(plugin, player).open();
            } else {
                showHelp(sender);
            }
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somedummy.spawn")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }

                DummyData dummy = plugin.getEntityManager().spawnDummy(player, player.getLocation());
                if (dummy != null && args.length >= 2) {
                    try {
                        EntityType type = EntityType.valueOf(args[1].toUpperCase());
                        dummy.setEntityType(type);
                        plugin.getStorageManager().saveDummyAsync(dummy);
                        Bukkit.getRegionScheduler().run(plugin, dummy.getLocation(), t -> {
                            plugin.getEntityManager().spawnDummyEntity(dummy);
                        });
                    } catch (Exception ignored) {}
                }
            }
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                new PlayerDummyListGUI(plugin, player).open();
            }
            case "presets" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somedummy.preset")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                new PresetLibraryGUI(plugin, player).open();
            }
            case "givepreset" -> {
                if (!sender.hasPermission("somedummy.admin")) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + "<yellow>Usage: /sd givepreset <player> <preset_id> [amount]</yellow>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + "<red>Player not found.</red>");
                    return;
                }
                String presetId = args[2].toLowerCase();
                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                }

                ItemStack item = plugin.getPresetManager().createPresetItem(presetId, amount);
                if (item != null) {
                    target.getInventory().addItem(item);
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + "<green>Gave " + amount + "x preset dummy (" + presetId + ") to " + target.getName() + ".</green>");
                    MessageUtil.sendMessage(target, cfg.getPrefix() + "<green>You received " + amount + "x preset dummy (" + presetId + ")!</green>");
                } else {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + "<red>Preset ID '" + presetId + "' not found.</red>");
                }
            }
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somedummy.admin")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                new AdminDummyManagerGUI(plugin, player).open();
            }
            case "config" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return;
                }
                if (!player.hasPermission("somedummy.admin")) {
                    MessageUtil.sendMessage(player, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                new AdminConfigGUI(plugin, player).open();
            }
            case "reload" -> {
                if (!sender.hasPermission("somedummy.admin")) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                cfg.load();
                plugin.getPresetManager().loadAllPresets();
                MessageUtil.sendMessage(sender, cfg.getPrefix() + cfg.getMessage("config-reloaded",
                        "<green>SomeDummy configuration reloaded successfully!</green>"));
            }
            case "purge" -> {
                if (!sender.hasPermission("somedummy.admin")) {
                    MessageUtil.sendMessage(sender, cfg.getPrefix() + cfg.getMessage("no-permission",
                            "<red>You do not have permission to execute this command.</red>"));
                    return;
                }
                for (DummyData d : new ArrayList<>(plugin.getStorageManager().getAllDummies())) {
                    plugin.getEntityManager().removeDummy(d);
                }
                MessageUtil.sendMessage(sender, cfg.getPrefix() + "<red>Purged all target dummies from the server.</red>");
            }
            case "help" -> {
                showHelp(sender);
            }
            default -> {
                if (sender instanceof Player player) {
                    new PlayerDummyListGUI(plugin, player).open();
                } else {
                    showHelp(sender);
                }
            }
        }
    }

    private void showHelp(CommandSender sender) {
        boolean isAdmin = sender.hasPermission("somedummy.admin");

        sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"));
        sender.sendMessage(MessageUtil.parse("           <gradient:#ff7675:#fab1a0><bold>SomeDummy Command Guide</bold></gradient>"));
        sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"));

        sender.sendMessage(MessageUtil.parse("<gold><bold>/sd</bold></gold> <dark_gray>-</dark_gray> <white>Opens your active dummy management list.</white>"));
        sender.sendMessage(MessageUtil.parse("<gold><bold>/sd spawn [mob]</bold></gold> <dark_gray>-</dark_gray> <white>Spawns a target dummy at your feet.</white>"));
        sender.sendMessage(MessageUtil.parse("<gold><bold>/sd list</bold></gold> <dark_gray>-</dark_gray> <white>Opens the My Dummies menu.</white>"));
        sender.sendMessage(MessageUtil.parse("<gold><bold>/sd presets</bold></gold> <dark_gray>-</dark_gray> <white>Opens preset dummy templates library.</white>"));
        sender.sendMessage(MessageUtil.parse("<gold><bold>/sd help</bold></gold> <dark_gray>-</dark_gray> <white>Displays this help menu.</white>"));

        if (isAdmin) {
            sender.sendMessage(MessageUtil.parse(""));
            sender.sendMessage(MessageUtil.parse("<yellow><bold>Administrator Commands:</bold></yellow>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sd admin</bold></aqua> <dark_gray>-</dark_gray> <white>Server-wide dummy manager dashboard.</white>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sd config</bold></aqua> <dark_gray>-</dark_gray> <white>In-game plugin settings control panel.</white>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sd givepreset <player> <id></bold></aqua> <dark_gray>-</dark_gray> <white>Give placeable preset figurine items.</white>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sd reload</bold></aqua> <dark_gray>-</dark_gray> <white>Reload configuration and preset templates.</white>"));
            sender.sendMessage(MessageUtil.parse("<aqua><bold>/sd purge</bold></aqua> <dark_gray>-</dark_gray> <white>Delete all target dummies across all worlds.</white>"));
        }
        sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</dark_gray>"));
    }

    private List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "spawn", "list", "presets"));
            if (sender.hasPermission("somedummy.admin")) {
                subs.addAll(List.of("admin", "config", "givepreset", "reload", "purge"));
            }
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("spawn")) {
                for (EntityType type : EntityType.values()) {
                    if (type.isAlive() && type.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(type.name());
                    }
                }
            } else if (sub.equals("givepreset")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("givepreset")) {
            for (String id : plugin.getPresetManager().getAllPresets().keySet()) {
                if (id.startsWith(args[2].toLowerCase())) {
                    completions.add(id);
                }
            }
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("givepreset")) {
            completions.addAll(List.of("1", "4", "16", "64"));
            return completions;
        }

        return completions;
    }
}
