package dev.dailyemerald.perks.command;

import dev.dailyemerald.perks.nations.NationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /nation join|leave|list|create|delete — self-serve team sign-up. */
public final class NationCommand implements TabExecutor {

    private static final String ADMIN_PERM = "emeraldperks.nation.admin";

    private final NationManager nations;

    public NationCommand(NationManager nations) {
        this.nations = nations;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!nations.isEnabled()) {
            sender.sendMessage(Component.text("The nation sign-up system is disabled.", NamedTextColor.RED));
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> join(sender, args);
            case "leave", "cancel" -> leave(sender);
            case "list" -> list(sender);
            case "create" -> {
                if (requireAdmin(sender)) create(sender, args);
            }
            case "delete", "remove" -> {
                if (requireAdmin(sender)) delete(sender, args);
            }
            default -> status(sender, label);
        }
        return true;
    }

    private void join(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can join a nation.", NamedTextColor.RED));
            return;
        }
        if (args.length != 2) {
            player.sendMessage(Component.text("Usage: /nation join <name>", NamedTextColor.YELLOW));
            return;
        }
        String canon = nations.canonical(args[1]);
        if (canon == null) {
            player.sendMessage(Component.text("There's no nation called \"" + args[1]
                    + "\". Use /nation list to see your options.", NamedTextColor.RED));
            return;
        }
        String current = nations.currentNation(player);
        if (canon.equalsIgnoreCase(current)) {
            player.sendMessage(Component.text("You're already in " + canon + ".", NamedTextColor.YELLOW));
            return;
        }
        nations.join(player, canon);
        // Announce to everyone, matching the datapack's assignment message style.
        Component msg = player.displayName().colorIfAbsent(NamedTextColor.AQUA)
                .append(Component.text(
                        current == null ? " joined the " : " switched to the ", NamedTextColor.WHITE))
                .append(Component.text(canon, NamedTextColor.GOLD))
                .append(Component.text(" nation.", NamedTextColor.WHITE));
        Bukkit.getServer().sendMessage(msg);
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can leave a nation.", NamedTextColor.RED));
            return;
        }
        String current = nations.currentNation(player);
        if (!nations.leave(player)) {
            player.sendMessage(Component.text("You're not in a nation right now.", NamedTextColor.YELLOW));
            return;
        }
        player.sendMessage(Component.text("You left the " + current
                + " nation. Use /nation join <name> to pick a new one.", NamedTextColor.GREEN));
    }

    private void list(CommandSender sender) {
        List<String> available = nations.available();
        if (available.isEmpty()) {
            sender.sendMessage(Component.text("No nations have been set up yet.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Nations you can join: ", NamedTextColor.GREEN)
                .append(Component.text(String.join(", ", available), NamedTextColor.GOLD)));
        if (sender instanceof Player player) {
            String current = nations.currentNation(player);
            sender.sendMessage(current == null
                    ? Component.text("You're not in a nation. Join one with /nation join <name>.",
                            NamedTextColor.GRAY)
                    : Component.text("You're currently in " + current + ".", NamedTextColor.GRAY));
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /nation create <name>", NamedTextColor.YELLOW));
            return;
        }
        if (!nations.isValidName(args[1])) {
            sender.sendMessage(Component.text(
                    "Nation names must be 1-16 letters, numbers, or underscores (no spaces).",
                    NamedTextColor.RED));
            return;
        }
        if (!nations.createNation(args[1])) {
            sender.sendMessage(Component.text("A nation called \"" + args[1] + "\" already exists.",
                    NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Created nation \"" + args[1]
                + "\". Players can now /nation join " + args[1] + ".", NamedTextColor.GREEN));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /nation delete <name>", NamedTextColor.YELLOW));
            return;
        }
        if (!nations.deleteNation(args[1])) {
            sender.sendMessage(Component.text("There's no nation called \"" + args[1] + "\".",
                    NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Deleted nation \"" + args[1]
                + "\" and removed everyone from it.", NamedTextColor.GREEN));
    }

    private void status(CommandSender sender, String label) {
        sender.sendMessage(Component.text("— Nations —", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " list — see the nations you can join",
                NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " join <name> — sign up (or switch)",
                NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " leave — leave your nation",
                NamedTextColor.YELLOW));
        if (sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(Component.text("/" + label + " create <name> — add a nation (admin)",
                    NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/" + label + " delete <name> — remove a nation (admin)",
                    NamedTextColor.GRAY));
        }
        if (sender instanceof Player player) {
            String current = nations.currentNation(player);
            if (current != null) {
                sender.sendMessage(Component.text("You're in the " + current + " nation.",
                        NamedTextColor.GREEN));
            }
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN_PERM)) return true;
        sender.sendMessage(Component.text("You don't have permission for that.", NamedTextColor.RED));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("join", "leave", "list"));
            if (sender.hasPermission(ADMIN_PERM)) {
                subs.add("create");
                subs.add("delete");
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("join") || sub.equals("delete") || sub.equals("remove")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return nations.available().stream()
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
            }
        }
        return List.of();
    }
}
