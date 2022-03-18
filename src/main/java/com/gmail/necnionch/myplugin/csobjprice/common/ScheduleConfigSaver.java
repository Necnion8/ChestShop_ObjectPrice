package com.gmail.necnionch.myplugin.csobjprice.common;

import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class ScheduleConfigSaver {
    private final Plugin owner;
    private final BukkitScheduler scheduler;
    private final ConfigSaver saver;
    private int saveDelay = 15;
    private int taskId = -1;

    public ScheduleConfigSaver(Plugin owner, BukkitScheduler scheduler, ConfigSaver saver) {
        this.owner = owner;
        this.scheduler = scheduler;
        this.saver = saver;
    }

    public void cancelSaveTask() {
        scheduler.cancelTask(taskId);
        taskId = -1;
    }

    public void setSaveDelay(int minutes) {
        this.saveDelay = minutes;
    }

    public void scheduleSave() {
//        if (!scheduler.isCurrentlyRunning(taskId)) {
        if (taskId == -1) {
            taskId = scheduler.scheduleSyncDelayedTask(owner, () -> {
                taskId = -1;
                saver.saveAll();
            }, saveDelay * 60 * 20);
        }
    }


    public interface ConfigSaver {
        void saveAll();
    }
}
