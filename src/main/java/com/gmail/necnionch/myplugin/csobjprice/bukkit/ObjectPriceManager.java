package com.gmail.necnionch.myplugin.csobjprice.bukkit;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPrice;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPriceShop;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.PriceValue;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.events.PriceObjectChangeValueEvent;
import com.gmail.necnionch.myplugin.csobjprice.common.BukkitConfigDriver;
import com.gmail.necnionch.myplugin.csobjprice.common.ScheduleConfigSaver;
import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectPriceManager extends BukkitConfigDriver {
//    private Map<String, ObjectPrice> objects = new HashMap<>();
    private final Map<String, PriceValue> priceValues = new HashMap<>();
    private final ScheduleConfigSaver saver;

    public static final PriceValue CONTAINER_SLOTS_VALUE = new ContainerPriceValue("slots") {
        public double getValue(ObjectPriceShop shop) {
            Container container = shop.getShopContainer();
            if (container == null)
                throw new IllegalStateException("no container");
            return container.getInventory().getSize();
        }
    };

    public static final PriceValue CONTAINER_CONTENTS_MAX_VALUE = new ContainerPriceValue("contents_max") {
        public double getValue(ObjectPriceShop shop) {
            Container container = shop.getShopContainer();
            if (container == null)
                throw new IllegalStateException("no container");
            return container.getInventory().getSize() * 64;
        }
    };

    public static final PriceValue CONTAINER_CONTENTS_COUNTS_VALUE = new ContainerPriceValue("contents_counts") {
        public double getValue(ObjectPriceShop shop) {
            Container container = shop.getShopContainer();
            if (container == null)
                throw new IllegalStateException("no container");
            int items = 0;
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null)
                    items += item.getAmount();
            }
            return items;
        }
    };

    public static final PriceValue CONTAINER_CONTENTS_FREE_VALUE = new ContainerPriceValue("contents_free") {
        public double getValue(ObjectPriceShop shop) {
            Container container = shop.getShopContainer();
            if (container == null)
                throw new IllegalStateException("no container");
            int max = container.getInventory().getSize() * 64;
            int items = 0;
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null)
                    items += item.getAmount();
            }
            return max - items;
        }
    };

    public static final PriceValue CONTAINER_CONTENTS_VALUE = new ContainerPriceValue("contents") {
        public double getValue(ObjectPriceShop shop) {
            Container container = shop.getShopContainer();
            if (container == null)
                throw new IllegalStateException("no container");
            int max = 0;
            int used = 0;
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null) {
                    max += item.getMaxStackSize();
                    used += item.getAmount();
                } else {
                    max += 64;
                }
            }
            return (double) used / max;
        }
    };

    public static final PriceValue[] CONTAINER_VALUES = {
            CONTAINER_SLOTS_VALUE,
            CONTAINER_CONTENTS_MAX_VALUE,
            CONTAINER_CONTENTS_COUNTS_VALUE,
            CONTAINER_CONTENTS_FREE_VALUE,
            CONTAINER_CONTENTS_VALUE
    };


    public ObjectPriceManager(JavaPlugin plugin) {
        super(plugin, "objects.yml", "objects.yml");
        this.saver = new ScheduleConfigSaver(plugin, Bukkit.getScheduler(), this::saveAll);
    }


    public void loadAll() {
        saver.cancelSaveTask();
        load();
        priceValues.clear();
        loadDefaults();

        ObjectPrice objPrice;
        for (String name : config.getKeys(false)) {
            try {
                objPrice = ObjectPrice.serialize(name, config.getConfigurationSection(name));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid object-price data '" + name + "': " + e.getMessage());
                continue;
            }

            priceValues.put(name, objPrice);
        }
    }

    public void saveAll() {
        saver.cancelSaveTask();
        ObjectPricePlugin.debug("price saving...");
        YamlConfiguration config = new YamlConfiguration();

        for (PriceValue priceValue : priceValues.values()) {
            if (priceValue instanceof ObjectPrice) {
                config.set(priceValue.getName(), ((ObjectPrice) priceValue).deserialize());
            }
        }

        this.config = config;
        save();
    }

    private void loadDefaults() {
        for (PriceValue containerValue : CONTAINER_VALUES) {
            add(containerValue);
        }
    }


    public void add(PriceValue objectPrice) {
        if (priceValues.containsKey(objectPrice.getName()))
            throw new IllegalStateException("Already exists name: " + objectPrice.getName());

        priceValues.put(objectPrice.getName(), objectPrice);
        scheduleSave();
    }

    public void remove(PriceValue objectPrice) {
        priceValues.remove(objectPrice.getName(), objectPrice);
        scheduleSave();
    }

    public void remove(String name) {
        priceValues.remove(name);
        scheduleSave();
    }

    @Nullable
    public PriceValue get(String name) {
        return priceValues.get(name);
    }


    public PriceValue[] getPrices() {
        return priceValues.values().toArray(new PriceValue[0]);
    }

    public String[] getNames() {
        return priceValues.keySet().toArray(new String[0]);
    }

    @Deprecated
    public Map<String, Double> getValues() {
        return priceValues.values().stream()
                .collect(Collectors.toMap(PriceValue::getName, PriceValue::getValue));
    }

    public Map<String, Double> getValues(ObjectPriceShop shop) {
        return priceValues.values().stream()
                .filter(pv -> shop.isRequireObject(pv.getName()))
                .collect(Collectors.toMap(PriceValue::getName, pv -> pv.getValue(shop)));
    }


    public void save(ObjectPrice objectPrice) {
        if (!priceValues.containsValue(objectPrice))
            throw new IllegalStateException("deleted objectPrice");

        scheduleSave();
    }

    public void updateValue(ObjectPrice objectPrice, Double oldValue) {
        if (!priceValues.containsValue(objectPrice))
            throw new IllegalStateException("deleted objectPrice");

        if (oldValue != null && objectPrice.getValue() != oldValue) {
            Bukkit.getPluginManager().callEvent(
                    new PriceObjectChangeValueEvent(objectPrice, objectPrice.getValue(), oldValue)
            );
        }

        if (oldValue == null || objectPrice.getValue() != oldValue) {
            save(objectPrice);
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



    public static abstract class ContainerPriceValue extends PriceValue {
        private final String name;

        public ContainerPriceValue(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean available(ObjectPriceShop shop, Sign sign) {
            return !ChestShopSign.isAdminShop(sign) && uBlock.findConnectedContainer(sign) != null;
        }
    }

}
