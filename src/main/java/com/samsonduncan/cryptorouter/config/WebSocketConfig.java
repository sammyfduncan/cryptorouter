package com.samsonduncan.cryptorouter.config;

import com.samsonduncan.cryptorouter.connectors.CoinbaseConnector;
import com.samsonduncan.cryptorouter.connectors.KrakenConnector;
import com.samsonduncan.cryptorouter.services.CoinbaseAuthService;
import com.samsonduncan.cryptorouter.services.OrderBookService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class WebSocketConfig {

    //KrakenConnector bean
    @Bean
    public KrakenConnector krakenConnector(OrderBookService orderBookService) throws URISyntaxException {
        URI serverUri = new URI("wss://ws.kraken.com");
        System.out.println("Creating KrakenConnector bean...");
        return new KrakenConnector(serverUri, orderBookService);
    }

    //CoinbaseConnector bean
    @Bean
    public CoinbaseConnector coinbaseConnector(CoinbaseAuthService authService, OrderBookService orderBookService) throws URISyntaxException, NoSuchAlgorithmException {
        URI serverUri = new URI("wss://ws-feed.exchange.coinbase.com");
        System.out.println("Creating CoinbaseConnector bean...");

        //get standard ssl context and factory
        SSLContext sslContext = SSLContext.getDefault();
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();

        return new CoinbaseConnector(serverUri, socketFactory, authService, orderBookService);
    }

    //Automatically receives connectors above and connects when app is ready
    @Bean
    public ApplicationRunner applicationRunner(
            KrakenConnector krakenConnector,
            CoinbaseConnector coinbaseConnector,
            OrderBookService orderBookService
    ) {
        return args -> {
            System.out.println("Starting connectors...");
            krakenConnector.connect();
            coinbaseConnector.connect();

            System.out.println("Connectors started");
        };
    }
}
