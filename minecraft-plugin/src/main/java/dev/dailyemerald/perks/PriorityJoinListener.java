package dev.dailyemerald.perks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Priority join: when the server is full and a priority player connects,
 * a random non-priority player is kicked to make room.
 *
 * Priority = active patrons (Board Member / Journalist), names on the manual
 * priority list (/patreon priority add), pre-linked patrons who haven't
 * joined yet, and ops/admins. Ops and admins are never kicked.
 */
public final class PriorityJoinListener implements Listener {

    private final EmeraldPerksPlugin plugin;
    private final PlayerDataStore store;

    public PriorityJoinListener(EmeraldPerksPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.KICK_FULL) return;
        if (!plugin.getConfig().getBoolean("priority.enabled", true)) return;
        if (!hasPriority(event.getPlayer().getUniqueId(), event.getPlayer().getName())) return;

        Player victim = pickVictim();
        if (victim == null) {
            // Everyone online has priority too — stay full.
            return;
        }
        victim.kick(Component.text(
                "You were bumped to make room for a priority player — sorry! "
                        + "Patrons at patreon.com/DailyEmerald always get a slot.",
                NamedTextColor.YELLOW));
        event.allow();
        plugin.getLogger().info("Priority join: kicked " + victim.getName()
                + " to make room for " + event.getPlayer().getName() + ".");
    }

    private boolean hasPriority(UUID id, String name) {
        if (store.getRole(id) != null) return true;
        if (store.isPriority(name)) return true;
        // Admin pre-linked patrons who are joining for the very first time.
        return store.pendingEmails().containsKey(name.toLowerCase(java.util.Locale.ROOT));
    }

    private Player pickVictim() {
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp() || online.hasPermission("emeraldperks.admin")) continue;
            if (online.hasPermission("emeraldperks.priority")) continue;
            if (hasPriority(online.getUniqueId(), online.getName())) continue;
            candidates.add(online);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
