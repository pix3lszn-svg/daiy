package dev.dailyemerald.morph;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Keeps morphs consistent and the morph entities untouchable. */
public final class MorphListeners implements Listener {

    private final MorphManager morphs;

    public MorphListeners(MorphManager morphs) {
        this.morphs = morphs;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        morphs.unmorph(event.getPlayer(), true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        morphs.unmorph(event.getPlayer(), true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Simpler and safer than migrating the mob across worlds; players can re-morph.
        morphs.unmorph(event.getPlayer(), true);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            morphs.unmorph(event.getPlayer(), true);
        }
    }

    // Nobody should interact with the morph entity (e.g. open a villager
    // trade window on a morphed player).
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (morphs.isMorphEntity(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (morphs.isMorphEntity(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() != null && morphs.isMorphEntity(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (morphs.isMorphEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
