package com.amit.seatreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateSeatRequest {
	
	@NotEmpty(message = "Seat numbers cannot be empty")
    @Size(max = 500, message = "Maximum 500 seats allowed")
    private List<
            @NotBlank(message = "Seat number cannot be blank")
            @Size(max = 20, message = "Seat number cannot exceed 20 characters")
            String
    > seatNumbers;
	
	public CreateSeatRequest() {
    }

	public List<String> getSeatNumbers() {
		return seatNumbers;
	}

	public void setSeatNumbers(List<String> seatNumbers) {
		this.seatNumbers = seatNumbers;
	}
	
	
}