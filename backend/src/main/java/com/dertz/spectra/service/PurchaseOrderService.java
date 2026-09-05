package com.dertz.spectra.service;

import com.dertz.spectra.Enum.AnalyticsAlertType;
import com.dertz.spectra.Enum.NotificationType;
import com.dertz.spectra.Enum.PurchaseOrderStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.AnalyticsAlert;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.PurchaseOrder;
import com.dertz.spectra.model.PurchaseOrderItem;
import com.dertz.spectra.model.PurchaseGrn;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.Supplier;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.UserBranch;
import com.dertz.spectra.repository.AnalyticsAlertRepository;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.PurchaseOrderItemRepository;
import com.dertz.spectra.repository.PurchaseOrderRepository;
import com.dertz.spectra.repository.ShelterProductRepository;
import com.dertz.spectra.repository.SupplierRepository;
import com.dertz.spectra.repository.TenantSettingsRepository;
import com.dertz.spectra.repository.UserBranchRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.request.GrnRequest;
import com.dertz.spectra.request.PoSettingsRequest;
import com.dertz.spectra.request.PurchaseOrderRequest;
import com.dertz.spectra.security.BranchScope;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

	private static final EnumSet<PurchaseOrderStatus> RECEIVABLE = EnumSet.of(
			PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseOrderItemRepository purchaseOrderItemRepository;
	private final SupplierRepository supplierRepository;
	private final ShelterProductRepository shelterProductRepository;
	private final UserRepository userRepository;
	private final UserBranchRepository userBranchRepository;
	private final TenantSettingsRepository tenantSettingsRepository;
	private final SequenceService sequenceService;
	private final NotificationService notificationService;
	private final PdfDocuments pdfDocuments;
	private final BranchRepository branchRepository;
	private final AnalyticsAlertRepository analyticsAlertRepository;

	@Transactional(readOnly = true)
	public List<PurchaseOrder> list() {
		assertCanDraft();
		return purchaseOrderRepository.findByBranchIdOrderByCreatedDateDesc(BranchScope.requireBranchId()).stream()
				.map(this::hydrate)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PurchaseOrder> receivable() {
		assertCanReceive();
		return purchaseOrderRepository.findByBranchIdAndStatusInOrderByCreatedDateDesc(
				BranchScope.requireBranchId(), RECEIVABLE).stream()
				.map(this::hydrate)
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] pdf(Long id) {
		assertCanDraft();
		PurchaseOrder po = hydrate(require(id));
		String org = TenantContext.require().tenantCode();
		String centre = branchRepository.findById(po.getBranchId()).map(Branch::getName).orElse("Centre");
		return pdfDocuments.purchaseOrder(po, org == null || org.isBlank() ? "Spectra" : org, centre);
	}

	@Transactional(readOnly = true)
	public PurchaseOrder get(Long id) {
		assertCanDraft();
		return hydrate(require(id));
	}

	@Transactional
	public PurchaseOrder create(PurchaseOrderRequest request) {
		assertCanDraft();
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		Supplier supplier = requireSupplier(request.getSupplierId());
		Long named = request.getApproverUserId();
		if (named != null) {
			assertEligibleApprover(named, branchId);
		}
		PurchaseOrder po = purchaseOrderRepository.save(PurchaseOrder.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.supplierId(supplier.getId())
				.poNumber(sequenceService.nextPoNumber())
				.status(PurchaseOrderStatus.DRAFT)
				.requestedByUserId(BranchScope.currentUserId())
				.approverUserId(named)
				.notes(request.getNotes())
				.expectedTotal(BigDecimal.ZERO)
				.receivedTotal(BigDecimal.ZERO)
				.build());
		replaceItems(po, request.getItems());
		if (adminSelfApproves() && hasLines(po.getId())) {
			approveAsRequester(po);
		}
		return hydrate(po);
	}

	@Transactional
	public PurchaseOrder update(Long id, PurchaseOrderRequest request) {
		assertCanDraft();
		PurchaseOrder po = require(id);
		if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
			throw new BusinessException("Only draft purchase orders can be edited");
		}
		if (!canEdit(po)) {
			throw new BusinessException("You cannot edit this purchase order", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		Supplier supplier = requireSupplier(request.getSupplierId());
		po.setSupplierId(supplier.getId());
		po.setNotes(request.getNotes());
		Long named = request.getApproverUserId();
		if (named != null) {
			assertEligibleApprover(named, po.getBranchId());
		}
		po.setApproverUserId(named);
		replaceItems(po, request.getItems());
		return hydrate(purchaseOrderRepository.save(po));
	}

	@Transactional
	public PurchaseOrder submit(Long id) {
		assertCanDraft();
		PurchaseOrder po = require(id);
		if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
			throw new BusinessException("Only draft purchase orders can be submitted");
		}
		if (!canEdit(po)) {
			throw new BusinessException("You cannot submit this purchase order", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());
		if (items.isEmpty()) {
			throw new BusinessException("Add at least one line before submitting");
		}
		if (adminSelfApproves()) {
			approveAsRequester(po);
			return hydrate(po);
		}
		TenantSettings settings = settingsEntity();
		Role role = settings.getPoApproverRole();
		Long named = po.getApproverUserId() != null ? po.getApproverUserId() : settings.getPoApproverUserId();
		if (named != null) {
			User approver = assertEligibleApprover(named, po.getBranchId());
			role = approver.getRole();
		}
		po.setApproverRole(role);
		po.setApproverUserId(named);
		po.setStatus(PurchaseOrderStatus.SUBMITTED);
		po.setSubmittedAt(Instant.now());
		purchaseOrderRepository.save(po);
		if (Boolean.TRUE.equals(settings.getPoNotifyOnSubmit())) {
			String title = "PO submitted " + po.getPoNumber();
			String body = requesterName(po) + " submitted " + po.getPoNumber() + " (" + inr(po.getExpectedTotal())
					+ ") for approval.";
			notifyUsers(approverUserIds(po), NotificationType.PO_SUBMITTED, title, body, po.getId());
		}
		return hydrate(po);
	}

	@Transactional
	public PurchaseOrder approve(Long id) {
		PurchaseOrder po = require(id);
		if (po.getStatus() != PurchaseOrderStatus.SUBMITTED) {
			throw new BusinessException("Only submitted purchase orders can be approved");
		}
		if (!canApprove(po)) {
			throw new BusinessException("You are not the approver for this purchase order", "FORBIDDEN",
					HttpStatus.FORBIDDEN);
		}
		po.setStatus(PurchaseOrderStatus.APPROVED);
		po.setApprovedByUserId(BranchScope.currentUserId());
		po.setApprovedAt(Instant.now());
		purchaseOrderRepository.save(po);
		TenantSettings settings = settingsEntity();
		if (Boolean.TRUE.equals(settings.getPoNotifyOnApprove())) {
			notifyUsers(List.of(po.getRequestedByUserId()), NotificationType.PO_APPROVED,
					"PO approved " + po.getPoNumber(),
					po.getPoNumber() + " was approved. Stores can receive against it.", po.getId());
		}
		return hydrate(po);
	}

	@Transactional
	public PurchaseOrder reject(Long id, String reason) {
		PurchaseOrder po = require(id);
		if (po.getStatus() != PurchaseOrderStatus.SUBMITTED) {
			throw new BusinessException("Only submitted purchase orders can be rejected");
		}
		if (!canApprove(po)) {
			throw new BusinessException("You are not the approver for this purchase order", "FORBIDDEN",
					HttpStatus.FORBIDDEN);
		}
		po.setStatus(PurchaseOrderStatus.REJECTED);
		po.setRejectReason(reason);
		po.setApprovedByUserId(BranchScope.currentUserId());
		po.setApprovedAt(Instant.now());
		purchaseOrderRepository.save(po);
		notifyUsers(List.of(po.getRequestedByUserId()), NotificationType.PO_REJECTED,
				"PO rejected " + po.getPoNumber(),
				po.getPoNumber() + " was rejected: " + reason, po.getId());
		return hydrate(po);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> receivePreview(Long id) {
		assertCanReceive();
		PurchaseOrder po = hydrate(require(id));
		if (!RECEIVABLE.contains(po.getStatus())) {
			throw new BusinessException("Purchase order is not approved for receiving");
		}
		List<Map<String, Object>> lines = new ArrayList<>();
		for (PurchaseOrderItem item : po.getLines()) {
			BigDecimal remaining = remaining(item);
			if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			lines.add(Map.of(
					"purchaseOrderItemId", item.getId(),
					"shelterProductId", item.getShelterProductId(),
					"productName", item.getProductName() == null ? "" : item.getProductName(),
					"sku", item.getSku() == null ? "" : item.getSku(),
					"lotTracked", Boolean.TRUE.equals(item.getLotTracked()),
					"qtyOrdered", item.getQtyOrdered(),
					"qtyReceived", item.getQtyReceived(),
					"qtyRemaining", remaining,
					"unitCost", item.getUnitCost(),
					"notes", item.getNotes() == null ? "" : item.getNotes()));
		}
		return Map.of("po", po, "lines", lines);
	}

	public void assertReceivable(Long purchaseOrderId, Long supplierId) {
		PurchaseOrder po = require(purchaseOrderId);
		if (!RECEIVABLE.contains(po.getStatus())) {
			throw new BusinessException("Purchase order must be approved before receiving");
		}
		if (!po.getSupplierId().equals(supplierId)) {
			throw new BusinessException("GRN supplier must match the purchase order");
		}
	}

	@Transactional
	public void applyReceipt(Long purchaseOrderId, PurchaseGrn grn, List<GrnRequest.Item> lines) {
		PurchaseOrder po = require(purchaseOrderId);
		assertReceivable(purchaseOrderId, po.getSupplierId());
		List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());
		Set<Long> used = new HashSet<>();
		List<String> deltas = new ArrayList<>();
		boolean variance = false;
		TenantSettings settings = settingsEntity();
		BigDecimal qtyTol = nz(settings.getPoQtyTolerancePct());
		BigDecimal costTol = nz(settings.getPoCostTolerancePct());
		BigDecimal receivedAdd = BigDecimal.ZERO;
		for (GrnRequest.Item line : lines) {
			PurchaseOrderItem poi = matchItem(items, line, used);
			used.add(poi.getId());
			BigDecimal remaining = remaining(poi);
			BigDecimal qty = line.getQuantity();
			BigDecimal cost = line.getUnitLandedCost();
			BigDecimal damaged = nz(line.getDamagedQty());
			receivedAdd = receivedAdd.add(qty.multiply(cost).setScale(2, RoundingMode.HALF_UP));
			if (exceedsTolerance(qty, remaining, qtyTol)) {
				variance = true;
				deltas.add(productLabel(poi) + " qty " + qty.stripTrailingZeros().toPlainString() + " vs remaining "
						+ remaining.stripTrailingZeros().toPlainString());
			}
			if (exceedsTolerance(cost, poi.getUnitCost(), costTol)) {
				variance = true;
				deltas.add(productLabel(poi) + " rate ₹" + cost.stripTrailingZeros().toPlainString() + " vs PO ₹"
						+ poi.getUnitCost().stripTrailingZeros().toPlainString());
			}
			if (damaged.compareTo(BigDecimal.ZERO) > 0) {
				variance = true;
				deltas.add(productLabel(poi) + " damaged " + damaged.stripTrailingZeros().toPlainString());
			}
			poi.setQtyReceived(nz(poi.getQtyReceived()).add(qty));
			poi.setDamagedQty(nz(poi.getDamagedQty()).add(damaged));
			purchaseOrderItemRepository.save(poi);
		}
		po.setReceivedTotal(nz(po.getReceivedTotal()).add(receivedAdd));
		boolean complete = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId()).stream()
				.allMatch(i -> nz(i.getQtyReceived()).compareTo(i.getQtyOrdered()) >= 0);
		po.setStatus(complete ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
		purchaseOrderRepository.save(po);
		if (variance && Boolean.TRUE.equals(settings.getPoNotifyOnVariance())) {
			String title = "PO variance: " + po.getPoNumber() + " / " + grn.getGrnNumber();
			String detail = String.join("; ", deltas);
			if (detail.length() > 1000) {
				detail = detail.substring(0, 997) + "...";
			}
			analyticsAlertRepository.save(AnalyticsAlert.builder()
					.tenantId(po.getTenantId())
					.branchId(po.getBranchId())
					.alertType(AnalyticsAlertType.PO_VARIANCE)
					.severity("HIGH")
					.title(title)
					.detail(detail)
					.detectedAt(Instant.now())
					.acknowledged(false)
					.build());
			notifyUsers(approverUserIds(po), NotificationType.PO_VARIANCE, title, detail, po.getId());
		}
	}

	@Transactional(readOnly = true)
	public TenantSettings settings() {
		assertCanDraft();
		return settingsEntity();
	}

	@Transactional
	public TenantSettings updateSettings(PoSettingsRequest request) {
		if (TenantContext.require().role() != Role.NGO_ADMIN) {
			throw new BusinessException("Only NGO admin can change PO routing", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		TenantSettings settings = settingsEntity();
		if (request.getPoApproverRole() != null) {
			if (request.getPoApproverRole() != Role.BRANCH_ADMIN && request.getPoApproverRole() != Role.NGO_ADMIN) {
				throw new BusinessException("Approver role must be BRANCH_ADMIN or NGO_ADMIN");
			}
			settings.setPoApproverRole(request.getPoApproverRole());
		}
		if (request.getPoApproverUserId() != null) {
			if (request.getPoApproverUserId() <= 0) {
				settings.setPoApproverUserId(null);
			} else {
				assertEligibleApprover(request.getPoApproverUserId(), BranchScope.requireBranchId());
				settings.setPoApproverUserId(request.getPoApproverUserId());
			}
		}
		if (request.getPoQtyTolerancePct() != null) {
			settings.setPoQtyTolerancePct(request.getPoQtyTolerancePct());
		}
		if (request.getPoCostTolerancePct() != null) {
			settings.setPoCostTolerancePct(request.getPoCostTolerancePct());
		}
		if (request.getPoNotifyOnSubmit() != null) {
			settings.setPoNotifyOnSubmit(request.getPoNotifyOnSubmit());
		}
		if (request.getPoNotifyOnApprove() != null) {
			settings.setPoNotifyOnApprove(request.getPoNotifyOnApprove());
		}
		if (request.getPoNotifyOnVariance() != null) {
			settings.setPoNotifyOnVariance(request.getPoNotifyOnVariance());
		}
		return tenantSettingsRepository.save(settings);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> approvers() {
		assertCanDraft();
		Long branchId = BranchScope.requireBranchId();
		List<Map<String, Object>> out = new ArrayList<>();
		for (User user : eligibleApprovers(branchId)) {
			out.add(Map.of(
					"id", user.getId(),
					"fullName", user.getFullName(),
					"role", user.getRole().name(),
					"email", user.getEmail()));
		}
		return out;
	}

	@Transactional(readOnly = true)
	public long pendingApprovalsCount() {
		Long branchId = BranchScope.requireBranchId();
		Role role = TenantContext.require().role();
		if (role != Role.NGO_ADMIN && role != Role.BRANCH_ADMIN) {
			return 0;
		}
		List<PurchaseOrder> submitted = purchaseOrderRepository.findByBranchIdAndStatusInOrderByCreatedDateDesc(
				branchId, List.of(PurchaseOrderStatus.SUBMITTED));
		return submitted.stream().filter(this::canApprove).count();
	}

	@Transactional(readOnly = true)
	public long pendingSubmittedCount() {
		return purchaseOrderRepository.countByBranchIdAndStatusAndRequestedByUserId(
				BranchScope.requireBranchId(), PurchaseOrderStatus.SUBMITTED, BranchScope.currentUserId());
	}

	private boolean adminSelfApproves() {
		Role role = TenantContext.require().role();
		return role == Role.NGO_ADMIN || role == Role.BRANCH_ADMIN;
	}

	private boolean hasLines(Long poId) {
		return !purchaseOrderItemRepository.findByPurchaseOrderId(poId).isEmpty();
	}

	private void approveAsRequester(PurchaseOrder po) {
		Instant now = Instant.now();
		po.setApproverRole(TenantContext.require().role());
		po.setApproverUserId(BranchScope.currentUserId());
		po.setStatus(PurchaseOrderStatus.APPROVED);
		if (po.getSubmittedAt() == null) {
			po.setSubmittedAt(now);
		}
		po.setApprovedByUserId(BranchScope.currentUserId());
		po.setApprovedAt(now);
		purchaseOrderRepository.save(po);
	}

	private void replaceItems(PurchaseOrder po, List<PurchaseOrderRequest.Item> lines) {
		purchaseOrderItemRepository.deleteByPurchaseOrderId(po.getId());
		purchaseOrderItemRepository.flush();
		BigDecimal expected = BigDecimal.ZERO;
		for (PurchaseOrderRequest.Item line : lines) {
			if (line.getQtyOrdered() == null || line.getQtyOrdered().compareTo(BigDecimal.ZERO) <= 0) {
				throw new BusinessException("Ordered quantity must be greater than zero");
			}
			ShelterProduct product = shelterProductRepository.findById(line.getShelterProductId())
					.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
			BigDecimal unit = nz(line.getUnitCost());
			expected = expected.add(line.getQtyOrdered().multiply(unit).setScale(2, RoundingMode.HALF_UP));
			purchaseOrderItemRepository.save(PurchaseOrderItem.builder()
					.tenantId(po.getTenantId())
					.purchaseOrderId(po.getId())
					.shelterProductId(product.getId())
					.qtyOrdered(line.getQtyOrdered())
					.unitCost(unit)
					.qtyReceived(BigDecimal.ZERO)
					.damagedQty(BigDecimal.ZERO)
					.notes(line.getNotes())
					.build());
		}
		po.setExpectedTotal(expected);
		purchaseOrderRepository.save(po);
	}

	private PurchaseOrder hydrate(PurchaseOrder po) {
		po.setSupplierName(supplierRepository.findById(po.getSupplierId()).map(Supplier::getName).orElse(""));
		po.setRequestedByName(nameOf(po.getRequestedByUserId()));
		po.setApproverName(nameOf(po.getApproverUserId()));
		po.setApprovedByName(nameOf(po.getApprovedByUserId()));
		List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());
		for (PurchaseOrderItem item : items) {
			shelterProductRepository.findById(item.getShelterProductId()).ifPresent(p -> {
				item.setProductName(p.getName());
				item.setSku(p.getSku());
				item.setLotTracked(p.getLotTracked());
			});
		}
		po.setLines(items);
		po.setCanApprove(po.getStatus() == PurchaseOrderStatus.SUBMITTED && canApprove(po));
		po.setCanEdit(po.getStatus() == PurchaseOrderStatus.DRAFT && canEdit(po));
		return po;
	}

	private boolean canEdit(PurchaseOrder po) {
		Role role = TenantContext.require().role();
		if (role == Role.NGO_ADMIN || role == Role.BRANCH_ADMIN || role == Role.INVENTORY_MANAGER) {
			return true;
		}
		return BranchScope.currentUserId().equals(po.getRequestedByUserId());
	}

	private boolean canApprove(PurchaseOrder po) {
		Role role = TenantContext.require().role();
		if (role == Role.NGO_ADMIN) {
			return true;
		}
		if (po.getApproverUserId() != null) {
			return BranchScope.currentUserId().equals(po.getApproverUserId());
		}
		return role == po.getApproverRole();
	}

	private User assertEligibleApprover(Long userId, Long branchId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Approver not found"));
		if (user.getRole() != Role.NGO_ADMIN && user.getRole() != Role.BRANCH_ADMIN) {
			throw new BusinessException("Named approver must be BRANCH_ADMIN or NGO_ADMIN");
		}
		if (user.getRole() == Role.BRANCH_ADMIN) {
			boolean onBranch = userBranchRepository.findByUserId(user.getId()).stream()
					.map(UserBranch::getBranchId)
					.anyMatch(branchId::equals);
			if (!onBranch) {
				throw new BusinessException("Named approver is not assigned to this centre");
			}
		}
		return user;
	}

	private List<User> eligibleApprovers(Long branchId) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		userRepository.findByRole(Role.NGO_ADMIN).forEach(u -> ids.add(u.getId()));
		Set<Long> onBranch = userBranchRepository.findByBranchId(branchId).stream()
				.map(UserBranch::getUserId)
				.collect(Collectors.toSet());
		userRepository.findByRole(Role.BRANCH_ADMIN).stream()
				.filter(u -> onBranch.contains(u.getId()))
				.forEach(u -> ids.add(u.getId()));
		return userRepository.findAllById(ids);
	}

	private List<Long> approverUserIds(PurchaseOrder po) {
		if (po.getApproverUserId() != null) {
			return List.of(po.getApproverUserId());
		}
		Role role = po.getApproverRole() == null ? Role.BRANCH_ADMIN : po.getApproverRole();
		if (role == Role.NGO_ADMIN) {
			return userRepository.findByRole(Role.NGO_ADMIN).stream().map(User::getId).toList();
		}
		Set<Long> onBranch = userBranchRepository.findByBranchId(po.getBranchId()).stream()
				.map(UserBranch::getUserId)
				.collect(Collectors.toSet());
		List<Long> branchAdmins = userRepository.findByRole(Role.BRANCH_ADMIN).stream()
				.map(User::getId)
				.filter(onBranch::contains)
				.toList();
		if (!branchAdmins.isEmpty()) {
			return branchAdmins;
		}
		return userRepository.findByRole(Role.NGO_ADMIN).stream().map(User::getId).toList();
	}

	private void notifyUsers(List<Long> userIds, NotificationType type, String title, String body, Long poId) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		for (Long userId : new LinkedHashSet<>(userIds)) {
			notificationService.notify(tenantId, branchId, userId, type, title, body, "PURCHASE_ORDER", poId);
		}
	}

	private PurchaseOrderItem matchItem(List<PurchaseOrderItem> items, GrnRequest.Item line, Set<Long> used) {
		if (line.getPurchaseOrderItemId() != null) {
			PurchaseOrderItem poi = items.stream()
					.filter(i -> i.getId().equals(line.getPurchaseOrderItemId()))
					.findFirst()
					.orElseThrow(() -> new BusinessException("GRN line does not belong to this purchase order"));
			if (!poi.getShelterProductId().equals(line.getShelterProductId())) {
				throw new BusinessException("GRN product does not match the purchase order line");
			}
			return poi;
		}
		return items.stream()
				.filter(i -> i.getShelterProductId().equals(line.getShelterProductId()) && !used.contains(i.getId()))
				.findFirst()
				.orElseThrow(() -> new BusinessException("GRN product is not on the purchase order"));
	}

	private boolean exceedsTolerance(BigDecimal actual, BigDecimal expected, BigDecimal pct) {
		BigDecimal delta = actual.subtract(expected).abs();
		if (delta.compareTo(BigDecimal.ZERO) == 0) {
			return false;
		}
		if (expected.compareTo(BigDecimal.ZERO) == 0) {
			return true;
		}
		BigDecimal allowed = expected.abs().multiply(nz(pct)).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
		return delta.compareTo(allowed) > 0;
	}

	private BigDecimal remaining(PurchaseOrderItem item) {
		BigDecimal rem = nz(item.getQtyOrdered()).subtract(nz(item.getQtyReceived()));
		return rem.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rem;
	}

	private PurchaseOrder require(Long id) {
		PurchaseOrder po = purchaseOrderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));
		if (!po.getBranchId().equals(BranchScope.requireBranchId())) {
			throw new ResourceNotFoundException("Purchase order not found");
		}
		return po;
	}

	private Supplier requireSupplier(Long id) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
		if (!supplier.getBranchId().equals(BranchScope.requireBranchId())) {
			throw new ResourceNotFoundException("Supplier not found");
		}
		return supplier;
	}

	private TenantSettings settingsEntity() {
		return tenantSettingsRepository.findByTenantId(TenantContext.require().tenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
	}

	private String nameOf(Long userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId).map(User::getFullName).orElse(null);
	}

	private String requesterName(PurchaseOrder po) {
		String name = nameOf(po.getRequestedByUserId());
		return name == null ? "Staff" : name;
	}

	private String productLabel(PurchaseOrderItem item) {
		return shelterProductRepository.findById(item.getShelterProductId())
				.map(ShelterProduct::getName)
				.orElse("Item");
	}

	private String inr(BigDecimal amount) {
		return "₹" + nz(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private void assertCanDraft() {
		Role role = TenantContext.require().role();
		if (role != Role.NGO_ADMIN && role != Role.BRANCH_ADMIN && role != Role.INVENTORY_MANAGER
				&& role != Role.EMPLOYEE) {
			throw new BusinessException("You cannot create purchase orders", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
	}

	private void assertCanReceive() {
		Role role = TenantContext.require().role();
		if (role != Role.NGO_ADMIN && role != Role.BRANCH_ADMIN && role != Role.INVENTORY_MANAGER) {
			throw new BusinessException("Only stores can receive against a purchase order", "FORBIDDEN",
					HttpStatus.FORBIDDEN);
		}
	}
}
