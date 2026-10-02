package com.amit.seatreservation.repository;

import com.amit.seatreservation.entity.ReservationSeat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

	 List<ReservationSeat> findByReservation_Id(Long reservationId);

	List<ReservationSeat> findBySeat_Id(Long seatId);

	long countByReservation_Id(Long reservationId);
}