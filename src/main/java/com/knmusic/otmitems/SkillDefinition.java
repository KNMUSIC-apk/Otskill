package com.knmusic.otmitems;

import org.bukkit.Particle;

/**
 * Định nghĩa một skill đọc từ config.yml.
 *
 * @param code               mã skill (chữ thường), ví dụ "hz63dse"
 * @param name               tên hiển thị (hỗ trợ mã màu)
 * @param range              tầm bắn tối đa (block)
 * @param freezeTicks        thời gian đóng băng (tick)
 * @param freezeRadius       bán kính đóng băng quanh điểm nổ (block)
 * @param damage             sát thương cộng thêm (0 = không gây sát thương)
 * @param slownessLevel      mức Slowness dùng để giữ chân
 * @param particle           hạt hiệu ứng đường đạn
 * @param cooldownMillis     thời gian hồi chiêu (ms)
 * @param customModelData    CustomModelData khi bật override (-1 = không đổi)
 * @param useMessage         thông báo khi kích hoạt
 * @param cooldownMessage    thông báo khi đang hồi chiêu
 * @param frozenMessage      thông báo gửi cho người bị đóng băng
 */
public record SkillDefinition(
        String code,
        String name,
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
        String frozenMessage
) {
}
