package com.amit.seatreservation.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.amit.seatreservation.dto.CreateShowRequest;
import com.amit.seatreservation.dto.CreateSeatRequest;
import com.amit.seatreservation.dto.ApiResponse;
import com.amit.seatreservation.dto.ShowResponse;
import com.amit.seatreservation.dto.SeatResponse;
import com.amit.seatreservation.service.ShowService;
import com.amit.seatreservation.service.SeatService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ShowService showService;
    private final SeatService seatService;

    public AdminController(
            ShowService showService,
            SeatService seatService) {

        this.showService = showService;
        this.seatService = seatService;
    }

    @PostMapping("/shows")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(
            @Valid @RequestBody CreateShowRequest request) {

        ShowResponse response = showService.createShow(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Show created successfully",
                        response
                ));
    }

    @PostMapping("/shows/{showId}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> createSeats(
            @PathVariable Long showId,
            @Valid @RequestBody CreateSeatRequest request) {

        List<SeatResponse> response =
                seatService.createSeats(showId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Seats created successfully",
                        response
                ));
    }
}
