package dev.dailyemerald.perks.nations;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.logging.Level;

/**
 * Self-serve nation/team sign-up. Joining a nation adds the scoreboard tag
 * "nation_&lt;name&gt;" (matching the server's datapack convention) and joins the
 * scoreboard team "&lt;name&gt;". Because it's all driven by plugin commands,
 * players can sign up in adventure mode without touching a command block.
 *
 * The list of available nations lives in nations.yml (kept separate from
 * config.yml so admin edits never disturb the Patreon token or its comments).
 * Membership itself isn't stored here — it lives on the player's scoreboard
 * tags, which persist across restarts and are readable by the datapack.
 */
public final class NationManager {

    public static final String TAG_PREFIX = "nation_";
    // Team and tag names only allow a safe subset of characters.
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final File file;
    private final YamlConfiguration yaml;

    public NationManager(JavaPlugin plugin, boolean enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.file = new File(plugin.getDataFolder(), "nations.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        if (!file.exists()) {
            // Seed with the one nation we know about; admins add the rest.
            yaml.set("available", new ArrayList<>(List.of("cherry")));
            save();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> available() {
        return yaml.getStringList("available");
    }

    public boolean isValidName(String nation) {
        return nation != null && VALID_NAME.matcher(nation).matches();
    }

    public boolean exists(String nation) {
        return canonical(nation) != null;
    }

    /** Returns the nation name as stored (preserving its casing), or null if unknown. */
    public String canonical(String nation) {
        if (nation == null) return null;
        return available().stream()
                .filter(n -> n.equalsIgnoreCase(nation))
                .findFirst().orElse(null);
    }

    public boolean createNation(String nation) {
        if (!isValidName(nation) || exists(nation)) return false;
        List<String> list = new ArrayList<>(available());
        list.add(nation);
        yaml.set("available", list);
        save();
        return true;
    }

    public boolean deleteNation(String nation) {
        String canon = canonical(nation);
        if (canon == null) return false;
        List<String> list = new ArrayList<>(available());
        list.removeIf(n -> n.equalsIgnoreCase(canon));
        yaml.set("available", list);
        save();
        // Pull everyone out of the disbanded nation and drop its team.
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (canon.equalsIgnoreCase(currentNation(online))) {
                clearNation(online);
            }
        }
        Team team = scoreboard().getTeam(canon);
        if (team != null) team.unregister();
        return true;
    }

    /** The player's current nation (read from their nation_ tag), or null. */
    public String currentNation(Player player) {
        for (String tag : player.getScoreboardTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                return tag.substring(TAG_PREFIX.length());
            }
        }
        return null;
    }

    /** Joins (or switches to) a nation by its canonical name. */
    public void join(Player player, String canonicalNation) {
        clearNation(player);
        player.addScoreboardTag(TAG_PREFIX + canonicalNation);
        getOrCreateTeam(canonicalNation).addEntry(player.getName());
    }

    /** Removes the player from their nation. Returns false if they had none. */
    public boolean leave(Player player) {
        if (currentNation(player) == null) return false;
        clearNation(player);
        return true;
    }

    private void clearNation(Player player) {
        for (String tag : new HashSet<>(player.getScoreboardTags())) {
            if (tag.startsWith(TAG_PREFIX)) {
                player.removeScoreboardTag(tag);
                Team team = scoreboard().getTeam(tag.substring(TAG_PREFIX.length()));
                if (team != null) team.removeEntry(player.getName());
            }
        }
    }

    private Team getOrCreateTeam(String nation) {
        Scoreboard board = scoreboard();
        Team team = board.getTeam(nation);
        // Reuse a datapack-made team (keeps its colour/options) if it exists.
        return team != null ? team : board.registerNewTeam(nation);
    }

    private Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save nations.yml", e);
        }
    }
}
