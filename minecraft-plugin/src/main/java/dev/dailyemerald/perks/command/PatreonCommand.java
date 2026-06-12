package dev.dailyemerald.perks.command;

import dev.dailyemerald.perks.EmeraldPerksPlugin;
import dev.dailyemerald.perks.PatreonSyncService;
import dev.dailyemerald.perks.PlayerDataStore;
import dev.dailyemerald.perks.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /patreon link|unlink|status|sync|set|horn|reload */
public final class PatreonCommand implements TabExecutor {

    private static final String ADMIN_PERM = "emeraldperks.admin";

    private final EmeraldPerksPlugin plugin;
    private final PlayerDataStore store;
    private final PatreonSyncService sync;

    public PatreonCommand(EmeraldPerksPlugin plugin, PlayerDataStore store, PatreonSyncService sync) {
        this.plugin = plugin;
        this.store = store;
        this.sync = sync;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "link" -> link(sender, args);
            case "unlink" -> unlink(sender);
            case "status" -> status(sender);
            case "sync" -> {
                if (requireAdmin(sender)) sync.sync(sender);
            }
            case "set" -> {
                if (requireAdmin(sender)) set(sender, args);
            }
            case "horn" -> {
                if (requireAdmin(sender)) horn(sender, args);
            }
            case "priority" -> {
                if (requireAdmin(sender)) priority(sender, args);
            }
            case "grant" -> {
                if (requireAdmin(sender)) grant(sender, args);
            }
            default -> help(sender);
        }
        return true;
    }

    private void link(CommandSender sender, String[] args) {
        // Admin form: /patreon link <mcname> <email> — pre-link + whitelist a
        // patron who has never joined (needed when the whitelist is on).
        if (args.length == 3) {
            if (requireAdmin(sender)) adminLink(sender, args[1], args[2]);
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Console usage: /patreon link <mcname> <email>", NamedTextColor.RED));
            return;
        }
        if (args.length != 2 || !args[1].contains("@")) {
            player.sendMessage(Component.text("Usage: /patreon link <your-patreon-email>", NamedTextColor.YELLOW));
            return;
        }
        if (!sync.isConfigured()) {
            player.sendMessage(Component.text(
                    "Patreon linking isn't set up on this server yet — poke an admin!", NamedTextColor.RED));
            return;
        }
        if (!sync.hasSyncedOnce()) {
            player.sendMessage(Component.text("Still loading the patron list, try again in a minute.",
                    NamedTextColor.YELLOW));
            sync.sync(null);
            return;
        }
        String email = args[1].toLowerCase(Locale.ROOT);
        Role role = sync.entitledRole(email);
        if (role == null) {
            player.sendMessage(Component.text(
                    "No active Board Member or Journalist pledge found for that email. Double-check it's "
                            + "the email on your Patreon account — and note new pledges can take a few "
                            + "minutes to show up.", NamedTextColor.RED));
            return;
        }
        store.link(player.getUniqueId(), email, role);
        player.sendMessage(Component.text("Linked! You are a ", NamedTextColor.GREEN)
                .append(Component.text(role.displayName(), NamedTextColor.GOLD))
                .append(Component.text(" — try /morph!", NamedTextColor.GREEN)));
        sync.giveHornIfOwed(player);
        plugin.applyRoleSideEffects(player);
    }

    private void adminLink(CommandSender sender, String name, String email) {
        if (!email.contains("@")) {
            sender.sendMessage(Component.text("Usage: /patreon link <mcname> <patreon-email>", NamedTextColor.YELLOW));
            return;
        }
        if (!sync.isConfigured() || !sync.hasSyncedOnce()) {
            sender.sendMessage(Component.text(
                    "Patron list not loaded yet — configure the token and/or run /patreon sync first.",
                    NamedTextColor.RED));
            sync.sync(null);
            return;
        }
        Role role = sync.entitledRole(email.toLowerCase(Locale.ROOT));
        if (role == null) {
            sender.sendMessage(Component.text(
                    "No active Board Member or Journalist pledge for that email.", NamedTextColor.RED));
            return;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            store.link(online.getUniqueId(), email, role);
            online.sendMessage(Component.text("An admin linked your Patreon — you are a ", NamedTextColor.GREEN)
                    .append(Component.text(role.displayName(), NamedTextColor.GOLD))
                    .append(Component.text("!", NamedTextColor.GREEN)));
            sync.giveHornIfOwed(online);
            plugin.applyRoleSideEffects(online);
            sender.sendMessage(Component.text("Linked " + online.getName() + " as " + role.displayName() + ".",
                    NamedTextColor.GREEN));
            return;
        }
        store.addPending(name, email, role);
        plugin.whitelistAdd(name);
        sender.sendMessage(Component.text("Pre-linked " + name + " as " + role.displayName()
                        + (plugin.manageWhitelist() ? " and added them to the whitelist." : ".")
                        + " Their perks unlock when they first join.",
                NamedTextColor.GREEN));
    }

    private void unlink(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        if (!store.isLinked(player.getUniqueId())) {
            player.sendMessage(Component.text("You aren't linked to a Patreon account.", NamedTextColor.YELLOW));
            return;
        }
        plugin.morphManager().unmorph(player, true);
        store.unlink(player.getUniqueId());
        player.sendMessage(Component.text("Unlinked your Patreon account.", NamedTextColor.GREEN));
    }

    private void status(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        Role role = store.getRole(player.getUniqueId());
        String email = store.getEmail(player.getUniqueId());
        if (role == null) {
            player.sendMessage(Component.text(
                    email == null
                            ? "Not linked. Use /patreon link <email> to claim your patron perks."
                            : "Linked to " + mask(email) + ", but no active perk tier right now.",
                    NamedTextColor.YELLOW));
            return;
        }
        player.sendMessage(Component.text("Role: ", NamedTextColor.GREEN)
                .append(Component.text(role.displayName(), NamedTextColor.GOLD))
                .append(Component.text(email != null ? "  (" + mask(email) + ")" : "  (granted by an admin)",
                        NamedTextColor.GRAY)));
        player.sendMessage(Component.text(
                role == Role.BOARD
                        ? "Perk: /morph into any mob (except the ender dragon) + goat horn."
                        : "Perk: /morph villager + goat horn.",
                NamedTextColor.GREEN));
    }

    private void set(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /patreon set <player> <board|journalist|none>",
                    NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player " + args[1] + " must be online.", NamedTextColor.RED));
            return;
        }
        if (args[2].equalsIgnoreCase("none")) {
            plugin.morphManager().unmorph(target, true);
            store.unlink(target.getUniqueId());
            if (!target.isOp() && !target.hasPermission(ADMIN_PERM)) {
                plugin.whitelistRemove(target.getName());
            }
            sender.sendMessage(Component.text("Cleared perks for " + target.getName() + ".", NamedTextColor.GREEN));
            return;
        }
        Role role = Role.fromString(args[2]);
        if (role == null) {
            sender.sendMessage(Component.text("Unknown role: " + args[2], NamedTextColor.RED));
            return;
        }
        store.setRole(target.getUniqueId(), role, true);
        plugin.morphManager().unmorphIfNoLongerAllowed(target, role);
        plugin.applyRoleSideEffects(target);
        sender.sendMessage(Component.text("Set " + target.getName() + " to " + role.displayName()
                + " (manual — Patreon sync won't change it).", NamedTextColor.GREEN));
        target.sendMessage(Component.text("You were granted the ", NamedTextColor.GREEN)
                .append(Component.text(role.displayName(), NamedTextColor.GOLD))
                .append(Component.text(" perks — try /morph!", NamedTextColor.GREEN)));
        sync.giveHornIfOwed(target);
    }

    private void horn(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /patreon horn <player>  — re-give a lost horn",
                    NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player " + args[1] + " must be online.", NamedTextColor.RED));
            return;
        }
        store.setHornGiven(target.getUniqueId(), false);
        sync.giveHornIfOwed(target);
        if (store.isHornGiven(target.getUniqueId())) {
            sender.sendMessage(Component.text("Gave " + target.getName() + " a fresh horn.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(target.getName() + " has no perk role, so no horn.",
                    NamedTextColor.RED));
        }
    }

    private void priority(CommandSender sender, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
            var names = store.priorityNames();
            sender.sendMessage(Component.text("Manual priority list (" + names.size() + "): "
                    + String.join(", ", names), NamedTextColor.GREEN));
            sender.sendMessage(Component.text("(All active patrons get priority automatically.)",
                    NamedTextColor.GRAY));
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /patreon priority <add|remove|list> [player]",
                    NamedTextColor.YELLOW));
            return;
        }
        String name = args[2];
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                store.addPriority(name);
                sender.sendMessage(Component.text(name + " now gets priority when the server is full.",
                        NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (store.removePriority(name)) {
                    sender.sendMessage(Component.text("Removed " + name + " from the priority list.",
                            NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(name + " wasn't on the priority list.",
                            NamedTextColor.YELLOW));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /patreon priority <add|remove|list> [player]",
                    NamedTextColor.YELLOW));
        }
    }

    /**
     * Bulk grant by IGN, no emails needed — built for pasting a comment
     * section's worth of usernames in one go:
     *   /patreon grant board Alice Bob Carol_123
     * Each name is whitelisted immediately; perks land when they first join.
     * These are manual grants, so the Patreon sync never revokes them.
     */
    private void grant(CommandSender sender, String[] args) {
        if (args.length < 3 || Role.fromString(args[1]) == null) {
            sender.sendMessage(Component.text("Usage: /patreon grant <board|journalist> <name> [name] [name] ...",
                    NamedTextColor.YELLOW));
            return;
        }
        Role role = Role.fromString(args[1]);
        List<String> granted = new java.util.ArrayList<>();
        List<String> skipped = new java.util.ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String name = args[i].trim();
            if (name.isEmpty()) continue;
            if (!name.matches("[\\w.*+-]{1,32}")) {
                skipped.add(name);
                continue;
            }
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) {
                store.setRole(online.getUniqueId(), role, true);
                plugin.morphManager().unmorphIfNoLongerAllowed(online, role);
                sync.giveHornIfOwed(online);
                plugin.applyRoleSideEffects(online);
                online.sendMessage(Component.text("You were granted the ", NamedTextColor.GREEN)
                        .append(Component.text(role.displayName(), NamedTextColor.GOLD))
                        .append(Component.text(" perks — try /morph!", NamedTextColor.GREEN)));
            } else {
                store.addPendingManual(name, role);
                plugin.whitelistAdd(name);
            }
            granted.add(name);
        }
        sender.sendMessage(Component.text("Granted " + role.displayName() + " to " + granted.size()
                        + " player(s): " + String.join(", ", granted), NamedTextColor.GREEN));
        if (!skipped.isEmpty()) {
            sender.sendMessage(Component.text("Skipped (not valid usernames): " + String.join(", ", skipped),
                    NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text(
                "Offline names were whitelisted and get their perks on first join. "
                        + "Note: these grants are manual — remove with /patreon set <name> none.",
                NamedTextColor.GRAY));
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Component.text("— EmeraldPerks —", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/patreon link <email> — claim your patron perks", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/patreon status — see your current perks", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/patreon unlink — disconnect your Patreon", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/morph <mob> & /unmorph — patron morphing", NamedTextColor.YELLOW));
        if (sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(Component.text("/patreon sync — pull the patron list now", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/patreon set <player> <board|journalist|none>", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/patreon horn <player> — re-give a lost horn", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/patreon link <mcname> <email> — pre-link & whitelist a patron",
                    NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/patreon priority <add|remove|list> [player]", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/patreon grant <board|journalist> <name...> — bulk grant by IGN",
                    NamedTextColor.GRAY));
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN_PERM)) return true;
        sender.sendMessage(Component.text("You don't have permission for that.", NamedTextColor.RED));
        return false;
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(at < 0 ? 0 : at);
        return email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new java.util.ArrayList<>(List.of("link", "unlink", "status"));
            if (sender.hasPermission(ADMIN_PERM)) {
                subs.addAll(List.of("sync", "set", "horn", "priority", "grant"));
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("priority")) {
            return List.of("add", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return List.of("board", "journalist", "none");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("grant")) {
            return List.of("board", "journalist");
        }
        return List.of();
    }
}
