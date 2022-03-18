package com.gmail.necnionch.myplugin.csobjprice.bukkit.hooks;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public abstract class PluginHook {
    protected final PluginManager pm;
    protected final JavaPlugin owner;

    PluginHook(JavaPlugin owner) {
        this.owner = owner;
        this.pm = owner.getServer().getPluginManager();
    }

    abstract public String getHookName();

    abstract public boolean isHooked();

    abstract public void hook() throws Throwable;

    public void unhook() throws Throwable {}

}
