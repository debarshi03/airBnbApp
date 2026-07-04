package com.deb.project.airBnbApp.strategy;

import com.deb.project.airBnbApp.entity.Inventory;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@RequiredArgsConstructor
public class OccupancyPricingStrategy implements PricingStrategy{

    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePricing(Inventory inventory) {
        BigDecimal price=wrapped.calculatePricing(inventory);
        double occupancyRate=(double) inventory.getBookedCount()/ inventory.getTotalCount();
        if(occupancyRate>0.8){
            price=price.multiply(BigDecimal.valueOf(1.5));
        }

        return price;
    }
}
