package com.deb.project.airBnbApp.strategy;

import com.deb.project.airBnbApp.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal calculatePricing(Inventory inventory);
}
