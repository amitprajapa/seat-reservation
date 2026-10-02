package com.amit.seatreservation.repository;

import com.amit.seatreservation.entity.Reservation;
import com.amit.seatreservation.entity.ReservationSeat;
import com.amit.seatreservation.enums.ReservationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	List<Reservation> findByUser_Id(Long userId);

	List<Reservation> findByShow_Id(Long showId);

	List<Reservation> findByUser_IdAndShow_IdAndStatus(Long userId, Long showId, ReservationStatus status);

	//Optional<Reservation> findByIdAndUser_Id(Long reservationId, Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT r
			FROM Reservation r
			WHERE r.id = :reservationId
			""")
	Optional<Reservation> findByIdForUpdate(@Param("reservationId") Long reservationId);
	
	//List<ReservationSeat> findByReservation_Id(Long reservationId);
	
	List<Reservation> findByUser_EmailOrderByCreatedAtDesc(String email);
	
	
}