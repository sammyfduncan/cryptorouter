package com.samsonduncan.cryptorouter.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsonduncan.cryptorouter.model.normalised.Exchange;
import com.samsonduncan.cryptorouter.services.OrderBookService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class OrderBookWebSocketHandler extends TextWebSocketHandler {

    private final OrderBookService orderBookService;
    private final List<WebSocketSession> sessionList = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public OrderBookWebSocketHandler(OrderBookService orderBookService, ObjectMapper objectMapper) {
        this.orderBookService = orderBookService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        sessionList.add(session);
        System.out.println("New client connected.");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessionList.remove(session);
        System.out.println("Client disconnected.");
    }

    //Spring calls every second
    @Scheduled(fixedRate = 1000)
    public void broadcastOrderBookUpdate() {

        //get data
        Map<BigDecimal, Map<Exchange, BigDecimal>> currentBids = orderBookService.getBids();
        Map<BigDecimal, Map<Exchange, BigDecimal>> currentAsks = orderBookService.getAsks();

        //package data
        Map<String, Object> data = Map.of("bids", currentBids, "asks", currentAsks);

        //convert to JSON
        //get JSON payload
        try {
            String jsonPayload = objectMapper.writeValueAsString(data);

            //send the data
            for (WebSocketSession session : sessionList) {
                if (session.isOpen()) {
                    //if session is open, send the payload
                    session.sendMessage(new TextMessage(jsonPayload));
                }
            }
        } catch (IOException e) {
            System.err.print("Error broadcasting order book update " + e.getMessage());
        }
    }
}
