package com.amit.seatreservation.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;

import com.amit.seatreservation.dto.ReservationResponse;
import com.amit.seatreservation.dto.ReserveSeatsRequest;
import com.amit.seatreservation.entity.IdempotencyRecord;
import com.amit.seatreservation.entity.Reservation;
import com.amit.seatreservation.entity.ReservationSeat;
import com.amit.seatreservation.entity.Seat;
import com.amit.seatreservation.entity.Show;
import com.amit.seatreservation.entity.User;
import com.amit.seatreservation.entity.UserShowQuota;
import com.amit.seatreservation.enums.ReservationStatus;
import com.amit.seatreservation.enums.SeatStatus;
import com.amit.seatreservation.exception.BusinessException;
import com.amit.seatreservation.exception.ResourceNotFoundException;
import com.amit.seatreservation.exception.SeatUnavailableException;
import com.amit.seatreservation.metrics.ReservationMetrics;
import com.amit.seatreservation.repository.IdempotencyRecordRepository;
import com.amit.seatreservation.repository.ReservationRepository;
import com.amit.seatreservation.repository.ReservationSeatRepository;
import com.amit.seatreservation.repository.SeatRepository;
import com.amit.seatreservation.repository.ShowRepository;
import com.amit.seatreservation.repository.UserRepository;
import com.amit.seatreservation.repository.UserShowQuotaRepository;
import com.amit.seatreservation.service.ReservationService;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ReservationServiceImpl implements ReservationService {
	
	private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final UserShowQuotaRepository quotaRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final ObjectMapper objectMapper;
    private final ReservationMetrics reservationMetrics;
    
	public ReservationServiceImpl(ReservationRepository reservationRepository, SeatRepository seatRepository,
			ReservationSeatRepository reservationSeatRepository, UserShowQuotaRepository quotaRepository,
			IdempotencyRecordRepository idempotencyRepository, UserRepository userRepository,
			ShowRepository showRepository, ObjectMapper objectMapper,ReservationMetrics reservationMetrics) {
		this.reservationRepository = reservationRepository;
		this.seatRepository = seatRepository;
		this.reservationSeatRepository = reservationSeatRepository;
		this.quotaRepository = quotaRepository;
		this.idempotencyRepository = idempotencyRepository;
		this.userRepository = userRepository;
		this.showRepository = showRepository;
		this.objectMapper = objectMapper;
		this.reservationMetrics = reservationMetrics;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ReservationResponse reserveSeats(Long showId, ReserveSeatsRequest request, String customerEmail,
			String idempotencyKey) {
			if(idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
				throw new BusinessException("Valid Idempotency-Key header is required");
			}
			
			List<Long> requestedSeatIds = request.getSeatIds();

	        if (requestedSeatIds == null || requestedSeatIds.isEmpty()) {
	            throw new BusinessException("At least one seat is required");
	        }

	        if (requestedSeatIds.size() > 4) {
	            throw new BusinessException("Maximum 4 seats can be reserved");
	        }

	        if (requestedSeatIds.stream().distinct().count()
	                != requestedSeatIds.size()) {
	            throw new BusinessException("Duplicate seat IDs are not allowed");
	        }
	        
	        // Sort IDs for deterministic locking and hashing
	        List<Long> seatIds = new ArrayList<>(requestedSeatIds);
	        Collections.sort(seatIds);
	        
	        //Identify authenticated customer
	        
	        User user = userRepository.findByEmail(customerEmail).orElseThrow(()->new ResourceNotFoundException("Customer not found"));
	        
	        //Find Show
	        Show show = showRepository.findById(showId).orElseThrow(()->new ResourceNotFoundException("Show not found"));
	        
	        //Create quota row if it does not exist
	        quotaRepository.createQuotaIfNotExists(user.getId(), showId);
	        
	        //Lock quote show
	        UserShowQuota quota = quotaRepository
	                .findQuotaForUpdate(user.getId(), showId)
	                .orElseThrow(() ->
	                        new BusinessException("Unable to initialize customer quota"));
	        
	        //Calculate request hash
	        String requestHash = calculateHash(seatIds);
	        
	        //Check existing idempotency record
	        var existingRecord = idempotencyRepository
	                .findByUser_IdAndShow_IdAndIdempotencyKey(
	                        user.getId(), showId, idempotencyKey);
	        
	        if (existingRecord.isPresent()) {

	            IdempotencyRecord record = existingRecord.get();

	            if (!record.getRequestHash().equals(requestHash)) {
	                throw new BusinessException(
	                        "Idempotency key already used with a different request");
	            }

	            try {
	                return objectMapper.readValue(record.getResponseJson(),ReservationResponse.class);
	            } catch (Exception e) {
	                throw new BusinessException(
	                        "Unable to retrieve original reservation response");
	            }
	            
	            
	        }
	        
	        //Lock All Request
	        List<Seat> seats = seatRepository.findSeatsForUpdate(seatIds);
	        if (seats.size() != seatIds.size()) {
	            throw new ResourceNotFoundException("One or more seats do not exist");
	        }
	        
	        //Validate seat ownership and availability
	        for (Seat seat : seats) {

	            if (!seat.getShow().getId().equals(showId)) {
	                throw new BusinessException(
	                        "Seat does not belong to the requested show");
	            }

	            if (seat.getStatus() != SeatStatus.AVAILABLE) {
	                throw new SeatUnavailableException("Seat " + seat.getSeatNumber() + " is unavailable");
	            }
	        }
	        
	     //Enforce customer seat limit
			int configuredLimit = show.getPerUserLimit() == null ? 4 : show.getPerUserLimit();
			int effectiveLimit = Math.min(4, configuredLimit);
			if (quota.getActiveSeats() + seats.size() > effectiveLimit) {
				throw new BusinessException("Maximum seat limit exceeded for this show");
			}
			
			//Calculate amount in paise
			long amountPaise;

	        try {
	            amountPaise = Math.multiplyExact(
	                    show.getPricePaise(),
	                    (long) seats.size());
	        } catch (ArithmeticException e) {
	            throw new BusinessException("Reservation amount overflow");
	        }
	        
	        //Create reservation
	        Reservation reservation = new Reservation();
	        reservation.setShow(show);
	        reservation.setUser(user);
	        reservation.setAmountPaise(amountPaise);
	        reservation.setStatus(ReservationStatus.CONFIRMED);

	        reservation = reservationRepository.save(reservation);
	        
	        //Update seats and create reservation-seat mappings
	        List<ReservationSeat> reservationSeats = new ArrayList<>();

	        for (Seat seat : seats) {
	            seat.setStatus(SeatStatus.CONFIRMED);
	            seat.setCurrentReservation(reservation);
	            ReservationSeat reservationSeat = new ReservationSeat();
	            reservationSeat.setReservation(reservation);
	            reservationSeat.setSeat(seat);

	            reservationSeats.add(reservationSeat);
	        }
	        
	        seatRepository.saveAll(seats);
	        reservationSeatRepository.saveAll(reservationSeats);

	        // 14. Update customer quota
	        quota.setActiveSeats(quota.getActiveSeats() + seats.size());
	        quotaRepository.save(quota);

	        // 15. Build response
	        List<Long> bookedSeatIds = new ArrayList<>();

	        for (Seat seat : seats) {
	            bookedSeatIds.add(seat.getId());
	        }
	        
			ReservationResponse response = new ReservationResponse(reservation.getId(), show.getId(), user.getId(),
					bookedSeatIds, amountPaise, reservation.getStatus().name(), reservation.getCreatedAt());
	        
			// 16. Save idempotency record in same transaction
	        IdempotencyRecord record = new IdempotencyRecord();

	        record.setUser(user);
	        record.setShow(show);
	        record.setIdempotencyKey(idempotencyKey);
	        record.setRequestHash(requestHash);
	        record.setReservation(reservation);
	        record.setResponseStatus(201);

	        try {
	            record.setResponseJson(
	                    objectMapper.writeValueAsString(response));
	        } catch (Exception e) {
	            throw new BusinessException(
	                    "Unable to store reservation response");
	        }

	        idempotencyRepository.save(record);
	        reservationMetrics.incrementConfirmed();
	        return response;
	}
	
	@Transactional
	@Override
	public void cancelReservation(Long reservationId, String customerEmail) {

		//Find authenticated customer
		User user = userRepository.findByEmail(customerEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		//Lock reservation
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

		// Verify ownership
		if (!reservation.getUser().getId().equals(user.getId())) {
			throw new BusinessException("You are not authorized to cancel this reservation");
		}

		//Prevent repeated cancellation
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			throw new BusinessException("Reservation already cancelled");
		}

		//Get reservation seat mappings
		List<ReservationSeat> mappings = reservationSeatRepository.findByReservation_Id(reservationId);

		List<Long> seatIds = mappings.stream().map(mapping -> mapping.getSeat().getId()).sorted().toList();

		//Lock seats
		List<Seat> seats = seatRepository.findSeatsForUpdate(seatIds);

		//Release seats
		for (Seat seat : seats) {

			if (!reservationId.equals(seat.getCurrentReservation().getId())) {
				throw new BusinessException("Seat reservation state is inconsistent");
			}

			seat.setStatus(SeatStatus.AVAILABLE);
			seat.setCurrentReservation(null);
		}

		seatRepository.saveAll(seats);

		//Lock customer quota
		UserShowQuota quota = quotaRepository.findQuotaForUpdate(user.getId(), reservation.getShow().getId())
				.orElseThrow(() -> new BusinessException("Customer quota not found"));

		//Restore quota
		int updatedActiveSeats = quota.getActiveSeats() - seats.size();

		if (updatedActiveSeats < 0) {
			throw new BusinessException("Invalid customer quota state");
		}

		quota.setActiveSeats(updatedActiveSeats);
		quotaRepository.save(quota);

		//Update reservation status
		reservation.setStatus(ReservationStatus.CANCELLED);
		reservationRepository.save(reservation);
		reservationMetrics.incrementCancelled();
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<ReservationResponse> getMyReservations(String customerEmail) {

		List<Reservation> reservations = reservationRepository.findByUser_EmailOrderByCreatedAtDesc(customerEmail);

		return reservations.stream().map(reservation -> {

			List<Long> seatIds = reservation.getReservationSeats().stream()
					.map(reservationSeat -> reservationSeat.getSeat().getId()).toList();

			return new ReservationResponse(reservation.getId(), reservation.getShow().getId(),
					reservation.getUser().getId(), seatIds, reservation.getAmountPaise(),
					reservation.getStatus().name(), reservation.getCreatedAt());
		}).toList();
	}

	private String calculateHash(List<Long> seatIds) {
		try {
			String canonicalRequest = seatIds.toString();
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to calculate request hash", e);
		}
	}
}
