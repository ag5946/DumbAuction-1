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

package com.turt2live.dumbauction.auction;

import com.turt2live.dumbauction.DumbAuction;
import com.turt2live.dumbauction.event.AuctionImpoundEvent;
import com.turt2live.dumbauction.event.AuctionRewardEvent;
import com.turt2live.dumbauction.event.RewardOverflowEvent;
import com.turt2live.dumbauction.rewards.RewardStore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Various utilities for managing auctions
 *
 * @author turt2live
 */
public class AuctionUtil {

    /**
     * Rewards items from an auction. If the auction has no bidders, the items are returned to
     * the seller.
     *
     * @param auction the auction to reward items from, cannot be null
     *
     * @return true if the items were able to be rewarded
     */
    public static boolean rewardItems(Auction auction) {
        if (auction == null) throw new IllegalArgumentException();
        int amount = auction.getItemAmount();
        ItemStack stack = auction.getTemplateItem();
        String player = auction.getHighestBid() == null || auction.wasCancelled() ? auction.getRealSeller() : auction.getHighestBid().getRealBidder();
        List<ItemStack> rewards = new ArrayList<ItemStack>();
        while (amount > 0) {
            ItemStack clone = stack.clone();
            int desiredAmount = clone.getType().getMaxStackSize();
            if (amount - desiredAmount < 0) desiredAmount = amount;
            clone.setAmount(desiredAmount);
            rewards.add(clone);
            amount -= desiredAmount;
        }

        AuctionRewardEvent event = new AuctionRewardEvent(auction, rewards, player);
        DumbAuction.getInstance().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        rewards = event.getRewards();
        player = event.getRewardee();

        Player onlinePlayer = DumbAuction.getInstance().getServer().getPlayerExact(player);
        RewardStore store = DumbAuction.getInstance().getRewardStores().getApplicableStore(onlinePlayer.getUniqueId());
        if (store != null) {
            store.store(onlinePlayer.getUniqueId(), rewards);
        } else {
            if (onlinePlayer != null) {
                HashMap<Integer, ItemStack> overflow = onlinePlayer.getInventory().addItem(rewards.toArray(new ItemStack[0]));
                if (overflow != null && !overflow.isEmpty()) {
                    List<ItemStack> over = new ArrayList<ItemStack>();
                    over.addAll(overflow.values());
                    DumbAuction.getInstance().getServer().getPluginManager().callEvent(new RewardOverflowEvent(over, onlinePlayer.getName()));
                }
            }
        }

        return true;
    }

    // Silent item rewards
    static void impoundItems(Auction auction, Player player) {
        if (auction == null || player == null) throw new IllegalArgumentException();
        int amount = auction.getItemAmount();
        ItemStack stack = auction.getTemplateItem();
        List<ItemStack> rewards = new ArrayList<ItemStack>();
        while (amount > 0) {
            ItemStack clone = stack.clone();
            int desiredAmount = clone.getType().getMaxStackSize();
            if (amount - desiredAmount < 0) desiredAmount = amount;
            clone.setAmount(desiredAmount);
            rewards.add(clone);
            amount -= desiredAmount;
        }

        // Fire the event for message sake
        DumbAuction.getInstance().getServer().getPluginManager().callEvent(new AuctionImpoundEvent(auction, player));

        Player onlinePlayer = DumbAuction.getInstance().getServer().getPlayerExact(player.getName());
        RewardStore store = DumbAuction.getInstance().getRewardStores().getApplicableStore(player.getUniqueId());
        if (store != null) {
            store.store(player.getUniqueId(), rewards);
        } else {
            if (onlinePlayer != null) {
                HashMap<Integer, ItemStack> overflow = onlinePlayer.getInventory().addItem(rewards.toArray(new ItemStack[0]));
                if (overflow != null && !overflow.isEmpty()) {
                    List<ItemStack> over = new ArrayList<ItemStack>();
                    over.addAll(overflow.values());
                    DumbAuction.getInstance().getServer().getPluginManager().callEvent(new RewardOverflowEvent(over, onlinePlayer.getName()));
                }
            }
        }
        return;
    }

    /**
     * Reserves the items in the auction by removing them from the seller's inventory
     *
     * @param auction the auction to reserve items for, cannot be null
     */

    public static void reserveItems(Auction auction) {
        if (auction == null) throw new IllegalArgumentException();
        int amount = auction.getItemAmount();
        ItemStack stack = auction.getTemplateItem();
        Player player = DumbAuction.getInstance().getServer().getPlayer(auction.getRealSeller());
        if (player != null) {
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack test = contents[i];
                if (test != null && stack.isSimilar(test)) {
                    int newAmount = amount - test.getAmount();
                    if (newAmount < 0) {
                        amount = 0;
                        test.setAmount(Math.abs(newAmount));
                        player.getInventory().setItem(i, test);
                    } else {
                        amount -= test.getAmount();
                        player.getInventory().setItem(i, null);
                    }
                    if (amount <= 0) return;
                }
            }
        }
    }

}
