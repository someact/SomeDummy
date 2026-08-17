package com.someact.somedummy.config;

import com.someact.somedummy.SomeDummyPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Enterprise configuration manager for SomeDummy with full comment preservation on disk save.
 */
public class ConfigManager {

    private final SomeDummyPlugin plugin;
    private final File configFile;
    private final Map<String, Object> values = new LinkedHashMap<>();

    public ConfigManager(SomeDummyPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "setting.conf");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        values.clear();

        // 1. Load defaults from JAR
        Map<String, Object> defaults = new LinkedHashMap<>();
        try (InputStream in = plugin.getResource("setting.conf")) {
            if (in != null) parseStream(in, defaults);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not parse default config stream: " + e.getMessage());
        }

        // 2. Load disk values
        Map<String, Object> fileValues = new LinkedHashMap<>();
        try (InputStream in = new FileInputStream(configFile)) {
            parseStream(in, fileValues);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load setting.conf: " + e.getMessage());
        }

        // 3. Merge
        values.putAll(defaults);
        values.putAll(fileValues);
    }

    private void saveDefaultConfig() {
        try (InputStream in = plugin.getResource("setting.conf")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save default setting.conf: " + e.getMessage());
        }
    }

    private void parseStream(InputStream in, Map<String, Object> targetMap) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String l;
            while ((l = reader.readLine()) != null) lines.add(l);
            parseLines(lines, targetMap);
        }
    }

    private void parseLines(List<String> lines, Map<String, Object> targetMap) {
        Deque<String> sectionStack = new ArrayDeque<>();

        for (int i = 0; i < lines.size(); i++) {
            String rawLine = lines.get(i).trim();
            if (rawLine.isEmpty() || rawLine.startsWith("#")) continue;

            String line = stripInlineComment(rawLine);
            if (line.isEmpty()) continue;

            if (line.endsWith("{")) {
                String sectionName = line.substring(0, line.length() - 1).trim();
                sectionStack.addLast(sectionName);
                continue;
            }

            if (line.equals("}")) {
                if (!sectionStack.isEmpty()) sectionStack.removeLast();
                continue;
            }

            if (line.contains("=")) {
                int eqIdx = line.indexOf('=');
                String keyPart = line.substring(0, eqIdx).trim();
                String valPart = line.substring(eqIdx + 1).trim();
                String fullKey = buildKey(sectionStack, keyPart);

                targetMap.put(fullKey, parseScalar(valPart));
            }
        }
    }

    private String stripInlineComment(String text) {
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            if (c == '#' && !inQuotes) break;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private String buildKey(Deque<String> stack, String subKey) {
        if (stack.isEmpty()) return subKey;
        return String.join(".", stack) + "." + subKey;
    }

    private Object parseScalar(String val) {
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            return val.substring(1, val.length() - 1);
        }
        if (val.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (val.equalsIgnoreCase("false")) return Boolean.FALSE;
        try {
            if (val.contains(".")) return Double.parseDouble(val);
            return Long.parseLong(val);
        } catch (NumberFormatException ignored) {
            return val;
        }
    }

    public synchronized void save() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            writer.println("# ==============================================================================");
            writer.println("#                     SOMEDUMMY ENTERPRISE CONFIGURATION");
            writer.println("# ==============================================================================");
            writer.println("# All text strings support Kyori Adventure MiniMessage formatting:");
            writer.println("# https://docs.advntr.dev/minimessage/format.html");
            writer.println("# Example: <gradient:#ff7675:#fab1a0>Text</gradient>, <gold><b>Bold Gold</b></gold>");
            writer.println("# Unicode glyphs and Resource Pack custom fonts are fully supported!");
            writer.println();

            writer.println("general {");
            writer.println("  # Silent startup in server console: if false, prints the SomeDummy banner on startup");
            writer.println("  silent-startup = " + isSilentStartup());
            writer.println();
            writer.println("  # Optional custom font namespace for GUI titles and text (leave empty for default)");
            writer.println("  # Example: \"minecraft:uniform\" or \"somedummy:gui\"");
            writer.println("  custom-ui-font = \"" + getCustomUiFont() + "\"");
            writer.println();
            writer.println("  # Maximum number of target dummies a single player can have spawned simultaneously (0 for unlimited)");
            writer.println("  max-dummies-per-player = " + getMaxDummiesPerPlayer());
            writer.println("}");
            writer.println();

            writer.println("wand {");
            writer.println("  # Material of the dummy wand tool used to open the Dummy Editor GUI");
            writer.println("  material = \"" + getWandMaterial().name() + "\"");
            writer.println();
            writer.println("  # Require player to sneak (Shift) while right-clicking the dummy with the wand");
            writer.println("  require-sneak = " + isWandRequireSneak());
            writer.println("}");
            writer.println();

            writer.println("defaults {");
            writer.println("  # Default entity type for newly spawned dummies (ZOMBIE, SKELETON, ARMOR_STAND, WARDEN, etc.)");
            writer.println("  entity-type = \"" + getDefaultEntityType().name() + "\"");
            writer.println();
            writer.println("  # Default display name template (<owner_name> placeholder is available)");
            writer.println("  custom-name = \"" + getDefaultCustomName().replace("\"", "\\\"") + "\"");
            writer.println();
            writer.println("  # Default max health (hearts * 2)");
            writer.println("  max-health = " + getDefaultMaxHealth());
            writer.println();
            writer.println("  # Infinite Health mode (Dummy takes damage and tracks DPS, but never dies)");
            writer.println("  infinite-health = " + isDefaultInfiniteHealth());
            writer.println();
            writer.println("  # AI enabled (false = completely frozen in place, true = moves and acts like vanilla mob)");
            writer.println("  has-ai = " + isDefaultHasAi());
            writer.println();
            writer.println("  # Physics & Gravity (true = falls with gravity, false = floats in mid-air)");
            writer.println("  has-gravity = " + isDefaultHasGravity());
            writer.println();
            writer.println("  # Silent mode (mutes ambient groans/sounds)");
            writer.println("  is-silent = " + isDefaultSilent());
            writer.println();
            writer.println("  # Auto-respawn at origin on defeat");
            writer.println("  respawnable = " + isDefaultRespawnable());
            writer.println();
            writer.println("  # Delay in seconds before respawning");
            writer.println("  respawn-delay-seconds = " + getDefaultRespawnDelay());
            writer.println();
            writer.println("  # Lifespan duration in seconds (0 for permanent until removed)");
            writer.println("  lifespan-seconds = " + getDefaultLifespan());
            writer.println();
            writer.println("  # Allow undead mobs (Zombies, Skeletons, etc.) to catch fire under direct sunlight");
            writer.println("  burn-in-sun = " + isDefaultBurnInSun());
            writer.println();
            writer.println("  # Static Hard Freeze mode (completely locked in 3D space - 0 gravity, 0 knockback, immobile statue)");
            writer.println("  is-static = " + isDefaultStatic());
            writer.println("}");
            writer.println();

            writer.println("damage-indicator {");
            writer.println("  # Master toggle for damage indicators and DPS tracking");
            writer.println("  enabled = " + isDamageIndicatorEnabled());
            writer.println();
            writer.println("  # Spawn floating pop-up text displays above the dummy on hit");
            writer.println("  show-floating-popups = " + isShowFloatingPopups());
            writer.println();
            writer.println("  # Duration in server ticks for the floating pop-up before fading out (20 ticks = 1 second)");
            writer.println("  popup-duration-ticks = " + getPopupDurationTicks());
            writer.println();
            writer.println("  # Max view distance in blocks for players to see the floating damage pop-ups");
            writer.println("  popup-view-distance-blocks = " + getPopupViewDistanceBlocks());
            writer.println();
            writer.println("  # Display real-time DPS stats on the player's Actionbar when attacking");
            writer.println("  show-actionbar-dps = " + isShowActionbarDps());
            writer.println();
            writer.println("  # Text format for normal floating hits");
            writer.println("  normal-hit-format = \"" + getNormalHitFormat().replace("\"", "\\\"") + "\"");
            writer.println();
            writer.println("  # Text format for critical floating hits");
            writer.println("  crit-hit-format = \"" + getCritHitFormat().replace("\"", "\\\"") + "\"");
            writer.println();
            writer.println("  # Format for the live Actionbar DPS summary");
            writer.println("  actionbar-format = \"" + getActionbarFormat().replace("\"", "\\\"") + "\"");
            writer.println("}");
            writer.println();

            writer.println("sounds {");
            writer.println("  # Master toggle to enable or disable all plugin sound effects");
            writer.println("  enabled = " + isSoundsEnabled());
            writer.println();
            writer.println("  # Granular sound customization for every in-game event:");
            for (String evt : List.of("dummy-spawn", "dummy-hit", "dummy-crit", "dummy-die", "dummy-respawn", "gui-click")) {
                writer.println("  " + evt + " {");
                writer.println("    sound = \"" + getSoundEventName(evt) + "\"");
                writer.println("    volume = " + getSoundEventVolume(evt, 1.0f));
                writer.println("    pitch = " + getSoundEventPitch(evt, 1.0f));
                writer.println("    enabled = " + isSoundEventEnabled(evt));
                writer.println("  }");
            }
            writer.println("}");
            writer.println();

            writer.println("messages {");
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (entry.getKey().startsWith("messages.")) {
                    String sub = entry.getKey().substring("messages.".length());
                    writer.println("  " + sub + " = \"" + entry.getValue().toString().replace("\"", "\\\"") + "\"");
                }
            }
            writer.println("}");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save setting.conf: " + e.getMessage());
        }
    }

    // Getters & Setters
    public boolean isSilentStartup() {
        Object val = values.get("general.silent-startup");
        if (val == null) val = values.get("general.SilentStartup");
        if (val == null) val = values.get("SilentStartup");
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return false;
    }

    public void setSilentStartup(boolean silent) {
        values.put("general.silent-startup", silent);
    }

    public String getCustomUiFont() {
        Object val = values.get("general.custom-ui-font");
        return val != null ? val.toString() : "";
    }

    public int getMaxDummiesPerPlayer() {
        Object val = values.get("general.max-dummies-per-player");
        if (val instanceof Number n) return n.intValue();
        return 5;
    }

    public void setMaxDummiesPerPlayer(int max) {
        values.put("general.max-dummies-per-player", Math.max(0, max));
    }

    public Material getWandMaterial() {
        Object val = values.get("wand.material");
        Material m = val != null ? Material.matchMaterial(val.toString()) : null;
        return m != null ? m : Material.STICK;
    }

    public void setWandMaterial(Material mat) {
        values.put("wand.material", mat.name());
    }

    public boolean isWandRequireSneak() {
        Object val = values.get("wand.require-sneak");
        if (val instanceof Boolean b) return b;
        return true;
    }

    // Defaults
    public EntityType getDefaultEntityType() {
        Object val = values.get("defaults.entity-type");
        try {
            return EntityType.valueOf(val != null ? val.toString().toUpperCase() : "ZOMBIE");
        } catch (Exception e) {
            return EntityType.ZOMBIE;
        }
    }

    public void setDefaultEntityType(EntityType type) {
        values.put("defaults.entity-type", type.name());
    }

    public String getDefaultCustomName() {
        Object val = values.get("defaults.custom-name");
        return val != null ? val.toString() : "<gradient:#ff7675:#fab1a0><bold>Combat Target Dummy</bold></gradient>";
    }

    public void setDefaultCustomName(String name) {
        values.put("defaults.custom-name", name);
    }

    public double getDefaultMaxHealth() {
        Object val = values.get("defaults.max-health");
        if (val instanceof Number n) return n.doubleValue();
        return 1000.0;
    }

    public void setDefaultMaxHealth(double health) {
        values.put("defaults.max-health", Math.max(1.0, health));
    }

    public boolean isDefaultInfiniteHealth() {
        Object val = values.get("defaults.infinite-health");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setDefaultInfiniteHealth(boolean infinite) {
        values.put("defaults.infinite-health", infinite);
    }

    public boolean isDefaultHasAi() {
        Object val = values.get("defaults.has-ai");
        if (val instanceof Boolean b) return b;
        return false;
    }

    public void setDefaultHasAi(boolean ai) {
        values.put("defaults.has-ai", ai);
    }

    public boolean isDefaultHasGravity() {
        Object val = values.get("defaults.has-gravity");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setDefaultHasGravity(boolean grav) {
        values.put("defaults.has-gravity", grav);
    }

    public boolean isDefaultSilent() {
        Object val = values.get("defaults.is-silent");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setDefaultSilent(boolean silent) {
        values.put("defaults.is-silent", silent);
    }

    public boolean isDefaultRespawnable() {
        Object val = values.get("defaults.respawnable");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setDefaultRespawnable(boolean respawn) {
        values.put("defaults.respawnable", respawn);
    }

    public int getDefaultRespawnDelay() {
        Object val = values.get("defaults.respawn-delay-seconds");
        if (val instanceof Number n) return n.intValue();
        return 3;
    }

    public void setDefaultRespawnDelay(int delay) {
        values.put("defaults.respawn-delay-seconds", Math.max(0, delay));
    }

    public long getDefaultLifespan() {
        Object val = values.get("defaults.lifespan-seconds");
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    public void setDefaultLifespan(long seconds) {
        values.put("defaults.lifespan-seconds", Math.max(0L, seconds));
    }

    public boolean isDefaultBurnInSun() {
        Object val = values.get("defaults.burn-in-sun");
        if (val instanceof Boolean b) return b;
        return false;
    }

    public void setDefaultBurnInSun(boolean burn) {
        values.put("defaults.burn-in-sun", burn);
    }

    public boolean isDefaultStatic() {
        Object val = values.get("defaults.is-static");
        if (val instanceof Boolean b) return b;
        return false;
    }

    public void setDefaultStatic(boolean stat) {
        values.put("defaults.is-static", stat);
    }

    // Damage Indicator
    public boolean isDamageIndicatorEnabled() {
        Object val = values.get("damage-indicator.enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public boolean isShowFloatingPopups() {
        Object val = values.get("damage-indicator.show-floating-popups");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setShowFloatingPopups(boolean show) {
        values.put("damage-indicator.show-floating-popups", show);
    }

    public int getPopupDurationTicks() {
        Object val = values.get("damage-indicator.popup-duration-ticks");
        if (val instanceof Number n) return n.intValue();
        return 25;
    }

    public int getPopupViewDistanceBlocks() {
        Object val = values.get("damage-indicator.popup-view-distance-blocks");
        if (val instanceof Number n) return n.intValue();
        return 32;
    }

    public boolean isShowActionbarDps() {
        Object val = values.get("damage-indicator.show-actionbar-dps");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setShowActionbarDps(boolean show) {
        values.put("damage-indicator.show-actionbar-dps", show);
    }

    public String getNormalHitFormat() {
        Object val = values.get("damage-indicator.normal-hit-format");
        return val != null ? val.toString() : "<red>-<damage> ❤</red>";
    }

    public String getCritHitFormat() {
        Object val = values.get("damage-indicator.crit-hit-format");
        return val != null ? val.toString() : "<gold><bold>CRIT! -<damage> ❤</bold></gold>";
    }

    public String getActionbarFormat() {
        Object val = values.get("damage-indicator.actionbar-format");
        return val != null ? val.toString() : "<yellow><bold>✦ Target Hit ✦</bold></yellow> <gray>|</gray> <red>Last: -<damage> ❤</red> <gray>|</gray> <gold>DPS: <dps></gold> <gray>|</gray> <aqua>Total: <total_damage></aqua> <gray>(Hits: <hits>)</gray>";
    }

    // Sounds
    public boolean isSoundsEnabled() {
        Object val = values.get("sounds.enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public void setSoundsEnabled(boolean enabled) {
        values.put("sounds.enabled", enabled);
    }

    public boolean isSoundEventEnabled(String eventKey) {
        Object val = values.get("sounds." + eventKey + ".enabled");
        if (val instanceof Boolean b) return b;
        return true;
    }

    public String getSoundEventName(String eventKey) {
        Object val = values.get("sounds." + eventKey + ".sound");
        return val != null ? val.toString() : "ENTITY_EXPERIENCE_ORB_PICKUP";
    }

    public Sound getSoundEvent(String eventKey, Sound fallback) {
        String name = getSoundEventName(eventKey);
        if (name == null || name.trim().isEmpty()) return fallback;
        name = name.trim();

        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT).replace('.', '_'));
        } catch (IllegalArgumentException ignored) {}

        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).replace('_', '.'));
            Sound s = Registry.SOUNDS.get(key);
            if (s != null) return s;
        } catch (Exception ignored) {}

        try {
            return Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        } catch (Exception ignored) {}

        return fallback;
    }

    public float getSoundEventVolume(String eventKey, float defaultVal) {
        Object val = values.get("sounds." + eventKey + ".volume");
        if (val instanceof Number n) return n.floatValue();
        return defaultVal;
    }

    public float getSoundEventPitch(String eventKey, float defaultVal) {
        Object val = values.get("sounds." + eventKey + ".pitch");
        if (val instanceof Number n) return n.floatValue();
        return defaultVal;
    }

    public String getMessage(String key, String defaultMsg) {
        Object val = values.get("messages." + key);
        return val != null ? val.toString() : defaultMsg;
    }

    public String getPrefix() {
        return getMessage("prefix", "<dark_gray>[<gradient:#ff7675:#fab1a0><bold>SomeDummy</bold></gradient>]</dark_gray> ");
    }
}
