package com.deb.project.airBnbApp.service;

import com.deb.project.airBnbApp.entity.Booking;

public interface CheckoutService {

    String getCheckoutSession(Booking booking, String successUrl, String failureUrl);
}
