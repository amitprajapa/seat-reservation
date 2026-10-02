package com.amit.seatreservation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private ErrorResponse buildError(HttpStatus status, String message, HttpServletRequest request) {

		return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message,
				request.getRequestURI());
	}

	// 1. Resource not found - 404
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request));
	}

	// 2. Seat unavailable - 409
	@ExceptionHandler(SeatUnavailableException.class)
	public ResponseEntity<ErrorResponse> handleSeatUnavailable(SeatUnavailableException ex,
			HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(buildError(HttpStatus.CONFLICT, ex.getMessage(), request));
	}

	// 3. Business rule violation - 409
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(buildError(HttpStatus.CONFLICT, ex.getMessage(), request));
	}

	// 4. Request validation failure - 400
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		Map<String, String> fieldErrors = new HashMap<>();

		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}

		Map<String, Object> response = new HashMap<>();

		response.put("timestamp", LocalDateTime.now());
		response.put("status", 400);
		response.put("error", "Bad Request");
		response.put("message", "Validation failed");
		response.put("fieldErrors", fieldErrors);
		response.put("path", request.getRequestURI());

		return ResponseEntity.badRequest().body(response);
	}

	// 5. Constraint validation failure - 400
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {

		return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, "Validation failed", request));
	}

	// 6. Database constraint violation - 409
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDatabaseConflict(DataIntegrityViolationException ex,
			HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(buildError(HttpStatus.CONFLICT, "Database constraint violation", request));
	}

	// 7. Unexpected error - 500
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request));
	}
}