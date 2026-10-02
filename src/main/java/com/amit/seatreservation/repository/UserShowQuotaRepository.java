package com.amit.seatreservation.repository;

import com.amit.seatreservation.entity.UserShowQuota;
import com.amit.seatreservation.entity.UserShowQuotaId;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserShowQuotaRepository extends JpaRepository<UserShowQuota, UserShowQuotaId> {

	@Modifying
	@Transactional
	@Query(value = """
			INSERT INTO user_show_quotas
			    (user_id, show_id, active_seats, updated_at)
			VALUES
			    (:userId, :showId, 0, CURRENT_TIMESTAMP(6))
			ON DUPLICATE KEY UPDATE
			    user_id = VALUES(user_id)
			""", nativeQuery = true)
	int createQuotaIfNotExists(@Param("userId") Long userId, @Param("showId") Long showId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Transactional
	@Query("""
			SELECT q
			FROM UserShowQuota q
			WHERE q.id.userId = :userId
			  AND q.id.showId = :showId
			""")
	Optional<UserShowQuota> findQuotaForUpdate(@Param("userId") Long userId, @Param("showId") Long showId);
}