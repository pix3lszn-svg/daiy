package dev.dailyemerald.morph;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Who may morph. OPs always can. Everyone else needs either the global
 * allow-all switch (/allowmorph) or an individual grant (/allowmorph name).
 * Persisted to data.yml so it survives restarts.
 */
public final class AccessStore {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    private boolean allowAll;
    private final Set<UUID> allowed = new HashSet<>();

    public AccessStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        this.allowAll = yaml.getBoolean("allow-all", false);
        for (String id : yaml.getStringList("allowed-players")) {
            try {
                allowed.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean canMorph(Player player) {
        return player.isOp() || allowAll || allowed.contains(player.getUniqueId());
    }

    public boolean isAllowAll() {
        return allowAll;
    }

    public void setAllowAll(boolean value) {
        allowAll = value;
        save();
    }

    /** @return true if newly added. */
    public boolean allow(UUID id) {
        boolean added = allowed.add(id);
        if (added) save();
        return added;
    }

    /** @return true if the player had an individual grant. */
    public boolean disallow(UUID id) {
        boolean removed = allowed.remove(id);
        if (removed) save();
        return removed;
    }

    /** Clears the global switch AND every individual grant. */
    public void revokeEveryone() {
        allowAll = false;
        allowed.clear();
        save();
    }

    public Set<UUID> allowedPlayers() {
        return Set.copyOf(allowed);
    }

    // ---- morph self-view preference (persisted here too) ----

    public boolean isSelfHidden(UUID id) {
        return yaml.getBoolean("self-hidden." + id, false);
    }

    public void setSelfHidden(UUID id, boolean hidden) {
        yaml.set("self-hidden." + id, hidden ? true : null);
        save();
    }

    private void save() {
        yaml.set("allow-all", allowAll);
        yaml.set("allowed-players", allowed.stream().map(UUID::toString).sorted().toList());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    /** Friendly name for a stored UUID, for /allowmorph list-style messages. */
    public static String nameOf(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }
}
