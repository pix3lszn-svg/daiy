package dev.dailyemerald.morph.command;

import dev.dailyemerald.morph.MorphManager;
import dev.dailyemerald.morph.MorphManager.MorphTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared parsing + tab completion for "&lt;mob&gt; [biome] [job]" morph arguments,
 * used by /morph, /morphall and /morphplayer so they all behave identically.
 */
final class MorphArgs {

    private MorphArgs() {
    }

    /**
     * Parses a morph target starting at args[mobIndex]. On any problem the
     * sender is messaged and null is returned.
     */
    static MorphTarget parse(CommandSender sender, MorphManager morphs, String[] args, int mobIndex) {
        EntityType type;
        try {
            type = EntityType.valueOf(args[mobIndex].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Unknown mob: " + args[mobIndex], NamedTextColor.RED));
            return null;
        }
        if (!morphs.allowedTypes().contains(type)) {
            sender.sendMessage(Component.text(
                    "You can't morph into a " + args[mobIndex].toLowerCase(Locale.ROOT).replace('_', ' ') + ".",
                    NamedTextColor.RED));
            return null;
        }
        if (type != EntityType.VILLAGER) {
            if (args.length > mobIndex + 1) {
                sender.sendMessage(Component.text(
                        "Only villagers take extra styles — using a plain "
                                + args[mobIndex].toLowerCase(Locale.ROOT).replace('_', ' ') + ".",
                        NamedTextColor.YELLOW));
            }
            return MorphTarget.mob(type);
        }
        return parseVillager(sender, args, mobIndex);
    }

    /** Parses "villager [biome] [job]" — both optional, either order. */
    private static MorphTarget parseVillager(CommandSender sender, String[] args, int mobIndex) {
        Villager.Type biome = null;
        Villager.Profession job = null;
        for (int i = mobIndex + 1; i < args.length && i <= mobIndex + 2; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            Villager.Type asBiome = lookupBiome(arg);
            Villager.Profession asJob = lookupJob(arg);
            if (asBiome != null && biome == null) {
                biome = asBiome;
            } else if (asJob != null && job == null) {
                job = asJob;
            } else if (asBiome != null) {
                sender.sendMessage(Component.text("You gave two biomes (\"" + args[i]
                        + "\" is also a biome). The structure is villager [biome] [job].",
                        NamedTextColor.RED));
                return null;
            } else if (asJob != null) {
                sender.sendMessage(Component.text("You gave two jobs (\"" + args[i]
                        + "\" is also a job). The structure is villager [biome] [job].",
                        NamedTextColor.RED));
                return null;
            } else {
                sender.sendMessage(Component.text("Unknown villager style: " + args[i], NamedTextColor.RED));
                sender.sendMessage(Component.text("Biomes: " + String.join(", ", biomeNames()),
                        NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Jobs: " + String.join(", ", jobNames()),
                        NamedTextColor.GRAY));
                return null;
            }
        }
        return new MorphTarget(EntityType.VILLAGER, biome, job);
    }

    /**
     * Tab completion for the "&lt;mob&gt; [biome] [job]" part. argIndex is the
     * position currently being typed; mobIndex is where the mob name lives.
     */
    static List<String> complete(MorphManager morphs, String[] args, int argIndex, int mobIndex) {
        if (argIndex == mobIndex) {
            List<String> options = new ArrayList<>();
            for (EntityType type : morphs.allowedTypes()) {
                options.add(type.name().toLowerCase(Locale.ROOT));
            }
            return filter(options, args[argIndex]);
        }
        if (!args[mobIndex].equalsIgnoreCase("villager")) {
            return List.of();
        }
        if (argIndex == mobIndex + 1) {
            List<String> options = new ArrayList<>(biomeNames());
            options.addAll(jobNames());
            return filter(options, args[argIndex]);
        }
        if (argIndex == mobIndex + 2) {
            // Second modifier completes the kind the first one wasn't.
            boolean firstWasBiome = lookupBiome(args[mobIndex + 1].toLowerCase(Locale.ROOT)) != null;
            return filter(firstWasBiome ? jobNames() : biomeNames(), args[argIndex]);
        }
        return List.of();
    }

    static Villager.Type lookupBiome(String name) {
        NamespacedKey key = NamespacedKey.fromString("minecraft:" + name);
        return key == null ? null : Registry.VILLAGER_TYPE.get(key);
    }

    static Villager.Profession lookupJob(String name) {
        NamespacedKey key = NamespacedKey.fromString("minecraft:" + name);
        return key == null ? null : Registry.VILLAGER_PROFESSION.get(key);
    }

    static List<String> biomeNames() {
        return Registry.VILLAGER_TYPE.stream().map(t -> t.getKey().getKey()).sorted().toList();
    }

    static List<String> jobNames() {
        return Registry.VILLAGER_PROFESSION.stream().map(p -> p.getKey().getKey()).sorted().toList();
    }

    static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(lower)).sorted().toList();
    }
}
