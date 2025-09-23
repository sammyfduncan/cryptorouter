package com.samsonduncan.cryptorouter.services;

import com.samsonduncan.cryptorouter.model.normalised.Exchange;
import com.samsonduncan.cryptorouter.model.normalised.NormalisedOrderBook;
import com.samsonduncan.cryptorouter.model.normalised.NormalisedOrderBookEntry;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/*
Central service subscribing to data streams from connectors.
Maintains a live, in memory consolidated order book for each trading pair.
When update arrives, performs core merging logic to update internal order book
exposes reactive stream (Flux) of consolidated order book
 */
@Service
public class OrderBookService {

    //Maps key is price, value is quantity
    //Field for bids map (sorted from highest to lowest)
    private final ConcurrentSkipListMap<BigDecimal, BigDecimal> bids =
            new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    //Asks map (from lowest to highest
    private final ConcurrentSkipListMap<BigDecimal, BigDecimal> asks =
            new ConcurrentSkipListMap<>();

    //Tracks specific price levels from each exchange, for when updates arrive
    private final Map<Exchange, Set<BigDecimal>> bidsExchange = new ConcurrentHashMap<>();
    private final Map<Exchange, Set<BigDecimal>> asksExchange = new ConcurrentHashMap<>();

    //Reactive sink (entry point from connectors)
    private final Sinks.Many<NormalisedOrderBook> orderBookSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public OrderBookService() {
        //access sink's data stream
        this.orderBookSink.asFlux()
            //define actions to take for each item (normalisedBook)
            //by subscribing to stream to process each item as it arrives
                .subscribe(normalisedBook -> {
                    //if book update is not empty, ignore
                    if (normalisedBook.getBids().isEmpty() && normalisedBook.getAsks().isEmpty()) {
                        return;
                    }

                    //determine source exchange from first entry
                    //all entries in one update are from same exchange
                    Exchange sourceExchange = normalisedBook.getBids().isEmpty() ?
                            normalisedBook.getAsks().get(0).exchange() :
                            normalisedBook.getBids().get(0).exchange();

                    //call helper to apply update
                    //updateBook(normalisedBook, sourceExchange);
                });
    }

    //Method for updating books
    public void processUpdate(NormalisedOrderBook bookUpdate) {
        orderBookSink.tryEmitNext(bookUpdate);
    }

    //Getters, returns immutable copy of current order books
    public Map<BigDecimal, BigDecimal> getBids() {
        return bids;
    }

    public Map<BigDecimal, BigDecimal> getAsks() {
        return asks;
    }


    /**
     * Helper to update consolidated order book
     * Removes all previous entries from a given exchange before adding new ones
     * Synchronised to ensure only one thread can execute at a time
     * @param newBook The new order book snapshot from a single exchange
     * @param sourceExchange The exchange that sent this update
     */
    private synchronized void updateBook(NormalisedOrderBook newBook, Exchange sourceExchange) {
        //Bids update:
        //remove all old bids from this exchange
        Set<BigDecimal> oldBids = bidsExchange.computeIfAbsent(
                sourceExchange, k -> new HashSet<>());
        for (BigDecimal price : oldBids) {
            bids.remove(price);
        }
        oldBids.clear(); //clear set for new prices

        //add new bids to consolidated order book and track prices
        for (NormalisedOrderBookEntry bid : newBook.getBids()) {
            bids.put(bid.price(), bid.quantity());
            oldBids.add(bid.price());
        }

        //Asks update:
        Set<BigDecimal> oldAsks = asksExchange.computeIfAbsent(
                sourceExchange, k -> new HashSet<>());
        for (BigDecimal price : oldAsks) {
            asks.remove(price);
        }
        oldAsks.clear();

        //add new asks
        for (NormalisedOrderBookEntry ask : newBook.getAsks()) {
            asks.put(ask.price(), ask.quantity());
            oldAsks.add(ask.price());
        }
    }

}

