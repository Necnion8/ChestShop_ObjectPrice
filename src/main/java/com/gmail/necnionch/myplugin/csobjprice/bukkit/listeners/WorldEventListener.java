package com.gmail.necnionch.myplugin.csobjprice.bukkit.listeners;

import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectShopManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class WorldEventListener implements Listener {
    private final ObjectShopManager container;

    public WorldEventListener(ObjectShopManager container) {
        this.container = container;
    }

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent event) {
        if (event.isNewChunk())
            return;

        Chunk chunk = event.getChunk();
        container.updateSigns(chunk);

    }

}
