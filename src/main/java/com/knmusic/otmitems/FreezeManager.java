package com.knmusic.otmitems;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đóng băng (giữ chân) thực thể trong một khoảng thời gian.
 * - Người chơi/thực thể: Slowness mức cao (đứng yên hoàn toàn).
 * - Mob: thêm tắt AI để không đi lại hay tấn công, khôi phục sau khi hết thời gian.
 */
public final class FreezeManager {

    private final OTMitems plugin;
    private final Map<UUID, FrozenState> frozen = new HashMap<>();

    private record FrozenState(LivingEntity entity, boolean originalAI, BukkitTask task) {
    }

    public FreezeManager(OTMitems plugin) {
        this.plugin = plugin;
    }

    public void freeze(LivingEntity target, int ticks, int slownessLevel) {
        UUID id = target.getUniqueId();
        boolean originalAI = true;

        FrozenState previous = frozen.remove(id);
        if (previous != null) {
            previous.task().cancel();
            originalAI = previous.originalAI();
        } else if (target instanceof Mob mob) {
            originalAI = mob.hasAI();
        }

        if (target instanceof Mob mob) {
            mob.setAI(false);
        }

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, ticks, slownessLevel, false, false, true));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> thaw(id), ticks);
        frozen.put(id, new FrozenState(target, originalAI, task));
    }

    private void thaw(UUID id) {
        FrozenState state = frozen.remove(id);
        if (state == null) {
            return;
        }
        if (state.entity() instanceof Mob mob && mob.isValid()) {
            mob.setAI(state.originalAI());
        }
    }

    /** Gọi khi tắt plugin: hủy toàn bộ timer và trả lại AI cho các mob đang bị đóng băng. */
    public void thawAll() {
        for (FrozenState state : frozen.values()) {
            state.task().cancel();
            if (state.entity() instanceof Mob mob && mob.isValid()) {
                mob.setAI(state.originalAI());
            }
        }
        frozen.clear();
    }
}
