package com.gmail.necnionch.myplugin.csobjprice.bukkit.entry;

import com.gmail.necnionch.myplugin.csobjprice.common.Perms;
import org.bukkit.block.Sign;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

public abstract class PriceValue {
    public abstract String getName();
    public @Nullable String[] getDescription() { return null; }

    public abstract double getValue(ObjectPriceShop shop) throws PriceValueError;

    public double getValue() { return 0d; }

    public abstract boolean available(ObjectPriceShop shop, Sign sign);



    public boolean allow(Action action, Permissible user) {
        switch (action) {
            case USE:
                return true;

            case CHANGE_VALUE:
            case VIEW_INFO:
                return (user.hasPermission(Perms.BYPASS_MANAGE_OBJECT) || user.hasPermission(Perms.BYPASS_CHANGE_VALUE));

            default:
                return (user.hasPermission(Perms.BYPASS_MANAGE_OBJECT));
        }

    }

    public enum Action {
        CHANGE_VALUE, REMOVE_OBJECT, VIEW_INFO, ADD_EDITOR, REMOVE_EDITOR, USE,
    }

}
