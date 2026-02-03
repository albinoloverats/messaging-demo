package net.albinoloverats.messaging.demo.controller.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler
{
	@ExceptionHandler(SubsystemUnavailableException.class)
	public ResponseEntity<String> handleUnavailable(SubsystemUnavailableException cause)
	{
		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(cause.getMessage());
	}

	@ExceptionHandler(UnsupportedOperationException.class)
	public ResponseEntity<String> handleUnsupported(UnsupportedOperationException cause)
	{
		return ResponseEntity
				.status(HttpStatus.NOT_IMPLEMENTED)
				.body(cause.getMessage());
	}
}
