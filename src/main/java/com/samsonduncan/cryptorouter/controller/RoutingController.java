package com.samsonduncan.cryptorouter.controller;

import com.samsonduncan.cryptorouter.routing.ExecutionPlan;
import com.samsonduncan.cryptorouter.routing.OrderSide;
import com.samsonduncan.cryptorouter.services.RoutingEngine;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class RoutingController {

    //inject routing engine
    private final RoutingEngine routingEngine;

    public RoutingController(RoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
    }

    //API endpoint method
    @PostMapping("/route")
    public ExecutionPlan getExecutionPlan(
            @RequestParam("side") String side,
            @RequestParam("quantity") String quantity
    ) {
        //convert inputs to correct types
        OrderSide sideEnum = OrderSide.valueOf(side.toUpperCase());
        BigDecimal quantityDecimal = new BigDecimal(quantity);

        //call the engine + return result
        return routingEngine.calculateRoute(sideEnum, quantityDecimal);


    }
}
