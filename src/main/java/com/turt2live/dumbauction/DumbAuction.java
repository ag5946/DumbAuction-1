/*******************************************************************************
 * Copyright (C) 2014 Travis Ralston (turt2live)
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.turt2live.dumbauction;

import com.turt2live.commonsense.DumbPlugin;
import com.turt2live.dumbauction.auction.AuctionManager;
import com.turt2live.dumbauction.command.AuctionCommandHandler;
import com.turt2live.dumbauction.hook.MobArenaHook;
import com.turt2live.dumbauction.listener.DumbAuctionListener;
import com.turt2live.dumbauction.listener.InternalListener;
import com.turt2live.dumbauction.rewards.OfflineStore;
import com.turt2live.dumbauction.rewards.StoreRegistry;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DumbAuction extends DumbPlugin {

    private static DumbAuction p;

    private Economy economy;
    private AuctionManager auctions;
    private List<String> ignoreBroadcast = new ArrayList<String>();
    private StoreRegistry rewardStores;
    private MobArenaHook maHook;

    @Override
    public void onEnable() {
        p = this;
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("A Vault-supported economy plugin was not found. Please add one to your server.");
            getLogger().severe("For a simple economy plugin, I suggest DumbCoin: http://dev.bukkit.org/bukkit-plugins/dumbcoin/");
            getServer().getPluginManager().disablePlugin(p);
            return;
        }
        initCommonSense(72073);

        getCommand("auction").setExecutor(new AuctionCommandHandler(this));
        getCommand("bid").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                String[] strings1 = new String[strings.length + 1];
                strings1[0] = "bid";
                for (int i = 0; i < strings.length; i++) {
                    strings1[i + 1] = strings[i];
                }
                return getCommand("auction").getExecutor().onCommand(commandSender, getCommand("auction"), s, strings1);
            }
        });

        getServer().getPluginManager().registerEvents(new InternalListener(), this);
        getServer().getPluginManager().registerEvents(new DumbAuctionListener(), this);

        auctions = new AuctionManager();

        if (getServer().getPluginManager().getPlugin("MobArena") != null) {
            try {
                maHook = new MobArenaHook();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        ignoreBroadcast = getConfig().getStringList("ignore-broadcast");
        if (ignoreBroadcast == null) {
            ignoreBroadcast = new ArrayList<String>();
        }

        rewardStores = new StoreRegistry();
        try {
            if (maHook != null) rewardStores.addStore(maHook);
            rewardStores.addStore(new OfflineStore(this));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (auctions != null) auctions.stop(); // Returns items
        if (rewardStores != null) rewardStores.save();

        getConfig().set("ignore-broadcast", ignoreBroadcast);
        saveConfig();
        p = null;
    }

    public StoreRegistry getRewardStores() {
        return rewardStores;
    }

    public AuctionManager getAuctionManager() {
        return auctions;
    }

    public MobArenaHook getMobArena() {
        return maHook;
    }

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage((ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", ChatColor.GRAY + "[DumbAuction]")) + " " + ChatColor.WHITE + message).trim());
    }

    public void broadcast(String message) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (!ignoreBroadcast.contains(player.getName())) {
                sendMessage(player, message);
            }
        }
        sendMessage(getServer().getConsoleSender(), message);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public void setIgnore(String player, boolean ignore) {
        if (ignore) ignoreBroadcast.add(player);
        else ignoreBroadcast.remove(player);
    }

    public boolean isIgnoring(String player) {
        return ignoreBroadcast.contains(player);
    }

    /**
     * Gets the active instance of DumbAuction
     *
     * @return the plugin instance
     */
    public static DumbAuction getInstance() {
        return p;
    }

    /**
     * Gets the Vault economy hook
     *
     * @return Vault economy hook
     */
    public Economy getEconomy() {
        return economy;
    }

}
