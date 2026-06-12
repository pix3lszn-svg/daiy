package dev.dailyemerald.perks;

import dev.dailyemerald.perks.morph.MorphManager;
import dev.dailyemerald.perks.patreon.PatreonClient;
import dev.dailyemerald.perks.patreon.PatreonException;
import dev.dailyemerald.perks.patreon.PatreonMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Periodically pulls the member list from Patreon and keeps player roles in
 * sync: active patrons keep their perks, lapsed pledges lose them.
 */
public final class PatreonSyncService {

    private final EmeraldPerksPlugin plugin;
    private final PatreonClient client;
    private final PlayerDataStore store;
    private final MorphManager morphs;
    private final HornService horns;
    private final List<String> boardPatterns;
    private final List<String> journalistPatterns;
    private final boolean revokeOnLapse;

    /** Last successful snapshot: patron email -> entitled role. */
    private final Map<String, Role> entitled = new ConcurrentHashMap<>();
    private volatile boolean hasSyncedOnce = false;

    public PatreonSyncService(EmeraldPerksPlugin plugin, PatreonClient client, PlayerDataStore store,
                              MorphManager morphs, HornService horns,
                              List<String> boardPatterns, List<String> journalistPatterns,
                              boolean revokeOnLapse) {
        this.plugin = plugin;
        this.client = client;
        this.store = store;
        this.morphs = morphs;
        this.horns = horns;
        this.boardPatterns = boardPatterns;
        this.journalistPatterns = journalistPatterns;
        this.revokeOnLapse = revokeOnLapse;
    }

    public boolean isConfigured() {
        return client.isConfigured();
    }

    public boolean hasSyncedOnce() {
        return hasSyncedOnce;
    }

    /** Role currently entitled for a patron email, from the last sync. */
    public Role entitledRole(String email) {
        return email == null ? null : entitled.get(email.toLowerCase(Locale.ROOT));
    }

    /** Kicks off an async sync. Feedback (if non-null) hears how it went. */
    public void sync(CommandSender feedback) {
        if (!client.isConfigured()) {
            if (feedback != null) {
                feedback.sendMessage(Component.text(
                        "Patreon is not configured yet — add your creator access token to "
                                + "plugins/EmeraldPerks/config.yml and run /patreon reload.",
                        NamedTextColor.RED));
            }
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PatreonMember> members;
            try {
                members = client.fetchActiveMembers();
            } catch (PatreonException e) {
                plugin.getLogger().log(Level.WARNING, "Patreon sync failed: " + e.getMessage());
                if (feedback != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> feedback.sendMessage(
                            Component.text("Patreon sync failed: " + e.getMessage(), NamedTextColor.RED)));
                }
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> apply(members, feedback));
        });
    }

    private void apply(List<PatreonMember> members, CommandSender feedback) {
        entitled.clear();
        for (PatreonMember member : members) {
            if (member.email() == null) continue;
            Role role = Role.resolve(member.tierTitles(), boardPatterns, journalistPatterns);
            if (role != null) {
                entitled.put(member.email(), role);
            }
        }
        hasSyncedOnce = true;

        int updated = 0;
        for (UUID id : store.linkedPlayers()) {
            if (store.isManual(id)) continue;
            String email = store.getEmail(id);
            if (email == null) continue;
            Role newRole = entitled.get(email);
            Role oldRole = store.getRole(id);
            if (newRole == oldRole) continue;
            if (newRole == null && !revokeOnLapse) continue;

            store.setRole(id, newRole, false);
            updated++;
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
                if (newRole == null) {
                    morphs.unmorphIfNoLongerAllowed(online, null);
                    online.sendMessage(Component.text(
                            "Your Patreon pledge is no longer active, so your perks were paused. "
                                    + "Thanks for having supported us!", NamedTextColor.YELLOW));
                } else {
                    morphs.unmorphIfNoLongerAllowed(online, newRole);
                    online.sendMessage(Component.text("Your Patreon perks were updated: you are now a ",
                                    NamedTextColor.GREEN)
                            .append(Component.text(newRole.displayName(), NamedTextColor.GOLD))
                            .append(Component.text("!", NamedTextColor.GREEN)));
                    giveHornIfOwed(online);
                    plugin.applyRoleSideEffects(online);
                }
            }
            if (newRole == null) {
                // Lapsed pledge: drop whitelist access too (never for ops/admins).
                var offline = Bukkit.getOfflinePlayer(id);
                if (!offline.isOp()) {
                    plugin.whitelistRemove(offline.getName());
                }
            } else if (online == null) {
                var offline = Bukkit.getOfflinePlayer(id);
                plugin.whitelistAdd(offline.getName());
            }
        }

        // Admin pre-links for players who never joined: drop them (and their
        // whitelist slot) if the pledge lapsed in the meantime.
        if (revokeOnLapse) {
            for (var pending : store.pendingEmails().entrySet()) {
                if (!entitled.containsKey(pending.getValue())) {
                    store.removePending(pending.getKey());
                    plugin.whitelistRemove(pending.getKey());
                }
            }
        }

        plugin.getLogger().info("Patreon sync complete: " + members.size() + " active patrons, "
                + entitled.size() + " with perk tiers, " + updated + " player role(s) updated.");
        if (feedback != null) {
            feedback.sendMessage(Component.text("Patreon sync complete: " + members.size()
                            + " active patrons, " + entitled.size() + " with perk tiers, "
                            + updated + " player role(s) updated.", NamedTextColor.GREEN));
        }
    }

    public void giveHornIfOwed(Player player) {
        UUID id = player.getUniqueId();
        if (store.getRole(id) != null && !store.isHornGiven(id)) {
            horns.give(player);
            store.setHornGiven(id, true);
        }
    }
}
