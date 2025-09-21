package com.samsonduncan.cryptorouter.model.kraken;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrakenOrderBookMessage {
    //for updates
    private List<KrakenOrderBookEntry> a;
    private List<KrakenOrderBookEntry> b;

    //for initial snapshot
    private List<KrakenOrderBookEntry> as;
    private List<KrakenOrderBookEntry> bs;
}


