package com.amit.seatreservation.repository;

import com.amit.seatreservation.entity.Show;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    boolean existsByName(String name);

	//long countByShow_Id(Long showId);
	
	//List<Show> findByShowId(Long showId);
}