package com.samsonduncan.cryptorouter.config;

import com.samsonduncan.cryptorouter.websocket.OrderBookWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketBrokerConfig implements WebSocketConfigurer {

    private final OrderBookWebSocketHandler orderBookWebSocketHandler;

    public WebSocketBrokerConfig(OrderBookWebSocketHandler orderBookWebSocketHandler) {
        this.orderBookWebSocketHandler = orderBookWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderBookWebSocketHandler, "/ws/orderbook")
                .setAllowedOrigins("*");
    }
}
