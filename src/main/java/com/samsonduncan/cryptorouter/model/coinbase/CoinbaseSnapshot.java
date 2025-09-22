package com.samsonduncan.cryptorouter.model.coinbase;

import lombok.Data;

import java.util.List;

//Represents 'snapshot' of entire order book sent from coinbase
@Data
public class CoinbaseSnapshot {

    private String type;
    private String product_id;
    //use lists of lists as coinbase sends each price level as arr of 2 str
    //eg ["price", "size"]
    private List<List<String>> bids;
    private List<List<String>> asks;
}
