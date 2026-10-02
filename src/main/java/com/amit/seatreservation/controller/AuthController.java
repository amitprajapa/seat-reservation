package com.amit.seatreservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.amit.seatreservation.dto.RegisterRequest;
import com.amit.seatreservation.dto.LoginRequest;
import com.amit.seatreservation.dto.AuthResponse;
import com.amit.seatreservation.dto.ApiResponse;
import com.amit.seatreservation.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {

		AuthResponse response = authService.register(request);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new ApiResponse<>(true, "Registration successful", response));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {

		AuthResponse response = authService.login(request);

		return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response));
	}
}