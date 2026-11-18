package com.knmusic.otmitems;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Hiển thị thanh đếm ngược hồi chiêu trên Action Bar, chỉ khi người chơi
 * đang cầm đúng vũ khí có skill đang hồi chiêu. Khi đổi item hoặc hết hồi chiêu
 * thì xóa thanh ngay lập tức.
 */
final class CooldownBarTask implements Runnable {

    private final PluginConfig config;
    private final WeaponData data;
    private final Set<UUID> showing = new HashSet<>();

    CooldownBarTask(PluginConfig config, WeaponData data) {
        this.config = config;
        this.data = data;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Component bar = render(player, now);
            if (bar != null) {
                player.sendActionBar(bar);
                showing.add(id);
            } else if (showing.remove(id)) {
                player.sendActionBar(Component.empty());
            }
        }
    }

    private Component render(Player player, long now) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        String code = data.getSkillCode(hand);
        if (code == null) {
            return null;
        }

        SkillDefinition skill = config.getSkill(code);
        if (skill == null || skill.cooldownMillis() <= 0) {
            return null;
        }

        long remaining = data.getCooldownUntil(hand) - now;
        if (remaining <= 0) {
            return null;
        }

        int length = config.getCooldownBarLength();
        double ratio = Math.min(1.0, (double) remaining / skill.cooldownMillis());
        int filled = (int) Math.ceil(ratio * length);
        long seconds = (remaining + 999L) / 1000L;

        String bar = config.getBarFilledColor() + config.getBarFilledChar().repeat(filled)
                + config.getBarEmptyColor() + config.getBarEmptyChar().repeat(length - filled);

        return Text.color(Text.format(config.getBarFormat(),
                "skill", skill.name(), "bar", bar, "time", String.valueOf(seconds)));
    }

    /** Xóa thanh hồi chiêu của tất cả người chơi đang hiển thị. */
    void clear() {
        for (UUID id : showing) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendActionBar(Component.empty());
            }
        }
        showing.clear();
    }

    void forget(UUID id) {
        showing.remove(id);
    }
}
