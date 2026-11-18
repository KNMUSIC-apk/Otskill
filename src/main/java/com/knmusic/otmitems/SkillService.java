package com.knmusic.otmitems;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Xử lý khi người chơi kích hoạt skill bằng chuột phải: kiểm tra cooldown, đặt lại cooldown trong PDC
 * và phóng đạn băng.
 */
public final class SkillService {

    /** Bỏ qua các lần click quá sát nhau (chống spam click, cũng chặn click kép từ 2 tay). */
    private static final long CLICK_DEBOUNCE_MS = 100L;
    /** Khoảng cách tối thiểu giữa 2 thông báo "đang hồi chiêu". */
    private static final long COOLDOWN_NOTICE_MS = 1000L;

    private final OTMitems plugin;
    private final PluginConfig config;
    private final WeaponData data;
    private final FreezeManager freezeManager;

    private final Map<UUID, Long> lastClick = new HashMap<>();
    private final Map<UUID, Long> lastCooldownNotice = new HashMap<>();

    public SkillService(OTMitems plugin, PluginConfig config, WeaponData data, FreezeManager freezeManager) {
        this.plugin = plugin;
        this.config = config;
        this.data = data;
        this.freezeManager = freezeManager;
    }

    /**
     * Thử kích hoạt skill trên vật phẩm đang cầm ở tay chính.
     *
     * @return true nếu vật phẩm có skill (listener dùng để chặn thao tác mặc định của chuột phải)
     */
    public boolean tryUse(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        String code = data.getSkillCode(hand);
        if (code == null) {
            return false;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long previousClick = lastClick.put(id, now);
        if (previousClick != null && now - previousClick < CLICK_DEBOUNCE_MS) {
            return true;
        }

        SkillDefinition skill = config.getSkill(code);
        if (skill == null) {
            player.sendMessage(config.message("skill-unknown-on-item", "code", code));
            return true;
        }

        long until = data.getCooldownUntil(hand);
        if (now < until) {
            Long lastNotice = lastCooldownNotice.get(id);
            if (lastNotice == null || now - lastNotice >= COOLDOWN_NOTICE_MS) {
                lastCooldownNotice.put(id, now);
                long remainingSeconds = (until - now + 999L) / 1000L;
                player.sendMessage(config.color(skill.cooldownMessage(),
                        "skill", skill.name(), "time", String.valueOf(remainingSeconds)));
            }
            return true;
        }

        data.setCooldownUntil(hand, now + skill.cooldownMillis());
        player.getInventory().setItemInMainHand(hand);

        player.sendMessage(config.color(skill.useMessage(), "skill", skill.name()));
        new IceBoltTask(plugin, freezeManager, player, skill).runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    /** Xóa dữ liệu chống spam của người chơi khi họ thoát. */
    public void forget(UUID id) {
        lastClick.remove(id);
        lastCooldownNotice.remove(id);
    }
}
