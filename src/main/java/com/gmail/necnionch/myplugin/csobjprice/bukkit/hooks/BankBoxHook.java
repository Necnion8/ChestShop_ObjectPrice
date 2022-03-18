package com.gmail.necnionch.myplugin.csobjprice.bukkit.hooks;

import com.gmail.necnionch.myplugin.bankbox.bukkit.BankBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BankBoxHook extends PluginHook {
    private BankBox bankBox;

    public BankBoxHook(JavaPlugin owner) {
        super(owner);
    }


    @Override
    public String getHookName() {
        return "BankBox";
    }

    @Override
    public boolean isHooked() {
        return bankBox != null;
    }

    @Override
    public void hook() {
        Plugin tmp = pm.getPlugin(getHookName());
        if (pm.isPluginEnabled(getHookName()) && tmp instanceof BankBox) {
            bankBox = (BankBox) tmp;
        }
    }

    @Override
    public void unhook() {
        bankBox = null;
    }


    public boolean isBankItem(ItemStack item) {
        if (!isHooked()) {
//            throw new IllegalStateException("BankBox is not available!");
            return false;
        }

        return bankBox.isMoneyItem(item);
    }


}
