package com.amit.seatreservation.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import com.amit.seatreservation.enums.SeatStatus;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_seat_show_number",
            columnNames = {"show_id", "seat_number"}
        )
    }
)
public class Seat {
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "show_id", nullable = false)
	    private Show show;

	    @Column(name = "seat_number", nullable = false, length = 20)
	    private String seatNumber;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "status", nullable = false, length = 20)
	    private SeatStatus status = SeatStatus.AVAILABLE;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "current_reservation_id")
	    private Reservation currentReservation;

	    @Column(name = "created_at", nullable = false, updatable = false)
	    private LocalDateTime createdAt;

	    @Column(name = "updated_at", nullable = false)
	    private LocalDateTime updatedAt;

	    @PrePersist
	    protected void onCreate() {
	        LocalDateTime now = LocalDateTime.now();

	        if (createdAt == null) {
	            createdAt = now;
	        }

	        if (updatedAt == null) {
	            updatedAt = now;
	        }
	    }

	    @PreUpdate
	    protected void onUpdate() {
	        updatedAt = LocalDateTime.now();
	    }

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Show getShow() {
			return show;
		}

		public void setShow(Show show) {
			this.show = show;
		}

		public String getSeatNumber() {
			return seatNumber;
		}

		public void setSeatNumber(String seatNumber) {
			this.seatNumber = seatNumber;
		}

		public SeatStatus getStatus() {
			return status;
		}

		public void setStatus(SeatStatus status) {
			this.status = status;
		}

		public Reservation getCurrentReservation() {
			return currentReservation;
		}

		public void setCurrentReservation(Reservation currentReservation) {
			this.currentReservation = currentReservation;
		}

		public LocalDateTime getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(LocalDateTime createdAt) {
			this.createdAt = createdAt;
		}

		public LocalDateTime getUpdatedAt() {
			return updatedAt;
		}

		public void setUpdatedAt(LocalDateTime updatedAt) {
			this.updatedAt = updatedAt;
		}
	    
	    

}
