package com.amit.seatreservation.dto;

public class AvailabilityResponse {
	private Long showId;
	private long total;
	private long available;
	private long held;
	private long confirmed;

	public AvailabilityResponse() {
	}

	public AvailabilityResponse(Long showId, long total, long available, long held, long confirmed) {

		this.showId = showId;
		this.total = total;
		this.available = available;
		this.held = held;
		this.confirmed = confirmed;
	}

	public Long getShowId() {
		return showId;
	}

	public void setShowId(Long showId) {
		this.showId = showId;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public long getAvailable() {
		return available;
	}

	public void setAvailable(long available) {
		this.available = available;
	}

	public long getHeld() {
		return held;
	}

	public void setHeld(long held) {
		this.held = held;
	}

	public long getConfirmed() {
		return confirmed;
	}

	public void setConfirmed(long confirmed) {
		this.confirmed = confirmed;
	}
	
	

}
