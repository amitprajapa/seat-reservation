package com.amit.seatreservation.service;

import java.util.List;

import com.amit.seatreservation.dto.ReservationResponse;
import com.amit.seatreservation.dto.ReserveSeatsRequest;

public interface ReservationService {

	ReservationResponse reserveSeats(Long showId, ReserveSeatsRequest request, String customerEmail,
			String idempotencyKey);
	
	void cancelReservation(Long reservationId, String customerEmail);

	List<ReservationResponse> getMyReservations(String customerEmail);
}
