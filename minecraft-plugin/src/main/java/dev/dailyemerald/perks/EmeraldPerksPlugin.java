package dev.dailyemerald.perks;

import dev.dailyemerald.perks.command.MorphCommand;
import dev.dailyemerald.perks.command.PatreonCommand;
import dev.dailyemerald.perks.morph.MorphListeners;
import dev.dailyemerald.perks.morph.MorphManager;
import dev.dailyemerald.perks.patreon.PatreonClient;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * EmeraldPerks — Patreon-powered perks for the DailyEmerald server.
 *
 *  Board Members  → /morph into any mob except the ender dragon, + goat horn
 *  Journalists    → /morph into a villager, + goat horn
 */
public final class EmeraldPerksPlugin extends JavaPlugin implements Listener {

    private PlayerDataStore store;
    private MorphManager morphManager;
    private PatreonSyncService syncService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        store = new PlayerDataStore(this);
        morphManager = new MorphManager(this, store);
        HornService horns = new HornService(config.getString("horn.variant", "RANDOM"));

        PatreonClient client = new PatreonClient(
                firstNonBlank(loadSavedToken("access-token"), config.getString("patreon.creator-access-token", "")),
                firstNonBlank(loadSavedToken("refresh-token"), config.getString("patreon.refresh-token", "")),
                config.getString("patreon.client-id", ""),
                config.getString("patreon.client-secret", ""),
                config.getString("patreon.campaign-id", ""),
                this::saveTokens);

        syncService = new PatreonSyncService(
                this, client, store, morphManager, horns,
                config.getStringList("roles.board.tier-names"),
                config.getStringList("roles.journalist.tier-names"),
                config.getBoolean("revoke-when-pledge-ends", true));

        PatreonCommand patreonCommand = new PatreonCommand(this, store, syncService);
        getCommand("patreon").setExecutor(patreonCommand);
        getCommand("patreon").setTabCompleter(patreonCommand);
        MorphCommand morphCommand = new MorphCommand(this, morphManager);
        getCommand("morph").setExecutor(morphCommand);
        getCommand("morph").setTabCompleter(morphCommand);
        getCommand("unmorph").setExecutor(morphCommand);
        getCommand("morphview").setExecutor(morphCommand);

        Bukkit.getPluginManager().registerEvents(new MorphListeners(morphManager), this);
        Bukkit.getPluginManager().registerEvents(new PriorityJoinListener(this, store), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        morphManager.start();

        if (client.isConfigured()) {
            long intervalTicks = Math.max(1, config.getInt("patreon.sync-interval-minutes", 10)) * 60L * 20L;
            // First sync shortly after startup, then on the configured interval.
            Bukkit.getScheduler().runTaskTimer(this, () -> syncService.sync(null), 100L, intervalTicks);
        } else {
            getLogger().warning("No Patreon creator access token set in config.yml — Patreon syncing is OFF.");
            getLogger().warning("You can still grant perks manually with /patreon set <player> <board|journalist>.");
        }

        getLogger().info("EmeraldPerks enabled. Board members morph freely; journalists get villager life.");
    }

    @Override
    public void onDisable() {
        if (morphManager != null) morphManager.shutdown();
        if (store != null) store.save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Delayed so we run after Essentials & co. restore gamemodes, and so
        // perks earned while offline (horn, pending links) land reliably.
        Bukkit.getScheduler().runTaskLater(this,
                () -> {
                    Player player = event.getPlayer();
                    if (!player.isOnline()) return;
                    completePendingLink(player);
                    syncService.giveHornIfOwed(player);
                    applyAdventureIfPatron(player);
                }, 40L);
    }

    /** Finishes an admin pre-link (/patreon link or /patreon grant) on the player's first join. */
    private void completePendingLink(Player player) {
        PlayerDataStore.Pending pending = store.takePending(player.getName());
        if (pending == null) return;
        if (pending.email() != null) {
            store.link(player.getUniqueId(), pending.email(), pending.role());
        } else {
            // Bulk/manual grant — no Patreon email behind it, so the sync
            // must never revoke it: mark it manual.
            store.setRole(player.getUniqueId(), pending.role(), true);
        }
        player.sendMessage(net.kyori.adventure.text.Component.text(
                        "Your Patreon was linked by an admin — welcome, ",
                        net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .append(net.kyori.adventure.text.Component.text(pending.role().displayName(),
                        net.kyori.adventure.text.format.NamedTextColor.GOLD))
                .append(net.kyori.adventure.text.Component.text("!",
                        net.kyori.adventure.text.format.NamedTextColor.GREEN)));
    }

    /** The player's current perk role (null = no perks). */
    public Role roleFor(Player player) {
        return store.getRole(player.getUniqueId());
    }

    public MorphManager morphManager() {
        return morphManager;
    }

    // ------------------------------------------------------------------
    // Whitelist + adventure-mode side effects of holding a patron role.
    // ------------------------------------------------------------------

    /** Valid Java and Floodgate/Bedrock-prefixed usernames; guards console dispatch. */
    private static final java.util.regex.Pattern SAFE_NAME =
            java.util.regex.Pattern.compile("^[\\w.*+-]{1,32}$");

    public boolean manageWhitelist() {
        return getConfig().getBoolean("whitelist.auto-manage", true);
    }

    public void whitelistAdd(String name) {
        if (!manageWhitelist() || name == null || !SAFE_NAME.matcher(name).matches()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + name);
    }

    public void whitelistRemove(String name) {
        if (!manageWhitelist() || name == null || !SAFE_NAME.matcher(name).matches()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + name);
    }

    /** Puts patrons (never admins/ops) into adventure mode when enabled. */
    public void applyAdventureIfPatron(Player player) {
        if (!getConfig().getBoolean("adventure.force-for-patrons", true)) return;
        if (player.isOp() || player.hasPermission("emeraldperks.admin")) return;
        if (store.getRole(player.getUniqueId()) != null
                && player.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        }
    }

    /** Everything that should happen when an online player gains a role. */
    public void applyRoleSideEffects(Player player) {
        whitelistAdd(player.getName());
        applyAdventureIfPatron(player);
    }

    // ------------------------------------------------------------------
    // Refreshed Patreon tokens are persisted to tokens.yml (not config.yml,
    // to avoid clobbering the user's comments).
    // ------------------------------------------------------------------

    private File tokensFile() {
        return new File(getDataFolder(), "tokens.yml");
    }

    private String loadSavedToken(String key) {
        File file = tokensFile();
        if (!file.exists()) return "";
        return YamlConfiguration.loadConfiguration(file).getString(key, "");
    }

    private void saveTokens(String accessToken, String refreshToken) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("access-token", accessToken);
        yaml.set("refresh-token", refreshToken);
        try {
            yaml.save(tokensFile());
            getLogger().info("Patreon access token refreshed and saved.");
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save refreshed Patreon tokens", e);
        }
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : (b == null ? "" : b);
    }
}
