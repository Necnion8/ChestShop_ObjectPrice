package com.gmail.necnionch.myplugin.csobjprice.bukkit.entry;

import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin;
import com.gmail.necnionch.myplugin.csobjprice.common.Perms;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


public class ObjectPrice {
    private final String name;
    private double value;
    private @Nullable UUID owner;
    private @NotNull Set<UUID> editors;


    public ObjectPrice(@NotNull String name, @Nullable UUID owner) {
        this.name = name;
        this.owner = owner;
        this.editors = new HashSet<>();
    }


    public static ObjectPrice serialize(String name, ConfigurationSection section) throws IllegalArgumentException {
        ObjectPrice object = new ObjectPrice(name, null);

        object.value = section.getDouble("value");
        try {
            String tmp = section.getString("owner");
            if (tmp != null && !tmp.isEmpty())
                object.owner = UUID.fromString(tmp);

        } catch (IllegalArgumentException ignored) {
        }

        for (String tmp : section.getStringList("editors")) {
            try {
                object.editors.add(UUID.fromString(tmp));
            } catch (IllegalArgumentException ignored) {

            }
        }
        return object;
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("value", value);
        data.put("owner", (owner != null) ? owner.toString() : null);
        data.put("editors", editors.stream().map(UUID::toString).collect(Collectors.toList()));

        return data;
    }


    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public UUID[] getEditors() {
        return editors.toArray(new UUID[0]);
    }

    public void setValue(double value) {
        double oldValue = this.value;
        this.value = value;
        ObjectPricePlugin.getPrices().updateValue(this, oldValue);
        ObjectPricePlugin.getShops().updateSigns(this);
    }

    public void setValueSilentEvent(double value) {
        this.value = value;
        ObjectPricePlugin.getPrices().update(this);
        ObjectPricePlugin.getPrices().updateValue(this, null);
        ObjectPricePlugin.getShops().updateSigns(this);
    }

    public boolean isEditor(UUID uuid) {
        return editors.contains(uuid);
    }

    public boolean addEditor(UUID uuid) {
        if (editors.add(uuid)) {
            ObjectPricePlugin.getPrices().update(this);
            return true;
        }
        return false;
    }

    public boolean removeEditor(UUID uuid) {
        if (editors.remove(uuid)) {
            ObjectPricePlugin.getPrices().update(this);
            return true;
        }
        return false;
    }



    public boolean allow(Action action, Permissible user) {
        UUID uuid = (user instanceof Player) ? ((Player) user).getUniqueId() : null;

        switch (action) {
            case USE:
                return true;

            case CHANGE_VALUE:
            case VIEW_INFO:
                if (user.hasPermission(Perms.BYPASS_MANAGE_OBJECT) || user.hasPermission(Perms.BYPASS_CHANGE_VALUE))
                    return true;

                return uuid != null && (uuid.equals(owner) || editors.contains(uuid));

            default:
                if (user.hasPermission(Perms.BYPASS_MANAGE_OBJECT))
                    return true;
                return uuid != null && (uuid.equals(owner));
        }
    }

    public static boolean checkNaming(String name) {
        return name.matches("^[a-zA-Z_]\\w*$") && !name.startsWith("dyn_");
    }


    public enum Action {
        CHANGE_VALUE, REMOVE_OBJECT, VIEW_INFO, ADD_EDITOR, REMOVE_EDITOR, USE,
    }
}
