package com.samsonduncan.cryptorouter.model.coinbase;

import lombok.Data;

import java.util.List;

//Holds update messages from coinbase
@Data
public class CoinbaseUpdate {
    private String type;
    private String product_id;
    //12update contains list of changes, each change is arr
    private List<List<String>> changes;
}

