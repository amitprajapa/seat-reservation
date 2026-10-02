package com.amit.seatreservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponse {
	private Long reservationId;
	private Long showId;
	private Long userId;
	private List<Long> seatIds;
	private Long amountPaise;
	private String status;
	private LocalDateTime createdAt;

	public ReservationResponse() {
	}

	public ReservationResponse(Long reservationId, Long showId, Long userId, List<Long> seatIds, Long amountPaise,
			String status, LocalDateTime createdAt) {

		this.reservationId = reservationId;
		this.showId = showId;
		this.userId = userId;
		this.seatIds = seatIds;
		this.amountPaise = amountPaise;
		this.status = status;
		this.createdAt = createdAt;
	}

	public Long getReservationId() {
		return reservationId;
	}

	public void setReservationId(Long reservationId) {
		this.reservationId = reservationId;
	}

	public Long getShowId() {
		return showId;
	}

	public void setShowId(Long showId) {
		this.showId = showId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public List<Long> getSeatIds() {
		return seatIds;
	}

	public void setSeatIds(List<Long> seatIds) {
		this.seatIds = seatIds;
	}

	public Long getAmountPaise() {
		return amountPaise;
	}

	public void setAmountPaise(Long amountPaise) {
		this.amountPaise = amountPaise;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	
}
