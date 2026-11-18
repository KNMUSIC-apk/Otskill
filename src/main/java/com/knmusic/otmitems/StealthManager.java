package com.knmusic.otmitems;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chế độ tàng hình: vô hình và tăng tốc trong thời gian nhất định.
 * Vũ khí được cộng sát thương trong lúc tàng hình. Mất tàng hình khi tấn công hoặc bị đánh.
 */
public final class StealthManager {

    private final OTMitems plugin;
    private final PluginConfig config;
    private final Map<UUID, Active> active = new HashMap<>();

    private record Active(SkillDefinition.Stealth settings, BukkitTask endTask) {
    }

    public StealthManager(OTMitems plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void activate(Player player, SkillDefinition skill) {
        SkillDefinition.Stealth settings = skill.stealth();
        if (settings == null) {
            return;
        }

        UUID id = player.getUniqueId();
        Active previous = active.remove(id);
        if (previous != null) {
            previous.endTask().cancel();
        }

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, settings.durationTicks(), 0, false, false, true));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, settings.durationTicks(), settings.speedLevel(), false, false, true));

        BukkitTask endTask = Bukkit.getScheduler().runTaskLater(
                plugin, () -> finish(id, "hết thời gian"), settings.durationTicks());
        active.put(id, new Active(settings, endTask));

        Location at = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.POOF, at, 30, 0.4, 0.8, 0.4, 0.02);
        player.getWorld().playSound(at, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        player.sendMessage(config.color(settings.enterMessage()));
    }

    /** Kết thúc tàng hình: gỡ hiệu ứng, hiệu ứng hình ảnh và báo lý do. */
    public void finish(UUID id, String reason) {
        Active state = active.remove(id);
        if (state == null) {
            return;
        }
        state.endTask().cancel();

        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return;
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);

        Location at = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.CLOUD, at, 25, 0.4, 0.8, 0.4, 0.02);
        player.getWorld().playSound(at, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        player.sendMessage(config.color(state.settings().exitMessage(), "reason", reason));
    }

    public boolean isStealthed(UUID id) {
        return active.containsKey(id);
    }

    /** Sát thương cộng thêm hiện tại của người đang tàng hình, 0 nếu không tàng hình. */
    public double bonusDamage(UUID id) {
        Active state = active.get(id);
        return state == null ? 0.0 : state.settings().bonusDamage();
    }

    /** Dọn trạng thái khi người chơi thoát hoặc chết, không gửi thông báo. */
    public void forget(UUID id) {
        Active state = active.remove(id);
        if (state == null) {
            return;
        }
        state.endTask().cancel();
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    /** Dùng khi tắt plugin: gỡ tàng hình của tất cả người chơi đang bật. */
    public void clearAll() {
        for (UUID id : new ArrayList<>(active.keySet())) {
            finish(id, "plugin tắt");
        }
    }
}
