package com.knmusic.otmitems;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Đọc và giữ toàn bộ cấu hình của plugin từ config.yml.
 */
public final class PluginConfig {

    private final OTMitems plugin;
    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();
    private final Set<Material> extraWeapons = EnumSet.noneOf(Material.class);
    private final Set<Material> protectedBlocks = EnumSet.noneOf(Material.class);

    private boolean overrideCustomModelData;
    private boolean allowAnyItem;
    private Material iceMaterial = Material.PACKED_ICE;
    private boolean cooldownBarEnabled;
    private long cooldownBarUpdateTicks;
    private int cooldownBarLength;
    private String barFilledChar;
    private String barEmptyChar;
    private String barFilledColor;
    private String barEmptyColor;
    private String barFormat;

    public PluginConfig(OTMitems plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        overrideCustomModelData = cfg.getBoolean("settings.override-custom-model-data", false);
        allowAnyItem = cfg.getBoolean("settings.allow-any-item", false);

        extraWeapons.clear();
        for (String name : cfg.getStringList("settings.extra-weapon-materials")) {
            Material material = parseMaterial(name, null);
            if (material != null) {
                extraWeapons.add(material);
            }
        }

        iceMaterial = parseMaterial(cfg.getString("settings.ice.material", "PACKED_ICE"), Material.PACKED_ICE);
        protectedBlocks.clear();
        for (String name : cfg.getStringList("settings.ice.protected-blocks")) {
            Material material = parseMaterial(name, null);
            if (material != null) {
                protectedBlocks.add(material);
            }
        }

        cooldownBarEnabled = cfg.getBoolean("settings.cooldown-bar.enabled", true);
        cooldownBarUpdateTicks = Math.max(1L, cfg.getLong("settings.cooldown-bar.update-ticks", 5L));
        cooldownBarLength = Math.max(1, cfg.getInt("settings.cooldown-bar.length", 20));
        barFilledChar = cfg.getString("settings.cooldown-bar.filled-char", "█");
        barEmptyChar = cfg.getString("settings.cooldown-bar.empty-char", "░");
        barFilledColor = cfg.getString("settings.cooldown-bar.filled-color", "&b");
        barEmptyColor = cfg.getString("settings.cooldown-bar.empty-color", "&7");
        barFormat = cfg.getString("settings.cooldown-bar.format", "&f{skill} &8[{bar}&8] &e{time}s");

        skills.clear();
        ConfigurationSection section = cfg.getConfigurationSection("skills");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                String code = key.toLowerCase(Locale.ROOT);
                SkillDefinition skill = parseSkill(code, s);
                if (skill != null) {
                    skills.put(code, skill);
                }
            }
        }
    }

    private SkillDefinition parseSkill(String code, ConfigurationSection s) {
        String rawType = s.getString("type", "FROST_BOLT").trim().toUpperCase(Locale.ROOT);
        SkillDefinition.SkillType type;
        try {
            type = SkillDefinition.SkillType.valueOf(rawType);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Skill " + code + ": type không hợp lệ (" + rawType + "), bỏ qua.");
            return null;
        }

        SkillDefinition.IceField ice = type == SkillDefinition.SkillType.FROST_BOLT
                ? new SkillDefinition.IceField(
                        Math.max(1, s.getInt("ice-field.radius", 3)),
                        secondsToTicks(s.getDouble("ice-field.duration-seconds", 10.0)),
                        Math.max(1, s.getInt("ice-field.melt-ring-ticks", 5)))
                : null;

        SkillDefinition.Stealth stealth = type == SkillDefinition.SkillType.STEALTH
                ? new SkillDefinition.Stealth(
                        secondsToTicks(s.getDouble("stealth.duration-seconds", 15.0)),
                        Math.max(0, s.getInt("stealth.speed-level", 1)),
                        Math.max(0.0, s.getDouble("stealth.bonus-damage", 2.0)),
                        s.getString("stealth.enter-message", "&bBạn đã vào chế độ &fTàng Hình&b!"),
                        s.getString("stealth.exit-message", "&7Mất tàng hình: &e{reason}&7."))
                : null;

        return new SkillDefinition(
                code,
                s.getString("name", code),
                type,
                Math.max(1.0, s.getDouble("range", 15.0)),
                secondsToTicks(s.getDouble("freeze-seconds", 3.0)),
                Math.max(0.5, s.getDouble("freeze-radius", 2.0)),
                Math.max(0.0, s.getDouble("damage", 0.0)),
                Math.max(0, s.getInt("slowness-level", 10)),
                parseParticle(code, s.getString("projectile-particle", "SNOWFLAKE")),
                Math.max(0L, Math.round(s.getDouble("cooldown-seconds", 30.0) * 1000.0)),
                s.getInt("custom-model-data", -1),
                s.getString("use-message", "&bĐã kích hoạt &3{skill}&b!"),
                s.getString("cooldown-message", "&c{skill} đang hồi chiêu, còn &e{time}s&c."),
                s.getString("frozen-message", "&bBạn bị &3{skill}&b, đứng yên trong &f{time}s&b!"),
                ice,
                stealth
        );
    }

    private Particle parseParticle(String code, String raw) {
        try {
            Particle particle = Particle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            if (particle.getDataType() != Void.class) {
                plugin.getLogger().warning("Particle " + raw + " cần dữ liệu phụ, dùng SNOWFLAKE cho skill " + code);
                return Particle.SNOWFLAKE;
            }
            return particle;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Particle không hợp lệ: " + raw + " (skill " + code + "), dùng SNOWFLAKE.");
            return Particle.SNOWFLAKE;
        }
    }

    private Material parseMaterial(String raw, Material fallback) {
        Material material = raw == null ? null : Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            if (raw != null) {
                plugin.getLogger().warning("Material không hợp lệ trong config: " + raw);
            }
            return fallback;
        }
        return material;
    }

    private static int secondsToTicks(double seconds) {
        return (int) Math.max(1L, Math.round(seconds * 20.0));
    }

    public SkillDefinition getSkill(String code) {
        return code == null ? null : skills.get(code.toLowerCase(Locale.ROOT));
    }

    public Set<String> getSkillCodes() {
        return Collections.unmodifiableSet(skills.keySet());
    }

    public int getSkillCount() {
        return skills.size();
    }

    /** Vật liệu có được phép gắn skill hay không. */
    public boolean isWeapon(Material material) {
        if (material.isAir()) {
            return false;
        }
        if (allowAnyItem) {
            return true;
        }
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || extraWeapons.contains(material);
    }

    public Material getIceMaterial() {
        return iceMaterial;
    }

    public boolean isProtectedBlock(Material material) {
        return protectedBlocks.contains(material);
    }

    /** Tin nhắn có tiền tố, lấy từ messages.<key>. */
    public Component message(String key, String... placeholders) {
        String template = plugin.getConfig().getString("messages." + key, "&c[Thiếu message: " + key + "]");
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return Text.color(prefix + Text.format(template, placeholders));
    }

    /** Chuỗi màu từ mẫu tùy ý (không có tiền tố). */
    public Component color(String template, String... placeholders) {
        return Text.color(Text.format(template, placeholders));
    }

    public boolean isOverrideCustomModelData() {
        return overrideCustomModelData;
    }

    public boolean isCooldownBarEnabled() {
        return cooldownBarEnabled;
    }

    public long getCooldownBarUpdateTicks() {
        return cooldownBarUpdateTicks;
    }

    public int getCooldownBarLength() {
        return cooldownBarLength;
    }

    public String getBarFilledChar() {
        return barFilledChar;
    }

    public String getBarEmptyChar() {
        return barEmptyChar;
    }

    public String getBarFilledColor() {
        return barFilledColor;
    }

    public String getBarEmptyColor() {
        return barEmptyColor;
    }

    public String getBarFormat() {
        return barFormat;
    }
}
