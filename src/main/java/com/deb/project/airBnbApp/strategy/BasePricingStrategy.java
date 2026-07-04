package com.deb.project.airBnbApp.strategy;

import com.deb.project.airBnbApp.entity.Inventory;

import java.math.BigDecimal;

public class BasePricingStrategy implements PricingStrategy{
    @Override
    public BigDecimal calculatePricing(Inventory inventory) {
        return inventory.getRoom().getBasePrice();
    }
}
