package com.amit.seatreservation.service.impl;


import com.amit.seatreservation.dto.CreateShowRequest;
import com.amit.seatreservation.dto.ShowResponse;
import com.amit.seatreservation.entity.Show;
import com.amit.seatreservation.exception.BusinessException;
import com.amit.seatreservation.exception.ResourceNotFoundException;
import com.amit.seatreservation.repository.ShowRepository;
import com.amit.seatreservation.service.ShowService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShowServiceImpl implements ShowService{
	
	private static final Logger log = LoggerFactory.getLogger(ShowServiceImpl.class);
	
	private final ShowRepository showRepository;
	
	public ShowServiceImpl(ShowRepository showRepository) {
        this.showRepository = showRepository;
    }
	
	@Override
	@Transactional
	public ShowResponse createShow(CreateShowRequest request) {
		log.info("Creating show. name={}", request.getName());
		if(showRepository.existsByName(request.getName())) {
			 log.warn("Show creation failed. Show already exists. name={}",
	                    request.getName());
			throw new BusinessException("Show already exists");
		}
		
		Show show = new Show();
		show.setName(request.getName());
		show.setPricePaise(request.getPricePaise());
		show.setPerUserLimit(request.getPerUserLimit());
		
		Show savedShow = showRepository.save(show);
		log.info("Show created successfully. showId={}, name={}",
                savedShow.getId(), savedShow.getName());
		
		return mapToResponse(savedShow);
		
	}
	
	private ShowResponse mapToResponse(Show show) {

        return new ShowResponse(
                show.getId(),
                show.getName(),
                show.getPricePaise(),
                show.getPerUserLimit()
        );
    }

	@Override
	@Transactional(readOnly = true)
	public ShowResponse getShowById(Long showId) {
		log.debug("Fetching show. showId={}", showId);
		Show show = showRepository.findById(showId)
				.orElseThrow(() -> new ResourceNotFoundException("Show not found with ID: " + showId));

		return mapToResponse(show);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShowResponse> getAllShows() {
		log.debug("Fetching all shows");
		List<ShowResponse> shows = showRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();

        log.info("Shows fetched successfully. count={}", shows.size());
        return shows;
	}

}
