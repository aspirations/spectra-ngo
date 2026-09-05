package com.dertz.spectra.controller;

import com.dertz.spectra.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

	protected ResponseEntity<byte[]> pdfFile(byte[] pdf, String filename) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdf);
	}

	protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
		return ResponseEntity.ok(ApiResponse.ok(data));
	}

	protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
		return ResponseEntity.ok(ApiResponse.ok(data, message));
	}
}
