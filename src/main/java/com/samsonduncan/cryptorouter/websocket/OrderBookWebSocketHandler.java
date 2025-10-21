package com.samsonduncan.cryptorouter.websocket;

import com.samsonduncan.cryptorouter.services.OrderBookService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class OrderBookWebSocketHandler extends TextWebSocketHandler {

    private final OrderBookService orderBookService;
    private final List<WebSocketSession> sessionList = new CopyOnWriteArrayList<>();

    public OrderBookWebSocketHandler(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
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
}
