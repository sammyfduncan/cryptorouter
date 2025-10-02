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

    //Maps key is price, value is map showing exchange and quantity
    //Field for bids map (sorted from highest to lowest)
    private final ConcurrentSkipListMap<BigDecimal, Map<Exchange, BigDecimal>> bids =
            new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    //Asks map (from lowest to highest)
    private final ConcurrentSkipListMap<BigDecimal, Map<Exchange, BigDecimal>> asks =
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
    public Map<BigDecimal, Map<Exchange, BigDecimal>> getBids() {
        return Collections.unmodifiableMap(bids);
    }

    public Map<BigDecimal, Map<Exchange, BigDecimal>> getAsks() {
        return Collections.unmodifiableMap(asks);
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
        Set<BigDecimal> oldBids = bidsExchange.getOrDefault(
                sourceExchange,
                new HashSet<>());

        for (BigDecimal price : oldBids) {
            //get inner map for price, from bids map
            Map<Exchange, BigDecimal> innerMap = bids.get(price);
            if (innerMap != null) {
                innerMap.remove(sourceExchange);
                //if innerMap now empty, remove entire price level
                if (innerMap.isEmpty()) {
                    bids.remove(price);
                }
            }
        }
        //reset tracking set for this exchange
        bidsExchange.put(sourceExchange, new HashSet<>());


        //add new bids to consolidated order book and track prices
        for (NormalisedOrderBookEntry bid : newBook.getBids()) {
            //get/create inner map for this price level
            Map<Exchange, BigDecimal> innerMap = bids.computeIfAbsent(
                    bid.price(),
                    k -> new ConcurrentHashMap<>()
            );
            //add this exchanges quantity to inner map
            innerMap.put(sourceExchange, bid.quantity());
            //track contribution from exchange
            bidsExchange.get(sourceExchange).add(bid.price());
        }


        //Asks update
        //First remove old asks from this exchange
        Set<BigDecimal> oldAsks = asksExchange.getOrDefault(
                sourceExchange, new HashSet<>());
        for (BigDecimal price : oldAsks) {
            Map<Exchange, BigDecimal> innerMap = asks.get(price);
            if (innerMap != null) {
                innerMap.remove(sourceExchange);
                if (innerMap.isEmpty()) {
                    asks.remove(price)
                }
            }
        }
        asksExchange.put(sourceExchange, new HashSet<>());

        //add new asks
        for (NormalisedOrderBookEntry ask : newBook.getAsks()) {
            Map<Exchange, BigDecimal> innerMap = asks.computeIfAbsent(
                    ask.price(), k -> new ConcurrentHashMap<>());
            innerMap.put(sourceExchange, ask.quantity());
            asksExchange.get(sourceExchange).add(ask.price());
        }
        System.out.println("Book updated");
    }

}

