package com.amit.seatreservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amit.seatreservation.dto.ApiResponse;
import com.amit.seatreservation.dto.AvailabilityResponse;
import com.amit.seatreservation.dto.CreateSeatRequest;
import com.amit.seatreservation.dto.SeatResponse;
import com.amit.seatreservation.service.SeatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shows/{showId}")
public class SeatController {

	private final SeatService seatService;

	public SeatController(SeatService seatService) {
		this.seatService = seatService;
	}

	// Get all seats for a show
	@GetMapping("/seats")
	public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeats(@PathVariable Long showId) {

		List<SeatResponse> seats = seatService.getSeatsByShow(showId);

		return ResponseEntity.ok(new ApiResponse<>(true, "Seats retrieved successfully", seats));
	}

	// Get seat availability
	@GetMapping("/availability")
	public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(@PathVariable Long showId) {

		AvailabilityResponse availabilityResponse = seatService.getAvailability(showId);

		return ResponseEntity.ok(new ApiResponse<>(true, "Availability retrieved successfully", availabilityResponse));
	}
}