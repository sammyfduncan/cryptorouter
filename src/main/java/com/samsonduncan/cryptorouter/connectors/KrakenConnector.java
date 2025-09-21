package com.samsonduncan.cryptorouter.connectors;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        //check what kind of message, first if contains status info
        if (message.contains("\"event\":\"subscriptionStatus\"")) {
            try {
                //if status message, parse into obj
                KrakenSubscriptionStatus status = objectMapper.readValue(
                        message,
                        KrakenSubscriptionStatus.class);
                System.out.println("Subscription status: " + status);
            } catch (Exception e) {
                //if error
                System.err.println("Error parsing subscription status: " + e.getMessage());
            }
        } else {
            //for now just print any other msg
            System.out.println("Recieved market data: " + message);
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

    //Start the connection
    @PostConstruct
    public void startConnection() {
        System.out.println("Starting construction, initialising kraken connection...");
        connect();
    }

}
