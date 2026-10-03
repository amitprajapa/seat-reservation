package com.amit.seatreservation.service;


import com.amit.seatreservation.dto.AuthResponse;
import com.amit.seatreservation.dto.LoginRequest;
import com.amit.seatreservation.dto.RegisterRequest;

import jakarta.validation.Valid;

public interface AuthService {
	
	AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
	AuthResponse adminRegister(@Valid RegisterRequest request);

}
