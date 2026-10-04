package com.dertz.spectra.controller;

import com.dertz.spectra.model.InternalConsumption;
import com.dertz.spectra.model.InventoryAuditLog;
import com.dertz.spectra.model.PurchaseGrn;
import com.dertz.spectra.model.PurchaseGrnItem;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.StockBatch;
import com.dertz.spectra.model.Supplier;
import com.dertz.spectra.model.SupplierPayment;
import com.dertz.spectra.request.AuditRequest;
import com.dertz.spectra.request.AuditReviewRequest;
import com.dertz.spectra.request.BatchExpiryRequest;
import com.dertz.spectra.request.ConsumeRequest;
import com.dertz.spectra.request.GrnRequest;
import com.dertz.spectra.request.IssueConsumeRequest;
import com.dertz.spectra.request.ShelterProductRequest;
import com.dertz.spectra.request.SupplierPaymentRequest;
import com.dertz.spectra.request.SupplierRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.InventoryService;
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
import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController extends BaseController {

	private final InventoryService inventoryService;

	@GetMapping("/products")
	public ResponseEntity<ApiResponse<List<ShelterProduct>>> products() {
		return ok(inventoryService.products());
	}

	@PostMapping("/products")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<ShelterProduct>> createProduct(@Valid @RequestBody ShelterProductRequest request) {
		return ok(inventoryService.createProduct(request), "Product created");
	}

	@PutMapping("/products/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<ShelterProduct>> updateProduct(@PathVariable Long id,
			@Valid @RequestBody ShelterProductRequest request) {
		return ok(inventoryService.updateProduct(id, request), "Product updated");
	}

	@GetMapping("/suppliers")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER','EMPLOYEE')")
	public ResponseEntity<ApiResponse<List<Supplier>>> suppliers() {
		return ok(inventoryService.suppliers());
	}

	@PostMapping("/suppliers")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<Supplier>> createSupplier(@Valid @RequestBody SupplierRequest request) {
		return ok(inventoryService.createSupplier(request), "Supplier created");
	}

	@PostMapping("/suppliers/{id}/payments")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<SupplierPayment>> pay(@PathVariable Long id,
			@Valid @RequestBody SupplierPaymentRequest request) {
		return ok(inventoryService.paySupplier(id, request), "Payment recorded");
	}

	@GetMapping("/suppliers/{id}/payments")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<SupplierPayment>>> payments(@PathVariable Long id) {
		return ok(inventoryService.supplierPayments(id));
	}

	@GetMapping("/low-stock")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> lowStock() {
		return ok(inventoryService.lowStock());
	}

	@GetMapping("/grn")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<PurchaseGrn>>> grns() {
		return ok(inventoryService.grns());
	}

	@GetMapping("/grn/{id}/items")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<PurchaseGrnItem>>> grnItems(@PathVariable Long id) {
		return ok(inventoryService.grnItems(id));
	}

	@PostMapping("/grn")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<PurchaseGrn>> createGrn(@Valid @RequestBody GrnRequest request) {
		return ok(inventoryService.createGrn(request), "GRN posted");
	}

	@GetMapping("/batches")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<StockBatch>>> batches() {
		return ok(inventoryService.batches());
	}

	@PutMapping("/batches/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<StockBatch>> updateBatchExpiry(@PathVariable Long id,
			@Valid @RequestBody BatchExpiryRequest request) {
		return ok(inventoryService.updateBatchExpiry(id, request), "Expiry updated");
	}

	@PostMapping("/consume")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER','VET_TECH_EMPLOYEE','EMPLOYEE')")
	public ResponseEntity<ApiResponse<InternalConsumption>> consume(@Valid @RequestBody ConsumeRequest request) {
		var result = inventoryService.postConsume(request);
		boolean draft = result.consumption().getStatus() != null
				&& "DRAFT".equals(result.consumption().getStatus().name());
		return ok(result.consumption(), draft ? "Requested from stores" : "Stock issued (FEFO)");
	}

	@PostMapping("/consume/{id}/issue")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<InternalConsumption>> issue(@PathVariable Long id,
			@Valid @RequestBody IssueConsumeRequest request) {
		return ok(inventoryService.issueConsume(id, request).consumption(), "Stock issued (FEFO)");
	}

	@PostMapping("/consume/{id}/reject")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<InternalConsumption>> reject(@PathVariable Long id) {
		return ok(inventoryService.rejectConsume(id), "Request rejected");
	}

	@GetMapping("/consume")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER','VET_TECH_EMPLOYEE','EMPLOYEE')")
	public ResponseEntity<ApiResponse<List<InternalConsumption>>> consumptions() {
		return ok(inventoryService.consumptions());
	}

	@GetMapping("/takers")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER','VET_TECH_EMPLOYEE','EMPLOYEE')")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> takers() {
		return ok(inventoryService.takers());
	}

	@GetMapping("/alerts")
	public ResponseEntity<ApiResponse<List<InternalConsumption>>> alerts() {
		return ok(inventoryService.alerts());
	}

	@GetMapping("/feed-estimate")
	public ResponseEntity<ApiResponse<Map<String, Object>>> feedEstimate() {
		var branchId = com.dertz.spectra.security.BranchScope.requireBranchId();
		return ok(Map.of("theoreticalFeedKg", inventoryService.theoreticalFeedKg(branchId)));
	}

	@GetMapping("/audits")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<InventoryAuditLog>>> audits() {
		return ok(inventoryService.audits());
	}

	@PostMapping("/audits")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<InventoryAuditLog>> flag(@Valid @RequestBody AuditRequest request) {
		return ok(inventoryService.flagAudit(request), "Discrepancy flagged");
	}

	@PostMapping("/audits/{id}/review")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<InventoryAuditLog>> review(@PathVariable Long id,
			@Valid @RequestBody AuditReviewRequest request) {
		return ok(inventoryService.reviewAudit(id, request), "Audit reviewed");
	}
}
