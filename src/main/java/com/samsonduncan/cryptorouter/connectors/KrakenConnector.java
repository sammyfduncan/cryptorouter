package com.samsonduncan.cryptorouter.connectors;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import jakarta.annotation.PostConstruct;

public class KrakenConnector extends WebSocketClient {

    public KrakenConnector(URI serverUri) {
        super(serverUri);
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
        System.out.println("Received message: " + message);
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
