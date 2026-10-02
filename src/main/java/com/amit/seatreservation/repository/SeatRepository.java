package com.amit.seatreservation.repository;

import com.amit.seatreservation.entity.Seat;
import com.amit.seatreservation.enums.SeatStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShow_Id(Long showId);

    List<Seat> findByShow_IdAndStatus(
            Long showId,
            SeatStatus status
    );

    long countByShow_IdAndStatus(
            Long showId,
            SeatStatus status
    );

    boolean existsByShow_IdAndSeatNumber(
            Long showId,
            String seatNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds ORDER BY s.id")
    List<Seat> findSeatsForUpdate(@Param("seatIds") List<Long> seatIds);

	long countByShow_Id(Long showId);
	
	long countByStatus(SeatStatus status);
}