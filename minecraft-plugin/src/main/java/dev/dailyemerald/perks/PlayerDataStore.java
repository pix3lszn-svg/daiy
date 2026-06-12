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

    // ------------------------------------------------------------------
    // Pending links: admin pre-linked a Minecraft name that hasn't joined
    // yet (so there's no UUID to attach the role to).
    //
    // pending:
    //   <lowercase name>:
    //     email: patron@example.com
    //     role: BOARD
    // ------------------------------------------------------------------

    public record Pending(String email, Role role) {}

    public void addPending(String name, String email, Role role) {
        String key = "pending." + name.toLowerCase(Locale.ROOT);
        yaml.set(key + ".email", email.toLowerCase(Locale.ROOT));
        yaml.set(key + ".role", role.name());
        save();
    }

    /** Pending grant with no Patreon email behind it (bulk/manual grants). */
    public void addPendingManual(String name, Role role) {
        String key = "pending." + name.toLowerCase(Locale.ROOT);
        yaml.set(key + ".email", null);
        yaml.set(key + ".role", role.name());
        save();
    }

    public boolean hasPending(String name) {
        return yaml.contains("pending." + name.toLowerCase(Locale.ROOT));
    }

    /** Returns and removes the pending link for this name, or null. */
    public Pending takePending(String name) {
        String key = "pending." + name.toLowerCase(Locale.ROOT);
        if (!yaml.contains(key)) return null;
        Pending pending = new Pending(
                yaml.getString(key + ".email"),
                Role.fromString(yaml.getString(key + ".role")));
        yaml.set(key, null);
        save();
        return pending.role() == null ? null : pending;
    }

    public void removePending(String name) {
        yaml.set("pending." + name.toLowerCase(Locale.ROOT), null);
        save();
    }

    /** Lowercase name -> email for every pending link. */
    public java.util.Map<String, String> pendingEmails() {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        var section = yaml.getConfigurationSection("pending");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                String email = yaml.getString("pending." + name + ".email");
                if (email != null) result.put(name, email);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Manual priority list: extra people (by name) who get priority join
    // even without a Patreon role.
    // ------------------------------------------------------------------

    public boolean isPriority(String name) {
        return yaml.getStringList("priority").contains(name.toLowerCase(Locale.ROOT));
    }

    public void addPriority(String name) {
        var list = new java.util.ArrayList<>(yaml.getStringList("priority"));
        String lower = name.toLowerCase(Locale.ROOT);
        if (!list.contains(lower)) {
            list.add(lower);
            yaml.set("priority", list);
            save();
        }
    }

    public boolean removePriority(String name) {
        var list = new java.util.ArrayList<>(yaml.getStringList("priority"));
        boolean removed = list.remove(name.toLowerCase(Locale.ROOT));
        if (removed) {
            yaml.set("priority", list);
            save();
        }
        return removed;
    }

    public java.util.List<String> priorityNames() {
        return yaml.getStringList("priority");
    }

    public synchronized void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }
}
