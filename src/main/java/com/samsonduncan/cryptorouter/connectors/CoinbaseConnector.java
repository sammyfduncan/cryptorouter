package com.samsonduncan.cryptorouter.connectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsonduncan.cryptorouter.model.coinbase.CoinbaseSnapshot;
import com.samsonduncan.cryptorouter.model.coinbase.CoinbaseUpdate;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class CoinbaseConnector extends WebSocketClient {

    //jackson objmapper engine
    private final ObjectMapper objectMapper;

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
                System.out.println("Received coinbase snapshot: " + snapshot);
            } else if (type.equals("12update")) {
                //if 12update, parse msg into CoinbaseUpdate
                CoinbaseUpdate update = objectMapper.readValue(
                        message,
                        CoinbaseUpdate.class);
                System.out.println("Received coinbase update: " + update);
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

    }

    public void onError(Exception e) {

    }

    public void startConnection() {

    }
}
