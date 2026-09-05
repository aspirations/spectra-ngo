package com.dertz.spectra.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

	public ResourceNotFoundException(String message) {
		super(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
	}
}
