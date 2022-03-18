package com.gmail.necnionch.myplugin.csobjprice.bukkit.hooks;

import com.gmail.necnionch.myplugin.groupbank.bukkit.GroupBank;
import com.gmail.necnionch.myplugin.groupbank.bukkit.account.BankAccount;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GroupBankHook extends PluginHook {
    private GroupBank groupBank;

    public GroupBankHook(JavaPlugin owner) {
        super(owner);
    }


    @Override
    public String getHookName() {
        return "GroupBank";
    }

    @Override
    public boolean isHooked() {
        return groupBank != null;
    }

    @Override
    public void hook() {
        Plugin tmp = pm.getPlugin(getHookName());
        if (pm.isPluginEnabled(getHookName()) && tmp instanceof GroupBank) {
            groupBank = (GroupBank) tmp;
        }
    }

    @Override
    public void unhook() {
        groupBank = null;
    }


    public BankAccount[] getAccounts() {
        if (!isHooked()) {
            return new BankAccount[0];
        }
        return GroupBank.getAccounts();
    }


}
