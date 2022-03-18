package com.gmail.necnionch.myplugin.csobjprice.bukkit.events;

import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPrice;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PriceObjectChangeValueEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final ObjectPrice objectPrice;
    private final double newValue;
    private final double oldValue;

    public PriceObjectChangeValueEvent(ObjectPrice objectPrice, double newValue, double oldValue) {
        this.objectPrice = objectPrice;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public ObjectPrice getObjectPrice() {
        return objectPrice;
    }

    public String getPriceName() {
        return objectPrice.getName();
    }

    public double getOldValue() {
        return oldValue;
    }

    public double getNewValue() {
        return newValue;
    }


    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }


}
