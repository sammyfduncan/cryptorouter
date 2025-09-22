package com.samsonduncan.cryptorouter.config;

import com.samsonduncan.cryptorouter.connectors.CoinbaseConnector;
import com.samsonduncan.cryptorouter.connectors.KrakenConnector;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class WebSocketConfig {

    //KrakenConnector bean
    @Bean
    public KrakenConnector krakenConnector() throws URISyntaxException {
        URI serverUri = new URI("wss://ws.kraken.com");
        System.out.println("Creating KrakenConnector bean...");
        return new KrakenConnector(serverUri);
    }

    //CoinbaseConnector bean
    @Bean
    public CoinbaseConnector coinbaseConnector() throws URISyntaxException {
        URI serverUri = new URI("wss://ws-feed.pro.coinbase.com");
        System.out.println("Creating CoinbaseConnector bean...");
        return new CoinbaseConnector(serverUri);
    }

    //Automatically receives connectors above and connects when app is ready
    @Bean
    public ApplicationRunner applicationRunner(
            KrakenConnector krakenConnector,
            CoinbaseConnector coinbaseConnector
    ) {
        return args -> {
            System.out.println("Starting connectors...");
            krakenConnector.connect();
            coinbaseConnector.connect();
            System.out.println("Connectors started");
        };
    }

}
