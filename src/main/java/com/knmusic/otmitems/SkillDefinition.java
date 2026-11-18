package com.knmusic.otmitems;

import org.bukkit.Particle;

/**
 * Định nghĩa một skill đọc từ config.yml.
 * Trường {@code type} quyết định hành vi; {@code ice} chỉ dùng cho FROST_BOLT, {@code stealth} chỉ dùng cho STEALTH.
 */
public record SkillDefinition(
        String code,
        String name,
        SkillType type,
        double range,
        int freezeTicks,
        double freezeRadius,
        double damage,
        int slownessLevel,
        Particle particle,
        long cooldownMillis,
        int customModelData,
        String useMessage,
        String cooldownMessage,
        String frozenMessage,
        IceField ice,
        Stealth stealth
) {

    public enum SkillType {
        /** Đạn băng + vùng băng quanh người dùng + đóng băng chân người chơi. */
        FROST_BOLT,
        /** Tàng hình, tăng tốc, cộng sát thương cho vũ khí. */
        STEALTH
    }

    /** Vùng băng: bán kính, thời gian tồn tại (tick), tốc độ tan mỗi vòng (tick). */
    public record IceField(int radius, int durationTicks, int meltRingTicks) {
    }

    /** Tàng hình: thời gian (tick), mức tăng tốc, sát thương cộng thêm, thông báo vào/ra. */
    public record Stealth(int durationTicks, int speedLevel, double bonusDamage,
                          String enterMessage, String exitMessage) {
    }
}
