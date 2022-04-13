package com.gmail.necnionch.myplugin.csobjprice.bukkit.hooks;

import jp.jyn.jecon.Jecon;
import jp.jyn.jecon.repository.BalanceRepository;
import jp.jyn.jecon.repository.LazyRepository;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

public class JeconHook extends PluginHook {
    private Jecon jecon;
    private Field razyRepoCacheField;

    public JeconHook(JavaPlugin owner) {
        super(owner);
    }


    @Override
    public String getHookName() {
        return "Jecon";
    }

    @Override
    public boolean isHooked() {
        return jecon != null;
    }

    @Override
    public void hook() {
        Plugin tmp = pm.getPlugin(getHookName());
        if (pm.isPluginEnabled(getHookName()) && tmp instanceof Jecon) {
            jecon = (Jecon) tmp;

            try {
                Class.forName("jp.jyn.jecon.repository.LazyRepository");
                razyRepoCacheField = LazyRepository.class.getDeclaredField("balanceCache");
                razyRepoCacheField.setAccessible(true);

            } catch (ClassNotFoundException | NoSuchFieldException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void unhook() {
        jecon = null;
    }


    public double getBalanceTotal(int limit) {
        if (!isHooked()) {
            return 0;
        }

        BalanceRepository repository = getBalanceRepository();

        Map<UUID, Double> balances = repository.top(limit, 0).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue()));

        Map<UUID, Double> cachedBalances = getLazyCacheBalance(repository);
        if (cachedBalances != null)
            balances.putAll(cachedBalances);

        return balances.values().stream().mapToDouble(e -> e).sum();
    }

    public Map<UUID, Double> getLazyCacheBalance(BalanceRepository repository) {
        if (razyRepoCacheField != null && repository instanceof LazyRepository) {
            Map<UUID, OptionalLong> cache;
            try {
                //noinspection unchecked
                cache = (Map<UUID, OptionalLong>) razyRepoCacheField.get(repository);

            } catch (IllegalArgumentException | IllegalStateException | IllegalAccessException | ClassCastException e) {
                e.printStackTrace();
                return null;
            }

            return cache.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> (double) e.getValue().orElse(0) / 100));
        }
        return null;
    }


    public void setBalance(UUID account, double value) {
        if (!isHooked())
            throw new IllegalStateException("Jecon plugin is not available");
        getBalanceRepository().set(account, value);
    }

    public BalanceRepository getBalanceRepository() {
        return jecon.getRepository();
    }
}
