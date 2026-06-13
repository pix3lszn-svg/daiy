package dev.dailyemerald.perks.morph;

import dev.dailyemerald.perks.PlayerDataStore;
import dev.dailyemerald.perks.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Self-contained morphing with no plugin dependencies: the player turns
 * invisible and a real (AI-less, invulnerable) mob mirrors their position
 * every tick. Because the mob is a real entity, it renders correctly for
 * Java AND Bedrock (Geyser) viewers.
 */
public final class MorphManager {

    /** Entity types board members may morph into: every real mob except the ender dragon. */
    private static final Set<EntityType> BOARD_TYPES = buildBoardTypes();

    private final JavaPlugin plugin;
    private final PlayerDataStore store;
    private final NamespacedKey morphKey;
    private final Map<UUID, LivingEntity> morphs = new ConcurrentHashMap<>();
    private final Map<UUID, EntityType> morphTypes = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public MorphManager(JavaPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
        this.morphKey = new NamespacedKey(plugin, "morph-owner");
    }

    private static Set<EntityType> buildBoardTypes() {
        EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);
        for (EntityType type : EntityType.values()) {
            Class<? extends Entity> clazz = type.getEntityClass();
            if (clazz != null && Mob.class.isAssignableFrom(clazz)
                    && type.isSpawnable()
                    && type != EntityType.ENDER_DRAGON) {
                types.add(type);
            }
        }
        return Set.copyOf(types);
    }

    public Set<EntityType> allowedTypes(Role role) {
        if (role == Role.BOARD) return BOARD_TYPES;
        if (role == Role.JOURNALIST) return Set.of(EntityType.VILLAGER);
        return Set.of();
    }

    public boolean isMorphEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(morphKey, PersistentDataType.STRING);
    }

    public boolean isMorphed(Player player) {
        return morphs.containsKey(player.getUniqueId());
    }

    public EntityType currentMorph(Player player) {
        return morphTypes.get(player.getUniqueId());
    }

    public void morph(Player player, EntityType type) {
        unmorph(player, true);
        LivingEntity entity = spawnMorphEntity(player, type);
        if (entity == null) {
            player.sendMessage(Component.text("Sorry, that mob can't be morphed into.", NamedTextColor.RED));
            return;
        }
        morphs.put(player.getUniqueId(), entity);
        morphTypes.put(player.getUniqueId(), type);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, true, false, false));
        hideTeam().addEntry(player.getName());
        player.sendMessage(Component.text("You morphed into a ", NamedTextColor.GREEN)
                .append(Component.text(prettyName(type), NamedTextColor.GOLD))
                .append(Component.text("! Use /unmorph to change back.", NamedTextColor.GREEN)));
    }

    private LivingEntity spawnMorphEntity(Player player, EntityType type) {
        Entity spawned = player.getWorld().spawnEntity(
                player.getLocation(), type, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    entity.getPersistentDataContainer().set(
                            morphKey, PersistentDataType.STRING, player.getUniqueId().toString());
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    entity.setPersistent(false);
                    entity.customName(player.displayName());
                    entity.setCustomNameVisible(true);
                    if (entity instanceof LivingEntity living) {
                        living.setAI(false);
                        living.setCollidable(false);
                        living.setRemoveWhenFarAway(false);
                        living.setCanPickupItems(false);
                    }
                });
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            return null;
        }
        // Respect the player's self-view preference: hide the follower mob from
        // their own client so it doesn't intercept their clicks/attacks.
        if (store.isMorphSelfHidden(player.getUniqueId())) {
            player.hideEntity(plugin, living);
        }
        return living;
    }

    /**
     * Toggles whether the player sees their own morph mob. Hidden = the mob is
     * invisible to them only (others still see it) so it stops eating their
     * clicks. Returns true if the mob is now hidden from the player.
     */
    public boolean toggleSelfView(Player player) {
        boolean nowHidden = !store.isMorphSelfHidden(player.getUniqueId());
        store.setMorphSelfHidden(player.getUniqueId(), nowHidden);
        LivingEntity entity = morphs.get(player.getUniqueId());
        if (entity != null) {
            if (nowHidden) {
                player.hideEntity(plugin, entity);
            } else {
                player.showEntity(plugin, entity);
            }
        }
        return nowHidden;
    }

    public void unmorph(Player player, boolean silent) {
        LivingEntity entity = morphs.remove(player.getUniqueId());
        morphTypes.remove(player.getUniqueId());
        if (entity != null) {
            entity.remove();
        }
        // Only strip state if they were actually morphed.
        if (entity != null) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            hideTeam().removeEntry(player.getName());
            if (!silent) {
                player.sendMessage(Component.text("You changed back to yourself.", NamedTextColor.GREEN));
            }
        }
    }

    /** Called when a player's role is revoked or removed. */
    public void unmorphIfNoLongerAllowed(Player player, Role role) {
        EntityType current = currentMorph(player);
        if (current != null && !allowedTypes(role).contains(current)
                && !player.hasPermission("emeraldperks.morph.all")) {
            unmorph(player, false);
        }
    }

    public void start() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        for (Map.Entry<UUID, LivingEntity> entry : morphs.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            LivingEntity entity = entry.getValue();
            if (owner == null || !owner.isOnline()) {
                entity.remove();
                morphs.remove(entry.getKey());
                morphTypes.remove(entry.getKey());
                continue;
            }
            if (!entity.isValid()) {
                // Entity got removed out from under us (e.g. chunk shuffle) — respawn it.
                EntityType type = morphTypes.get(entry.getKey());
                LivingEntity fresh = type == null ? null : spawnMorphEntity(owner, type);
                if (fresh != null) {
                    morphs.put(entry.getKey(), fresh);
                }
                continue;
            }
            Location target = owner.getLocation();
            if (!entity.getWorld().equals(target.getWorld()) || entity.getLocation().distanceSquared(target) > 0.0001
                    || entity.getLocation().getYaw() != target.getYaw()) {
                entity.teleport(target);
            }
        }
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        for (UUID id : Set.copyOf(morphs.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                unmorph(player, true);
            } else {
                LivingEntity entity = morphs.remove(id);
                if (entity != null) entity.remove();
                morphTypes.remove(id);
            }
        }
    }

    /** Team that hides the (invisible) player's floating name tag; the mob carries the name instead. */
    private Team hideTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("ep_morphed");
        if (team == null) {
            team = board.registerNewTeam("ep_morphed");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }

    public static String prettyName(EntityType type) {
        return type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
