package com.amit.seatreservation.metrics;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amit.seatreservation.enums.SeatStatus;
import com.amit.seatreservation.repository.SeatRepository;

@Component
public class AvailabilityMetricsScheduler {

    private final SeatRepository seatRepository;
    private final AvailabilityMetrics availabilityMetrics;

    public AvailabilityMetricsScheduler(
            SeatRepository seatRepository,
            AvailabilityMetrics availabilityMetrics) {

        this.seatRepository = seatRepository;
        this.availabilityMetrics = availabilityMetrics;
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshAvailableSeats() {

        long count = seatRepository.countByStatus(
                SeatStatus.AVAILABLE
        );

        availabilityMetrics.updateAvailableSeats(count);
    }
}