package com.gmail.necnionch.myplugin.csobjprice.bukkit.entry;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.gmail.necnionch.myplugin.csobjprice.common.Utils;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

import static com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin.debug;
import static com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin.getShops;

public class ObjectPriceShop {
    private final int x, y, z;
    private final String worldName;
    private @Nullable String buyExpression;
    private @Nullable String sellExpression;
    private @Nullable String[] buyObjects;
    private @Nullable String[] sellObjects;
//    private Set<String> requireObjects;
    private @Nullable UUID owner;

    private final String locationKey;
    private final String chunkKey;

    public ObjectPriceShop(int x, int y, int z, @NotNull String worldName,
                           @Nullable String buyExpression, @Nullable String sellExpression,
                           @Nullable String[] buyObjects, @Nullable String[] sellObjects, @Nullable UUID owner) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        this.buyExpression = buyExpression;
        this.sellExpression = sellExpression;
        this.buyObjects = buyObjects;
        this.sellObjects = sellObjects;
        this.owner = owner;

        locationKey = translateToLocKey(worldName, x, y, z);
        chunkKey = translateToChunkKey(worldName, getChunkX(), getChunkZ());
    }


    public static ObjectPriceShop serialize(String key, ConfigurationSection section) throws IllegalArgumentException {
        int x, y, z;
        String worldName;
        try {
            String[] tmp = key.split(",", 4);
            x = Integer.parseInt(tmp[0]);
            y = Integer.parseInt(tmp[1]);
            z = Integer.parseInt(tmp[2]);
            worldName = tmp[3];
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }

        String tmp;
        UUID owner = null;
        try {
            tmp = section.getString("owner");
            owner = (tmp != null && !tmp.isEmpty()) ? UUID.fromString(tmp) : null;

        } catch (IllegalArgumentException ignored) {
        }

        tmp = section.getString("buy-expression", null);
        String buy = (tmp != null && !tmp.isEmpty()) ? tmp : null;

        tmp = section.getString("sell-expression", null);
        String sell = (tmp != null && !tmp.isEmpty()) ? tmp : null;

        String[] buyObjects = section.getStringList("buy-objects").toArray(new String[0]);
        String[] sellObjects = section.getStringList("sell-objects").toArray(new String[0]);

        return new ObjectPriceShop(x, y, z, worldName, buy, sell, buyObjects, sellObjects, owner);
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("owner", (owner != null) ? owner.toString() : null);
        data.put("buy-expression", buyExpression);
        data.put("sell-expression", sellExpression);
        data.put("buy-objects", (buyObjects != null) ? Arrays.asList(buyObjects) : null);
        data.put("sell-objects", (sellObjects != null) ? Arrays.asList(sellObjects) : null);

        return data;
    }


    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return x >> 4;
    }

    public int getChunkZ() {
        return z >> 4;
    }

    @Nullable
    public String getBuyExpression() {
        return buyExpression;
    }

    @Nullable
    public String getSellExpression() {
        return sellExpression;
    }

    @Nullable
    public String[] getBuyObjects() {
        return buyObjects;
    }

    @Nullable
    public String[] getSellObjects() {
        return sellObjects;
    }

    public boolean isRequireObject(String objectName) {
        return (buyObjects != null && Arrays.asList(buyObjects).contains(objectName)) || (sellObjects != null && Arrays.asList(sellObjects).contains(objectName));
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setBuyExpression(@Nullable String expression, String[] objects) {
        this.buyExpression = expression;
        this.buyObjects = objects;
        getShops().scheduleSave();
    }

    public void setSellExpression(@Nullable String expression, String[] objects) {
        this.sellExpression = expression;
        this.sellObjects = objects;
        getShops().scheduleSave();
    }

    public String getLocationKey() {
        return locationKey;
    }

    public String getChunkKey() {
        return chunkKey;
    }



    public boolean updateSign(Map<String, Double> prices) throws CalculationError {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            debug("world '%s' not found", worldName);
            return false;
        }
        if (!world.isChunkLoaded(getChunkX(), getChunkZ())) {
            debug("chunk (%d,%d) not loaded)", getChunkX(), getChunkZ());
            return false;
        }
        Block block = world.getBlockAt(x, y, z);
        if (!ChestShopSign.isValid(block) || !(block.getState() instanceof Sign)) {
            debug("block (%d,%d,%d) is not sign (Type: %s)", x, y, z, block.getType().name());
            return false;
        }

        Sign sign = (Sign) block.getState();

        int priceLine = ChestShopSign.PRICE_LINE;
        BigDecimal decimal;
        Double buyPrice = null, sellPrice = null;
        // buy
        decimal = PriceUtil.getExactBuyPrice(sign.getLine(priceLine));
        if (buyExpression != null) {
            try {
                buyPrice = Utils.evalPriceExpression(buyExpression, prices);

            } catch (Throwable th) {
                debug("-> Sign (%d,%d,%d) buy eval error: %s", x, y, z, th.getMessage());
                if (!PriceUtil.NO_PRICE.equals(decimal))
                    buyPrice = decimal.doubleValue();
            }
        } else if (!PriceUtil.NO_PRICE.equals(decimal)) {
            buyPrice = decimal.doubleValue();
        }

        // sell
        decimal = PriceUtil.getExactSellPrice(sign.getLine(priceLine));
        if (sellExpression != null) {
            try {
                sellPrice = Utils.evalPriceExpression(sellExpression, prices);

            } catch (Throwable th) {
                debug("-> Sign (%d,%d,%d) sell eval error: %s", x, y, z, th.getMessage());
                if (!PriceUtil.NO_PRICE.equals(decimal))
                    sellPrice = decimal.doubleValue();
            }
        } else if (!PriceUtil.NO_PRICE.equals(decimal)) {
            sellPrice = decimal.doubleValue();
        }


        // update
        Utils.setPriceLine(sign, buyPrice, sellPrice);
        debug("-> updated (%s,%d,%d,%d / chunkAt %d, %d) buy=%f, sell=%f",
                worldName, x, y, z, getChunkX(), getChunkZ(), buyPrice, sellPrice);
        return true;
    }




    public static String translateToLocKey(@NotNull String worldName, int x, int y, int z) {
        return x + "," + y + "," + z + "," + worldName;
    }

    public static String translateToChunkKey(@NotNull String worldName, int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ + "," + worldName;
    }


    public static class Builder {
        private int x, y, z;
        private String worldName;
        private String buyExpression;
        private String sellExpression;
        private String[] buyObjects;
        private String[] sellObjects;
        private UUID owner;

        public Builder() {}


        public Builder blockLocation(Block block) {
            this.worldName = block.getWorld().getName();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
            return this;
        }

        public Builder buyExpression(String expression, String[] objects) {
            this.buyExpression = expression;
            this.buyObjects = objects;
            return this;
        }

        public Builder sellExpression(String expression, String[] objects) {
            this.sellExpression = expression;
            this.sellObjects = objects;
            return this;
        }

        public Builder owner(UUID owner) {
            this.owner = owner;
            return this;
        }


        public ObjectPriceShop create() {
            if (worldName == null || (buyExpression == null && sellExpression == null))
                throw new IllegalArgumentException("worldName or buyExpr or sellExpr has not set");

            return new ObjectPriceShop(x, y, z, worldName, buyExpression, sellExpression, buyObjects, sellObjects, owner);
        }


        public String getBuyExpression() {
            return buyExpression;
        }

        public String[] getBuyObjects() {
            return buyObjects;
        }

        public String getSellExpression() {
            return sellExpression;
        }

        public String[] getSellObjects() {
            return sellObjects;
        }

    }
}