package com.amit.seatreservation.metrics;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class AvailabilityMetrics {

    private final AtomicLong availableSeats = new AtomicLong(0);

    public AvailabilityMetrics(MeterRegistry registry) {

        Gauge.builder(
                "reservation.available.seats",
                availableSeats,
                AtomicLong::get
        )
        .description("Current available seats")
        .register(registry);
    }

    public void updateAvailableSeats(long count) {
        availableSeats.set(count);
    }
}
