package com.knmusic.otmitems;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đóng băng thực thể trong một khoảng thời gian.
 * - Mọi thực thể: Slowness mức cao.
 * - Người chơi: khối băng kẹt ở ô chân, vị trí bị khóa cho đến khi băng tan.
 * - Mob: tắt AI, khôi phục sau khi hết thời gian.
 */
public final class FreezeManager {

    private final OTMitems plugin;
    private final IceBlocks iceBlocks;
    private final Map<UUID, FrozenState> frozen = new HashMap<>();

    private record FrozenState(LivingEntity entity, boolean originalAI, BukkitTask task,
                               Location lock, Block iceBlock) {
    }

    public FreezeManager(OTMitems plugin, IceBlocks iceBlocks) {
        this.plugin = plugin;
        this.iceBlocks = iceBlocks;
    }

    public void freeze(LivingEntity target, int ticks, int slownessLevel) {
        UUID id = target.getUniqueId();
        boolean originalAI = true;
        Location lock = null;
        Block iceBlock = null;

        FrozenState previous = frozen.remove(id);
        if (previous != null) {
            previous.task().cancel();
            originalAI = previous.originalAI();
            lock = previous.lock();
            iceBlock = previous.iceBlock();
        } else {
            if (target instanceof Mob mob) {
                originalAI = mob.hasAI();
            }
            if (target instanceof Player player) {
                lock = player.getLocation().clone();
                Block feet = player.getLocation().getBlock();
                if (iceBlocks.place(feet)) {
                    iceBlock = feet;
                }
            }
        }

        if (target instanceof Mob mob) {
            mob.setAI(false);
        }

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, ticks, slownessLevel, false, false, true));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> thaw(id), ticks);
        frozen.put(id, new FrozenState(target, originalAI, task, lock, iceBlock));
    }

    private void thaw(UUID id) {
        FrozenState state = frozen.remove(id);
        if (state != null) {
            release(state);
        }
    }

    private void release(FrozenState state) {
        if (state.iceBlock() != null) {
            iceBlocks.restore(state.iceBlock());
        }
        if (state.entity() instanceof Mob mob && mob.isValid()) {
            mob.setAI(state.originalAI());
        }
    }

    /** Gọi khi tắt plugin: hủy timer, trả băng và AI về trạng thái gốc. */
    public void thawAll() {
        for (FrozenState state : frozen.values()) {
            state.task().cancel();
            release(state);
        }
        frozen.clear();
    }

    /** Khóa vị trí người chơi đang bị đóng băng: vẫn xoay đầu được nhưng không đi, không nhảy. */
    public void handleMove(PlayerMoveEvent event) {
        FrozenState state = frozen.get(event.getPlayer().getUniqueId());
        if (state == null || state.lock() == null) {
            return;
        }
        Location to = event.getTo();
        Location lock = state.lock();
        if (to.getX() == lock.getX() && to.getY() == lock.getY() && to.getZ() == lock.getZ()) {
            return;
        }
        Location fixed = lock.clone();
        fixed.setYaw(to.getYaw());
        fixed.setPitch(to.getPitch());
        event.setTo(fixed);
    }
}
