package com.gmail.necnionch.myplugin.csobjprice.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PlayerSessionManager implements Listener {
    private final ObjectPricePlugin pl;

    public PlayerSessionManager(ObjectPricePlugin plugin) {
        this.pl = plugin;
    }

    private final Map<UUID, InteractListener> players = new HashMap<>();


    public void interact(UUID player, InteractListener listener) {
        remove(player);
        players.put(player, listener);
    }

    public boolean contains(UUID player) {
        return players.containsKey(player);
    }


    public void remove(UUID player) {
        players.remove(player);
    }

    public void clear() {
        players.clear();
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        UUID player = event.getPlayer().getUniqueId();

        InteractListener listener = players.get(player);
        if (listener != null) {
            boolean exiting;
            try {
                exiting = listener.onSelect(event.getPlayer(), event);
            } catch (Throwable e) {
                e.printStackTrace();
                exiting = true;
                pl.send(event.getPlayer(), "&c内部エラーが発生しました。");
            }
            if (exiting)
                remove(player);

        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        remove(event.getPlayer().getUniqueId());
    }



    public interface InteractListener {
        boolean onSelect(Player player, PlayerInteractEvent event);
    }

}
