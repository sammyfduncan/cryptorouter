package com.samsonduncan.cryptorouter.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsonduncan.cryptorouter.model.kraken.KrakenOrderBookEntry;
import com.samsonduncan.cryptorouter.model.kraken.KrakenOrderBookMessage;
import com.samsonduncan.cryptorouter.model.normalised.Exchange;
import com.samsonduncan.cryptorouter.model.normalised.NormalisedOrderBook;
import com.samsonduncan.cryptorouter.model.normalised.NormalisedOrderBookEntry;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.samsonduncan.cryptorouter.model.kraken.KrakenSubscriptionStatus;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class KrakenConnector extends WebSocketClient {

    //main engine from Jackson
    private final ObjectMapper objectMapper;

    public KrakenConnector(URI serverUri) {
        super(serverUri);

        //configure objectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("Connected to Kraken");

        String subscriptionMessage = """
            {
             "event": "subscribe",
             "pair": ["XBT/USD"],
             "subscription": { "name": "book", "depth": 10 }
            }
            """;

        send(subscriptionMessage);
        System.out.println("Sent subscription message for XBT/USD book");
    }

    //later, here parse JSON and update order book
    @Override
    public void onMessage(String message) {
        try {
            //check what kind of message
            if (message.contains("\"event\":\"subscriptionStatus\"")) {
                KrakenSubscriptionStatus status = objectMapper.readValue(
                        message,
                        KrakenSubscriptionStatus.class);
                System.out.println("Subscription status: " + status);

            } else if (message.contains("\"book-10\"")) {
                //it's an order book snapshot or update
                JsonNode rootNode = objectMapper.readTree(message);
                JsonNode payload = rootNode.get(1); //data obj is second element
                String pair = rootNode.get(3).asText(); //pair is fourth element

                //parse message
                KrakenOrderBookMessage bookMessage = objectMapper.treeToValue(
                        payload,
                        KrakenOrderBookMessage.class);

                //now translate parsed message
                NormalisedOrderBook normalisedBook = translateMessage(
                        bookMessage,
                        pair);
                System.out.println("Parsed and translated book data: " + normalisedBook);

            } else {
                //some other message, log for now
                System.out.println("Received other message: " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to process message: " + message);
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
    }

    @Override
    public void onError(Exception e) {
        System.out.println("Error occurred: " + e);
    }

    //helper to translate raw msg into NormalisedOrderBook
    private NormalisedOrderBook translateMessage(
            KrakenOrderBookMessage krakenMessage,
            String tradingPair) {
        NormalisedOrderBook normalisedBook = new NormalisedOrderBook();
        Instant lastUpdated = Instant.now();

        normalisedBook.setTradingPair(tradingPair);
        normalisedBook.setLastUpdated(lastUpdated);

        //translate bids

        //if message has snapshot of bids 'bs'
        if (krakenMessage.getBs() != null) {
            //loop thru each entry in kraken snapshot bid list
            for (KrakenOrderBookEntry krakenEntry : krakenMessage.getBs()) {
                //translate kraken entry into normalised entry
                NormalisedOrderBookEntry normalisedEntry = new NormalisedOrderBookEntry(
                        new BigDecimal(krakenEntry.getPrice()), //convert price str to BD
                        new BigDecimal(krakenEntry.getVolume()), //convert volume str
                        Exchange.KRAKEN //set exchange enum
                );
                //add translated entry to normalised book bid list
                normalisedBook.getBids().add(normalisedEntry);
            }
        }

        //if message has update of bids 'b'
        if (krakenMessage.getB() != null) {
            for (KrakenOrderBookEntry krakenEntry : krakenMessage.getB()) {
                NormalisedOrderBookEntry normalisedEntry = new NormalisedOrderBookEntry(
                        new BigDecimal(krakenEntry.getPrice()),
                        new BigDecimal(krakenEntry.getVolume()),
                        Exchange.KRAKEN
                );
                normalisedBook.getBids().add(normalisedEntry);
            }
        }

        //if message has snapshot of asks 'as'
        if (krakenMessage.getAs() != null) {
            for (KrakenOrderBookEntry krakenEntry : krakenMessage.getAs()) {
                NormalisedOrderBookEntry normalisedEntry = new NormalisedOrderBookEntry(
                        new BigDecimal(krakenEntry.getPrice()),
                        new BigDecimal(krakenEntry.getVolume()),
                        Exchange.KRAKEN);
                normalisedBook.getAsks().add(normalisedEntry);
            }
        }

        //if message has snapshot of asks 'a'
        if (krakenMessage.getA() != null) {
            for (KrakenOrderBookEntry krakenEntry : krakenMessage.getA()) {
                NormalisedOrderBookEntry normalisedEntry = new NormalisedOrderBookEntry(
                        new BigDecimal(krakenEntry.getPrice()),
                        new BigDecimal(krakenEntry.getVolume()),
                        Exchange.KRAKEN);
                normalisedBook.getAsks().add(normalisedEntry);
            }
        }

        return normalisedBook;
    }

}
