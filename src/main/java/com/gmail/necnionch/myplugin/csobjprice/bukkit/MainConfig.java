package com.gmail.necnionch.myplugin.csobjprice.bukkit;

import com.gmail.necnionch.myplugin.csobjprice.common.BukkitConfigDriver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class MainConfig extends BukkitConfigDriver {
    public MainConfig(JavaPlugin plugin) {
        super(plugin);
    }

    private boolean debug;
    private String[] jeconSyncAccountObjects;


    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        config.set("debug", debug);
        save();
    }

    public int getSaveDelay() {
        return config.getInt("save-delay", 15);
    }

    public int getTotalBalanceDelay() {
        return config.getInt("total-balance-delay", 30);
    }

    public @Nullable UUID getJeconSyncAccountUUID() {
        try {
            return UUID.fromString(config.getString("jecon-sync-account.uuid", null));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public @Nullable String[] getJeconSyncAccountObjects() {
        return jeconSyncAccountObjects;
    }

    public @Nullable String getJeconSyncAccountExpression() {
        return config.getString("jecon-sync-account.expression", null);
    }

    public void setJeconSyncAccount(@Nullable UUID account, String expression, String[] objects) {
        config.set("jecon-sync-account.uuid", (account != null) ? account.toString() : null);
        config.set("jecon-sync-account.objects", (account != null) ? Arrays.asList(objects) : null);
        config.set("jecon-sync-account.expression", (account != null) ? expression : null);
        save();
    }

    public @Nullable Location getBankBoxChest() {
        String loc = config.getString("bankbox-chest-location", null);
        if (loc == null || loc.isEmpty())
            return null;

        try {
            String[] spl = loc.split(",", 4);

            World world = Bukkit.getWorld(spl[3]);
            if (world == null)
                return null;
            return new Location(world, Integer.parseInt(spl[0]), Integer.parseInt(spl[1]), Integer.parseInt(spl[2]));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean setBankBoxChest(Location blockLocation) {
        String locText = String.format("%d,%d,%d,%s",
                blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), blockLocation.getWorld().getName());
        if (!locText.equals(config.getString("bankbox-chest-location", null))) {
            config.set("bankbox-chest-location", locText);
            save();
            return true;
        }
        return false;
    }


    @Override
    public boolean onLoaded(FileConfiguration config) {
        if (super.onLoaded(config)) {
            debug = config.getBoolean("debug", false);
            jeconSyncAccountObjects = config.getStringList("jecon-sync-account.objects").toArray(new String[0]);
            return true;
        }
        return false;
    }



}
