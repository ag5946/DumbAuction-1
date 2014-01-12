package com.turt2live.dumbauction.auction;

import com.turt2live.dumbauction.DumbAuction;
import com.turt2live.dumbauction.event.AuctionBidEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an auction
 *
 * @author turt2live
 */
public class Auction {

    private ItemStack templateItem;
    private int amount;
    private double minimumBid;
    private double bidIncrement;
    private List<Bid> bids = new ArrayList<Bid>();
    private long time;
    private String seller;

    /**
     * Creates a new auction
     *
     * @param seller        the seller
     * @param startingPrice the starting price of the auction
     * @param bidIncrement  the bid increment for the auction
     * @param time          the time required for the auction
     * @param amount        the amount of the item to auction
     * @param item          the template item to auction
     */
    public Auction(String seller, double startingPrice, double bidIncrement, long time, int amount, ItemStack item) {
        if (seller == null || startingPrice <= 0 || bidIncrement <= 0 || time <= 0 || amount <= 0 || item == null)
            throw new IllegalArgumentException();
        this.seller = seller;
        this.minimumBid = startingPrice;
        this.bidIncrement = bidIncrement;
        this.time = time;
        this.amount = amount;
        this.templateItem = item.clone();

        // Just in case...
        this.templateItem.setAmount(1);
    }

    /**
     * Gets the seller of the auction
     *
     * @return the auction seller
     */
    public String getSeller() {
        return seller;
    }

    /**
     * Gets the minimum bid (starting price) for the auction
     *
     * @return the minimum bid
     */
    public double getMinimumBid() {
        return minimumBid;
    }

    /**
     * Gets the bid increment for the auction
     *
     * @return the bid increment
     */
    public double getBidIncrement() {
        return bidIncrement;
    }

    /**
     * Gets the total required time needed to run this auction. This is not updated as the auction
     * counts down.
     *
     * @return the total required time
     */
    public long getRequiredTime() {
        return time;
    }

    /**
     * Gets the amount of the item this auction is providing
     *
     * @return the amount of the item
     */
    public int getItemAmount() {
        return amount;
    }

    /**
     * Gets a template item of the item to be auctioned. This is not a live copy.
     *
     * @return the template item
     */
    public ItemStack getTemplateItem() {
        return templateItem.clone();
    }

    /**
     * Gets an UNMODIFIABLE list of all bids
     *
     * @return a list of all bids
     */
    public List<Bid> getAllBids() {
        return Collections.unmodifiableList(bids);
    }

    /**
     * Gets the highest bid or null if there is none
     *
     * @return the highest bid, or null if none
     */
    public Bid getHighestBid() {
        Bid maximum = null;
        for (Bid bid : bids) {
            if (maximum == null || bid.getAmount() > maximum.getAmount()) {
                maximum = bid;
            }
        }
        return maximum;
    }

    /**
     * Gets the next amount the next bid will have to be in order to be able to be accepted
     *
     * @return the next bid amount
     */
    public double getNextMinimum() {
        Bid highest = getHighestBid();
        if (highest != null) return highest.getAmount() + getBidIncrement();
        return getMinimumBid();
    }

    /**
     * Determines if a bid can be accepted by this auction. This will not actually apply the bid.
     *
     * @param bid the bid to test, cannot be null
     * @return true if the bid would be accepted
     */
    public boolean canAccept(Bid bid) {
        if (bid != null) {
            return bid.getAmount() >= getNextMinimum();
        }
        return false;
    }

    /**
     * Submits a bid to the auction
     *
     * @param bid the bid to submit, cannot be null
     * @return true on success. false if the bid was rejected
     */
    public boolean submitBid(Bid bid) {
        if (bid != null) {
            if (canAccept(bid)) {
                AuctionBidEvent event = new AuctionBidEvent(this, bid);
                DumbAuction.getInstance().getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    bids.add(bid);
                    return true;
                }
            }
        }
        return false;
    }

    void cancel(){

    }

}
