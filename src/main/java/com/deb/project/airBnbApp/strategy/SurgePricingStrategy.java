package com.deb.project.airBnbApp.strategy;

import com.deb.project.airBnbApp.entity.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@RequiredArgsConstructor
public class SurgePricingStrategy implements PricingStrategy{

    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePricing(Inventory inventory) {
        return wrapped.calculatePricing(inventory).multiply(inventory.getSurgeFactor());
    }
}
