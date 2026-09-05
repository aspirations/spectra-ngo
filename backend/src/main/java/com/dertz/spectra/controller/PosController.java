package com.dertz.spectra.controller;

import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.request.PosOrderRequest;
import com.dertz.spectra.request.StaffProductRequest;
import com.dertz.spectra.request.StaffPurchaseRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.StaffPosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosController extends BaseController {

	private final StaffPosService staffPosService;

	@GetMapping("/products")
	public ResponseEntity<ApiResponse<List<ShelterProduct>>> products(@RequestParam(required = false) String q) {
		return ok(staffPosService.products(q));
	}

	@PostMapping("/products")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<ShelterProduct>> createProduct(@Valid @RequestBody StaffProductRequest request) {
		return ok(staffPosService.createProduct(request), "Retail SKU saved");
	}

	@PutMapping("/products/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<ShelterProduct>> updateProduct(@PathVariable Long id,
			@Valid @RequestBody StaffProductRequest request) {
		return ok(staffPosService.updateProduct(id, request), "Retail SKU updated");
	}

	@PostMapping("/purchases")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<Map<String, Object>>> receivePurchase(@Valid @RequestBody StaffPurchaseRequest request) {
		return ok(staffPosService.receivePurchase(request), "Purchase received into staff store");
	}

	@GetMapping("/purchases")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> purchases() {
		return ok(staffPosService.purchases());
	}

	@GetMapping("/pnl")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<Map<String, Object>>> pnl(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ok(staffPosService.profitAndLoss(from, to));
	}

	@PostMapping("/orders")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<Map<String, Object>>> checkout(@Valid @RequestBody PosOrderRequest request) {
		return ok(staffPosService.checkout(request), "Sale completed");
	}

	@GetMapping("/orders")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> orders() {
		return ok(staffPosService.branchOrders());
	}

	@GetMapping("/passbook/{userId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> passbook(@PathVariable Long userId) {
		return ok(staffPosService.passbook(userId));
	}

	@GetMapping("/credit/{userId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> credit(@PathVariable Long userId) {
		return ok(staffPosService.creditSnapshot(userId));
	}
}
