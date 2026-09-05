package com.dertz.spectra.controller;

import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardController extends BaseController {

	private final DashboardService dashboardService;

	@GetMapping("/dashboard")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER','VET_TECH_EMPLOYEE','EMPLOYEE')")
	public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
		return ok(dashboardService.snapshot());
	}
}
