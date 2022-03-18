package com.gmail.necnionch.myplugin.csobjprice.bukkit.listeners;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectShopManager;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;


public class BlockEventListener implements Listener {
    private final ObjectShopManager container;

    public BlockEventListener(ObjectShopManager container) {
        this.container = container;
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreakBlock(BlockBreakEvent event) {
        Block block = event.getBlock();
        container.remove(block);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().forEach(container::remove);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChangeSign(SignChangeEvent event) {
        Block block = event.getBlock();
        if (!ChestShopSign.isValid(block))
            return;

        Sign sign = (Sign) block.getState();
        String current = sign.getLine(ChestShopSign.PRICE_LINE);
        String updated = event.getLine(ChestShopSign.PRICE_LINE);

        if (!current.equalsIgnoreCase(updated)) {
            container.remove(block);
        }
    }

}
