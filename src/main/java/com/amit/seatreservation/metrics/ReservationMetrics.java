package com.amit.seatreservation.metrics;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;

@Component
public class ReservationMetrics {

    private final Counter confirmedCounter;
    private final Counter declinedCounter;
    private final Counter cancelledCounter;

    public ReservationMetrics(MeterRegistry registry) {

        this.confirmedCounter = Counter.builder(
                "reservation.confirmed.total")
                .description("Total successful reservations")
                .register(registry);

        this.declinedCounter = Counter.builder(
                "reservation.declined.total")
                .description("Total declined reservation attempts")
                .register(registry);

        this.cancelledCounter = Counter.builder(
                "reservation.cancelled.total")
                .description("Total successful cancellations")
                .register(registry);
    }

    public void incrementConfirmed() {
        confirmedCounter.increment();
    }

    public void incrementDeclined() {
        declinedCounter.increment();
    }

    public void incrementCancelled() {
        cancelledCounter.increment();
    }
}
