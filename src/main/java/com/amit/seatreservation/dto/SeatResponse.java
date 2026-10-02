package com.amit.seatreservation.dto;

public class SeatResponse {
	private Long id;
    private String seatNumber;
    private String status;

    public SeatResponse() {
    }

    public SeatResponse(Long id, String seatNumber, String status) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.status = status;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSeatNumber() {
		return seatNumber;
	}

	public void setSeatNumber(String seatNumber) {
		this.seatNumber = seatNumber;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
    
    
}
