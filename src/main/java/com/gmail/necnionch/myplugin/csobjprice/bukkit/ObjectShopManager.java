package com.gmail.necnionch.myplugin.csobjprice.bukkit;

import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPrice;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPriceShop;
import com.gmail.necnionch.myplugin.csobjprice.common.BukkitConfigDriver;
import com.gmail.necnionch.myplugin.csobjprice.common.ScheduleConfigSaver;
import com.google.common.collect.LinkedHashMultimap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin.debug;
import static com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin.getPrices;


public class ObjectShopManager extends BukkitConfigDriver {
    private final JavaPlugin pl;
    private final ScheduleConfigSaver saver;

    public ObjectShopManager(JavaPlugin plugin) {
        super(plugin, "locations.yml", "locations.yml");
        this.pl = plugin;
        this.saver = new ScheduleConfigSaver(plugin, Bukkit.getScheduler(), this::saveAll);
    }

    private static HashMap<String, ObjectPriceShop> shopOfLocation = new HashMap<>();
    private static LinkedHashMultimap<String, ObjectPriceShop> shopsOfChunk = LinkedHashMultimap.create();


    public void loadAll() {
        saver.cancelSaveTask();
        load();
        shopOfLocation.clear();
        shopsOfChunk.clear();

        ObjectPriceShop shop;
        for (String key : config.getKeys(false)) {
            try {
                shop = ObjectPriceShop.serialize(key, config.getConfigurationSection(key));

            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid shop data '" + key + "': " + e.getMessage());
                continue;
            }

            shopOfLocation.put(shop.getLocationKey(), shop);
            shopsOfChunk.put(shop.getChunkKey(), shop);
        }
    }

    public void saveAll() {
        saver.cancelSaveTask();
        ObjectPricePlugin.debug("shop saving...");
        YamlConfiguration config = new YamlConfiguration();

        for (ObjectPriceShop shop : shopOfLocation.values()) {
            config.set(shop.getLocationKey(), shop.deserialize());
        }

        this.config = config;
        save();
    }


    public void add(ObjectPriceShop shop, boolean override) {
        if (shopOfLocation.containsKey(shop.getLocationKey()) && !override)
            throw new IllegalStateException("Already exists at location: " + shop.getLocationKey());

        remove(shop);  // contains check with LocationKey
        shopOfLocation.put(shop.getLocationKey(), shop);
        shopsOfChunk.put(shop.getChunkKey(), shop);

        scheduleSave();
    }

    public void remove(ObjectPriceShop shop) {
        shopsOfChunk.remove(shop.getChunkKey(), shop);
        if (shopOfLocation.remove(shop.getLocationKey()) != null) {
            scheduleSave();
        }
    }

    public void remove(Block block) {
        ObjectPriceShop shop = get(block.getLocation());
        if (shop != null) {
            remove(shop);
        }
    }

    public void remove(ObjectPriceShop... shops) {
        for (ObjectPriceShop shop : shops) {
            shopOfLocation.remove(shop.getLocationKey());
            shopsOfChunk.remove(shop.getChunkKey(), shop);
        }
        scheduleSave();
    }

    @Nullable
    public ObjectPriceShop get(@NotNull String worldName, int x, int y, int z) {
        String locKey = ObjectPriceShop.translateToLocKey(worldName, x, y, z);
        return shopOfLocation.get(locKey);
    }

    @Nullable
    public ObjectPriceShop get(Location blockLocation) {
        return get(Objects.requireNonNull(blockLocation.getWorld()).getName(), blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());
    }

    public ObjectPriceShop[] getAtChunk(@NotNull String worldName, int chunkX, int chunkZ) {
        String chunkKey = ObjectPriceShop.translateToChunkKey(worldName, chunkX, chunkZ);
        return shopsOfChunk.get(chunkKey).toArray(new ObjectPriceShop[0]);
    }

    public ObjectPriceShop[] getAtChunk(Chunk chunk) {
        return getAtChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public ObjectPriceShop[] getShopAll() {
        return shopOfLocation.values().toArray(new ObjectPriceShop[0]);
    }


    public ObjectPriceShop[] getShops(ObjectPrice objectPrice) {
        return shopOfLocation.values().stream()
                .filter(shop -> shop.isRequireObject(objectPrice.getName()))
                .toArray(ObjectPriceShop[]::new);
    }


    public void updateSigns() {
        debug("updateSigns ALL");
        int all = 0, success = 0;
        long delay = System.currentTimeMillis();

        Map<String, Double> prices = getPrices().getValues();
        for (ObjectPriceShop shop : getShopAll()) {
            all++;
            try {
                if (shop.updateSign(prices))
                    success++;

            } catch (Exception e) {
                getLogger().warning(String.format("Shop sign(%s,%d,%d,%d) updating fail: %s",
                        shop.getWorldName(), shop.getX(), shop.getY(), shop.getZ(), e.getMessage()));
            }
        }
        delay = System.currentTimeMillis() - delay;
        debug("updateSigns result > success=%d (all: %d) processTime=%dms", success, all, delay);
    }

    public void updateSigns(ObjectPrice objectPrice) {
        debug("updateSigns object='%s'", objectPrice.getName());
        int all = 0, success = 0;
        long delay = System.currentTimeMillis();

        Map<String, Double> prices = getPrices().getValues();
        for (ObjectPriceShop shop : getShopAll()) {
            if (!shop.isRequireObject(objectPrice.getName()))
                continue;

            all++;
            try {
                if (shop.updateSign(prices))
                    success++;

            } catch (Exception e) {
                getLogger().warning(String.format("Shop sign(%s,%d,%d,%d) updating fail: %s",
                        shop.getWorldName(), shop.getX(), shop.getY(), shop.getZ(), e.getMessage()));
            }
        }
        delay = System.currentTimeMillis() - delay;
        debug("updateSigns result > success=%d (all: %d) processTime=%dms", success, all, delay);
    }

    public void updateSigns(Chunk chunk) {
        if (!shopsOfChunk.containsKey(ObjectPriceShop.translateToChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ())))
            return;

        debug("updateSigns chunk(%d, %d)", chunk.getX(), chunk.getZ());
        if (!chunk.isLoaded()) {
            debug("  chunk is unloaded (ignored)");
            return;
        }
        int all = 0, success = 0;
        long delay = System.currentTimeMillis();

        Map<String, Double> prices = getPrices().getValues();
        for (ObjectPriceShop shop : getAtChunk(chunk)) {
            all++;
            try {
                if (shop.updateSign(prices))
                    success++;

            } catch (Exception e) {
                getLogger().warning(String.format("Shop sign(%s,%d,%d,%d) updating fail: %s",
                        shop.getWorldName(), shop.getX(), shop.getY(), shop.getZ(), e.getMessage()));
            }
        }
        delay = System.currentTimeMillis() - delay;
        debug("updateSigns result > success=%d (all: %d) processTime=%dms", success, all, delay);
    }



    public void scheduleSave() {
        saver.scheduleSave();
    }

    public void setSaveDelay(int minutes) {
        saver.setSaveDelay(minutes);
    }
}
