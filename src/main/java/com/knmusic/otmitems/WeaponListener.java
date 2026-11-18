package com.knmusic.otmitems;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Bắt chuột phải để kích hoạt skill, khóa vị trí người bị đóng băng,
 * và xử lý tàng hình khi tấn công hoặc bị đánh.
 */
public final class WeaponListener implements Listener {

    private final OTMitems plugin;

    public WeaponListener(OTMitems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClickAir(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (plugin.getSkillService().tryUse(event.getPlayer())) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (plugin.getSkillService().tryUse(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        plugin.getFreezeManager().handleMove(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        StealthManager stealth = plugin.getStealthManager();

        if (event.getDamager() instanceof Player attacker && stealth.isStealthed(attacker.getUniqueId())) {
            ItemStack hand = attacker.getInventory().getItemInMainHand();
            if (plugin.getPluginConfig().isWeapon(hand.getType())) {
                event.setDamage(event.getDamage() + stealth.bonusDamage(attacker.getUniqueId()));
            }
            stealth.finish(attacker.getUniqueId(), "đã tấn công");
        }

        if (event.getEntity() instanceof Player victim && stealth.isStealthed(victim.getUniqueId())) {
            stealth.finish(victim.getUniqueId(), "bị tấn công");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getStealthManager().forget(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.getSkillService().forget(id);
        plugin.getCooldownBarTask().forget(id);
        plugin.getStealthManager().forget(id);
    }
}
