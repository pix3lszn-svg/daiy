package dev.dailyemerald.morph.command;

import dev.dailyemerald.morph.AccessStore;
import dev.dailyemerald.morph.MorphManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /allowmorph            — everyone may /morph
 * /allowmorph &lt;name&gt;     — grant one player
 * /unallowmorph          — revoke everyone (and unmorph all non-OPs)
 * /unallowmorph &lt;name&gt;   — revoke one player (and unmorph them)
 */
public final class AllowMorphCommand implements TabExecutor {

    private final AccessStore access;
    private final MorphManager morphs;

    public AllowMorphCommand(AccessStore access, MorphManager morphs) {
        this.access = access;
        this.morphs = morphs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean allowing = command.getName().equalsIgnoreCase("allowmorph");
        if (args.length == 0) {
            if (allowing) {
                allowEveryone(sender);
            } else {
                revokeEveryone(sender);
            }
        } else if (args.length == 1) {
            if (allowing) {
                allowOne(sender, args[0]);
            } else {
                revokeOne(sender, args[0]);
            }
        } else {
            sender.sendMessage(Component.text("Usage: /" + label + " [player]", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void allowEveryone(CommandSender sender) {
        if (access.isAllowAll()) {
            sender.sendMessage(Component.text("Morphing is already enabled for everyone.", NamedTextColor.YELLOW));
            return;
        }
        access.setAllowAll(true);
        Bukkit.getServer().sendMessage(Component.text(
                "Morphing is now enabled for everyone — try /morph! (/morph villager plains librarian, "
                        + "/morphview if your own mob blocks your aim, /unmorph to change back)",
                NamedTextColor.GREEN));
    }

    private void revokeEveryone(CommandSender sender) {
        boolean wasAllowAll = access.isAllowAll();
        int hadGrants = access.allowedPlayers().size();
        access.revokeEveryone();
        int reverted = morphs.unmorphAllNonOps();
        if (!wasAllowAll && hadGrants == 0 && reverted == 0) {
            sender.sendMessage(Component.text("Morphing was already OP-only.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Morphing is back to OP-only. Cleared " + hadGrants
                + " individual grant(s) and reverted " + reverted + " morphed player(s).",
                NamedTextColor.GREEN));
    }

    private void allowOne(CommandSender sender, String name) {
        OfflinePlayer target = resolve(name);
        if (target == null) {
            sender.sendMessage(Component.text("Couldn't find a player called " + name
                    + " (they need to have joined the server at least once).", NamedTextColor.RED));
            return;
        }
        if (!access.allow(target.getUniqueId())) {
            sender.sendMessage(Component.text(displayName(target) + " already had morph access.",
                    NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text(displayName(target) + " can now use /morph.", NamedTextColor.GREEN));
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage(Component.text(
                    "You've been given morph access — try /morph! (Tab-complete lists every mob; "
                            + "villagers take styles like /morph villager plains librarian.)",
                    NamedTextColor.GREEN));
        }
    }

    private void revokeOne(CommandSender sender, String name) {
        OfflinePlayer target = resolve(name);
        if (target == null) {
            sender.sendMessage(Component.text("Couldn't find a player called " + name + ".", NamedTextColor.RED));
            return;
        }
        boolean hadGrant = access.disallow(target.getUniqueId());
        Player online = target.getPlayer();
        boolean reverted = false;
        if (online != null && !online.isOp() && morphs.isMorphed(online)) {
            morphs.unmorph(online, false);
            reverted = true;
        }
        if (!hadGrant && !reverted) {
            sender.sendMessage(Component.text(displayName(target) + " had no individual morph grant.",
                    NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Revoked morph access for " + displayName(target)
                    + (reverted ? " and changed them back." : "."), NamedTextColor.GREEN));
        }
        if (access.isAllowAll()) {
            sender.sendMessage(Component.text(
                    "Heads up: /allowmorph is still on for EVERYONE, so they can still morph. "
                            + "Run /unallowmorph (no name) to turn the global switch off.",
                    NamedTextColor.YELLOW));
        }
    }

    /** Online player first, then anyone who has joined before (cached offline). */
    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached != null && cached.hasPlayedBefore() ? cached : null;
    }

    private static String displayName(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
    }
}
