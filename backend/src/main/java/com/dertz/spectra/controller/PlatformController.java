package com.dertz.spectra.controller;

import com.dertz.spectra.dto.NgoDTO;
import com.dertz.spectra.request.CreateNgoRequest;
import com.dertz.spectra.request.NgoStatusRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.PlatformService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/platform/ngos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformController extends BaseController {

	private final PlatformService platformService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<NgoDTO>>> list() {
		return ok(platformService.listNgos());
	}

	@PostMapping
	public ResponseEntity<ApiResponse<NgoDTO>> create(@Valid @RequestBody CreateNgoRequest request) {
		return ok(platformService.createNgo(request), "Organisation created");
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<ApiResponse<NgoDTO>> status(@PathVariable Long id, @Valid @RequestBody NgoStatusRequest request) {
		return ok(platformService.setStatus(id, request.getStatus()), "Organisation updated");
	}
}
