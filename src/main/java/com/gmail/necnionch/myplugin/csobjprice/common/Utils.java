package com.gmail.necnionch.myplugin.csobjprice.common;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.Acrobot.ChestShop.Permission.ADMIN_SHOP;
import static com.Acrobot.ChestShop.Permission.OTHER_NAME_ACCESS;

public class Utils {

    public static boolean isEmpty(Inventory inventory) {
        for (ItemStack is : inventory.getContents()) {
            if (is != null && !Material.AIR.equals(is.getType()))
                return false;
        }
        return true;
    }

    public static List<String> generateSuggests(String arg, String... arguments) {
        String lowerArg = arg.toLowerCase(Locale.ROOT);
        return Stream.of(arguments)
                .filter(a -> a.toLowerCase(Locale.ROOT).startsWith(lowerArg))
                .collect(Collectors.toList());
    }

    public static boolean accessibleShopSign(Player player, Block signBlock) {
        if (!ChestShopSign.isValid(signBlock))
            return false;

        Sign sign = (Sign) signBlock.getState();

        if (ChestShopSign.isAdminShop(sign)) {
            return player.hasPermission(ADMIN_SHOP.getPermission());
        } if (ChestShopSign.isOwner(player, sign)) {
            return true;
        } else {
            return player.hasPermission(OTHER_NAME_ACCESS.getPermission());
        }

    }

    public static void setPriceLine(Sign sign, @Nullable Double buy, @Nullable Double sell) {
        StringBuilder line = new StringBuilder();

        if (sell != null)
            sell = (double) Math.round(Math.max(0, sell) * 100) / 100;

        if (buy != null) {
            buy = (double) Math.round(Math.max(0, buy) * 100) / 100;

            line.append("B ").append(formatPriceText(buy));
            if (sell != null) {
                line.append(" : ").append(formatPriceText(sell)).append(" S");
            }
        } else if (sell != null) {
            line.append("S ").append(formatPriceText(sell));
        } else {
            return;
        }

        sign.setLine(ChestShopSign.PRICE_LINE, line.toString());
        sign.update();
    }

    private static String formatPriceText(Double price) {
        if (price == 0)
            return PriceUtil.FREE_TEXT;

        String tmp = String.valueOf(Math.round(price * 100) / 100d);
        if (tmp.endsWith(".0"))
            tmp = tmp.substring(0, tmp.length() - 2);
        return tmp;
    }


    public static double evalPriceExpression(String expression, Map<String, Double> prices) {
        return buildPriceExpression(expression, prices).evaluate();
    }

    public static Expression buildPriceExpression(String expression, Map<String, Double> prices) {
        return new ExpressionBuilder(expression)
                .variables(prices.keySet())
                .functions(EXPRESSION_FUNCTIONS)
                .build()  // throws
                .setVariables(prices);
    }

    private static final Function[] EXPRESSION_FUNCTIONS = {
            new Function("min", 2) {
                public double apply(double... args) {
                    return Math.min(args[0], args[1]);
                }
            },
            new Function("max", 2) {
                public double apply(double... args) {
                    return Math.max(args[0], args[1]);
                }
            }
    };

}

