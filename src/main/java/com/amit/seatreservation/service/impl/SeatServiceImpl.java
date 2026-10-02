package com.amit.seatreservation.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amit.seatreservation.dto.AvailabilityResponse;
import com.amit.seatreservation.dto.CreateSeatRequest;
import com.amit.seatreservation.dto.SeatResponse;
import com.amit.seatreservation.dto.ShowResponse;
import com.amit.seatreservation.entity.Seat;
import com.amit.seatreservation.entity.Show;
import com.amit.seatreservation.enums.SeatStatus;
import com.amit.seatreservation.exception.BusinessException;
import com.amit.seatreservation.exception.ResourceNotFoundException;
import com.amit.seatreservation.repository.SeatRepository;
import com.amit.seatreservation.repository.ShowRepository;
import com.amit.seatreservation.service.SeatService;



@Service
public class SeatServiceImpl implements SeatService {
	
	private final SeatRepository seatRepository;
	private final ShowRepository showRepository;

	public SeatServiceImpl(SeatRepository seatRepository, ShowRepository showRepository) {
		this.seatRepository = seatRepository;
		this.showRepository = showRepository;
	}
	

	@Override
	public List<SeatResponse> createSeats(Long showId, CreateSeatRequest request) {
		
		Show show = showRepository.findById(showId).orElseThrow(()->new ResourceNotFoundException("Show not found with ID: " + showId));
		
		List<String> seatNumbers = request.getSeatNumbers();
		Set<String> uniqueSeatNumbers = new HashSet<>(seatNumbers);

        if (uniqueSeatNumbers.size() != seatNumbers.size()) {
            throw new BusinessException("Duplicate seat numbers in request"
            );
        }
        
     // Check whether seats already exist for this show.
        for (String seatNumber : seatNumbers) {

            if (seatRepository.existsByShow_IdAndSeatNumber(showId, seatNumber)) {
                throw new BusinessException("Seat already exists: " + seatNumber
                );
            }
        }
		List<Seat> seats = seatNumbers.stream().map(seatNumber -> {
			Seat seat = new Seat();

			seat.setShow(show);
			seat.setSeatNumber(seatNumber);
			seat.setStatus(SeatStatus.AVAILABLE);
			return seat;
		}).toList();
        
		List<Seat> savedSeatsList = seatRepository.saveAll(seats);
		
		return savedSeatsList.stream()
                .map(this::mapToResponse)
                .toList();
	}
	private SeatResponse mapToResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getStatus().name()
        );
    }

	@Override
	@Transactional(readOnly = true)
	public List<SeatResponse> getSeatsByShow(Long showId) {
		
		if(!seatRepository.existsById(showId)) {
			throw new ResourceNotFoundException(
                    "Show not found with ID: " + showId
            );
		}
		
		return seatRepository.findByShow_Id(showId)
                .stream()
                .map(this::mapToResponse)
                .toList();
		
	}

	@Override
	public AvailabilityResponse getAvailability(Long showId) {

		if(!seatRepository.existsById(showId)) {
			throw new ResourceNotFoundException(
                    "Show not found with ID: " + showId
            );
		}
		
		long total = seatRepository.countByShow_Id(showId);
		
		long available = seatRepository.countByShow_IdAndStatus(
                showId,
                SeatStatus.AVAILABLE
        );
		
		long confirmed = seatRepository.countByShow_IdAndStatus(showId, SeatStatus.CONFIRMED);
		
		long held = 0;
		
		return new AvailabilityResponse(showId, total, available, held, confirmed);
	}

}
