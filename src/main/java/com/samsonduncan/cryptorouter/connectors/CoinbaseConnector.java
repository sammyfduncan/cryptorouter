package com.samsonduncan.cryptorouter.connectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsonduncan.cryptorouter.model.coinbase.CoinbaseSnapshot;
import com.samsonduncan.cryptorouter.model.coinbase.CoinbaseUpdate;
import com.samsonduncan.cryptorouter.model.normalised.Exchange;
import com.samsonduncan.cryptorouter.model.normalised.NormalisedOrderBook;
import com.samsonduncan.cryptorouter.model.normalised.NormalisedOrderBookEntry;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class CoinbaseConnector extends WebSocketClient {

    //jackson objmapper engine
    private final ObjectMapper objectMapper;

    //holds most recent version of order book
    private NormalisedOrderBook coinbaseOrderBook;

    public CoinbaseConnector(URI serverUri) {
        super(serverUri);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("Connected to coinbase");

        String subscriptionMessage = """
            {
             "type": "subscribe",
             "product_ids": [ "BTC-USD" ],
             "channels": [ "level2" ]
            }
            """;

        send(subscriptionMessage);
        System.out.println("Sent subscription message for BTC/USD book");
    }

    @Override
    public void onMessage(String message) {
        try {
            //find what type of msg by inspecting JSON
            JsonNode node = objectMapper.readTree(message);
            //now get type field from node
            String type = node.get("type").asText();

            if (type.equals("snapshot")) {
                //if snapshot, parse full msg into CoinbaseSnapshot
                CoinbaseSnapshot snapshot = objectMapper.readValue(
                        message,
                        CoinbaseSnapshot.class);

                //now translate parsed msg
                this.coinbaseOrderBook = translateSnapshot(snapshot);

                System.out.println("Parsed and translated Coinbase snapshot: " + this.coinbaseOrderBook);

            } else if (type.equals("12update")) {
                //if 12update, first check orderbook has been initialised
                if (coinbaseOrderBook != null) {
                    //if not null, parse message
                    CoinbaseUpdate update = objectMapper.readValue(
                            message,
                            CoinbaseUpdate.class);

                    //then apply update with helper
                    updateNormalised(update, this.coinbaseOrderBook);

                    System.out.println("Received coinbase update: " + this.coinbaseOrderBook);
                }

            } else if (type.equals("subscriptions")) {
                //coinbase sends to confirm subscription is successful
                System.out.println("Coinbase subscription confirmed");

            } else if (type.equals("heartbeat")) {
                //sent periodically to check connection is still alive
            } else {
                System.out.println("Received unknown message from coinbase:" + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to process message: " + message);
            e.printStackTrace();
        }
    }

    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
    }

    public void onError(Exception e) {
        System.out.println("Error occurred: " + e);
    }

    //Translation helper for snapshots only
    private NormalisedOrderBook translateSnapshot(CoinbaseSnapshot snapshot) {

        NormalisedOrderBook normalisedBook = new NormalisedOrderBook();

        //set tradingPair and lastUpdated on new obj
        normalisedBook.setTradingPair(snapshot.getProduct_id());
        normalisedBook.setLastUpdated(Instant.now());

        //translate bids, loop through list of list of strings
        if (snapshot.getBids() != null) {
            //loop through each inner list representing one price level
            for (List<String> bidEntry : snapshot.getBids()) {
                //extract price (index 0) and quantity (index 1)
                String price = bidEntry.get(0);
                String quantity = bidEntry.get(1);

                //translate coinbase entry into normalised entry
                NormalisedOrderBookEntry normalisedEntry = new NormalisedOrderBookEntry(
                        new BigDecimal(price),
                        new BigDecimal(quantity),
                        Exchange.COINBASE
                );
                normalisedBook.getBids().add(normalisedEntry);
            }
        }

        //translate asks
        if (snapshot.getAsks() != null) {
            for (List<String> askEntry : snapshot.getAsks()) {
                String price = askEntry.get(0);
                String quantity = askEntry.get(1);

                NormalisedOrderBookEntry normalisedEntry = new NormalisedOrderBookEntry(
                        new BigDecimal(price),
                        new BigDecimal(quantity),
                        Exchange.COINBASE
                );
                normalisedBook.getAsks().add(normalisedEntry);
            }
        }
        return normalisedBook;
    }


    //Translation helper for updates (12update) messages only
    //will modify existing NormalisedOrderBook as update only contains changes
    private void updateNormalised(
            CoinbaseUpdate update,
            NormalisedOrderBook orderBook
    ) {
        //loop through list of changes for update obj
        for (List<String> change : update.getChanges()) {
            //extract data
            String sideStr = change.get(0); //'buy' or 'sell'
            String priceStr = change.get(1);
            String quantityStr = change.get(2);

            //convert to BigDecimal
            BigDecimal side = new BigDecimal(sideStr);
            BigDecimal price = new BigDecimal(priceStr);
            BigDecimal quantity = new BigDecimal(quantityStr);

            //apply to correct side
            //if buy
            if (side.equals("buy")) {
                //first, check if new quantity is zero, if so remove
                if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                    //remove if entry has matching price to entry from bids list
                    orderBook.getBids().removeIf(entry -> entry.price().equals(price));
                } else {
                    //if quantity is not zero, it's an update
                    //remove old entry at this price level first
                    orderBook.getBids().removeIf(entry -> entry.price().equals(price));

                    //then add new entry with updated quantity
                    orderBook.getBids().add(new NormalisedOrderBookEntry(
                            price,
                            quantity,
                            Exchange.COINBASE
                    ));
                    //after adding, list needs re-sorting from highest to lowest
                    orderBook.getBids().sort(Comparator.comparing(
                            NormalisedOrderBookEntry::price).reversed()
                    );
                }
            } else if (side.equals("sell")) {
                if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                    orderBook.getAsks().removeIf(entry -> entry.price().equals(price));
                } else {
                    orderBook.getAsks().removeIf(
                            entry -> entry.price().equals(price));
                    orderBook.getAsks().add(new NormalisedOrderBookEntry(
                            price,
                            quantity,
                            Exchange.COINBASE
                    ));
                    //sort from lowest to highest
                    orderBook.getAsks().sort(Comparator.comparing(
                            NormalisedOrderBookEntry::price
                    ));
                }
            }
        }
    }
}
