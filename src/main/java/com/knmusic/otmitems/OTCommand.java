package com.knmusic.otmitems;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lệnh /ot:
 *   /ot <mã>          gắn skill lên vũ khí đang cầm (ví dụ: /ot hz63dse)
 *   /ot set <mã>      giống trên
 *   /ot del [mã]      xóa skill khỏi vũ khí đang cầm (kiểm tra mã nếu có)
 *   /ot reload        nạp lại config.yml
 */
public final class OTCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "ot.admin";
    private static final List<String> SUBCOMMANDS = List.of("reload", "del", "set");

    private final OTMitems plugin;

    public OTCommand(OTMitems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PluginConfig config = plugin.getPluginConfig();

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(config.message("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(config.message("usage"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "del" -> removeSkill(sender, args.length >= 2 ? args[1] : null);
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage(config.message("usage"));
                } else {
                    applySkill(sender, args[1]);
                }
            }
            default -> applySkill(sender, args[0]);
        }
        return true;
    }

    private void applySkill(CommandSender sender, String rawCode) {
        PluginConfig config = plugin.getPluginConfig();
        WeaponData data = plugin.getWeaponData();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("player-only"));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            sender.sendMessage(config.message("not-holding-item"));
            return;
        }
        if (!config.isWeapon(hand.getType())) {
            sender.sendMessage(config.message("invalid-item"));
            return;
        }
        if (hand.getAmount() != 1) {
            sender.sendMessage(config.message("stack-not-allowed"));
            return;
        }

        SkillDefinition skill = config.getSkill(rawCode);
        if (skill == null) {
            sender.sendMessage(config.message("unknown-skill", "code", rawCode));
            return;
        }

        String current = data.getSkillCode(hand);
        if (skill.code().equals(current)) {
            sender.sendMessage(config.message("skill-already", "skill", skill.name()));
            return;
        }

        data.attach(hand, skill, config.isOverrideCustomModelData());
        player.getInventory().setItemInMainHand(hand);

        sender.sendMessage(config.message(
                current == null ? "skill-applied" : "skill-replaced",
                "skill", skill.name()));
    }

    private void removeSkill(CommandSender sender, String code) {
        PluginConfig config = plugin.getPluginConfig();
        WeaponData data = plugin.getWeaponData();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("player-only"));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            sender.sendMessage(config.message("not-holding-item"));
            return;
        }

        String current = data.getSkillCode(hand);
        if (current == null) {
            sender.sendMessage(config.message("no-skill-on-item"));
            return;
        }
        if (code != null && !current.equalsIgnoreCase(code)) {
            SkillDefinition currentSkill = config.getSkill(current);
            sender.sendMessage(config.message("skill-mismatch",
                    "code", code,
                    "skill", currentSkill != null ? currentSkill.name() : current));
            return;
        }

        SkillDefinition skill = config.getSkill(current);
        String name = skill != null ? skill.name() : current;

        data.detach(hand);
        player.getInventory().setItemInMainHand(hand);

        sender.sendMessage(config.message("skill-removed", "skill", name));
    }

    private void reload(CommandSender sender) {
        plugin.reloadAll();
        PluginConfig config = plugin.getPluginConfig();
        sender.sendMessage(config.message("reloaded", "count", String.valueOf(config.getSkillCount())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }

        PluginConfig config = plugin.getPluginConfig();

        if (args.length == 1) {
            List<String> options = new ArrayList<>(SUBCOMMANDS);
            options.addAll(config.getSkillCodes());
            return filter(options, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("set") || sub.equals("del")) {
                return filter(new ArrayList<>(config.getSkillCodes()), args[1]);
            }
        }

        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.startsWith(lower))
                .sorted()
                .toList();
    }
}
