package com.samsonduncan.cryptorouter.config;

import com.samsonduncan.cryptorouter.connectors.KrakenConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class WebSocketConfig {

    @Bean
    public KrakenConnector krakenConnector() throws URISyntaxException {
        URI serverUri = new URI("wss://ws.kraken.com");
        System.out.println("Creating KrakenConnector bean...");
        return new KrakenConnector(serverUri);
    }
}
