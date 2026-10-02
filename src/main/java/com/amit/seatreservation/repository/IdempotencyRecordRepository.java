package com.amit.seatreservation.repository;

import com.amit.seatreservation.entity.IdempotencyRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

	Optional<IdempotencyRecord> findByUser_IdAndShow_IdAndIdempotencyKey(Long userId, Long showId,
			String idempotencyKey);

	boolean existsByUser_IdAndShow_IdAndIdempotencyKey(Long userId, Long showId, String idempotencyKey);
}