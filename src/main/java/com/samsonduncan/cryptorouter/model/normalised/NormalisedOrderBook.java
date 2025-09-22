package com.samsonduncan.cryptorouter.model.normalised;

import lombok.Data;
import java.time.Instant;
import java.util.*;

//Standardised order book obj for a single trading pair.
@Data
public class NormalisedOrderBook {

    private String tradingPair;
    private Instant lastUpdated; //timestamp of the update

    //list of all bids sorted by highest
    private List<NormalisedOrderBookEntry> bids = new ArrayList<>();
    //list of asks sorted by lowest
    private List<NormalisedOrderBookEntry> asks = new ArrayList<>();
}

