package com.amit.seatreservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ReserveSeatsRequest {
	
	@NotEmpty(message = "At least one seat must be selected")
    @Size(max = 4, message = "Maximum 4 seats can be requested")
    private List<
            @NotNull(message = "Seat ID cannot be null")
            @Positive(message = "Seat ID must be positive")
            Long
    > seatIds;

    public ReserveSeatsRequest() {
    }

	public List<Long> getSeatIds() {
		return seatIds;
	}

	public void setSeatIds(List<Long> seatIds) {
		this.seatIds = seatIds;
	}
    
    
}
