package dev.dailyemerald.perks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Hands out the patron goat horn. */
public final class HornService {

    private static final java.util.Map<String, MusicInstrument> VARIANTS = java.util.Map.of(
            "PONDER", MusicInstrument.PONDER_GOAT_HORN,
            "SING", MusicInstrument.SING_GOAT_HORN,
            "SEEK", MusicInstrument.SEEK_GOAT_HORN,
            "FEEL", MusicInstrument.FEEL_GOAT_HORN,
            "ADMIRE", MusicInstrument.ADMIRE_GOAT_HORN,
            "CALL", MusicInstrument.CALL_GOAT_HORN,
            "YEARN", MusicInstrument.YEARN_GOAT_HORN,
            "DREAM", MusicInstrument.DREAM_GOAT_HORN);

    private final String configuredVariant;

    public HornService(String configuredVariant) {
        this.configuredVariant = configuredVariant == null ? "RANDOM" : configuredVariant.trim().toUpperCase(Locale.ROOT);
    }

    public void give(Player player) {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        MusicInstrumentMeta meta = (MusicInstrumentMeta) horn.getItemMeta();
        meta.setInstrument(pickVariant());
        meta.displayName(Component.text("Patron Horn", NamedTextColor.GOLD));
        meta.lore(List.of(Component.text("Thanks for supporting the server!", NamedTextColor.GRAY)));
        horn.setItemMeta(meta);

        var leftovers = player.getInventory().addItem(horn);
        // Inventory full — drop it at their feet instead of eating it.
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.sendMessage(Component.text("You received your patron goat horn! 🐐", NamedTextColor.GOLD));
    }

    private MusicInstrument pickVariant() {
        MusicInstrument configured = VARIANTS.get(configuredVariant);
        if (configured != null) return configured;
        List<MusicInstrument> all = List.copyOf(VARIANTS.values());
        return all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }
}
