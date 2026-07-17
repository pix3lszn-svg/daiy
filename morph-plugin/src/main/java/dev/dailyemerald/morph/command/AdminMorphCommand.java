package dev.dailyemerald.morph.command;

import dev.dailyemerald.morph.MorphManager;
import dev.dailyemerald.morph.MorphManager.MorphTarget;
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

/**
 * Operator tools that force morphs regardless of the /allowmorph state:
 *
 * /morphall &lt;mob&gt; [biome] [job]   — morph every online player
 * /morphall off                    — unmorph every online player
 * /morphplayer &lt;name&gt; &lt;mob&gt; [biome] [job] — morph one player
 * /morphplayer &lt;name&gt; off          — unmorph one player
 */
public final class AdminMorphCommand implements TabExecutor {

    private final MorphManager morphs;

    public AdminMorphCommand(MorphManager morphs) {
        this.morphs = morphs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("morphall")) {
            morphAll(sender, label, args);
        } else {
            morphPlayer(sender, label, args);
        }
        return true;
    }

    private void morphAll(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + label + " <mob> [biome] [job]  —  or /" + label
                    + " off to change everyone back.", NamedTextColor.YELLOW));
            return;
        }
        if (args[0].equalsIgnoreCase("off")) {
            int count = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (morphs.isMorphed(online)) {
                    morphs.unmorph(online, false);
                    count++;
                }
            }
            sender.sendMessage(Component.text("Unmorphed " + count + " player(s).", NamedTextColor.GREEN));
            return;
        }
        MorphTarget target = MorphArgs.parse(sender, morphs, args, 0);
        if (target == null) return;
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            morphs.morph(online, target);
            count++;
        }
        sender.sendMessage(Component.text("Morphed " + count + " player(s) into a ", NamedTextColor.GREEN)
                .append(Component.text(target.describe(), NamedTextColor.GOLD))
                .append(Component.text(".", NamedTextColor.GREEN)));
    }

    private void morphPlayer(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " <player> <mob> [biome] [job]  —  or /"
                    + label + " <player> off.", NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player " + args[0] + " must be online.", NamedTextColor.RED));
            return;
        }
        if (args[1].equalsIgnoreCase("off")) {
            if (morphs.isMorphed(target)) {
                morphs.unmorph(target, false);
                sender.sendMessage(Component.text("Changed " + target.getName() + " back to normal.",
                        NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text(target.getName() + " isn't morphed.", NamedTextColor.YELLOW));
            }
            return;
        }
        MorphTarget morphTarget = MorphArgs.parse(sender, morphs, args, 1);
        if (morphTarget == null) return;
        morphs.morph(target, morphTarget);
        sender.sendMessage(Component.text("Morphed " + target.getName() + " into a ", NamedTextColor.GREEN)
                .append(Component.text(morphTarget.describe(), NamedTextColor.GOLD))
                .append(Component.text(".", NamedTextColor.GREEN)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean all = command.getName().equalsIgnoreCase("morphall");
        int mobIndex = all ? 0 : 1;
        if (args.length == 0) return List.of();

        if (!all && args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .toList();
        }
        if (args.length - 1 == mobIndex) {
            List<String> options = new ArrayList<>(List.of("off"));
            options.addAll(MorphArgs.complete(morphs, args, mobIndex, mobIndex));
            return MorphArgs.filter(options, args[mobIndex]);
        }
        return MorphArgs.complete(morphs, args, args.length - 1, mobIndex);
    }
}
