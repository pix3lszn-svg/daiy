package dev.dailyemerald.perks.command;

import dev.dailyemerald.perks.EmeraldPerksPlugin;
import dev.dailyemerald.perks.Role;
import dev.dailyemerald.perks.morph.MorphManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** /morph &lt;mob|off&gt; and /unmorph. */
public final class MorphCommand implements TabExecutor {

    private final EmeraldPerksPlugin plugin;
    private final MorphManager morphs;

    public MorphCommand(EmeraldPerksPlugin plugin, MorphManager morphs) {
        this.plugin = plugin;
        this.morphs = morphs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can morph.", NamedTextColor.RED));
            return true;
        }

        if (command.getName().equalsIgnoreCase("morphview")
                || (args.length > 0 && args[0].equalsIgnoreCase("view"))) {
            boolean hidden = morphs.toggleSelfView(player);
            if (hidden) {
                player.sendMessage(Component.text(
                        "Your morph is now hidden from your own view — others still see it, and it "
                                + "won't get in the way of your clicks or aim anymore.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text(
                        "You can see your own morph again. Run /morphview to hide it if it blocks your aim.",
                        NamedTextColor.GREEN));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("unmorph")
                || (args.length > 0 && args[0].equalsIgnoreCase("off"))) {
            if (morphs.isMorphed(player)) {
                morphs.unmorph(player, false);
            } else {
                player.sendMessage(Component.text("You aren't morphed right now.", NamedTextColor.YELLOW));
            }
            return true;
        }

        Set<EntityType> allowed = allowedFor(player);
        if (allowed.isEmpty()) {
            player.sendMessage(Component.text(
                    "Morphing is a patron perk! Link your Patreon with /patreon link <email>.",
                    NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /" + label + " <mob>  —  or /unmorph to change back.",
                    NamedTextColor.YELLOW));
            if (allowed.size() == 1) {
                player.sendMessage(Component.text("You can morph into: villager", NamedTextColor.GRAY));
            } else {
                player.sendMessage(Component.text(
                        "You can morph into any mob except the ender dragon. Tab-complete to browse!",
                        NamedTextColor.GRAY));
            }
            return true;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown mob: " + args[0], NamedTextColor.RED));
            return true;
        }
        if (!allowed.contains(type)) {
            player.sendMessage(Component.text(
                    "You can't morph into a " + MorphManager.prettyName(type) + ".", NamedTextColor.RED));
            return true;
        }
        morphs.morph(player, type);
        return true;
    }

    private Set<EntityType> allowedFor(Player player) {
        if (player.hasPermission("emeraldperks.morph.all")) {
            return morphs.allowedTypes(Role.BOARD);
        }
        Role role = plugin.roleFor(player);
        EnumSet<EntityType> allowed = EnumSet.noneOf(EntityType.class);
        allowed.addAll(morphs.allowedTypes(role));
        return allowed;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("unmorph") || args.length != 1
                || !(sender instanceof Player player)) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        options.add("off");
        options.add("view");
        for (EntityType type : allowedFor(player)) {
            options.add(type.name().toLowerCase(Locale.ROOT));
        }
        return options.stream().filter(o -> o.startsWith(prefix)).sorted().toList();
    }
}
