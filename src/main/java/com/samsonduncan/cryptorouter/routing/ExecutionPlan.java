package com.samsonduncan.cryptorouter.routing;

import com.samsonduncan.cryptorouter.model.normalised.Exchange;

import java.math.BigDecimal;
import java.util.List;

//Represents a single portion of the total order
public record ExecutionPlan(
    List<ExecutionLeg> legs,
    BigDecimal vwap,
    String notes
) {}
