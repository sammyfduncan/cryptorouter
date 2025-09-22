package com.samsonduncan.cryptorouter.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsonduncan.cryptorouter.model.kraken.KrakenOrderBookMessage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.samsonduncan.cryptorouter.model.kraken.KrakenSubscriptionStatus;
import java.net.URI;
import java.net.URISyntaxException;
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

                KrakenOrderBookMessage bookMessage = objectMapper.treeToValue(
                        payload,
                        KrakenOrderBookMessage.class);
                System.out.println("Parsed book data for: " + pair + ": " + bookMessage);

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
        System.out.println("Error occured: " + e);
    }

}
