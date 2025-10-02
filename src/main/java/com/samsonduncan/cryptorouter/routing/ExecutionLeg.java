package com.samsonduncan.cryptorouter.routing;

import com.samsonduncan.cryptorouter.model.normalised.Exchange;

import java.math.BigDecimal;

public record ExecutionLeg(
        Exchange exchange,
        BigDecimal quantity,
        BigDecimal price
) {}
