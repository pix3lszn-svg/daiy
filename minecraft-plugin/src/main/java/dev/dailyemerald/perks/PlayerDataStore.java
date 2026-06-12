package dev.dailyemerald.perks;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persists player links and perk state in data.yml:
 *
 * players:
 *   <uuid>:
 *     email: patron@example.com   # null for manual grants
 *     role: BOARD                 # last known role, kept while pledge is active
 *     manual: false               # true = granted by an admin, sync won't touch it
 *     horn-given: true
 */
public final class PlayerDataStore {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public PlayerDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private String path(UUID id, String key) {
        return "players." + id + "." + key;
    }

    public void link(UUID id, String email, Role role) {
        yaml.set(path(id, "email"), email.toLowerCase(Locale.ROOT));
        yaml.set(path(id, "role"), role == null ? null : role.name());
        yaml.set(path(id, "manual"), false);
        save();
    }

    public void unlink(UUID id) {
        yaml.set("players." + id, null);
        save();
    }

    public boolean isLinked(UUID id) {
        return yaml.contains("players." + id);
    }

    public String getEmail(UUID id) {
        return yaml.getString(path(id, "email"));
    }

    public Role getRole(UUID id) {
        return Role.fromString(yaml.getString(path(id, "role")));
    }

    public void setRole(UUID id, Role role, boolean manual) {
        yaml.set(path(id, "role"), role == null ? null : role.name());
        yaml.set(path(id, "manual"), manual);
        save();
    }

    public boolean isManual(UUID id) {
        return yaml.getBoolean(path(id, "manual"), false);
    }

    public boolean isHornGiven(UUID id) {
        return yaml.getBoolean(path(id, "horn-given"), false);
    }

    public void setHornGiven(UUID id, boolean given) {
        yaml.set(path(id, "horn-given"), given);
        save();
    }

    /** All UUIDs that have an entry. */
    public java.util.Set<UUID> linkedPlayers() {
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        var section = yaml.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    ids.add(UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return ids;
    }

    public synchronized void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }
}
