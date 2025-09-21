package com.samsonduncan.cryptorouter.model.kraken;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({ "price", "volume", "timestamp", "updateType" })
public class KrakenOrderBookEntry {
    private String price;
    private String volume;
    private String timestamp;
    private String updateType; //optional
}
