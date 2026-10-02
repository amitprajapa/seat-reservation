package com.amit.seatreservation.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amit.seatreservation.dto.ApiResponse;
import com.amit.seatreservation.dto.ReservationResponse;
import com.amit.seatreservation.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
public class MyReservationsController {

    private final ReservationService reservationService;

    public MyReservationsController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getMyReservations(
            Authentication authentication) {

        List<ReservationResponse> reservations =
                reservationService.getMyReservations(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Bookings fetched successfully",
                        reservations
                )
        );
    }
}
