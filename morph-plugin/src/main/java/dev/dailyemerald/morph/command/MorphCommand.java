package dev.dailyemerald.morph.command;

import dev.dailyemerald.morph.AccessStore;
import dev.dailyemerald.morph.MorphManager;
import dev.dailyemerald.morph.MorphManager.MorphTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        EntityType type;
        try {
            type = EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown mob: " + args[0], NamedTextColor.RED));
            return true;
        }
        if (!morphs.allowedTypes().contains(type)) {
            player.sendMessage(Component.text(
                    "You can't morph into a " + args[0].toLowerCase(Locale.ROOT).replace('_', ' ') + ".",
                    NamedTextColor.RED));
            return true;
        }

        MorphTarget target;
        if (type == EntityType.VILLAGER) {
            target = parseVillager(player, args);
            if (target == null) return true; // parseVillager already messaged them
        } else {
            if (args.length > 1) {
                player.sendMessage(Component.text(
                        "Only villagers take extra styles — morphing you into a plain "
                                + args[0].toLowerCase(Locale.ROOT).replace('_', ' ') + ".",
                        NamedTextColor.YELLOW));
            }
            target = MorphTarget.mob(type);
        }
        morphs.morph(player, target);
        return true;
    }

    /**
     * Parses "/morph villager [biome] [job]". Both modifiers are optional, and
     * we're lenient: a job given where a biome was expected still works.
     */
    private MorphTarget parseVillager(Player player, String[] args) {
        Villager.Type biome = null;
        Villager.Profession job = null;
        for (int i = 1; i < args.length && i <= 2; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            Villager.Type asBiome = lookupBiome(arg);
            Villager.Profession asJob = lookupJob(arg);
            if (asBiome != null && biome == null) {
                biome = asBiome;
            } else if (asJob != null && job == null) {
                job = asJob;
            } else if (asBiome != null) {
                player.sendMessage(Component.text("You gave two biomes (\"" + args[i]
                        + "\" is also a biome). The structure is /morph villager [biome] [job].",
                        NamedTextColor.RED));
                return null;
            } else if (asJob != null) {
                player.sendMessage(Component.text("You gave two jobs (\"" + args[i]
                        + "\" is also a job). The structure is /morph villager [biome] [job].",
                        NamedTextColor.RED));
                return null;
            } else {
                player.sendMessage(Component.text("Unknown villager style: " + args[i], NamedTextColor.RED));
                player.sendMessage(Component.text("Biomes: " + String.join(", ", biomeNames()),
                        NamedTextColor.GRAY));
                player.sendMessage(Component.text("Jobs: " + String.join(", ", jobNames()),
                        NamedTextColor.GRAY));
                return null;
            }
        }
        return new MorphTarget(EntityType.VILLAGER, biome, job);
    }

    private static Villager.Type lookupBiome(String name) {
        NamespacedKey key = NamespacedKey.fromString("minecraft:" + name);
        return key == null ? null : Registry.VILLAGER_TYPE.get(key);
    }

    private static Villager.Profession lookupJob(String name) {
        NamespacedKey key = NamespacedKey.fromString("minecraft:" + name);
        return key == null ? null : Registry.VILLAGER_PROFESSION.get(key);
    }

    private static List<String> biomeNames() {
        return Registry.VILLAGER_TYPE.stream().map(t -> t.getKey().getKey()).sorted().toList();
    }

    private static List<String> jobNames() {
        return Registry.VILLAGER_PROFESSION.stream().map(p -> p.getKey().getKey()).sorted().toList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("unmorph")
                || command.getName().equalsIgnoreCase("morphview")
                || !(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("off", "view"));
            if (access.canMorph(player)) {
                for (EntityType type : morphs.allowedTypes()) {
                    options.add(type.name().toLowerCase(Locale.ROOT));
                }
            }
            return filter(options, args[0]);
        }
        if (args[0].equalsIgnoreCase("villager") && access.canMorph(player)) {
            if (args.length == 2) {
                List<String> options = new ArrayList<>(biomeNames());
                options.addAll(jobNames());
                return filter(options, args[1]);
            }
            if (args.length == 3) {
                // Second modifier completes the kind the first one wasn't.
                boolean firstWasBiome = lookupBiome(args[1].toLowerCase(Locale.ROOT)) != null;
                return filter(firstWasBiome ? jobNames() : biomeNames(), args[2]);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(lower)).sorted().toList();
    }
}
