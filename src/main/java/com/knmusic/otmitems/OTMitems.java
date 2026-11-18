package com.knmusic.otmitems;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Plugin gắn skill lên vũ khí bằng PersistentDataContainer (PDC) và CustomModelData.
 */
public final class OTMitems extends JavaPlugin {

    private PluginConfig pluginConfig;
    private WeaponData weaponData;
    private IceBlocks iceBlocks;
    private FreezeManager freezeManager;
    private IceFieldManager iceFieldManager;
    private StealthManager stealthManager;
    private SkillService skillService;
    private CooldownBarTask cooldownBarTask;
    private BukkitTask cooldownBarHandle;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();
        weaponData = new WeaponData(this);
        iceBlocks = new IceBlocks(pluginConfig);
        freezeManager = new FreezeManager(this, iceBlocks);
        iceFieldManager = new IceFieldManager(this, iceBlocks);
        stealthManager = new StealthManager(this, pluginConfig);
        skillService = new SkillService(this, pluginConfig, weaponData,
                freezeManager, iceFieldManager, stealthManager);
        cooldownBarTask = new CooldownBarTask(pluginConfig, weaponData);
        restartCooldownBar();

        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);

        OTCommand command = new OTCommand(this);
        PluginCommand otCommand = getCommand("ot");
        if (otCommand != null) {
            otCommand.setExecutor(command);
            otCommand.setTabCompleter(command);
        }

        getLogger().info("OTMitems đã bật với " + pluginConfig.getSkillCount() + " skill.");
    }

    @Override
    public void onDisable() {
        stopCooldownBar();
        if (stealthManager != null) {
            stealthManager.clearAll();
        }
        if (freezeManager != null) {
            freezeManager.thawAll();
        }
        if (iceBlocks != null) {
            iceBlocks.restoreAll();
        }
    }

    /** Dùng cho /ot reload: nạp lại config và khởi động lại thanh hồi chiêu. */
    public void reloadAll() {
        pluginConfig.load();
        restartCooldownBar();
    }

    private void restartCooldownBar() {
        stopCooldownBar();
        if (!pluginConfig.isCooldownBarEnabled()) {
            return;
        }
        cooldownBarHandle = getServer().getScheduler().runTaskTimer(
                this, cooldownBarTask, 1L, pluginConfig.getCooldownBarUpdateTicks());
    }

    private void stopCooldownBar() {
        if (cooldownBarHandle != null) {
            cooldownBarHandle.cancel();
            cooldownBarHandle = null;
        }
        if (cooldownBarTask != null) {
            cooldownBarTask.clear();
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public WeaponData getWeaponData() {
        return weaponData;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public StealthManager getStealthManager() {
        return stealthManager;
    }

    public SkillService getSkillService() {
        return skillService;
    }

    public CooldownBarTask getCooldownBarTask() {
        return cooldownBarTask;
    }
}
