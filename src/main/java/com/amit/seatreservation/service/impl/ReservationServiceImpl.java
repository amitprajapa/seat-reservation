package com.amit.seatreservation.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.amit.seatreservation.enums.Role;
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

	private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

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
			ShowRepository showRepository, ObjectMapper objectMapper, ReservationMetrics reservationMetrics) {

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

		log.info("Starting seat reservation. showId={}, customerEmail={}", showId, customerEmail);

		if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {

			log.warn("Invalid idempotency key provided. showId={}, customerEmail={}", showId, customerEmail);

			throw new BusinessException("Valid Idempotency-Key header is required");
		}

		List<Long> requestedSeatIds = request.getSeatIds();

		if (requestedSeatIds == null || requestedSeatIds.isEmpty()) {

			log.warn("Reservation attempted without seats. showId={}, customerEmail={}", showId, customerEmail);

			throw new BusinessException("At least one seat is required");
		}

		if (requestedSeatIds.size() > 4) {

			log.warn("Reservation exceeds maximum seat limit. showId={}, requestedSeats={}", showId,
					requestedSeatIds.size());

			throw new BusinessException("Maximum 4 seats can be reserved");
		}

		if (requestedSeatIds.stream().distinct().count() != requestedSeatIds.size()) {

			log.warn("Duplicate seat IDs in reservation request. showId={}, customerEmail={}", showId, customerEmail);

			throw new BusinessException("Duplicate seat IDs are not allowed");
		}

		// Sort IDs for deterministic locking and hashing
		List<Long> seatIds = new ArrayList<>(requestedSeatIds);
		Collections.sort(seatIds);

		log.debug("Sorted seat IDs for reservation. showId={}, seatIds={}", showId, seatIds);

		// Identify authenticated customer
		User user = userRepository.findByEmail(customerEmail).orElseThrow(() -> {
			log.warn("Customer not found. customerEmail={}", customerEmail);

			return new ResourceNotFoundException("Customer not found");
		});
		
		if(Role.ADMIN.equals(user.getRole())) {
			throw new ResourceNotFoundException("Only Users Can Book Seats");
		}

		// Find Show
		Show show = showRepository.findById(showId).orElseThrow(() -> {
			log.warn("Show not found. showId={}", showId);

			return new ResourceNotFoundException("Show not found");
		});

		log.debug("Customer and show identified. userId={}, showId={}", user.getId(), showId);

		// Create quota row if it does not exist
		quotaRepository.createQuotaIfNotExists(user.getId(), showId);

		// Lock quota row
		UserShowQuota quota = quotaRepository.findQuotaForUpdate(user.getId(), showId).orElseThrow(() -> {
			log.error("Unable to initialize customer quota. userId={}, showId={}", user.getId(), showId);

			return new BusinessException("Unable to initialize customer quota");
		});

		// Calculate request hash
		String requestHash = calculateHash(seatIds);

		log.debug("Reservation request hash calculated. userId={}, showId={}", user.getId(), showId);

		// Check existing idempotency record
		var existingRecord = idempotencyRepository.findByUser_IdAndShow_IdAndIdempotencyKey(user.getId(), showId,
				idempotencyKey);

		if (existingRecord.isPresent()) {

			log.info("Existing idempotency record found. userId={}, showId={}", user.getId(), showId);

			IdempotencyRecord record = existingRecord.get();

			if (!record.getRequestHash().equals(requestHash)) {

				log.warn("Idempotency key reused with different request. userId={}, showId={}", user.getId(), showId);

				throw new BusinessException("Idempotency key already used with a different request");
			}

			try {

				log.info("Returning original reservation response for idempotent request. userId={}, showId={}",
						user.getId(), showId);

				return objectMapper.readValue(record.getResponseJson(), ReservationResponse.class);

			} catch (Exception e) {

				log.error("Unable to deserialize original reservation response. userId={}, showId={}", user.getId(),
						showId, e);

				throw new BusinessException("Unable to retrieve original reservation response");
			}
		}

		// Lock all requested seats
		log.debug("Locking requested seats. showId={}, seatIds={}", showId, seatIds);

		List<Seat> seats = seatRepository.findSeatsForUpdate(seatIds);

		if (seats.size() != seatIds.size()) {

			log.warn("One or more requested seats do not exist. showId={}, requestedSeats={}, foundSeats={}", showId,
					seatIds.size(), seats.size());

			throw new ResourceNotFoundException("One or more seats do not exist");
		}

		// Validate seat ownership and availability
		for (Seat seat : seats) {

			if (!seat.getShow().getId().equals(showId)) {

				log.warn("Seat does not belong to requested show. seatId={}, showId={}", seat.getId(), showId);

				throw new BusinessException("Seat does not belong to the requested show");
			}

			if (seat.getStatus() != SeatStatus.AVAILABLE) {

				log.warn("Seat is unavailable. seatId={}, seatNumber={}, status={}", seat.getId(), seat.getSeatNumber(),
						seat.getStatus());

				throw new SeatUnavailableException("Seat " + seat.getSeatNumber() + " is unavailable");
			}
		}

		log.debug("All requested seats are available. showId={}, seatCount={}", showId, seats.size());

		// Enforce customer seat limit
		int configuredLimit = show.getPerUserLimit() == null ? 4 : show.getPerUserLimit();

		int effectiveLimit = Math.min(4, configuredLimit);

		if (quota.getActiveSeats() + seats.size() > effectiveLimit) {

			log.warn("Customer seat limit exceeded. userId={}, showId={}, activeSeats={}, requestedSeats={}, limit={}",
					user.getId(), showId, quota.getActiveSeats(), seats.size(), effectiveLimit);

			throw new BusinessException("Maximum seat limit exceeded for this show");
		}

		// Calculate amount in paise
		long amountPaise;

		try {

			amountPaise = Math.multiplyExact(show.getPricePaise(), (long) seats.size());

		} catch (ArithmeticException e) {

			log.error("Reservation amount overflow. showId={}, seatCount={}, pricePaise={}", showId, seats.size(),
					show.getPricePaise(), e);

			throw new BusinessException("Reservation amount overflow");
		}

		log.debug("Reservation amount calculated. showId={}, seatCount={}, amountPaise={}", showId, seats.size(),
				amountPaise);

		// Create reservation
		Reservation reservation = new Reservation();

		reservation.setShow(show);
		reservation.setUser(user);
		reservation.setAmountPaise(amountPaise);
		reservation.setStatus(ReservationStatus.CONFIRMED);

		reservation = reservationRepository.save(reservation);

		log.info("Reservation created. reservationId={}, userId={}, showId={}", reservation.getId(), user.getId(),
				showId);

		// Update seats and create reservation-seat mappings
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

		log.debug("Seats confirmed and reservation-seat mappings created. reservationId={}, seatCount={}",
				reservation.getId(), seats.size());

		// Update customer quota
		quota.setActiveSeats(quota.getActiveSeats() + seats.size());

		quotaRepository.save(quota);

		log.debug("Customer quota updated. userId={}, showId={}, activeSeats={}", user.getId(), showId,
				quota.getActiveSeats());

		// Build response
		List<Long> bookedSeatIds = new ArrayList<>();

		for (Seat seat : seats) {
			bookedSeatIds.add(seat.getId());
		}

		ReservationResponse response = new ReservationResponse(reservation.getId(), show.getId(), user.getId(),
				bookedSeatIds, amountPaise, reservation.getStatus().name(), reservation.getCreatedAt());

		// Save idempotency record in same transaction
		IdempotencyRecord record = new IdempotencyRecord();

		record.setUser(user);
		record.setShow(show);
		record.setIdempotencyKey(idempotencyKey);
		record.setRequestHash(requestHash);
		record.setReservation(reservation);
		record.setResponseStatus(201);

		try {

			record.setResponseJson(objectMapper.writeValueAsString(response));

		} catch (Exception e) {

			log.error("Unable to serialize reservation response. reservationId={}, userId={}, showId={}",
					reservation.getId(), user.getId(), showId, e);

			throw new BusinessException("Unable to store reservation response");
		}

		idempotencyRepository.save(record);

		reservationMetrics.incrementConfirmed();

		log.info(
				"Reservation completed successfully. reservationId={}, userId={}, showId={}, seatCount={}, amountPaise={}",
				reservation.getId(), user.getId(), showId, seats.size(), amountPaise);

		return response;
	}

	@Transactional
	@Override
	public void cancelReservation(Long reservationId, String customerEmail) {

		log.info("Starting reservation cancellation. reservationId={}, customerEmail={}", reservationId, customerEmail);

		// Find authenticated customer
		User user = userRepository.findByEmail(customerEmail).orElseThrow(() -> {
			log.warn("User not found while cancelling reservation. customerEmail={}", customerEmail);

			return new ResourceNotFoundException("User not found");
		});

		// Lock reservation
		Reservation reservation = reservationRepository.findByIdForUpdate(reservationId).orElseThrow(() -> {
			log.warn("Reservation not found. reservationId={}", reservationId);

			return new ResourceNotFoundException("Reservation not found");
		});

		// Verify ownership
		if (!reservation.getUser().getId().equals(user.getId())) {

			log.warn("Unauthorized reservation cancellation attempt. reservationId={}, userId={}", reservationId,
					user.getId());

			throw new BusinessException("You are not authorized to cancel this reservation");
		}

		// Prevent repeated cancellation
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {

			log.warn("Reservation already cancelled. reservationId={}, userId={}", reservationId, user.getId());

			throw new BusinessException("Reservation already cancelled");
		}

		// Get reservation seat mappings
		List<ReservationSeat> mappings = reservationSeatRepository.findByReservation_Id(reservationId);

		List<Long> seatIds = mappings.stream().map(mapping -> mapping.getSeat().getId()).sorted().toList();

		log.debug("Seats associated with reservation. reservationId={}, seatIds={}", reservationId, seatIds);

		// Lock seats
		List<Seat> seats = seatRepository.findSeatsForUpdate(seatIds);

		// Release seats
		for (Seat seat : seats) {

			if (!reservationId.equals(seat.getCurrentReservation().getId())) {

				log.error("Seat reservation state is inconsistent. reservationId={}, seatId={}", reservationId,
						seat.getId());

				throw new BusinessException("Seat reservation state is inconsistent");
			}

			seat.setStatus(SeatStatus.AVAILABLE);
			seat.setCurrentReservation(null);
		}

		seatRepository.saveAll(seats);

		log.debug("Seats released successfully. reservationId={}, seatCount={}", reservationId, seats.size());

		// Lock customer quota
		UserShowQuota quota = quotaRepository.findQuotaForUpdate(user.getId(), reservation.getShow().getId())
				.orElseThrow(() -> {

					log.error("Customer quota not found. userId={}, showId={}", user.getId(),
							reservation.getShow().getId());

					return new BusinessException("Customer quota not found");
				});

		// Restore quota
		int updatedActiveSeats = quota.getActiveSeats() - seats.size();

		if (updatedActiveSeats < 0) {

			log.error("Invalid customer quota state. userId={}, showId={}, activeSeats={}, releasedSeats={}",
					user.getId(), reservation.getShow().getId(), quota.getActiveSeats(), seats.size());

			throw new BusinessException("Invalid customer quota state");
		}

		quota.setActiveSeats(updatedActiveSeats);
		quotaRepository.save(quota);

		// Update reservation status
		reservation.setStatus(ReservationStatus.CANCELLED);
		reservationRepository.save(reservation);

		reservationMetrics.incrementCancelled();

		log.info("Reservation cancelled successfully. reservationId={}, userId={}, showId={}, releasedSeats={}",
				reservationId, user.getId(), reservation.getShow().getId(), seats.size());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReservationResponse> getMyReservations(String customerEmail) {

		log.info("Fetching reservations for customer. customerEmail={}", customerEmail);

		List<Reservation> reservations = reservationRepository.findByUser_EmailOrderByCreatedAtDesc(customerEmail);

		log.debug("Found {} reservations for customerEmail={}", reservations.size(), customerEmail);

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
			log.error("Unable to calculate reservation request hash. seatIds={}", seatIds, e);
			throw new IllegalStateException("Unable to calculate request hash", e);
		}
	}
}
