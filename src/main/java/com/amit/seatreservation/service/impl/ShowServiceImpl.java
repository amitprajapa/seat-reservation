package com.amit.seatreservation.service.impl;


import com.amit.seatreservation.dto.CreateShowRequest;
import com.amit.seatreservation.dto.ShowResponse;
import com.amit.seatreservation.entity.Show;
import com.amit.seatreservation.exception.BusinessException;
import com.amit.seatreservation.exception.ResourceNotFoundException;
import com.amit.seatreservation.repository.ShowRepository;
import com.amit.seatreservation.service.ShowService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShowServiceImpl implements ShowService{
	
	private final ShowRepository showRepository;
	
	public ShowServiceImpl(ShowRepository showRepository) {
        this.showRepository = showRepository;
    }
	
	@Override
	@Transactional
	public ShowResponse createShow(CreateShowRequest request) {
		
		if(showRepository.existsByName(request.getName())) {
			throw new BusinessException("Show already exists");
		}
		
		Show show = new Show();
		show.setName(request.getName());
		show.setPricePaise(request.getPricePaise());
		show.setPerUserLimit(request.getPerUserLimit());
		
		Show savedShow = showRepository.save(show);
		
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
		Show show = showRepository.findById(showId)
				.orElseThrow(() -> new ResourceNotFoundException("Show not found with ID: " + showId));

		return mapToResponse(show);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShowResponse> getAllShows() {

		return showRepository.findAll().stream().map(this::mapToResponse).toList();
	}

}
