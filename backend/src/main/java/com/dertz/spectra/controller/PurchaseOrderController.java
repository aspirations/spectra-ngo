package com.dertz.spectra.controller;

import com.dertz.spectra.Enum.PurchaseOrderStatus;
import com.dertz.spectra.model.AppNotification;
import com.dertz.spectra.model.PurchaseOrder;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.request.PoSettingsRequest;
import com.dertz.spectra.request.PurchaseOrderRequest;
import com.dertz.spectra.request.RejectPurchaseOrderRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.NotificationService;
import com.dertz.spectra.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PurchaseOrderController extends BaseController {

	private static final String DRAFT_ROLES = "hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER','EMPLOYEE')";
	private static final String RECEIVE_ROLES = "hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')";

	private final PurchaseOrderService purchaseOrderService;
	private final NotificationService notificationService;

	@GetMapping("/inventory/po")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<ApiResponse<List<PurchaseOrder>>> list() {
		return ok(purchaseOrderService.list());
	}

	@GetMapping("/inventory/po/receivable")
	@PreAuthorize(RECEIVE_ROLES)
	public ResponseEntity<ApiResponse<List<PurchaseOrder>>> receivable() {
		return ok(purchaseOrderService.receivable());
	}

	@GetMapping("/inventory/po/settings")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<ApiResponse<TenantSettings>> settings() {
		return ok(purchaseOrderService.settings());
	}

	@PutMapping("/inventory/po/settings")
	@PreAuthorize("hasRole('NGO_ADMIN')")
	public ResponseEntity<ApiResponse<TenantSettings>> updateSettings(@RequestBody PoSettingsRequest request) {
		return ok(purchaseOrderService.updateSettings(request), "PO routing saved");
	}

	@GetMapping("/inventory/po/approvers")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> approvers() {
		return ok(purchaseOrderService.approvers());
	}

	@GetMapping("/inventory/po/{id:\\d+}")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<ApiResponse<PurchaseOrder>> get(@PathVariable Long id) {
		return ok(purchaseOrderService.get(id));
	}

	@PostMapping("/inventory/po")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<ApiResponse<PurchaseOrder>> create(@Valid @RequestBody PurchaseOrderRequest request) {
		PurchaseOrder po = purchaseOrderService.create(request);
		return ok(po, po.getStatus() == PurchaseOrderStatus.APPROVED ? "Purchase order approved" : "Purchase order drafted");
	}

	@PutMapping("/inventory/po/{id:\\d+}")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<ApiResponse<PurchaseOrder>> update(@PathVariable Long id,
			@Valid @RequestBody PurchaseOrderRequest request) {
		return ok(purchaseOrderService.update(id, request), "Purchase order updated");
	}

	@PostMapping("/inventory/po/{id:\\d+}/submit")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<ApiResponse<PurchaseOrder>> submit(@PathVariable Long id) {
		PurchaseOrder po = purchaseOrderService.submit(id);
		return ok(po, po.getStatus() == PurchaseOrderStatus.APPROVED ? "Purchase order approved" : "Sent for approval");
	}

	@PostMapping("/inventory/po/{id:\\d+}/approve")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN')")
	public ResponseEntity<ApiResponse<PurchaseOrder>> approve(@PathVariable Long id) {
		return ok(purchaseOrderService.approve(id), "Purchase order approved");
	}

	@PostMapping("/inventory/po/{id:\\d+}/reject")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN')")
	public ResponseEntity<ApiResponse<PurchaseOrder>> reject(@PathVariable Long id,
			@Valid @RequestBody RejectPurchaseOrderRequest request) {
		return ok(purchaseOrderService.reject(id, request.getReason()), "Purchase order rejected");
	}

	@GetMapping("/inventory/po/{id:\\d+}/pdf")
	@PreAuthorize(DRAFT_ROLES)
	public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
		return pdfFile(purchaseOrderService.pdf(id), purchaseOrderService.get(id).getPoNumber() + ".pdf");
	}

	@GetMapping("/inventory/po/{id:\\d+}/receive-preview")
	@PreAuthorize(RECEIVE_ROLES)
	public ResponseEntity<ApiResponse<Map<String, Object>>> receivePreview(@PathVariable Long id) {
		return ok(purchaseOrderService.receivePreview(id));
	}

	@GetMapping("/inventory/notifications")
	public ResponseEntity<ApiResponse<List<AppNotification>>> notifications() {
		return ok(notificationService.mine());
	}

	@PostMapping("/inventory/notifications/{id}/read")
	public ResponseEntity<ApiResponse<AppNotification>> read(@PathVariable Long id) {
		return ok(notificationService.markRead(id));
	}
}
