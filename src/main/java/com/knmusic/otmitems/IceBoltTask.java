package com.knmusic.otmitems;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.UUID;

/**
 * Đạn băng bay theo hướng nhìn của người chơi, tối đa {@code range} block (1 block/tick).
 * Khi trúng thực thể, bề mặt không khí phía trước hoặc đến hết tầm, đạn nổ và
 * đóng băng mọi thực thể sống trong bán kính quanh điểm nổ.
 */
final class IceBoltTask extends BukkitRunnable {

    private static final double STEP = 0.5;
    private static final int STEPS_PER_TICK = 2;
    private static final double HIT_BOX = 0.6;

    private final OTMitems plugin;
    private final FreezeManager freezeManager;
    private final Player caster;
    private final SkillDefinition skill;
    private final Vector direction;
    private Location current;
    private double traveled;

    IceBoltTask(OTMitems plugin, FreezeManager freezeManager, Player caster, SkillDefinition skill) {
        this.plugin = plugin;
        this.freezeManager = freezeManager;
        this.caster = caster;
        this.skill = skill;

        Location eye = caster.getEyeLocation();
        this.direction = eye.getDirection().normalize();
        this.current = eye.add(direction.clone().multiply(0.5));
    }

    @Override
    public void run() {
        if (!caster.isOnline()) {
            cancel();
            return;
        }

        for (int i = 0; i < STEPS_PER_TICK; i++) {
            if (traveled >= skill.range()) {
                impact(current, null);
                cancel();
                return;
            }

            Location next = current.clone().add(direction.clone().multiply(STEP));
            if (!next.getBlock().isPassable()) {
                impact(current, null);
                cancel();
                return;
            }

            current = next;
            traveled += STEP;
            trail(current);

            LivingEntity hit = findTarget(current);
            if (hit != null) {
                impact(current, hit);
                cancel();
                return;
            }
        }
    }

    private void trail(Location loc) {
        loc.getWorld().spawnParticle(skill.particle(), loc, 3, 0.05, 0.05, 0.05, 0.0);
    }

    private LivingEntity findTarget(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, HIT_BOX, HIT_BOX, HIT_BOX)) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                return living;
            }
        }
        return null;
    }

    private boolean isValidTarget(LivingEntity entity) {
        return entity.isValid() && !entity.getUniqueId().equals(caster.getUniqueId());
    }

    private void impact(Location at, LivingEntity direct) {
        World world = at.getWorld();
        world.playSound(at, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.4f);
        world.spawnParticle(skill.particle(), at, 40, 0.6, 0.6, 0.6, 0.02);

        UUID directId = direct == null ? null : direct.getUniqueId();
        if (direct != null) {
            applyFreeze(direct);
        }

        double radius = skill.freezeRadius();
        for (Entity entity : world.getNearbyEntities(at, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || !isValidTarget(living)) {
                continue;
            }
            if (living.getUniqueId().equals(directId)) {
                continue;
            }
            if (living.getLocation().distance(at) <= radius) {
                applyFreeze(living);
            }
        }
    }

    private void applyFreeze(LivingEntity target) {
        freezeManager.freeze(target, skill.freezeTicks(), skill.slownessLevel());

        if (skill.damage() > 0) {
            target.damage(skill.damage(), caster);
        }

        if (target instanceof Player player) {
            String seconds = String.format(Locale.ROOT, "%.1f", skill.freezeTicks() / 20.0);
            player.sendMessage(Text.color(Text.format(skill.frozenMessage(),
                    "skill", skill.name(), "time", seconds)));
        }
    }
}
