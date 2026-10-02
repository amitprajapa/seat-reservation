package com.amit.seatreservation.service;

import java.util.List;

import com.amit.seatreservation.dto.AvailabilityResponse;
import com.amit.seatreservation.dto.CreateSeatRequest;
import com.amit.seatreservation.dto.SeatResponse;

public interface SeatService {
	List<SeatResponse> createSeats(Long showId, CreateSeatRequest request);
	List<SeatResponse> getSeatsByShow(Long showId);
	AvailabilityResponse getAvailability(Long showId);

}
