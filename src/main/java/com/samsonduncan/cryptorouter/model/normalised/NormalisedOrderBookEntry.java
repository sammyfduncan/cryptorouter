package com.samsonduncan.cryptorouter.model.normalised;

import java.math.*;

//Record to represent single price level data
public record NormalisedOrderBookEntry (
    BigDecimal price,
    BigDecimal quantity,
    Exchange exchange //uses Exchange enum for type safety
) {}


