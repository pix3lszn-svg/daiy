package dev.dailyemerald.morph;

import dev.dailyemerald.morph.command.AllowMorphCommand;
import dev.dailyemerald.morph.command.MorphCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EmeraldMorph — standalone morphing for the DailyEmerald server.
 *
 * /morph &lt;mob&gt;                      — become any mob
 * /morph villager [biome] [job]     — styled villagers (e.g. plains librarian)
 * /unmorph, /morphview              — change back / hide your own mob
 * /allowmorph [player]              — open morphing to everyone or one player
 * /unallowmorph [player]            — back to OP-only (reverts morphed players)
 */
public final class EmeraldMorphPlugin extends JavaPlugin {

    private MorphManager morphManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        AccessStore access = new AccessStore(this);
        morphManager = new MorphManager(this, access);

        MorphCommand morphCommand = new MorphCommand(morphManager, access);
        getCommand("morph").setExecutor(morphCommand);
        getCommand("morph").setTabCompleter(morphCommand);
        getCommand("unmorph").setExecutor(morphCommand);
        getCommand("morphview").setExecutor(morphCommand);

        AllowMorphCommand allowCommand = new AllowMorphCommand(access, morphManager);
        getCommand("allowmorph").setExecutor(allowCommand);
        getCommand("allowmorph").setTabCompleter(allowCommand);
        getCommand("unallowmorph").setExecutor(allowCommand);
        getCommand("unallowmorph").setTabCompleter(allowCommand);

        Bukkit.getPluginManager().registerEvents(new MorphListeners(morphManager), this);
        morphManager.start();

        getLogger().info("EmeraldMorph enabled. Morphing is "
                + (access.isAllowAll() ? "open to everyone" : "OP-only (open it with /allowmorph)") + ".");
    }

    @Override
    public void onDisable() {
        if (morphManager != null) morphManager.shutdown();
    }
}
