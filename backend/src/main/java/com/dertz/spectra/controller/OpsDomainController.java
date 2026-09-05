package com.dertz.spectra.controller;

import com.dertz.spectra.model.AnalyticsAlert;
import com.dertz.spectra.model.DogExpenseLink;
import com.dertz.spectra.model.FuelLog;
import com.dertz.spectra.model.OperatingExpense;
import com.dertz.spectra.request.DogExpenseRequest;
import com.dertz.spectra.request.FuelLogRequest;
import com.dertz.spectra.request.OperatingExpenseRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.OpsDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
public class OpsDomainController extends BaseController {

	private final OpsDomainService opsDomainService;

	@GetMapping("/snapshot")
	public ResponseEntity<ApiResponse<Map<String, Object>>> snapshot() {
		return ok(opsDomainService.snapshot());
	}

	@GetMapping("/expenses")
	public ResponseEntity<ApiResponse<List<OperatingExpense>>> expenses() {
		return ok(opsDomainService.expenses());
	}

	@GetMapping("/dog-expenses")
	public ResponseEntity<ApiResponse<List<DogExpenseLink>>> dogExpenses() {
		return ok(opsDomainService.dogExpenses());
	}

	@GetMapping("/dog-expenses/{residentId}")
	public ResponseEntity<ApiResponse<List<DogExpenseLink>>> dogExpensesFor(@PathVariable Long residentId) {
		return ok(opsDomainService.dogExpensesFor(residentId));
	}

	@GetMapping("/fuel")
	public ResponseEntity<ApiResponse<List<FuelLog>>> fuel() {
		return ok(opsDomainService.fuelLogs());
	}

	@GetMapping("/alerts")
	public ResponseEntity<ApiResponse<List<AnalyticsAlert>>> alerts() {
		return ok(opsDomainService.alerts());
	}

	@PostMapping("/alerts/{id}/ack")
	public ResponseEntity<ApiResponse<AnalyticsAlert>> ack(@PathVariable Long id) {
		return ok(opsDomainService.acknowledge(id), "Alert closed");
	}

	@PostMapping("/expenses")
	public ResponseEntity<ApiResponse<OperatingExpense>> recordExpense(@Valid @RequestBody OperatingExpenseRequest request) {
		return ok(opsDomainService.recordExpense(request), "Expense recorded");
	}

	@PostMapping("/fuel")
	public ResponseEntity<ApiResponse<FuelLog>> recordFuel(@Valid @RequestBody FuelLogRequest request) {
		return ok(opsDomainService.recordFuel(request), "Fuel fill recorded");
	}

	@PostMapping("/dog-expenses")
	public ResponseEntity<ApiResponse<DogExpenseLink>> recordDogExpense(@Valid @RequestBody DogExpenseRequest request) {
		return ok(opsDomainService.recordDogExpense(request), "Dog cost linked");
	}

	@GetMapping("/sla")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> sla() {
		return ok(opsDomainService.sla());
	}

	@GetMapping("/activity")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> activity() {
		return ok(opsDomainService.activity());
	}
}
