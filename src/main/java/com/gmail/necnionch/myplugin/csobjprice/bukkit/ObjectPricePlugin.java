package com.gmail.necnionch.myplugin.csobjprice.bukkit;

import com.gmail.necnionch.myplugin.csobjprice.bukkit.commands.MainCommand;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPrice;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.PriceValue;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.events.PriceObjectChangeValueEvent;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.hooks.*;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.listeners.BlockEventListener;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.listeners.ContainerEventListener;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.listeners.WorldEventListener;
import com.gmail.necnionch.myplugin.csobjprice.common.ObjectPriceName;
import com.gmail.necnionch.myplugin.csobjprice.common.Utils;
import com.gmail.necnionch.myplugin.groupbank.bukkit.account.BankAccount;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;


public final class ObjectPricePlugin extends JavaPlugin implements Listener {
    private static ObjectPricePlugin instance;
    private final MainConfig mainConfig = new MainConfig(this);
    private final ObjectShopManager shopManager = new ObjectShopManager(this);
    private final ObjectPriceManager priceManager = new ObjectPriceManager(this);
    private final PlayerSessionManager sessionManager = new PlayerSessionManager(this);
    public static final String PREFIX = ChatColor
            .translateAlternateColorCodes('&', "&7[&aObjectPrice&7] &f");

    private final JeconHook jeconHook = new JeconHook(this);
    private final BankBoxHook bankBoxHook = new BankBoxHook(this);
    private final ChestShopHook csHook = new ChestShopHook(this);
    private final GroupBankHook groupBankHook = new GroupBankHook(this);
    private final PluginHook[] hooks = new PluginHook[] {jeconHook, bankBoxHook, csHook, groupBankHook};


    @Override
    public void onEnable() {
        instance = this;

//        mainConfig.load();
        reloadMainConfig();
        priceManager.loadAll();
        shopManager.loadAll();


        for (PluginHook hook : hooks) {
            try {
                hook.hook();
            } catch (Throwable th) {
                th.printStackTrace();
                getLogger().warning("Failed to hook to " + hook.getHookName());
            }
            if (hook.isHooked())
                getLogger().info(hook.getHookName() + " plugin hooked!");
        }

        new MainCommand(this, getCommand("csobjprice"));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new WorldEventListener(shopManager), this);
        getServer().getPluginManager().registerEvents(new BlockEventListener(shopManager), this);
        getServer().getPluginManager().registerEvents(new ContainerEventListener(), this);
        getServer().getPluginManager().registerEvents(sessionManager, this);

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            updateTotalBalance();
            shopManager.updateSigns();
        });

    }

    @Override
    public void onDisable() {
        sessionManager.clear();
        priceManager.saveAll();
        shopManager.saveAll();

        for (PluginHook hook : hooks) {
            try {
                hook.unhook();
            } catch (Throwable th) {
                th.printStackTrace();
                getLogger().warning("Failed to unhook to " + hook.getHookName());
            }
        }
    }

    public static ObjectPricePlugin getInstance() {
        return instance;
    }

    public static ObjectShopManager getShops() {
        return Objects.requireNonNull(instance, "Plugin is not enabled!").shopManager;
    }

    public static ObjectPriceManager getPrices() {
        return Objects.requireNonNull(instance, "Plugin is not enabled!").priceManager;
    }

    public static PlayerSessionManager getSessions() {
        return Objects.requireNonNull(instance, "Plugin is not enabled!").sessionManager;
    }

    public MainConfig getObjectPriceConfig() {
        return Objects.requireNonNull(instance, "Plugin is not enabled!").mainConfig;
    }


    public boolean reloadMainConfig() {
        if (!mainConfig.load())
            return false;

        priceManager.setSaveDelay(mainConfig.getSaveDelay());
        shopManager.setSaveDelay(mainConfig.getSaveDelay());

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.cancelTasks(this);
        scheduler.scheduleSyncRepeatingTask(this, this::updateTotalBalance, 0, mainConfig.getTotalBalanceDelay() * 60 * 20);
        return true;
    }


    public JeconHook getJeconHook() {
        return jeconHook;
    }

    public BankBoxHook getBankBoxHook() {
        return bankBoxHook;
    }

    public GroupBankHook getGroupBankHook() {
        return groupBankHook;
    }

    public ChestShopHook getChestShopHook() {
        return csHook;
    }

    @Nullable
    public Chest getBankBoxChest() {
        Location location = mainConfig.getBankBoxChest();
        if (location == null)
            return null;

        Block block = location.getBlock();
        if (!(block.getState() instanceof Chest))
            return null;

        return (Chest) block.getState();
    }

    public int getBankBoxItems() {
        if (!bankBoxHook.isHooked())
            return 0;
        Chest chest = getBankBoxChest();
        int count = 0;
        if (chest != null) {
            for (ItemStack is : chest.getInventory().getContents()) {
                if (bankBoxHook.isBankItem(is))
                    count += is.getAmount();
            }
        }
        return count;
    }



    public void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void debug(String m, Object... args) {
        if (instance != null && instance.mainConfig.isDebug())
            instance.getLogger().warning(String.format("[DEBUG]: " + m, args));
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPlugin is disabled"));
        return true;
    }


    public void updateTotalBalance() {
        debug("update total balance");

        long total = 0;
        if (jeconHook.isHooked()) {
            try {
                long jeconTotal = (long) jeconHook.getBalanceTotal(500);
                total += Math.max(0, jeconTotal);

                PriceValue price = priceManager.get(ObjectPriceName.JECON_BALANCE_TOTAL);
                if (price == null) {
                    price = new ObjectPrice(ObjectPriceName.JECON_BALANCE_TOTAL, null);
                    priceManager.add(price);
                }
                ((ObjectPrice) price).setValue(jeconTotal);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        if (getBankBoxHook().isHooked()) {
            try {
                long bankTotal = Stream.of(groupBankHook.getAccounts()).mapToLong(BankAccount::getBalance).sum();
                total += Math.max(0, bankTotal);

                PriceValue price = priceManager.get(ObjectPriceName.GROUP_BANK_BALANCE_TOTAL);
                if (price == null) {
                    price = new ObjectPrice(ObjectPriceName.GROUP_BANK_BALANCE_TOTAL, null);
                    priceManager.add(price);
                }
                ((ObjectPrice) price).setValue(bankTotal);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        PriceValue price = priceManager.get(ObjectPriceName.BALANCE_TOTAL);
        if (price == null) {
            price = new ObjectPrice(ObjectPriceName.BALANCE_TOTAL, null);
            priceManager.add(price);
        }
        ((ObjectPrice) price).setValue(total);

    }

    public void updateJeconSyncAccountBalance() {
        if (!jeconHook.isHooked())
            return;

        debug("update jecon account sync balance");

        UUID account = mainConfig.getJeconSyncAccountUUID();
        String expression = mainConfig.getJeconSyncAccountExpression();
        if (account == null || expression == null)
            return;

        double value;
        try {
            value = Utils.evalPriceExpression(expression, priceManager.getValues());
        } catch (Throwable th) {
            th.printStackTrace();
            return;
        }

        jeconHook.setBalance(account, value);

    }

    public void updateBankBoxItems(int extra) {
        if (!bankBoxHook.isHooked())
            return;

        Chest chest = getBankBoxChest();
        if (chest == null)
            return;

        debug("update bankbox items");

        int count = 0;
        for (ItemStack is : chest.getInventory().getContents()) {
            if (bankBoxHook.isBankItem(is)) {
                count += is.getAmount();
            }
        }
        PriceValue price = priceManager.get(ObjectPriceName.BANKBOX_ITEMS);
        if (price == null) {
            price = new ObjectPrice(ObjectPriceName.BANKBOX_ITEMS, null);
            priceManager.add(price);
        }
        ((ObjectPrice) price).setValue(Math.max(0, count + extra));
    }


    @EventHandler
    public void onChanged(PriceObjectChangeValueEvent event) {
        String[] objects = mainConfig.getJeconSyncAccountObjects();
        if (objects == null)
            return;

        if (Arrays.asList(objects).contains(event.getPriceName())) {
            updateJeconSyncAccountBalance();
        }

    }



}
