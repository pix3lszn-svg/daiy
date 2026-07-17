package dev.dailyemerald.morph.command;

import dev.dailyemerald.morph.AccessStore;
import dev.dailyemerald.morph.MorphManager;
import dev.dailyemerald.morph.MorphManager.MorphTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /morph &lt;mob&gt;, /morph villager [biome] [job], /morph off,
 * /unmorph, and /morphview.
 */
public final class MorphCommand implements TabExecutor {

    private final MorphManager morphs;
    private final AccessStore access;

    public MorphCommand(MorphManager morphs, AccessStore access) {
        this.morphs = morphs;
        this.access = access;
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
            player.sendMessage(Component.text(hidden
                            ? "Your morph is now hidden from your own view — others still see it, and it "
                                    + "won't get in the way of your clicks or aim anymore."
                            : "You can see your own morph again. Run /morphview to hide it if it blocks your aim.",
                    NamedTextColor.GREEN));
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

        if (!access.canMorph(player)) {
            player.sendMessage(Component.text(
                    "Morphing isn't enabled for you right now — an operator can open it up with /allowmorph.",
                    NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /" + label + " <mob>  —  or /unmorph to change back.",
                    NamedTextColor.YELLOW));
            player.sendMessage(Component.text(
                    "Villagers take extra styles: /" + label + " villager [biome] [job] "
                            + "(e.g. /" + label + " villager plains librarian). Tab-complete to browse!",
                    NamedTextColor.GRAY));
            return true;
        }

        MorphTarget target = MorphArgs.parse(player, morphs, args, 0);
        if (target != null) {
            morphs.morph(player, target);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("unmorph")
                || command.getName().equalsIgnoreCase("morphview")
                || args.length == 0
                || !(sender instanceof Player player)) {
            return List.of();
        }
        if (!access.canMorph(player)) {
            return args.length == 1 ? MorphArgs.filter(List.of("off", "view"), args[0]) : List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("off", "view"));
            options.addAll(MorphArgs.complete(morphs, args, 0, 0));
            return MorphArgs.filter(options, args[0]);
        }
        return MorphArgs.complete(morphs, args, args.length - 1, 0);
    }
}
