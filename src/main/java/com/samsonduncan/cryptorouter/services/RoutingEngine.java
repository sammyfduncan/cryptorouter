package com.samsonduncan.cryptorouter.services;

import com.samsonduncan.cryptorouter.model.normalised.Exchange;
import com.samsonduncan.cryptorouter.routing.ExecutionLeg;
import com.samsonduncan.cryptorouter.routing.ExecutionPlan;
import com.samsonduncan.cryptorouter.routing.OrderSide;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/*
Core routing logic
Responsible for taking consolidated data from OrderBookService and using
it to calculate the most optimal trade execution plan
 */
@Service
public class RoutingEngine {

    private final OrderBookService orderBookService;

    public RoutingEngine(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    //Main algorithm for finding routing positions
    public ExecutionPlan calculateRoute(
            OrderSide side, BigDecimal totalQuantity) {

        //variables
        List<ExecutionLeg> legs = new ArrayList<>();
        BigDecimal quantityLeft = totalQuantity;
        //used to calculate final VWAP:
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQuantityFilled = BigDecimal.ZERO;

        Map<BigDecimal, Map<Exchange, BigDecimal>> bookToWalk;

        //select the correct book
        if (side.equals(OrderSide.BUY)) {
            //assign asks map to bookToWalk
            bookToWalk = orderBookService.getAsks();
        } else {
            //assign bids map to bookToWalk
            bookToWalk = orderBookService.getBids();
        }

        //walk the book loop
        //outer loop starts here
        for (Map.Entry<BigDecimal, Map<Exchange, BigDecimal>> priceLevelEntry : bookToWalk.entrySet()) {
            BigDecimal price = priceLevelEntry.getKey();
            Map<Exchange, BigDecimal> liquidityAtPrice = priceLevelEntry.getValue();

            //inner loop (exchanges at this price)
            for (Map.Entry<Exchange, BigDecimal> exchangeEntry : liquidityAtPrice.entrySet()) {
                Exchange exchange = exchangeEntry.getKey();
                BigDecimal availableQuantity = exchangeEntry.getValue();

                //fill logic
                //determine how much to take; min of what's needed vs what's available
                BigDecimal quantityToTake = quantityLeft.min(availableQuantity);

                //if taking a non-zero amount, record it
                if (quantityToTake.compareTo(BigDecimal.ZERO) > 0) {
                    //create new leg for execution plan
                    ExecutionLeg leg = new ExecutionLeg(exchange, quantityToTake, price);
                    legs.add(leg); //add to list of legs

                    //update tracked vars
                    quantityLeft = quantityLeft.subtract(quantityToTake); //decrease remaining quantity
                    totalQuantityFilled = totalQuantityFilled.add(quantityToTake); //increase total filled
                    totalCost = totalCost.add(quantityToTake.multiply(price)); //add cost of this leg

                    //check for completion inside inner loop
                    //if filled entire order, stop immediately
                    if (quantityLeft.compareTo(BigDecimal.ZERO) <= 0) {
                        break; //exit inner exchange loop
                    }
                }
            }
            //end of inner loop
            //now check for completion of outer loop
            //after checking all exchanges at price level, check again if done
            if (quantityLeft.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
        //end of outer loop
        //finalise and return plan
        //handle insufficient liquidity
        String notes = "Order filled successfully.";
        if (quantityLeft.compareTo(BigDecimal.ZERO) > 0) {
            notes = "Could not fill full quantity. " + quantityLeft + "remaining.";
        }

        //calculate VWAP
        BigDecimal vwap = BigDecimal.ZERO;
        if (totalQuantityFilled.compareTo(BigDecimal.ZERO) > 0) {
            vwap = totalCost.divide(totalQuantityFilled, MathContext.DECIMAL64);
        }

        //return the plan
        return new ExecutionPlan(legs, vwap, notes);
    }
}
