package com.gmail.necnionch.myplugin.csobjprice.bukkit.commands;

import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPriceManager;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectPricePlugin;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.ObjectShopManager;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.PlayerSessionManager;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPrice;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.ObjectPriceShop;
import com.gmail.necnionch.myplugin.csobjprice.bukkit.entry.PriceValue;
import com.gmail.necnionch.myplugin.csobjprice.common.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.objecthunter.exp4j.Expression;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;


public class MainCommand {
    private final ObjectPricePlugin pl;
    private final ObjectShopManager shopManager = ObjectPricePlugin.getShops();
    private final ObjectPriceManager priceManager = ObjectPricePlugin.getPrices();
    private final PlayerSessionManager sessionManager = ObjectPricePlugin.getSessions();
    private final Map<String, ChildCommand> subCommands = new HashMap<>();

    public MainCommand(ObjectPricePlugin plugin, PluginCommand command) {
        this.pl = plugin;
        command.setExecutor(this::onCommand);
        command.setTabCompleter(this::onComplete);

        TabCompleter emptyCompleter = (s, c, l, a) -> Collections.emptyList();
        addChild("addobj", false, this::cmdAddObj, emptyCompleter);
        addChild("removeobj", false, this::cmdRemoveObj, this::genObjects);
        addChild("infoobj", false, this::cmdInfoObj, this::genObjects);
        addChild("listobj", false, this::cmdListObj, emptyCompleter);
        addChild("setvalue", false, this::cmdSetValue, this::genObjects);
        addChild("setprice", true, this::cmdSetPrice, emptyCompleter);
        addChild("checkprice", true, this::cmdCheckPrice, emptyCompleter);
        addChild("setbuyprice", true, this::cmdSetBuyPrice, emptyCompleter);
        addChild("setsellprice", true, this::cmdSetSellPrice, emptyCompleter);
        addChild("test", false, this::cmdCalcTest, emptyCompleter);
        addChild("addobjmember", false, this::cmdAddMember, this::genAddMember);
        addChild("removeobjmember", false, this::cmdRemoveMember, this::genRemoveMember);
        addChild("syncplayerbalance", false, this::cmdSyncPlayerBalance, this::genPlayers);
        addChild("setbankbox", true, this::cmdSetBankBox, emptyCompleter);
        addChild("debug", false, this::cmdToggleDebug, (s, c, l, a) -> (a.length == 1) ? Arrays.asList("true", "false") : Collections.emptyList());
        addChild("reload", false, this::cmdConfigReload, emptyCompleter);
    }

    // parsers

    private PriceValue parseObjectPrice(CommandSender s, String[] args) {
        String objectName;
        try {
            objectName = args[0];

        } catch (IndexOutOfBoundsException e) {
            pl.send(s, "&cオブジェクト名を指定してください。");
            return null;
        }

        PriceValue objectPrice = priceManager.get(objectName);
        if (objectPrice == null) {
            pl.send(s, "&cそのオブジェクトはありません。");
            return null;
        }
        return objectPrice;
    }


    // executors

    private boolean cmdAddObj(CommandSender s, Command cmd, String l, String[] args) {
        String objectName;
        try {
            objectName = args[0];

            if (!ObjectPrice.checkNaming(objectName)) {
                pl.send(s, "&c利用できない文字が含まれています。&7(アルファベットとアンダーバー、数字のみ可)");
                return true;
            }

        } catch (IndexOutOfBoundsException e) {
            pl.send(s, "&cオブジェクト名を指定してください。");
            return true;
        }

        if (priceManager.get(objectName) != null) {
            pl.send(s, "&cそのオブジェクト名は既に存在します。");
            return true;
        }

        ObjectPrice objPrice = new ObjectPrice(objectName, (s instanceof Player) ? ((Player) s).getUniqueId() : null);
        double value = 0;
        try {
            value = Double.parseDouble(args[1]);

        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
        }

        priceManager.add(objPrice);
        objPrice.setValue(value);
        pl.send(s, "&aオブジェクト " + objectName + " を作成しました。&7(値: " + objPrice.getValue() + ")");
        return true;
    }

    private boolean cmdRemoveObj(CommandSender s, Command cmd, String l, String[] args) {
        PriceValue objectPrice = parseObjectPrice(s, args);
        if (objectPrice == null)
            return true;

        if (!objectPrice.allow(ObjectPrice.Action.REMOVE_OBJECT, s)) {
            pl.send(s, "&c許可がありません。");
            return true;
        }

        ObjectPriceShop[] shops = shopManager.getShops(objectPrice);
        if (shops.length > 0) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("force")) {
                shopManager.remove(shops);
            } else {
                pl.send(s, "&cこのオブジェクトを" + shops.length + "件のショップで利用しているため削除できません。");
                return true;
            }
        }
        priceManager.remove(objectPrice);
        pl.send(s, "&aオブジェクト " + objectPrice.getName() + " を削除しました。");
        return true;
    }

    private boolean cmdInfoObj(CommandSender s, Command cmd, String l, String[] args) {
        PriceValue objectPrice = parseObjectPrice(s, args);
        if (objectPrice == null)
            return true;

        if (!objectPrice.allow(ObjectPrice.Action.VIEW_INFO, s)) {
            pl.send(s, "&c許可されていません。");
            return true;
        }

        Function<UUID, String> nameFinder = (uuid) -> {
            String label = Bukkit.getOfflinePlayer(uuid).getName();
            if (label == null)
                label = uuid.toString();
            return label;
        };

        List<String> editors = new ArrayList<>();
        UUID objOwner = null;
        if (objectPrice instanceof ObjectPrice) {
            objOwner = ((ObjectPrice) objectPrice).getOwner();
            for (UUID editor : ((ObjectPrice) objectPrice).getEditors()) {
                editors.add(nameFinder.apply(editor));
            }
        }

        ObjectPriceShop[] shops = shopManager.getShops(objectPrice);

        pl.send(s, "&eオブジェクト情報");

        if (objectPrice instanceof ObjectPrice) {
            s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "  値: &e" + ((ObjectPrice) objectPrice).getValue() + " &7(利用数: " + shops.length + ")"));
        } else {
            s.sendMessage(ChatColor.translateAlternateColorCodes('&', "  利用数: " + shops.length));
        }

        if (objOwner != null) {
            s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "  作成者: &e" + nameFinder.apply(objOwner)));
        }
        if (!editors.isEmpty()) {
            s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "  編集者: " + String.join("&7, &f", editors)));
        }
        return true;
    }

    private boolean cmdListObj(CommandSender s, Command cmd, String l, String[] args) {
        pl.send(s, "オブジェクト: &e" + String.join("&7, &e", priceManager.getNames()));
        return true;
    }

    private boolean cmdSetValue(CommandSender s, Command cmd, String l, String[] args) {
        PriceValue priceValue = parseObjectPrice(s, args);
        if (priceValue == null)
            return true;

        if (!priceValue.allow(ObjectPrice.Action.CHANGE_VALUE, s) || !(priceValue instanceof ObjectPrice)) {
            pl.send(s, "&cこのオブジェクトの変更は許可されていません。");
            return true;
        }
        ObjectPrice objectPrice = (ObjectPrice) priceValue;

        double value;
        try {
            value = Double.parseDouble(args[1]);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            pl.send(s, "&c数値を指定してください。&7(現在の値: " + objectPrice.getValue() + ")");
            return true;
        }

        objectPrice.setValue(value);
        pl.send(s, "&aオブジェクト " + objectPrice.getName() + " の値を " + value + " に設定しました。");
        return true;
    }

    private boolean cmdSetPrice(CommandSender s, Command cmd, String l, String[] args) {
        Player p = (Player) s;

        if (args.length <= 0) {
            pl.send(s, "&c計算式を指定してください。");
            return true;
        }

        String expression = String.join(" ", args);
        String[] objects;
        double testValue;
        try {
            Expression exp = Utils.buildPriceExpression(expression, priceManager.getValues());

            testValue = exp.evaluate();
            objects = exp.getVariableNames().toArray(new String[0]);

        } catch (Exception e) {
            pl.send(s, "&cテストに失敗しました: ");
            pl.send(s, "&c> &7" + e.getMessage());
            return true;
        }

        ObjectPriceShop.Builder builder = new ObjectPriceShop.Builder()
                .owner(p.getUniqueId())
                .buyExpression(expression, objects)
                .sellExpression(expression, objects);
        pl.send(s, "設定するショップ看板に触れてください。");


        sessionManager.interact(p.getUniqueId(), (player, event) -> {
            Block block = event.getClickedBlock();
            if (block == null || Material.AIR.equals(block.getType()))
                return false;  // continue

            event.setCancelled(true);

            if (!ChestShopSign.isValid(block)) {
                pl.send(player, "&cショップ看板ではありません、中止します。");

            } else if (!Utils.accessibleShopSign(player, block)) {
                pl.send(player, "&c許可がありません。");

            } else if (objects.length <= 0) {  // 結局、変数を使ってない。
                shopManager.remove(block);
                Utils.setPriceLine((Sign) block.getState(), testValue, testValue);
                pl.send(s, "&a設定しました！");

            } else {
                ObjectPriceShop shop = shopManager.get(block.getLocation());
                if (shop != null) {  // update
                    shop.setBuyExpression(builder.getBuyExpression(), builder.getBuyObjects());
                    shop.setSellExpression(builder.getSellExpression(), builder.getSellObjects());

                } else {
                    shop = builder.blockLocation(block).create();
                    shopManager.add(shop, true);
                }

                shop.updateSign(priceManager.getValues(shop));
                pl.send(s, "&a設定しました！");
            }

            return true;  // exiting
        });
        return true;
    }

    private boolean cmdCheckPrice(CommandSender s, Command cmd, String l, String[] args) {
        Player p = (Player) s;
        pl.send(s, "設定するショップ看板に触れてください。");

        sessionManager.interact(p.getUniqueId(), ((player, event) -> {
            Block block = event.getClickedBlock();
            if (block == null || Material.AIR.equals(block.getType()))
                return false;  // continue

            event.setCancelled(true);

            if (!ChestShopSign.isValid(block) || (!(block.getState() instanceof Sign))) {
                pl.send(player, "&cショップ看板ではありません、中止します。");

            } else if (!Utils.accessibleShopSign(player, block)) {
                pl.send(player, "&c許可がありません。");

            } else {
                String buyExpression = null;
                String sellExpression = null;

                ObjectPriceShop objectShop = shopManager.get(block.getLocation());
                if (objectShop != null) {
                    buyExpression = objectShop.getBuyExpression();
                    sellExpression = objectShop.getSellExpression();
                }

                int priceLine = ChestShopSign.PRICE_LINE;
                Sign sign = (Sign) block.getState();
                BigDecimal buyPrice = PriceUtil.getExactBuyPrice(sign.getLine(priceLine));
                BigDecimal sellPrice = PriceUtil.getExactSellPrice(sign.getLine(priceLine));

                BaseComponent[] components = new ComponentBuilder("")
                        .appendLegacy(ObjectPricePlugin.PREFIX)
                        .append("価格設定").color(ChatColor.YELLOW)
                        .create();
                s.spigot().sendMessage(components);

                if (buyExpression != null) {
                    String resultText;
                    try {
                        resultText = String.valueOf(Utils.evalPriceExpression(buyExpression, ObjectPricePlugin.getPrices().getValues(objectShop)));
                    } catch (Throwable th) {
                        resultText = "&4エラー";
                    }
                    s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "  &f&lBUY  &r: &e" + buyExpression + " &6= " + resultText));

                } else if (!PriceUtil.NO_PRICE.equals(buyPrice)) {
                    s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "  &f&lBUY  &r: &e" + buyPrice.doubleValue()));
                }

                if (sellExpression != null) {
                    String resultText;
                    try {
                        resultText = String.valueOf(Utils.evalPriceExpression(sellExpression, ObjectPricePlugin.getPrices().getValues(objectShop)));
                    } catch (Throwable th) {
                        resultText = "&4エラー";
                    }
                    s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "  &f&lSELL &r: &e" + sellExpression + " &6= " + resultText));

                } else if (!PriceUtil.NO_PRICE.equals(sellPrice)) {
                    s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "  &f&lSELL &r: &e" + sellPrice.doubleValue()));
                }

            }
            return true;  // exiting
        }));
        return true;
    }

    private boolean cmdSetBuyPrice(CommandSender s, Command cmd, String l, String[] args) {
        Player p = (Player) s;

        if (args.length <= 0) {
            pl.send(s, "&c計算式を指定してください。");
            return true;
        }

        String expression = String.join(" ", args);
        String[] objects;
        double testValue;
        try {
            Expression exp = Utils.buildPriceExpression(expression, priceManager.getValues());

            testValue = exp.evaluate();
            objects = exp.getVariableNames().toArray(new String[0]);

        } catch (Exception e) {
            pl.send(s, "&cテストに失敗しました: ");
            pl.send(s, "&c> &7" + e.getMessage());
            return true;
        }

        ObjectPriceShop.Builder builder = new ObjectPriceShop.Builder()
                .owner(p.getUniqueId())
                .buyExpression(expression, objects);
        pl.send(s, "設定するショップ看板に触れてください。");


        sessionManager.interact(p.getUniqueId(), (player, event) -> {
            Block block = event.getClickedBlock();
            if (block == null || Material.AIR.equals(block.getType()))
                return false;  // continue

            event.setCancelled(true);

            if (!ChestShopSign.isValid(block)) {
                pl.send(player, "&cショップ看板ではありません、中止します。");

            } else if (!Utils.accessibleShopSign(player, block)) {
                pl.send(player, "&c許可がありません。");

            } else if (objects.length <= 0) {  // 結局、変数を使ってない。
                shopManager.remove(block);
                Utils.setPriceLine((Sign) block.getState(), testValue, null);
                pl.send(s, "&a設定しました！");

            } else {
                ObjectPriceShop shop = shopManager.get(block.getLocation());
                if (shop != null) {  // update
                    shop.setBuyExpression(builder.getBuyExpression(), builder.getBuyObjects());

                } else {
                    shop = builder.blockLocation(block).create();
                    shopManager.add(shop, true);
                }

                shop.updateSign(priceManager.getValues(shop));
                pl.send(s, "&a設定しました！");
            }

            return true;  // exiting
        });
        return true;
    }

    private boolean cmdSetSellPrice(CommandSender s, Command cmd, String l, String[] args) {
        Player p = (Player) s;

        if (args.length <= 0) {
            pl.send(s, "&c計算式を指定してください。");
            return true;
        }

        String expression = String.join(" ", args);
        String[] objects;
        double testValue;
        try {
            Expression exp = Utils.buildPriceExpression(expression, priceManager.getValues());

            testValue = exp.evaluate();
            objects = exp.getVariableNames().toArray(new String[0]);

        } catch (Exception e) {
            pl.send(s, "&cテストに失敗しました: ");
            pl.send(s, "&c> &7" + e.getMessage());
            return true;
        }

        ObjectPriceShop.Builder builder = new ObjectPriceShop.Builder()
                .owner(p.getUniqueId())
                .sellExpression(expression, objects);
        pl.send(s, "設定するショップ看板に触れてください。");


        sessionManager.interact(p.getUniqueId(), (player, event) -> {
            Block block = event.getClickedBlock();
            if (block == null || Material.AIR.equals(block.getType()))
                return false;  // continue

            event.setCancelled(true);

            if (!ChestShopSign.isValid(block)) {
                pl.send(player, "&cショップ看板ではありません、中止します。");

            } else if (!Utils.accessibleShopSign(player, block)) {
                pl.send(player, "&c許可がありません。");

            } else if (objects.length <= 0) {  // 結局、変数を使ってない。
                shopManager.remove(block);
                Utils.setPriceLine((Sign) block.getState(), null, testValue);
                pl.send(s, "&a設定しました！");

            } else {
                ObjectPriceShop shop = shopManager.get(block.getLocation());
                if (shop != null) {  // update
                    shop.setSellExpression(builder.getSellExpression(), builder.getSellObjects());

                } else {
                    shop = builder.blockLocation(block).create();
                    shopManager.add(shop, true);
                }

                shop.updateSign(priceManager.getValues(shop));
                pl.send(s, "&a設定しました！");
            }

            return true;  // exiting
        });
        return true;
    }

    private boolean cmdCalcTest(CommandSender s, Command cmd, String l, String[] args) {
        if (args.length <= 0) {
            pl.send(s, "&c計算式を指定してください。");
            return true;
        }

        String expression = String.join(" ", args);
        double testValue;
        try {
            testValue = Utils.evalPriceExpression(expression, priceManager.getValues());

        } catch (Exception e) {
            pl.send(s, "&cテストに失敗しました: ");
            pl.send(s, "&c> &7" + e.getMessage());
            return true;
        }

        pl.send(s, "入力: &7" + expression);
        pl.send(s, "出力: &6= " + testValue);
        return true;
    }

    private boolean cmdAddMember(CommandSender s, Command cmd, String l, String[] args) {
        PriceValue priceValue = parseObjectPrice(s, args);
        if (priceValue == null)
            return true;

        if (!priceValue.allow(ObjectPrice.Action.ADD_EDITOR, s)) {
//        if (objectPrice.getOwner() != null && (s instanceof Player) && !((Player) s).getUniqueId().equals(objectPrice.getOwner())) {
            pl.send(s, "&c許可がありません。");
            return true;
        }

        if (!(priceValue instanceof ObjectPrice)) {
            pl.send(s, "&cこのオブジェクトはメンバーを持ちません。");
            return true;
        }

        if (args.length <= 1) {
            pl.send(s, "&c値の変更を許可するプレイヤーを指定してください。");
            return true;
        }

        ObjectPrice objectPrice = (ObjectPrice) priceValue;

        UUID uuid = null;
        String name;
        try {
            uuid = UUID.fromString(args[1]);
            name = Bukkit.getOfflinePlayer(uuid).getName();

        } catch (IllegalArgumentException e) {
            name = args[1];
            Player player = Bukkit.getPlayer(name);
            if (player != null) {
                name = player.getName();
                uuid = player.getUniqueId();
            }
        }

        if (uuid == null) {
            pl.send(s, "&cプレイヤーが見つかりません。");
            return true;
        }

        if (objectPrice.addEditor(uuid)) {
            pl.send(s, String.format("&a%s を許可プレイヤーに加えました。", (name != null) ? name : uuid.toString()));
        } else {
            pl.send(s, "&c既に許可されています。");
        }
        return true;
    }

    private boolean cmdRemoveMember(CommandSender s, Command cmd, String l, String[] args) {
        PriceValue priceValue = parseObjectPrice(s, args);
        if (priceValue == null)
            return true;

        if (!priceValue.allow(ObjectPrice.Action.REMOVE_EDITOR, s)) {
//        if (objectPrice.getOwner() != null && (s instanceof Player) && !((Player) s).getUniqueId().equals(objectPrice.getOwner())) {
            pl.send(s, "&c許可がありません。");
            return true;
        }

        if (!(priceValue instanceof ObjectPrice)) {
            pl.send(s, "&cこのオブジェクトはメンバーを持ちません。");
            return true;
        }

        if (args.length <= 1) {
            pl.send(s, "&c値の変更を許可しないプレイヤーを指定してください。");
            return true;
        }

        ObjectPrice objectPrice = (ObjectPrice) priceValue;

        UUID uuid = null;
        String name;
        try {
            uuid = UUID.fromString(args[1]);
            name = Bukkit.getOfflinePlayer(uuid).getName();

        } catch (IllegalArgumentException e) {
            name = args[1];
            Player player = Bukkit.getPlayer(name);
            if (player != null) {
                name = player.getName();
                uuid = player.getUniqueId();
            }
        }

        if (uuid == null) {
            pl.send(s, "&cプレイヤーが見つかりません。");
            return true;
        }

        if (objectPrice.removeEditor(uuid)) {
            pl.send(s, String.format("&a%s を許可プレイヤーから除外しました。", (name != null) ? name : uuid.toString()));
        } else {
            pl.send(s, "&c既に許可されていません。");
        }
        return true;
    }

    private boolean cmdSyncPlayerBalance(CommandSender s, Command cmd, String l, String[] args) {
        if (!pl.getJeconHook().isHooked()) {
            pl.send(s, "&cJeconプラグインを利用できません。");
            return true;
        }

        if (args.length <= 0) {
            pl.send(s, "&cアカウント名を指定してください。");
            return true;
        }

        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        String accountName = argsList.remove(0);
        String expression = String.join(" ", argsList);

        if (expression.isEmpty()) {
            pl.send(s, "&c計算式を指定してください。");
            return true;
        }

        UUID account = Bukkit.getOfflinePlayer(accountName).getUniqueId();
        String[] objects;

        try {
            Expression exp = Utils.buildPriceExpression(expression, priceManager.getValues());

            exp.evaluate();
            objects = exp.getVariableNames().toArray(new String[0]);

        } catch (Exception e) {
            pl.send(s, "&cテストに失敗しました: ");
            pl.send(s, "&c> &7" + e.getMessage());
            return true;
        }

        pl.getObjectPriceConfig().setJeconSyncAccount(account, expression, objects);
        pl.send(s, "&a同期用アカウントを設定しました。");
        pl.updateJeconSyncAccountBalance();
        return true;
    }

    private boolean cmdSetBankBox(CommandSender s, Command cmd, String l, String[] args) {
        pl.send(s, "BankBoxアイテムが格納されるチェストに触れてください。");
        sessionManager.interact(((Player) s).getUniqueId(), (p, evt) -> {
            Block block = evt.getClickedBlock();
            if (block == null || Material.AIR.equals(block.getType()))
                return false;

            evt.setCancelled(true);

            if (!Material.CHEST.equals(block.getType())) {
                pl.send(s, "&cチェストではないため中止します。");
                return true;
            }

            if (pl.getObjectPriceConfig().setBankBoxChest(block.getLocation())) {
                int items = pl.getBankBoxItems();
                pl.send(s, "&aチェストを設定しました。&7(アイテム数: " + items + ")");
            } else {
                int items = pl.getBankBoxItems();
                pl.send(s, "&c既に指定されたチェストが設定されています。&7(アイテム数: " + items + ")");
            }
            pl.updateBankBoxItems(0);
            return true;
        });
        return true;
    }

    private boolean cmdToggleDebug(CommandSender s, Command cmd, String l, String[] args) {
        boolean debug;
        if (args.length <= 0) {
            debug = !pl.getObjectPriceConfig().isDebug();
        } else {
            debug = args[0].equalsIgnoreCase("true");
        }
        pl.send(s, "コンソールデバッグ: " + debug);
        pl.getObjectPriceConfig().setDebug(debug);

        return true;
    }

    private boolean cmdConfigReload(CommandSender s, Command cmd, String l, String[] args) {
        pl.updateTotalBalance();
        pl.updateBankBoxItems(0);

        if (pl.reloadMainConfig()) {
            pl.send(s, "&a再読み込みしました。");
        } else {
            pl.send(s, "&c内部エラーが発生しました。コンソールログを確認してください。");
        }
        return true;
    }

    // generators

    private List<String> genObjects(CommandSender s, Command cmd, String l, String[] args) {
        if (args.length == 1) {
            return Utils.generateSuggests(args[0], priceManager.getNames());
        }
        return Collections.emptyList();
    }

    private List<String> genAddMember(CommandSender s, Command cmd, String l, String[] args) {
        if (args.length == 1)
            return genObjects(s, cmd, l, args);

        PriceValue priceValue = priceManager.get(args[0]);
        if (!(priceValue instanceof ObjectPrice) || !priceValue.allow(PriceValue.Action.ADD_EDITOR, s))
            return Collections.emptyList();

        ObjectPrice objectPrice = (ObjectPrice) priceValue;

        return Utils.generateSuggests(args[1], Bukkit.getOnlinePlayers().stream()
                .filter(p -> !objectPrice.isEditor(p.getUniqueId()))
                .filter(p -> !p.getUniqueId().equals(objectPrice.getOwner()))
                .map(Player::getName)
                .toArray(String[]::new));
    }

    private List<String> genRemoveMember(CommandSender s, Command cmd, String l, String[] args) {
        if (args.length == 1)
            return genObjects(s, cmd, l, args);

        PriceValue priceValue = priceManager.get(args[0]);
        if (!(priceValue instanceof ObjectPrice) || !priceValue.allow(PriceValue.Action.REMOVE_EDITOR, s))
            return Collections.emptyList();

        ObjectPrice objectPrice = (ObjectPrice) priceValue;

        Set<String> suggests = new HashSet<>();
        String playerName;
        for (UUID editor : objectPrice.getEditors()) {
            playerName = Bukkit.getOfflinePlayer(editor).getName();
            suggests.add(playerName != null ? playerName : editor.toString());
        }
        return Utils.generateSuggests(args[1], suggests.toArray(new String[0]));
    }

    private List<String> genPlayers(CommandSender s, Command cmd, String l, String[] args) {
        return Utils.generateSuggests(args[0], Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toArray(String[]::new));
    }


    // abc

    private void addChild(@NotNull String name, boolean playerOnly, @NotNull CommandExecutor executor, @NotNull TabCompleter completer) {
        subCommands.put(name, new ChildCommand(
                name, new Permission("chestshopobjectprice.command." + name), playerOnly, executor, completer
        ));
    }

    private boolean checkExecutable(CommandSender sender, ChildCommand command) {
        if (command.playerOnly && !(sender instanceof Player)) {
            return false;
        }
        return command.permission == null || sender.hasPermission(command.permission);
    }

    private boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (subCommands.values().stream().noneMatch(c -> c.permission == null || sender.hasPermission(c.permission))) {
            pl.send(sender, "&cこのコマンドを実行する権限がありません。");
            return true;
        }

        if (args.length <= 0) {
            pl.send(sender, "コマンド一覧");
            return false;
        }

        ChildCommand command = subCommands.get(args[0].toLowerCase());
        if (command == null) {
            pl.send(sender, "コマンド一覧");
            return false;
        }

        if (command.playerOnly && !(sender instanceof Player)) {
            pl.send(sender, "&cThis command cannot be run on the console!");
            return true;
        }

        if (command.permission != null && !sender.hasPermission(command.permission)) {
            pl.send(sender, "&cこのコマンドを実行する権限がありません。");
            return true;
        }

        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.remove(0);
        return command.executor.onCommand(sender, cmd, label, arguments.toArray(new String[0]));
    }

    private List<String> onComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return Utils.generateSuggests(args[0], subCommands.values().stream()
                    .filter(c -> c.permission == null || sender.hasPermission(c.permission))
                    .filter(c -> checkExecutable(sender, c))
                    .map(c -> c.name).toArray(String[]::new));
        }
        ChildCommand command = subCommands.get(args[0]);
        if (command == null || !checkExecutable(sender, command))
            return Collections.emptyList();

        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.remove(0);
        if (arguments.isEmpty())
            return Collections.emptyList();

        return command.completer.onTabComplete(sender, cmd, label, arguments.toArray(new String[0]));
    }


    static class ChildCommand {
        private final Permission permission;
        private final String name;
        private final boolean playerOnly;
        private final CommandExecutor executor;
        private final TabCompleter completer;

        public ChildCommand(@NotNull String name, @Nullable Permission permission, boolean playerOnly, @NotNull CommandExecutor executor, @NotNull TabCompleter completer) {
            this.name = name;
            this.permission = permission;
            this.playerOnly = playerOnly;
            this.executor = executor;
            this.completer = completer;
        }

    }

}
