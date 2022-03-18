package com.gmail.necnionch.myplugin.csobjprice.bukkit;

import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPrice;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPriceShop;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.events.PriceObjectChangeValueEvent;
import com.gmail.necnionch.myplugin.csobjprice.common.BukkitConfigDriver;
import com.gmail.necnionch.myplugin.csobjprice.common.ScheduleConfigSaver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectPriceManager extends BukkitConfigDriver {
    private Map<String, ObjectPrice> objects = new HashMap<>();
    private final ScheduleConfigSaver saver;

    public ObjectPriceManager(JavaPlugin plugin) {
        super(plugin, "objects.yml", "objects.yml");
        this.saver = new ScheduleConfigSaver(plugin, Bukkit.getScheduler(), this::saveAll);
    }


    public void loadAll() {
        saver.cancelSaveTask();
        load();
        objects.clear();

        ObjectPrice objPrice;
        for (String name : config.getKeys(false)) {
            try {
                objPrice = ObjectPrice.serialize(name, config.getConfigurationSection(name));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid object-price data '" + name + "': " + e.getMessage());
                continue;
            }

            objects.put(name, objPrice);
        }
    }

    public void saveAll() {
        saver.cancelSaveTask();
        ObjectPricePlugin.debug("price saving...");
        YamlConfiguration config = new YamlConfiguration();

        for (ObjectPrice objPrice : objects.values()) {
            config.set(objPrice.getName(), objPrice.deserialize());
        }

        this.config = config;
        save();
    }


    public void add(ObjectPrice objectPrice) {
        if (objects.containsKey(objectPrice.getName()))
            throw new IllegalStateException("Already exists name: " + objectPrice.getName());

        objects.put(objectPrice.getName(), objectPrice);
        scheduleSave();
    }

    public void remove(ObjectPrice objectPrice) {
        objects.remove(objectPrice.getName(), objectPrice);
        scheduleSave();
    }

    public void remove(String name) {
        objects.remove(name);
        scheduleSave();
    }

    @Nullable
    public ObjectPrice get(String name) {
        return objects.get(name);
    }


    public ObjectPrice[] getPrices() {
        return objects.values().toArray(new ObjectPrice[0]);
    }

    public String[] getNames() {
        return objects.keySet().toArray(new String[0]);
    }


    public Map<String, Double> getValues() {
        return objects.values().stream()
                .collect(Collectors.toMap(ObjectPrice::getName, ObjectPrice::getValue));
    }


    public void update(ObjectPrice objectPrice) {
        if (!objects.containsValue(objectPrice))
            throw new IllegalStateException("deleted objectPrice");

        scheduleSave();
    }

    public void updateValue(ObjectPrice objectPrice, Double oldValue) {
        if (!objects.containsValue(objectPrice))
            throw new IllegalStateException("deleted objectPrice");

        if (oldValue != null && objectPrice.getValue() != oldValue) {
            Bukkit.getPluginManager().callEvent(
                    new PriceObjectChangeValueEvent(objectPrice, objectPrice.getValue(), oldValue)
            );
        }

        if (oldValue == null || objectPrice.getValue() != oldValue) {
            update(objectPrice);
        }
    }



    private ObjectShopManager getShops() {
        return ObjectPricePlugin.getShops();
    }



    public void scheduleSave() {
        saver.scheduleSave();
    }

    public void setSaveDelay(int minutes) {
        saver.setSaveDelay(minutes);
    }

}
