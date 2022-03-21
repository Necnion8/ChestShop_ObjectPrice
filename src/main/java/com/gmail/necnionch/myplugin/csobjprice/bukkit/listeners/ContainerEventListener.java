package com.gmail.necnionch.myplugin.csobjprice.bukkit.listeners;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPriceManager;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectShopManager;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPriceShop;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.PriceValue;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class ContainerEventListener implements Listener {

    private final ObjectShopManager shops;
    private final ObjectPriceManager prices;

    public ContainerEventListener() {
        shops = ObjectPricePlugin.getShops();
        prices = ObjectPricePlugin.getPrices();
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventory(InventoryCloseEvent event) {
        Location invLocation = event.getInventory().getLocation();
        Block invBlock = invLocation.getBlock();

        if (invBlock == null || !(invBlock.getState() instanceof Container))
            return;

        Sign sign = uBlock.findAnyNearbyShopSign(invBlock);
        if (sign == null)
            return;

        ObjectPriceShop shop = shops.get(sign.getLocation());
        if (shop == null)
            return;

        onUpdateSign(shop);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShop(TransactionEvent event) {
        Sign sign = event.getSign();
        ObjectPriceShop shop = shops.get(sign.getLocation());
        if (shop == null)
            return;
        onUpdateSign(shop);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPreShop(PreTransactionEvent event) {

//        event.setExactPrice();
//        event.setStock();
//        event.setOwnerAccount();
//        event.getStock();
//        event.getExactPrice();

    }


    private void onUpdateSign(ObjectPriceShop shop) {
        boolean used = false;
        for (PriceValue containerValue : ObjectPriceManager.CONTAINER_VALUES) {
            if (shop.isRequireObject(containerValue.getName())) {
                used = true;
                break;
            }
        }
        if (!used)
            return;

        shops.updateSigns(shop);

    }


}
