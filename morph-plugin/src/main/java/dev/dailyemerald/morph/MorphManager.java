package dev.dailyemerald.morph;

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
import org.bukkit.entity.Villager;
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
 * every tick. Real entities render correctly for Java AND Bedrock (Geyser)
 * viewers. Villager morphs can carry a biome type and a profession.
 */
public final class MorphManager {

    /** What a player is morphed into, including villager styling. */
    public record MorphTarget(EntityType type, Villager.Type villagerType,
                              Villager.Profession villagerProfession) {

        public static MorphTarget mob(EntityType type) {
            return new MorphTarget(type, null, null);
        }

        public String describe() {
            if (type != EntityType.VILLAGER) {
                return type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            }
            StringBuilder sb = new StringBuilder();
            if (villagerType != null) {
                sb.append(villagerType.getKey().getKey()).append(' ');
            }
            if (villagerProfession != null
                    && !"none".equals(villagerProfession.getKey().getKey())) {
                sb.append(villagerProfession.getKey().getKey()).append(' ');
            }
            return sb.append("villager").toString();
        }
    }

    private final JavaPlugin plugin;
    private final AccessStore store;
    private final NamespacedKey morphKey;
    private final Map<UUID, LivingEntity> morphs = new ConcurrentHashMap<>();
    private final Map<UUID, MorphTarget> targets = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public MorphManager(JavaPlugin plugin, AccessStore store) {
        this.plugin = plugin;
        this.store = store;
        this.morphKey = new NamespacedKey(plugin, "morph-owner");
    }

    /** All mob types a player may morph into (spawnable real mobs, minus config blocks). */
    public Set<EntityType> allowedTypes() {
        EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);
        Set<String> blocked = Set.copyOf(
                plugin.getConfig().getStringList("blocked-mobs").stream()
                        .map(s -> s.toUpperCase(Locale.ROOT)).toList());
        for (EntityType type : EntityType.values()) {
            Class<? extends Entity> clazz = type.getEntityClass();
            if (clazz != null && Mob.class.isAssignableFrom(clazz)
                    && type.isSpawnable()
                    // The ender dragon ignores setAI(false) and runs its hardcoded
                    // flight/attack logic, so it can never be a well-behaved morph.
                    && type != EntityType.ENDER_DRAGON
                    && !blocked.contains(type.name())) {
                types.add(type);
            }
        }
        return Set.copyOf(types);
    }

    public boolean isMorphEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(morphKey, PersistentDataType.STRING);
    }

    public boolean isMorphed(Player player) {
        return morphs.containsKey(player.getUniqueId());
    }

    public void morph(Player player, MorphTarget target) {
        unmorph(player, true);
        LivingEntity entity = spawnMorphEntity(player, target);
        if (entity == null) {
            player.sendMessage(Component.text("Sorry, that mob can't be morphed into.", NamedTextColor.RED));
            return;
        }
        morphs.put(player.getUniqueId(), entity);
        targets.put(player.getUniqueId(), target);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, true, false, false));
        hideTeam().addEntry(player.getName());
        player.sendMessage(Component.text("You morphed into a ", NamedTextColor.GREEN)
                .append(Component.text(target.describe(), NamedTextColor.GOLD))
                .append(Component.text("! Use /unmorph to change back.", NamedTextColor.GREEN)));
    }

    private LivingEntity spawnMorphEntity(Player player, MorphTarget target) {
        Entity spawned = player.getWorld().spawnEntity(
                player.getLocation(), target.type(), CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
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
                    if (entity instanceof Villager villager) {
                        if (target.villagerType() != null) {
                            villager.setVillagerType(target.villagerType());
                        }
                        if (target.villagerProfession() != null) {
                            villager.setProfession(target.villagerProfession());
                            // A levelled badge reads better than the plain lvl-1 look.
                            villager.setVillagerLevel(1);
                        }
                    }
                });
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            return null;
        }
        // Respect the player's self-view preference: hide the follower mob from
        // their own client so it doesn't intercept their clicks/attacks.
        if (store.isSelfHidden(player.getUniqueId())) {
            player.hideEntity(plugin, living);
        }
        return living;
    }

    public void unmorph(Player player, boolean silent) {
        LivingEntity entity = morphs.remove(player.getUniqueId());
        targets.remove(player.getUniqueId());
        if (entity != null) {
            entity.remove();
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            hideTeam().removeEntry(player.getName());
            if (!silent) {
                player.sendMessage(Component.text("You changed back to yourself.", NamedTextColor.GREEN));
            }
        }
    }

    /** Unmorphs every non-OP player (used by /unallowmorph). Returns how many. */
    public int unmorphAllNonOps() {
        int count = 0;
        for (UUID id : Set.copyOf(morphs.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && !player.isOp()) {
                unmorph(player, false);
                count++;
            }
        }
        return count;
    }

    /**
     * Toggles whether the player sees their own morph mob. Hidden = invisible
     * to them only, so it stops eating their clicks. Returns true if now hidden.
     */
    public boolean toggleSelfView(Player player) {
        boolean nowHidden = !store.isSelfHidden(player.getUniqueId());
        store.setSelfHidden(player.getUniqueId(), nowHidden);
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
                targets.remove(entry.getKey());
                continue;
            }
            if (!entity.isValid()) {
                // Entity got removed out from under us (e.g. chunk shuffle) — respawn it.
                MorphTarget target = targets.get(entry.getKey());
                LivingEntity fresh = target == null ? null : spawnMorphEntity(owner, target);
                if (fresh != null && fresh.isValid()) {
                    morphs.put(entry.getKey(), fresh);
                } else {
                    // Couldn't respawn here (e.g. a region plugin blocks mob spawns) —
                    // end the morph cleanly instead of retrying every tick forever.
                    if (fresh != null) fresh.remove();
                    unmorph(owner, false);
                }
                continue;
            }
            Location target = owner.getLocation();
            if (!entity.getWorld().equals(target.getWorld())
                    || entity.getLocation().distanceSquared(target) > 0.0001
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
                targets.remove(id);
            }
        }
    }

    /** Team that hides the (invisible) player's floating name tag; the mob carries the name instead. */
    private Team hideTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("em_morphed");
        if (team == null) {
            team = board.registerNewTeam("em_morphed");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }
}
