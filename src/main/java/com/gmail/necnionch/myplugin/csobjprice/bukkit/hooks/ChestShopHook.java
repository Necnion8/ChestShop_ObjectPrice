package com.gmail.necnionch.myplugin.csobjprice.bukkit.hooks;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;


public class ChestShopHook extends PluginHook implements Listener {
    private boolean available;
    private boolean registeredListener;

    public ChestShopHook(JavaPlugin owner) {
        super(owner);
    }

    @Override
    public String getHookName() {
        return "ChestShop";
    }

    @Override
    public boolean isHooked() {
        return available;
    }

    @Override
    public void hook() {
        available = pm.isPluginEnabled(getHookName());
        if (available && !registeredListener) {
            pm.registerEvents(this, owner);
            registeredListener = true;
        }
    }

    @Override
    public void unhook() {
        available = false;
    }



    @EventHandler(ignoreCancelled = true)
    public void onBuyShop(com.Acrobot.ChestShop.Events.TransactionEvent event) {
        Chest chest = getPlugin().getBankBoxChest();
        if (!getPlugin().getBankBoxHook().isHooked() || chest == null)
            return;

        int amount = 0;
        for (ItemStack is : event.getStock()) {
            if (getPlugin().getBankBoxHook().isBankItem(is))
                amount += is.getAmount();
        }

        if (TransactionEvent.TransactionType.BUY.equals(event.getTransactionType()))
            amount *= -1;
        getPlugin().updateBankBoxItems(amount);
    }


    private ObjectPricePlugin getPlugin() {
        return ObjectPricePlugin.getInstance();
    }

}
