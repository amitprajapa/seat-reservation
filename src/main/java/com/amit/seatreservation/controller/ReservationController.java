package com.amit.seatreservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.amit.seatreservation.dto.ApiResponse;
import com.amit.seatreservation.dto.ReservationResponse;
import com.amit.seatreservation.dto.ReserveSeatsRequest;
import com.amit.seatreservation.service.ReservationService;

@RestController
@RequestMapping("/api/shows/{showId}/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ReservationResponse>> reserveSeats(@PathVariable Long showId,
			@Valid @RequestBody ReserveSeatsRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey,
			Authentication authentication) {

		String customerEmail = authentication.getName();

		ReservationResponse response = reservationService.reserveSeats(showId, request, customerEmail, idempotencyKey);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new ApiResponse<>(true, "Seats reserved successfully", response));
	}
	
	@PostMapping("/{reservationId}/cancel")
	public ResponseEntity<ApiResponse<String>> cancelReservation(@PathVariable Long reservationId,
			Authentication authentication) {
		reservationService.cancelReservation(reservationId, authentication.getName());
		return ResponseEntity.ok(new ApiResponse<>(true, "Reservation cancelled successfully", "CANCELLED"));
	}
	
	@GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getMyReservations(
            Authentication authentication) {

        String customerEmail = authentication.getName();

        List<ReservationResponse> reservations =
                reservationService.getMyReservations(customerEmail);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Bookings fetched successfully",
                        reservations
                )
        );
    }
}
