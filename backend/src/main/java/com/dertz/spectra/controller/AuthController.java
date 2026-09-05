package com.dertz.spectra.controller;

import com.dertz.spectra.dto.AuthResponse;
import com.dertz.spectra.dto.UserDTO;
import com.dertz.spectra.request.LoginRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		return ok(authService.login(request), "Signed in");
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserDTO>> me() {
		return ok(authService.me());
	}
}
