package com.amit.seatreservation.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amit.seatreservation.dto.LoginRequest;
import com.amit.seatreservation.dto.RegisterRequest;
import com.amit.seatreservation.dto.AuthResponse;
import com.amit.seatreservation.enums.Role;
import com.amit.seatreservation.entity.User;
import com.amit.seatreservation.exception.BusinessException;
import com.amit.seatreservation.repository.UserRepository;
import com.amit.seatreservation.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
	 private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;

	@Value("${app.jwt.expiration}")
	private long jwtExpiration;

	public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {

		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
	}

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {

		String email = request.getEmail().trim().toLowerCase();
		
		log.info("Registration attempt for email: {}", email);

		if (userRepository.existsByEmail(email)) {
			log.warn("Registration failed. Email already registered: {}", email);
			throw new BusinessException("Email is already registered");
		}

		User user = new User();

		user.setName(request.getName().trim());
		user.setEmail(email);

		// Never store plain-text passwords
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		user.setRole(Role.CUSTOMER);

		User savedUser = userRepository.save(user);

		log.info("User registered successfully. userId={}, email={}", savedUser.getId(), savedUser.getEmail());

		return generateToken(savedUser);
	}

	@Override
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {

		String email = request.getEmail().trim().toLowerCase();
		log.info("Login attempt for email: {}", email);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException("Invalid email or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			log.warn("Login failed. Invalid password for email: {}", email);
			throw new BusinessException("Invalid email or password");
		}
		
		log.info("Login successful. userId={}, email={}", user.getId(), user.getEmail());

		return generateToken(user);
	}
	
	@Override
	@Transactional
	public AuthResponse adminRegister(RegisterRequest request) {

		String email = request.getEmail().trim().toLowerCase();
		
		log.info("Registration attempt for email: {}", email);

		if (userRepository.existsByEmail(email)) {
			log.warn("Admin Registration failed. Email already registered: {}", email);
			throw new BusinessException("Email is already registered");
		}

		User user = new User();

		user.setName(request.getName().trim());
		user.setEmail(email);

		// Never store plain-text passwords
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		user.setRole(Role.ADMIN);

		User savedUser = userRepository.save(user);

		log.info("Admin registered successfully. userId={}, email={}", savedUser.getId(), savedUser.getEmail());

		return generateToken(savedUser);
	}

	private AuthResponse generateToken(User user) {
		log.debug("Generating JWT token. userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());
		Instant now = Instant.now();

		Instant expiry = now.plus(jwtExpiration, ChronoUnit.MILLIS);

		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("seat-reservation-api").issuedAt(now).expiresAt(expiry)
				.subject(user.getEmail()).claim("userId", user.getId()).claim("roles", List.of(user.getRole().name()))
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
		        .build();

		String token = jwtEncoder
		        .encode(JwtEncoderParameters.from(header, claims))
		        .getTokenValue();
		
		log.debug("JWT token generated successfully. userId={}, expiresAt={}", user.getId(), expiry);


		return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getRole().name());
	}
}