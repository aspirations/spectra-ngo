package com.dertz.spectra.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

	private boolean success;
	private T data;
	private String message;
	private String errorCode;

	public static <T> ApiResponse<T> ok(T data) {
		return ApiResponse.<T>builder()
				.success(true)
				.data(data)
				.message("OK")
				.build();
	}

	public static <T> ApiResponse<T> ok(T data, String message) {
		return ApiResponse.<T>builder()
				.success(true)
				.data(data)
				.message(message)
				.build();
	}

	public static <T> ApiResponse<T> fail(String message, String errorCode) {
		return ApiResponse.<T>builder()
				.success(false)
				.message(message)
				.errorCode(errorCode)
				.build();
	}
}
