package com.samsonduncan.cryptorouter.model.kraken;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
public class KrakenSubscriptionStatus {

    private String event;
    private String pair;
    private String status;

    @JsonProperty("subscription")
    private SubscriptionDetails subscription;

    @Data
    public static class SubscriptionDetails {
        private String name;
    }
}
