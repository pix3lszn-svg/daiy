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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.logging.Level;

/**
 * Two-layer team system for the Overworld Cup datapack:
 *
 *  - NATION  (tag "nation_&lt;name&gt;")  the broad pool — supporters, fans, anyone
 *    who runs /nation join. Not everyone here plays.
 *  - SQUAD   (tag "squad_&lt;name&gt;")   the ≤12 who actually take the field. Drawn
 *    from the nation: reserved players first, then a random fill of online
 *    members. Every squad member is also a nation member.
 *
 * The datapack's startmatch reads the squad tag to put players on the pitch.
 * Membership rides on scoreboard tags (readable by the datapack); the reserved
 * list and the last drawn squad are persisted in nations.yml so the squad tag
 * can be reconciled when offline players log back in.
 */
public final class NationManager {

    public static final String TAG_PREFIX = "nation_";
    public static final String SQUAD_PREFIX = "squad_";
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    public record DrawResult(List<String> squad, int reservedCount, int randomCount,
                             List<String> offlineMembers, int eligibleOnline) {}

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final int squadSize;
    private final File file;
    private final YamlConfiguration yaml;

    public NationManager(JavaPlugin plugin, boolean enabled, int squadSize) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.squadSize = Math.max(1, squadSize);
        this.file = new File(plugin.getDataFolder(), "nations.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        if (!file.exists()) {
            yaml.set("available", new ArrayList<>(List.of("cherry")));
            save();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int squadSize() {
        return squadSize;
    }

    // ---- available nations -------------------------------------------------

    public List<String> available() {
        return yaml.getStringList("available");
    }

    public boolean isValidName(String nation) {
        return nation != null && VALID_NAME.matcher(nation).matches();
    }

    public boolean exists(String nation) {
        return canonical(nation) != null;
    }

    public String canonical(String nation) {
        if (nation == null) return null;
        return available().stream().filter(n -> n.equalsIgnoreCase(nation)).findFirst().orElse(null);
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
        yaml.set("reserved." + canon.toLowerCase(Locale.ROOT), null);
        yaml.set("squad." + canon.toLowerCase(Locale.ROOT), null);
        save();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (canon.equalsIgnoreCase(currentNation(online))) clearNation(online);
            online.removeScoreboardTag(SQUAD_PREFIX + canon);
        }
        Team team = scoreboard().getTeam(canon);
        if (team != null) team.unregister();
        return true;
    }

    // ---- broad nation membership ------------------------------------------

    public String currentNation(Player player) {
        for (String tag : player.getScoreboardTags()) {
            if (tag.startsWith(TAG_PREFIX)) return tag.substring(TAG_PREFIX.length());
        }
        return null;
    }

    public void join(Player player, String canonicalNation) {
        clearNation(player);
        player.addScoreboardTag(TAG_PREFIX + canonicalNation);
        getOrCreateTeam(canonicalNation).addEntry(player.getName());
    }

    public boolean leave(Player player) {
        if (currentNation(player) == null) return false;
        clearNation(player);
        return true;
    }

    private void clearNation(Player player) {
        // Strip EVERY nation_ and squad_ tag (not just matched pairs), drop any
        // nation scoreboard-team entry, and remove them from all stored squads.
        for (String tag : new HashSet<>(player.getScoreboardTags())) {
            if (tag.startsWith(TAG_PREFIX)) {
                player.removeScoreboardTag(tag);
                Team team = scoreboard().getTeam(tag.substring(TAG_PREFIX.length()));
                if (team != null) team.removeEntry(player.getName());
            } else if (tag.startsWith(SQUAD_PREFIX)) {
                player.removeScoreboardTag(tag);
            }
        }
        dropFromSquadRosters(player.getName(), null);
    }

    /** Remove every squad_ scoreboard tag from a player except optionally one nation's. */
    private void stripSquadTags(Player player, String keepNation) {
        String keep = keepNation == null ? null : SQUAD_PREFIX + keepNation;
        for (String tag : new HashSet<>(player.getScoreboardTags())) {
            if (tag.startsWith(SQUAD_PREFIX) && !tag.equals(keep)) {
                player.removeScoreboardTag(tag);
            }
        }
    }

    /** Remove a player from every stored squad roster except (optionally) one nation's. */
    private void dropFromSquadRosters(String playerName, String keepNation) {
        boolean changed = false;
        for (String nation : available()) {
            if (keepNation != null && nation.equalsIgnoreCase(keepNation)) continue;
            String path = "squad." + nation.toLowerCase(Locale.ROOT);
            List<String> roster = yaml.getStringList(path);
            if (roster.removeIf(n -> n.equalsIgnoreCase(playerName))) {
                yaml.set(path, roster);
                changed = true;
            }
        }
        if (changed) save();
    }

    // ---- reserved players --------------------------------------------------

    public List<String> reservedFor(String canonicalNation) {
        return yaml.getStringList("reserved." + canonicalNation.toLowerCase(Locale.ROOT));
    }

    public boolean addReserved(String canonicalNation, String playerName) {
        List<String> list = new ArrayList<>(reservedFor(canonicalNation));
        if (list.stream().anyMatch(n -> n.equalsIgnoreCase(playerName))) return false;
        list.add(playerName);
        yaml.set("reserved." + canonicalNation.toLowerCase(Locale.ROOT), list);
        save();
        return true;
    }

    public boolean removeReserved(String canonicalNation, String playerName) {
        List<String> list = new ArrayList<>(reservedFor(canonicalNation));
        boolean removed = list.removeIf(n -> n.equalsIgnoreCase(playerName));
        if (removed) {
            yaml.set("reserved." + canonicalNation.toLowerCase(Locale.ROOT), list);
            save();
        }
        return removed;
    }

    // ---- squad / the draw --------------------------------------------------

    public List<String> squadFor(String canonicalNation) {
        return yaml.getStringList("squad." + canonicalNation.toLowerCase(Locale.ROOT));
    }

    private void setSquad(String canonicalNation, List<String> squad) {
        yaml.set("squad." + canonicalNation.toLowerCase(Locale.ROOT), squad);
        save();
    }

    /**
     * Draws the squad for a nation: reserved players (always), then a random
     * fill from online nation members up to the squad size.
     */
    public DrawResult draw(String canonicalNation) {
        List<String> reserved = reservedFor(canonicalNation);
        Set<String> reservedLower = new HashSet<>();
        for (String r : reserved) reservedLower.add(r.toLowerCase(Locale.ROOT));

        // Online members of this nation who aren't already reserved.
        List<Player> fillCandidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getScoreboardTags().contains(TAG_PREFIX + canonicalNation)
                    && !reservedLower.contains(online.getName().toLowerCase(Locale.ROOT))) {
                fillCandidates.add(online);
            }
        }
        Collections.shuffle(fillCandidates);

        // Build squad: reserved first (capped), then random fill.
        List<String> squad = new ArrayList<>();
        for (String r : reserved) {
            if (squad.size() >= squadSize) break;
            squad.add(r);
        }
        int reservedInSquad = squad.size();
        for (Player p : fillCandidates) {
            if (squad.size() >= squadSize) break;
            squad.add(p.getName());
        }

        List<String> previous = squadFor(canonicalNation);
        setSquad(canonicalNation, squad);
        applySquadTags(canonicalNation, previous, squad);

        // Reserved players who couldn't be tagged yet because they're offline.
        List<String> offline = new ArrayList<>();
        for (String name : squad) {
            if (Bukkit.getPlayerExact(name) == null) offline.add(name);
        }
        return new DrawResult(squad, reservedInSquad, squad.size() - reservedInSquad,
                offline, fillCandidates.size());
    }

    public boolean clearSquad(String canonicalNation) {
        List<String> previous = squadFor(canonicalNation);
        if (previous.isEmpty()) return false;
        setSquad(canonicalNation, new ArrayList<>());
        applySquadTags(canonicalNation, previous, List.of());
        return true;
    }

    private void applySquadTags(String nation, List<String> previous, List<String> current) {
        Set<String> currentLower = new HashSet<>();
        for (String n : current) currentLower.add(n.toLowerCase(Locale.ROOT));

        // Drop the squad tag from anyone no longer selected (online only — offline
        // stragglers are reconciled when they next log in).
        for (String name : previous) {
            if (!currentLower.contains(name.toLowerCase(Locale.ROOT))) {
                Player p = Bukkit.getPlayerExact(name);
                if (p != null) p.removeScoreboardTag(SQUAD_PREFIX + nation);
            }
        }
        // Tag the chosen players. Each can only be in ONE squad, so first drop
        // them from every other nation's roster and strip any other squad tag.
        for (String name : current) {
            dropFromSquadRosters(name, nation);
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) {
                stripSquadTags(p, nation);
                p.addScoreboardTag(SQUAD_PREFIX + nation);
                p.addScoreboardTag(TAG_PREFIX + nation);
                getOrCreateTeam(nation).addEntry(p.getName());
            }
        }
    }

    /**
     * On login, line a player's squad tags up with the stored rosters — adds the
     * tag if they were drawn while offline, removes a stale one if they were cut.
     */
    public void reconcileSquadTags(Player player) {
        String name = player.getName();
        // Find the one roster they belong to (rosters are kept mutually exclusive).
        String inNation = null;
        for (String nation : available()) {
            if (squadFor(nation).stream().anyMatch(s -> s.equalsIgnoreCase(name))) {
                inNation = nation;
                break;
            }
        }
        // Strip any squad tag that doesn't match, then ensure the right one is set.
        stripSquadTags(player, inNation);
        if (inNation != null && !player.getScoreboardTags().contains(SQUAD_PREFIX + inNation)) {
            player.addScoreboardTag(SQUAD_PREFIX + inNation);
            player.addScoreboardTag(TAG_PREFIX + inNation);
            getOrCreateTeam(inNation).addEntry(player.getName());
        }
    }

    private Team getOrCreateTeam(String nation) {
        Scoreboard board = scoreboard();
        Team team = board.getTeam(nation);
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
