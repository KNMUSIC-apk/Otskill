package com.knmusic.otmitems;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Đọc/ghi dữ liệu skill, cooldown và CustomModelData gốc trong PDC của vật phẩm.
 *
 * PDC được lưu trong NBT của ItemStack nên vẫn còn khi vật phẩm bị vứt xuống đất,
 * cất vào rương, chuyển sang server khác hoặc khi người chơi thoát rồi vào lại.
 */
public final class WeaponData {

    private final NamespacedKey skillKey;
    private final NamespacedKey cooldownKey;
    private final NamespacedKey cmdOverriddenKey;
    private final NamespacedKey hadCmdKey;
    private final NamespacedKey originalCmdKey;

    public WeaponData(OTMitems plugin) {
        this.skillKey = new NamespacedKey(plugin, "skill_code");
        this.cooldownKey = new NamespacedKey(plugin, "skill_cooldown_until");
        this.cmdOverriddenKey = new NamespacedKey(plugin, "cmd_overridden");
        this.hadCmdKey = new NamespacedKey(plugin, "original_cmd_exists");
        this.originalCmdKey = new NamespacedKey(plugin, "original_cmd");
    }

    /** Trả về mã skill đang gắn, hoặc null nếu không có. */
    public String getSkillCode(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(skillKey, PersistentDataType.STRING);
    }

    /** Thời điểm (epoch ms) hồi chiêu kết thúc, 0 nếu không có. */
    public long getCooldownUntil(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return 0L;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0L;
        }
        return meta.getPersistentDataContainer().getOrDefault(cooldownKey, PersistentDataType.LONG, 0L);
    }

    public void setCooldownUntil(ItemStack item, long until) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, until);
        item.setItemMeta(meta);
    }

    /**
     * Gắn skill lên vật phẩm. Nếu vật phẩm chưa có skill nào thì sao lưu CustomModelData gốc.
     * Nếu bật override và skill khai báo custom-model-data >= 0 thì đổi CustomModelData.
     */
    public void attach(ItemStack item, SkillDefinition skill, boolean overrideCmd) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(skillKey, PersistentDataType.STRING)) {
            boolean hadCmd = meta.hasCustomModelData();
            pdc.set(hadCmdKey, PersistentDataType.BYTE, (byte) (hadCmd ? 1 : 0));
            pdc.set(originalCmdKey, PersistentDataType.INTEGER, hadCmd ? meta.getCustomModelData() : 0);
        }

        boolean wasOverridden = pdc.getOrDefault(cmdOverriddenKey, PersistentDataType.BYTE, (byte) 0) == 1;
        if (overrideCmd && skill.customModelData() >= 0) {
            meta.setCustomModelData(skill.customModelData());
            pdc.set(cmdOverriddenKey, PersistentDataType.BYTE, (byte) 1);
        } else if (wasOverridden) {
            restoreCustomModelData(meta, pdc);
            pdc.remove(cmdOverriddenKey);
        }

        pdc.set(skillKey, PersistentDataType.STRING, skill.code());
        pdc.remove(cooldownKey);
        item.setItemMeta(meta);
    }

    /** Xóa skill khỏi vật phẩm và khôi phục CustomModelData gốc nếu trước đó đã bị đổi. */
    public void detach(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        boolean wasOverridden = pdc.getOrDefault(cmdOverriddenKey, PersistentDataType.BYTE, (byte) 0) == 1;
        if (wasOverridden) {
            restoreCustomModelData(meta, pdc);
        }

        pdc.remove(skillKey);
        pdc.remove(cooldownKey);
        pdc.remove(cmdOverriddenKey);
        pdc.remove(hadCmdKey);
        pdc.remove(originalCmdKey);
        item.setItemMeta(meta);
    }

    private void restoreCustomModelData(ItemMeta meta, PersistentDataContainer pdc) {
        byte hadCmd = pdc.getOrDefault(hadCmdKey, PersistentDataType.BYTE, (byte) 0);
        if (hadCmd == 1) {
            meta.setCustomModelData(pdc.getOrDefault(originalCmdKey, PersistentDataType.INTEGER, 0));
        } else {
            meta.setCustomModelData(null);
        }
    }
}
