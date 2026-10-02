package com.amit.seatreservation.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amit.seatreservation.dto.ApiResponse;
import com.amit.seatreservation.dto.CreateShowRequest;
import com.amit.seatreservation.dto.ShowResponse;
import com.amit.seatreservation.service.ShowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

	private final ShowService showService;

	public ShowController(ShowService showService) {
		this.showService = showService;
	}

	// GET: Retrieve show by ID
	@GetMapping("/{showId}")
	public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable Long showId) {

		ShowResponse response = showService.getShowById(showId);

		return ResponseEntity.ok(new ApiResponse<>(true, "Show retrieved successfully", response));
	}

	// GET: Retrieve all shows
	@GetMapping
	public ResponseEntity<ApiResponse<List<ShowResponse>>> getAllShows() {

		List<ShowResponse> shows = showService.getAllShows();

		return ResponseEntity.ok(new ApiResponse<>(true, "Shows retrieved successfully", shows));
	}
}
