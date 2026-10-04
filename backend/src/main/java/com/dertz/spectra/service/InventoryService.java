package com.dertz.spectra.service;

import com.dertz.spectra.Enum.AnalyticsAlertType;
import com.dertz.spectra.Enum.AuditReason;
import com.dertz.spectra.Enum.AuditStatus;
import com.dertz.spectra.Enum.ConsumptionStatus;
import com.dertz.spectra.Enum.ConsumptionType;
import com.dertz.spectra.Enum.CostCenter;
import com.dertz.spectra.Enum.CareStatus;
import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.Enum.ShelterProductCategory;
import com.dertz.spectra.Enum.TreatmentStatus;
import com.dertz.spectra.Enum.WarehouseType;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.AnalyticsAlert;
import com.dertz.spectra.model.BranchStock;
import com.dertz.spectra.model.DogExpenseLink;
import com.dertz.spectra.model.DogVaccination;
import com.dertz.spectra.model.Resident;
import com.dertz.spectra.model.InternalConsumption;
import com.dertz.spectra.model.InternalConsumptionItem;
import com.dertz.spectra.model.InventoryAuditLog;
import com.dertz.spectra.model.PurchaseGrn;
import com.dertz.spectra.model.PurchaseGrnItem;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.StockBatch;
import com.dertz.spectra.model.Supplier;
import com.dertz.spectra.model.SupplierPayment;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.UserBranch;
import com.dertz.spectra.model.Warehouse;
import com.dertz.spectra.repository.AnalyticsAlertRepository;
import com.dertz.spectra.repository.BranchStockRepository;
import com.dertz.spectra.repository.DogExpenseLinkRepository;
import com.dertz.spectra.repository.DogVaccinationRepository;
import com.dertz.spectra.repository.ResidentRepository;
import com.dertz.spectra.repository.InternalConsumptionItemRepository;
import com.dertz.spectra.repository.InternalConsumptionRepository;
import com.dertz.spectra.repository.InventoryAuditLogRepository;
import com.dertz.spectra.repository.PurchaseGrnItemRepository;
import com.dertz.spectra.repository.PurchaseGrnRepository;
import com.dertz.spectra.repository.ShelterProductRepository;
import com.dertz.spectra.repository.StockBatchRepository;
import com.dertz.spectra.repository.SupplierPaymentRepository;
import com.dertz.spectra.repository.SupplierRepository;
import com.dertz.spectra.repository.TenantSettingsRepository;
import com.dertz.spectra.repository.UserBranchRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.repository.WarehouseRepository;
import com.dertz.spectra.request.AuditRequest;
import com.dertz.spectra.request.BatchExpiryRequest;
import com.dertz.spectra.request.AuditReviewRequest;
import com.dertz.spectra.request.ConsumeRequest;
import com.dertz.spectra.request.GrnRequest;
import com.dertz.spectra.request.IssueConsumeRequest;
import com.dertz.spectra.request.ShelterProductRequest;
import com.dertz.spectra.request.SupplierPaymentRequest;
import com.dertz.spectra.request.SupplierRequest;
import com.dertz.spectra.security.BranchScope;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

	public record ConsumeResult(InternalConsumption consumption, List<InternalConsumptionItem> items) {
	}

	private final ShelterProductRepository shelterProductRepository;
	private final SupplierRepository supplierRepository;
	private final SupplierPaymentRepository supplierPaymentRepository;
	private final PurchaseGrnRepository purchaseGrnRepository;
	private final PurchaseGrnItemRepository purchaseGrnItemRepository;
	private final StockBatchRepository stockBatchRepository;
	private final BranchStockRepository branchStockRepository;
	private final WarehouseRepository warehouseRepository;
	private final InternalConsumptionRepository consumptionRepository;
	private final InternalConsumptionItemRepository consumptionItemRepository;
	private final InventoryAuditLogRepository auditLogRepository;
	private final ResidentRepository residentRepository;
	private final TenantSettingsRepository tenantSettingsRepository;
	private final SequenceService sequenceService;
	private final UserRepository userRepository;
	private final UserBranchRepository userBranchRepository;
	private final DogExpenseLinkRepository dogExpenseLinkRepository;
	private final AnalyticsAlertRepository analyticsAlertRepository;
	private final DogVaccinationRepository dogVaccinationRepository;
	private final PurchaseOrderService purchaseOrderService;

	@Transactional
	public ShelterProduct createProduct(ShelterProductRequest request) {
		boolean lotTracked = request.getLotTracked() != null
				? request.getLotTracked()
				: request.getCategory() != ShelterProductCategory.STAFF_RETAIL;
		boolean staffSale = request.getStaffSale() != null
				? request.getStaffSale()
				: request.getCategory() == ShelterProductCategory.STAFF_RETAIL;
		boolean clinical = request.getClinicalUse() != null
				? request.getClinicalUse()
				: request.getCategory() != ShelterProductCategory.STAFF_RETAIL;
		ShelterProduct product = ShelterProduct.builder()
				.tenantId(TenantContext.require().tenantId())
				.sku(request.getSku().trim())
				.name(request.getName().trim())
				.unit(request.getUnit())
				.category(request.getCategory())
				.barcode(request.getBarcode())
				.vaccineIntervalDays(request.getVaccineIntervalDays())
				.reorderLevel(request.getReorderLevel() == null ? BigDecimal.ZERO : request.getReorderLevel())
				.lotTracked(lotTracked)
				.qtyOnHand(BigDecimal.ZERO)
				.unitPrice(nz(request.getUnitPrice()))
				.unitCost(nz(request.getUnitCost()))
				.staffSale(staffSale)
				.clinicalUse(clinical)
				.active(true)
				.build();
		product = shelterProductRepository.save(product);
		if (!lotTracked && nz(request.getQtyOnHand()).signum() > 0) {
			setSimpleQty(shelterWarehouse(BranchScope.requireBranchId()).getId(), product, request.getQtyOnHand());
		}
		return product;
	}

	@Transactional
	public ShelterProduct updateProduct(Long id, ShelterProductRequest request) {
		ShelterProduct product = requireProduct(id);
		product.setSku(request.getSku().trim());
		product.setName(request.getName().trim());
		product.setUnit(request.getUnit());
		product.setBarcode(request.getBarcode());
		product.setVaccineIntervalDays(request.getVaccineIntervalDays());
		if (request.getReorderLevel() != null) {
			product.setReorderLevel(request.getReorderLevel());
		}
		if (request.getUnitPrice() != null) {
			product.setUnitPrice(request.getUnitPrice());
		}
		if (request.getUnitCost() != null) {
			product.setUnitCost(request.getUnitCost());
		}
		product = shelterProductRepository.save(product);
		adjustSimpleQty(product, request.getQtyOnHand());
		return product;
	}

	/** Sets this branch's quantity for a simple product (no-op for lot-tracked or null) and records the adjustment. */
	@Transactional
	public void adjustSimpleQty(ShelterProduct product, BigDecimal newQty) {
		if (newQty == null || Boolean.TRUE.equals(product.getLotTracked())) {
			return;
		}
		Long branchId = BranchScope.requireBranchId();
		Warehouse warehouse = shelterWarehouse(branchId);
		BigDecimal old = simpleQtyForUpdate(warehouse.getId(), product.getId());
		if (old.compareTo(newQty) == 0) {
			return;
		}
		setSimpleQty(warehouse.getId(), product, newQty);
		logAdjustment(branchId, warehouse.getId(), product.getId(), null, old, newQty);
	}

	/** Seeding helper: sets a branch quantity without an audit trail. */
	@Transactional
	public void seedSimpleQty(Long branchId, ShelterProduct product, BigDecimal qty) {
		setSimpleQty(shelterWarehouse(branchId).getId(), product, qty);
	}

	/** Receives stock of a simple product into a warehouse, re-averaging the unit cost. */
	@Transactional
	public void receiveSimple(Long warehouseId, ShelterProduct product, BigDecimal qty, BigDecimal unitCost) {
		BigDecimal old = simpleQtyForUpdate(warehouseId, product.getId());
		product.setUnitCost(weightedAverage(old, product.getUnitCost(), qty, unitCost));
		shelterProductRepository.save(product);
		setSimpleQty(warehouseId, product, old.add(qty));
	}

	private void logAdjustment(Long branchId, Long warehouseId, Long productId, Long batchId, BigDecimal oldQty,
			BigDecimal newQty) {
		auditLogRepository.save(InventoryAuditLog.builder()
				.tenantId(TenantContext.require().tenantId())
				.branchId(branchId)
				.warehouseId(warehouseId)
				.shelterProductId(productId)
				.batchId(batchId)
				.systemQty(oldQty)
				.physicalQty(newQty)
				.reason(AuditReason.MANUAL_ADJUSTMENT)
				.status(AuditStatus.APPROVED_WRITE_OFF)
				.reviewedBy(BranchScope.currentUserId())
				.reviewedAt(Instant.now())
				.notes("Quantity edited directly")
				.build());
	}

	private BigDecimal simpleQty(Long warehouseId, Long productId) {
		return branchStockRepository.findByWarehouseIdAndShelterProductId(warehouseId, productId)
				.map(row -> nz(row.getQtyOnHand()))
				.orElse(BigDecimal.ZERO);
	}

	private BigDecimal simpleQtyForUpdate(Long warehouseId, Long productId) {
		return branchStockRepository.lockByWarehouseAndProduct(warehouseId, productId)
				.map(row -> nz(row.getQtyOnHand()))
				.orElse(BigDecimal.ZERO);
	}

	private void setSimpleQty(Long warehouseId, ShelterProduct product, BigDecimal qty) {
		BranchStock row = branchStockRepository.lockByWarehouseAndProduct(warehouseId, product.getId())
				.orElseGet(() -> BranchStock.builder()
						.tenantId(TenantContext.require().tenantId())
						.warehouseId(warehouseId)
						.shelterProductId(product.getId())
						.build());
		row.setQtyOnHand(qty);
		branchStockRepository.save(row);
	}

	@Transactional(readOnly = true)
	public List<ShelterProduct> products() {
		Warehouse warehouse = shelterWarehouse(BranchScope.requireBranchId());
		List<ShelterProduct> products = shelterProductRepository.findByActiveTrueOrderByNameAsc();
		for (ShelterProduct product : products) {
			hydrateAvailableQty(warehouse.getId(), product);
		}
		return products;
	}

	@Transactional
	public Supplier createSupplier(SupplierRequest request) {
		return supplierRepository.save(Supplier.builder()
				.tenantId(TenantContext.require().tenantId())
				.branchId(BranchScope.requireBranchId())
				.name(request.getName())
				.contactPhone(request.getContactPhone())
				.contactEmail(request.getContactEmail())
				.payableBalance(BigDecimal.ZERO)
				.build());
	}

	@Transactional(readOnly = true)
	public List<Supplier> suppliers() {
		return supplierRepository.findByBranchIdOrderByNameAsc(BranchScope.requireBranchId());
	}

	@Transactional
	public PurchaseGrn createGrn(GrnRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		Warehouse warehouse = shelterWarehouse(branchId);
		Supplier supplier = supplierRepository.findById(request.getSupplierId())
				.orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
		if (request.getPurchaseOrderId() != null) {
			purchaseOrderService.assertReceivable(request.getPurchaseOrderId(), supplier.getId());
		}
		PurchaseGrn grn = purchaseGrnRepository.save(PurchaseGrn.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.warehouseId(warehouse.getId())
				.supplierId(supplier.getId())
				.purchaseOrderId(request.getPurchaseOrderId())
				.grnNumber(sequenceService.nextGrnNumber())
				.receivedAt(Instant.now())
				.status("RECEIVED")
				.totalAmount(BigDecimal.ZERO)
				.notes(request.getNotes())
				.build());
		BigDecimal total = BigDecimal.ZERO;
		for (GrnRequest.Item line : request.getItems()) {
			ShelterProduct product = requireProduct(line.getShelterProductId());
			BigDecimal lineTotal = line.getQuantity().multiply(line.getUnitLandedCost()).setScale(2, RoundingMode.HALF_UP);
			total = total.add(lineTotal);
			boolean lot = Boolean.TRUE.equals(product.getLotTracked());
			if (lot && (line.getBatchNumber() == null || line.getBatchNumber().isBlank() || line.getExpiryDate() == null)) {
				throw new BusinessException("Lot-tracked item " + product.getName() + " needs batch number and expiry");
			}
			PurchaseGrnItem item = purchaseGrnItemRepository.save(PurchaseGrnItem.builder()
					.tenantId(tenantId)
					.grnId(grn.getId())
					.shelterProductId(product.getId())
					.purchaseOrderItemId(line.getPurchaseOrderItemId())
					.damagedQty(nz(line.getDamagedQty()))
					.batchNumber(lot ? line.getBatchNumber().trim() : line.getBatchNumber())
					.expiryDate(line.getExpiryDate())
					.quantity(line.getQuantity())
					.unitLandedCost(line.getUnitLandedCost())
					.lineTotal(lineTotal)
					.build());
			if (lot) {
				stockBatchRepository.save(StockBatch.builder()
						.tenantId(tenantId)
						.warehouseId(warehouse.getId())
						.shelterProductId(product.getId())
						.grnItemId(item.getId())
						.batchNumber(line.getBatchNumber().trim())
						.expiryDate(line.getExpiryDate())
						.qtyOnHand(line.getQuantity())
						.unitLandedCost(line.getUnitLandedCost())
						.receivedAt(grn.getReceivedAt())
						.build());
			} else {
				receiveSimple(warehouse.getId(), product, line.getQuantity(), line.getUnitLandedCost());
			}
		}
		grn.setTotalAmount(total);
		supplier.setPayableBalance(supplier.getPayableBalance().add(total));
		supplierRepository.save(supplier);
		if (request.getPurchaseOrderId() != null) {
			purchaseOrderService.applyReceipt(request.getPurchaseOrderId(), grn, request.getItems());
		}
		return purchaseGrnRepository.save(grn);
	}

	@Transactional(readOnly = true)
	public List<PurchaseGrn> grns() {
		return purchaseGrnRepository.findByBranchIdOrderByReceivedAtDesc(BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public List<PurchaseGrnItem> grnItems(Long grnId) {
		return purchaseGrnItemRepository.findByGrnId(grnId);
	}

	@Transactional(readOnly = true)
	public List<StockBatch> batches() {
		return stockBatchRepository.findByWarehouseIdOrderByExpiryDateAsc(shelterWarehouse(BranchScope.requireBranchId()).getId());
	}

	@Transactional
	public StockBatch updateBatchExpiry(Long batchId, BatchExpiryRequest request) {
		StockBatch batch = stockBatchRepository.findById(batchId)
				.orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
		batch.setExpiryDate(request.getExpiryDate());
		BigDecimal old = batch.getQtyOnHand();
		if (request.getQtyOnHand() != null && old.compareTo(request.getQtyOnHand()) != 0) {
			Warehouse warehouse = shelterWarehouse(BranchScope.requireBranchId());
			if (!warehouse.getId().equals(batch.getWarehouseId())) {
				throw new ResourceNotFoundException("Batch not found");
			}
			batch.setQtyOnHand(request.getQtyOnHand());
			logAdjustment(warehouse.getBranchId(), warehouse.getId(), batch.getShelterProductId(), batch.getId(), old,
					request.getQtyOnHand());
		}
		return stockBatchRepository.save(batch);
	}

	@Transactional
	public ConsumeResult postConsume(ConsumeRequest request) {
		Role role = TenantContext.require().role();
		boolean manager = role == Role.NGO_ADMIN || role == Role.BRANCH_ADMIN || role == Role.INVENTORY_MANAGER;
		if (manager) {
			return consume(request);
		}
		return requestConsume(request);
	}

	@Transactional
	public ConsumeResult requestConsume(ConsumeRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		Warehouse warehouse = shelterWarehouse(branchId);
		Role role = TenantContext.require().role();
		List<InternalConsumptionItem> persisted = new ArrayList<>();
		InternalConsumption header = consumptionRepository.save(InternalConsumption.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.warehouseId(warehouse.getId())
				.consumptionDate(LocalDate.now())
				.type(request.getType() == null ? ConsumptionType.MIXED : request.getType())
				.status(ConsumptionStatus.DRAFT)
				.varianceAlert(false)
				.notes(request.getNotes())
				.requestedByUserId(BranchScope.currentUserId())
				.build());
		for (ConsumeRequest.Item line : request.getItems()) {
			ShelterProduct product = requireProduct(line.getShelterProductId());
			if (role == Role.EMPLOYEE && product.getCategory() != ShelterProductCategory.FOOD) {
				throw new BusinessException("Caretakers can only request kennel feed", "FOOD_REQUEST_ONLY",
						HttpStatus.FORBIDDEN);
			}
			if (product.getCategory() == ShelterProductCategory.STAFF_RETAIL) {
				throw new BusinessException("Staff retail is sold at POS, not issued here");
			}
			CostCenter costCenter = line.getCostCenter() != null
					? line.getCostCenter()
					: (product.getCategory() == ShelterProductCategory.FOOD
							? CostCenter.KENNEL_FEEDING
							: CostCenter.CLINICAL_TREATMENT);
			persisted.add(consumptionItemRepository.save(InternalConsumptionItem.builder()
					.tenantId(tenantId)
					.consumptionId(header.getId())
					.shelterProductId(product.getId())
					.batchId(null)
					.qty(line.getQuantity())
					.unitCost(BigDecimal.ZERO)
					.costCenter(costCenter)
					.residentId(line.getResidentId())
					.build()));
		}
		header.setLines(persisted);
		return new ConsumeResult(header, persisted);
	}

	@Transactional
	public ConsumeResult issueConsume(Long id, IssueConsumeRequest request) {
		InternalConsumption header = requireDraft(id);
		Long tenantId = header.getTenantId();
		Warehouse warehouse = shelterWarehouse(header.getBranchId());
		Long takerId = request.getTakenByUserId();
		requireTaker(takerId, header.getBranchId());
		List<InternalConsumptionItem> stubs = consumptionItemRepository.findByConsumptionId(header.getId());
		if (stubs.isEmpty()) {
			throw new BusinessException("Draft has no lines");
		}
		consumptionItemRepository.deleteAll(stubs);
		consumptionItemRepository.flush();
		List<InternalConsumptionItem> persisted = new ArrayList<>();
		BigDecimal feedIssued = BigDecimal.ZERO;
		for (InternalConsumptionItem stub : stubs) {
			ShelterProduct product = requireProduct(stub.getShelterProductId());
			if (product.getCategory() == ShelterProductCategory.FOOD) {
				feedIssued = feedIssued.add(stub.getQty());
			}
			List<InternalConsumptionItem> allocated = deduct(tenantId, header.getId(), warehouse.getId(), product,
					stub.getQty(), stub.getCostCenter(), takerId, stub.getResidentId());
			persisted.addAll(allocated);
			if (stub.getResidentId() != null) {
				linkDogExpense(header, product, allocated, stub.getResidentId());
			}
		}
		header.setTakenByUserId(takerId);
		header.setIssuedByUserId(BranchScope.currentUserId());
		header.setIssuedAt(Instant.now());
		header.setStatus(ConsumptionStatus.ISSUED);
		header.setActualQty(feedIssued);
		applyFeedVariance(header, header.getBranchId(), feedIssued);
		consumptionRepository.save(header);
		completeTreatments(header, persisted);
		header.setLines(persisted);
		return new ConsumeResult(header, persisted);
	}

	@Transactional
	public InternalConsumption rejectConsume(Long id) {
		InternalConsumption header = requireDraft(id);
		header.setStatus(ConsumptionStatus.REJECTED);
		header.setIssuedByUserId(BranchScope.currentUserId());
		header.setIssuedAt(Instant.now());
		consumptionRepository.save(header);
		for (DogVaccination tx : dogVaccinationRepository.findByConsumptionId(header.getId())) {
			if (tx.getStatus() == TreatmentStatus.PENDING_ISSUE) {
				tx.setStatus(TreatmentStatus.CANCELLED);
				dogVaccinationRepository.save(tx);
			}
		}
		return header;
	}

	@Transactional
	public ConsumeResult consume(ConsumeRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		Warehouse warehouse = shelterWarehouse(branchId);
		Long headerTakerId = request.getTakenByUserId() != null
				? request.getTakenByUserId()
				: BranchScope.currentUserId();
		requireTaker(headerTakerId, branchId);
		List<InternalConsumptionItem> persisted = new ArrayList<>();
		BigDecimal feedIssued = BigDecimal.ZERO;
		InternalConsumption header = consumptionRepository.save(InternalConsumption.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.warehouseId(warehouse.getId())
				.consumptionDate(LocalDate.now())
				.type(request.getType() == null ? ConsumptionType.MIXED : request.getType())
				.status(ConsumptionStatus.ISSUED)
				.varianceAlert(false)
				.notes(request.getNotes())
				.takenByUserId(headerTakerId)
				.requestedByUserId(BranchScope.currentUserId())
				.issuedByUserId(BranchScope.currentUserId())
				.issuedAt(Instant.now())
				.build());
		for (ConsumeRequest.Item line : request.getItems()) {
			ShelterProduct product = requireProduct(line.getShelterProductId());
			CostCenter costCenter = line.getCostCenter() != null
					? line.getCostCenter()
					: (product.getCategory() == ShelterProductCategory.FOOD
							? CostCenter.KENNEL_FEEDING
							: CostCenter.CLINICAL_TREATMENT);
			Long lineTakerId = line.getTakenByUserId() != null ? line.getTakenByUserId() : headerTakerId;
			if (!lineTakerId.equals(headerTakerId)) {
				requireTaker(lineTakerId, branchId);
			}
			if (product.getCategory() == ShelterProductCategory.FOOD) {
				feedIssued = feedIssued.add(line.getQuantity());
			}
			List<InternalConsumptionItem> allocated = deduct(tenantId, header.getId(), warehouse.getId(), product,
					line.getQuantity(), costCenter, lineTakerId, line.getResidentId());
			persisted.addAll(allocated);
			if (line.getResidentId() != null) {
				linkDogExpense(header, product, allocated, line.getResidentId());
			}
		}
		header.setActualQty(feedIssued);
		applyFeedVariance(header, branchId, feedIssued);
		consumptionRepository.save(header);
		header.setLines(persisted);
		return new ConsumeResult(header, persisted);
	}

	@Transactional(readOnly = true)
	public List<InternalConsumption> consumptions() {
		Long branchId = BranchScope.requireBranchId();
		LocalDate to = LocalDate.now();
		List<InternalConsumption> drafts = consumptionRepository
				.findByBranchIdAndStatusOrderByCreatedDateDesc(branchId, ConsumptionStatus.DRAFT);
		List<InternalConsumption> recent = consumptionRepository
				.findByBranchIdAndConsumptionDateBetweenOrderByConsumptionDateDesc(branchId, to.minusDays(30), to)
				.stream()
				.filter(row -> row.getStatus() != ConsumptionStatus.DRAFT)
				.toList();
		List<InternalConsumption> rows = new ArrayList<>(drafts);
		java.util.HashSet<Long> seen = new java.util.HashSet<>();
		for (InternalConsumption row : drafts) {
			seen.add(row.getId());
		}
		for (InternalConsumption row : recent) {
			if (seen.add(row.getId())) {
				rows.add(row);
			}
		}
		hydrateConsumeList(rows);
		return rows;
	}

	@Transactional(readOnly = true)
	public long pendingIssueCount() {
		return consumptionRepository.countByBranchIdAndStatus(BranchScope.requireBranchId(), ConsumptionStatus.DRAFT);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> takers() {
		Long branchId = BranchScope.requireBranchId();
		java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
		userBranchRepository.findByBranchId(branchId).forEach(ub -> ids.add(ub.getUserId()));
		userRepository.findByBranchId(branchId).forEach(u -> ids.add(u.getId()));
		ids.add(BranchScope.currentUserId());
		return ids.stream()
				.map(id -> userRepository.findById(id).orElse(null))
				.filter(Objects::nonNull)
				.filter(u -> u.getStatus() == EntityStatus.ACTIVE && u.getRole() != Role.PLATFORM_ADMIN)
				.sorted(java.util.Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
				.map(u -> {
					Map<String, Object> row = new java.util.LinkedHashMap<>();
					row.put("id", u.getId());
					row.put("fullName", u.getFullName());
					row.put("role", u.getRole().name());
					return row;
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public List<InternalConsumption> alerts() {
		return consumptionRepository.findByBranchIdAndVarianceAlertTrueOrderByConsumptionDateDesc(BranchScope.requireBranchId());
	}

	@Transactional
	public InventoryAuditLog flagAudit(AuditRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		Warehouse warehouse = shelterWarehouse(branchId);
		ShelterProduct product = requireProduct(request.getShelterProductId());
		BigDecimal systemQty;
		if (request.getBatchId() != null) {
			StockBatch batch = stockBatchRepository.findById(request.getBatchId())
					.orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
			systemQty = batch.getQtyOnHand();
		} else if (Boolean.TRUE.equals(product.getLotTracked())) {
			systemQty = stockBatchRepository.sumQty(warehouse.getId(), product.getId());
		} else {
			systemQty = simpleQty(warehouse.getId(), product.getId());
		}
		return auditLogRepository.save(InventoryAuditLog.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.warehouseId(warehouse.getId())
				.shelterProductId(request.getShelterProductId())
				.batchId(request.getBatchId())
				.systemQty(systemQty)
				.physicalQty(request.getPhysicalQty())
				.reason(request.getReason())
				.status(AuditStatus.FLAGGED_UNCERTAIN)
				.notes(request.getNotes())
				.build());
	}

	@Transactional
	public InventoryAuditLog reviewAudit(Long id, AuditReviewRequest request) {
		InventoryAuditLog log = auditLogRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Audit not found"));
		if (log.getStatus() != AuditStatus.FLAGGED_UNCERTAIN) {
			throw new BusinessException("Audit already reviewed");
		}
		if (request.getStatus() != AuditStatus.APPROVED_WRITE_OFF && request.getStatus() != AuditStatus.REJECTED) {
			throw new BusinessException("Review must approve write-off or reject");
		}
		log.setStatus(request.getStatus());
		log.setReviewedBy(BranchScope.currentUserId());
		log.setReviewedAt(Instant.now());
		if (request.getNotes() != null) {
			log.setNotes(request.getNotes());
		}
		if (request.getStatus() == AuditStatus.APPROVED_WRITE_OFF) {
			BigDecimal delta = log.getPhysicalQty().subtract(log.getSystemQty());
			ShelterProduct product = requireProduct(log.getShelterProductId());
			if (log.getBatchId() != null) {
				StockBatch batch = stockBatchRepository.findById(log.getBatchId())
						.orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
				BigDecimal next = batch.getQtyOnHand().add(delta);
				if (next.compareTo(BigDecimal.ZERO) < 0) {
					next = BigDecimal.ZERO;
				}
				batch.setQtyOnHand(next);
				stockBatchRepository.save(batch);
			} else if (!Boolean.TRUE.equals(product.getLotTracked())) {
				BigDecimal next = simpleQtyForUpdate(log.getWarehouseId(), product.getId()).add(delta);
				if (next.compareTo(BigDecimal.ZERO) < 0) {
					next = BigDecimal.ZERO;
				}
				setSimpleQty(log.getWarehouseId(), product, next);
			} else if (delta.compareTo(BigDecimal.ZERO) < 0) {
				deduct(log.getTenantId(), null, log.getWarehouseId(), product, delta.abs(), null, null, null);
			}
		}
		return auditLogRepository.save(log);
	}

	@Transactional(readOnly = true)
	public List<InventoryAuditLog> audits() {
		return auditLogRepository.findByBranchIdOrderByCreatedDateDesc(BranchScope.requireBranchId());
	}

	@Transactional
	public SupplierPayment paySupplier(Long supplierId, SupplierPaymentRequest request) {
		Supplier supplier = supplierRepository.findById(supplierId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
		if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Amount must be positive");
		}
		supplier.setPayableBalance(supplier.getPayableBalance().subtract(request.getAmount()));
		supplierRepository.save(supplier);
		return supplierPaymentRepository.save(SupplierPayment.builder()
				.tenantId(TenantContext.require().tenantId())
				.branchId(BranchScope.requireBranchId())
				.supplierId(supplierId)
				.amount(request.getAmount())
				.paidAt(Instant.now())
				.notes(request.getNotes())
				.build());
	}

	@Transactional(readOnly = true)
	public List<SupplierPayment> supplierPayments(Long supplierId) {
		supplierRepository.findById(supplierId).orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
		return supplierPaymentRepository.findBySupplierIdOrderByPaidAtDesc(supplierId);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> lowStock() {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ShelterProduct product : products()) {
			if (product.getReorderLevel() == null || product.getReorderLevel().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal qty = availableQty(product);
			if (qty.compareTo(product.getReorderLevel()) >= 0) {
				continue;
			}
			Map<String, Object> row = new java.util.LinkedHashMap<>();
			row.put("id", product.getId());
			row.put("sku", product.getSku());
			row.put("name", product.getName());
			row.put("qtyOnHand", qty);
			row.put("reorderLevel", product.getReorderLevel());
			rows.add(row);
		}
		return rows;
	}

	private void linkDogExpense(InternalConsumption header, ShelterProduct product,
			List<InternalConsumptionItem> allocated, Long residentId) {
		Resident resident = residentRepository.findById(residentId)
				.orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
		if (!resident.getBranchId().equals(header.getBranchId())) {
			throw new BusinessException("Resident is not in this branch");
		}
		BigDecimal amount = allocated.stream()
				.map(i -> nz(i.getUnitCost()).multiply(nz(i.getQty())))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		dogExpenseLinkRepository.save(DogExpenseLink.builder()
				.tenantId(header.getTenantId())
				.branchId(header.getBranchId())
				.residentId(residentId)
				.shelterProductId(product.getId())
				.consumptionId(header.getId())
				.label(product.getName())
				.amount(amount)
				.expenseDate(header.getConsumptionDate())
				.build());
	}

	public BigDecimal theoreticalFeedKg(Long branchId) {
		List<Resident> active = residentRepository.findByBranchIdAndStatus(branchId, CareStatus.ACTIVE);
		TenantSettings settings = tenantSettingsRepository.findByTenantId(TenantContext.require().tenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
		long grams = 0;
		for (Resident resident : active) {
			grams += switch (resident.getCategory()) {
				case ADULT -> settings.getAdultFeedGramsPerDay();
				case JUVENILE -> settings.getJuvenileFeedGramsPerDay();
				case POST_OP -> settings.getPostOpFeedGramsPerDay();
				case CRITICAL -> settings.getIsolationFeedGramsPerDay();
			};
		}
		return BigDecimal.valueOf(grams).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
	}

	public List<InternalConsumptionItem> deduct(Long productId, BigDecimal qty) {
		Long tenantId = TenantContext.require().tenantId();
		Warehouse warehouse = shelterWarehouse(BranchScope.requireBranchId());
		return deduct(tenantId, null, warehouse.getId(), requireProduct(productId), qty, null, null, null);
	}

	@Transactional(readOnly = true)
	public BigDecimal availableQty(ShelterProduct product) {
		if (Boolean.TRUE.equals(product.getLotTracked())) {
			return stockBatchRepository.sumQty(shelterWarehouse(BranchScope.requireBranchId()).getId(), product.getId());
		}
		return simpleQty(shelterWarehouse(BranchScope.requireBranchId()).getId(), product.getId());
	}

	private List<InternalConsumptionItem> deduct(Long tenantId, Long consumptionId, Long warehouseId,
			ShelterProduct product, BigDecimal qty, CostCenter costCenter, Long takenByUserId, Long residentId) {
		if (Boolean.TRUE.equals(product.getLotTracked())) {
			return allocateFefo(tenantId, consumptionId, warehouseId, product.getId(), qty, costCenter, takenByUserId,
					residentId);
		}
		BigDecimal onHand = simpleQtyForUpdate(warehouseId, product.getId());
		if (onHand.compareTo(qty) < 0) {
			throw new BusinessException("Insufficient stock for " + product.getName(), "INSUFFICIENT_STOCK",
					HttpStatus.CONFLICT);
		}
		setSimpleQty(warehouseId, product, onHand.subtract(qty));
		List<InternalConsumptionItem> items = new ArrayList<>();
			if (consumptionId != null) {
				items.add(consumptionItemRepository.save(InternalConsumptionItem.builder()
						.tenantId(tenantId)
						.consumptionId(consumptionId)
						.shelterProductId(product.getId())
						.batchId(null)
						.qty(qty)
						.unitCost(nz(product.getUnitCost()))
						.costCenter(costCenter)
						.takenByUserId(takenByUserId)
						.residentId(residentId)
						.build()));
			} else {
				items.add(InternalConsumptionItem.builder()
						.qty(qty)
						.unitCost(nz(product.getUnitCost()))
						.build());
			}
		return items;
	}

	private List<InternalConsumptionItem> allocateFefo(Long tenantId, Long consumptionId, Long warehouseId,
			Long productId, BigDecimal qty, CostCenter costCenter, Long takenByUserId, Long residentId) {
		if (qty.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Quantity must be positive");
		}
		List<StockBatch> batches = stockBatchRepository
				.findByWarehouseIdAndShelterProductIdAndQtyOnHandGreaterThanOrderByExpiryDateAscReceivedAtAsc(
						warehouseId, productId, BigDecimal.ZERO);
		BigDecimal remaining = qty;
		List<InternalConsumptionItem> items = new ArrayList<>();
		for (StockBatch batch : batches) {
			if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}
			BigDecimal take = batch.getQtyOnHand().min(remaining);
			batch.setQtyOnHand(batch.getQtyOnHand().subtract(take));
			stockBatchRepository.save(batch);
			InternalConsumptionItem row = InternalConsumptionItem.builder()
					.tenantId(tenantId)
					.consumptionId(consumptionId)
					.shelterProductId(productId)
					.batchId(batch.getId())
					.qty(take)
					.unitCost(batch.getUnitLandedCost())
					.costCenter(costCenter)
					.takenByUserId(takenByUserId)
					.residentId(residentId)
					.build();
			if (consumptionId != null) {
				row = consumptionItemRepository.save(row);
			}
			items.add(row);
			remaining = remaining.subtract(take);
		}
		if (remaining.compareTo(BigDecimal.ZERO) > 0) {
			throw new BusinessException("Insufficient FEFO stock for product " + productId, "INSUFFICIENT_STOCK",
					HttpStatus.CONFLICT);
		}
		return items;
	}

	private User requireTaker(Long userId, Long branchId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
		if (!Objects.equals(user.getTenantId(), TenantContext.require().tenantId())) {
			throw new BusinessException("Staff is not in this organisation");
		}
		if (user.getStatus() != EntityStatus.ACTIVE) {
			throw new BusinessException("Staff is not active");
		}
		boolean onBranch = Objects.equals(user.getBranchId(), branchId)
				|| user.getRole() == Role.NGO_ADMIN
				|| userBranchRepository.findByUserId(userId).stream()
						.anyMatch(ub -> ub.getBranchId().equals(branchId));
		if (!onBranch) {
			throw new BusinessException("Staff is not assigned to this centre");
		}
		return user;
	}

	private void applyFeedVariance(InternalConsumption header, Long branchId, BigDecimal feedIssued) {
		if (feedIssued.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		BigDecimal theoretical = theoreticalFeedKg(branchId);
		header.setTheoreticalQty(theoretical);
		if (theoretical.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal variance = feedIssued.subtract(theoretical).abs().divide(theoretical, 4, RoundingMode.HALF_UP);
			header.setVariancePct(variance);
			header.setVarianceAlert(variance.compareTo(new BigDecimal("0.15")) > 0);
		}
		if (Boolean.TRUE.equals(header.getVarianceAlert())) {
			String title = "Feed variance " + header.getConsumptionDate() + " #" + header.getId();
			if (!analyticsAlertRepository.existsByBranchIdAndAlertTypeAndTitleAndAcknowledgedFalse(branchId,
					AnalyticsAlertType.BURN_RATE, title)) {
				analyticsAlertRepository.save(AnalyticsAlert.builder()
						.tenantId(header.getTenantId())
						.branchId(branchId)
						.alertType(AnalyticsAlertType.BURN_RATE)
						.severity("HIGH")
						.title(title)
						.detail("Issued " + header.getActualQty() + " kg vs theory " + header.getTheoreticalQty()
								+ " kg (>" + new BigDecimal("15") + "%).")
						.detectedAt(Instant.now())
						.acknowledged(false)
						.build());
			}
		}
	}

	private InternalConsumption requireDraft(Long id) {
		InternalConsumption header = consumptionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Issue request not found"));
		if (!header.getBranchId().equals(BranchScope.requireBranchId())) {
			throw new BusinessException("Issue request is not in this centre");
		}
		if (header.getStatus() != ConsumptionStatus.DRAFT) {
			throw new BusinessException("Only draft requests can be issued", "NOT_DRAFT", HttpStatus.CONFLICT);
		}
		return header;
	}

	private void completeTreatments(InternalConsumption header, List<InternalConsumptionItem> allocated) {
		for (DogVaccination tx : dogVaccinationRepository.findByConsumptionId(header.getId())) {
			if (tx.getStatus() != TreatmentStatus.PENDING_ISSUE) {
				continue;
			}
			InternalConsumptionItem first = allocated.stream()
					.filter(i -> Objects.equals(i.getShelterProductId(), tx.getShelterProductId()))
					.findFirst()
					.orElse(allocated.isEmpty() ? null : allocated.get(0));
			ShelterProduct product = shelterProductRepository.findById(tx.getShelterProductId()).orElse(null);
			int interval = product != null && product.getVaccineIntervalDays() != null
					? product.getVaccineIntervalDays()
					: tenantSettingsRepository.findByTenantId(header.getTenantId())
							.map(TenantSettings::getDefaultVaccineIntervalDays)
							.orElse(365);
			tx.setStatus(TreatmentStatus.ISSUED);
			tx.setAdministeredAt(Instant.now());
			if (tx.getNextDueDate() == null) {
				tx.setNextDueDate(LocalDate.now().plusDays(interval));
			}
			tx.setConsumptionItemId(first == null ? null : first.getId());
			dogVaccinationRepository.save(tx);
		}
	}

	private void hydrateConsumeList(List<InternalConsumption> rows) {
		hydrateTakenByNames(rows);
		List<Long> people = rows.stream()
				.flatMap(row -> java.util.stream.Stream.of(row.getRequestedByUserId(), row.getIssuedByUserId()))
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		Map<Long, String> names = new java.util.HashMap<>();
		if (!people.isEmpty()) {
			for (User u : userRepository.findAllById(people)) {
				names.put(u.getId(), u.getFullName());
			}
		}
		for (InternalConsumption row : rows) {
			row.setRequestedByName(names.get(row.getRequestedByUserId()));
			row.setIssuedByName(names.get(row.getIssuedByUserId()));
			List<InternalConsumptionItem> lines = consumptionItemRepository.findByConsumptionId(row.getId());
			hydrateLineNames(lines);
			row.setLines(lines);
		}
	}

	private void hydrateLineNames(List<InternalConsumptionItem> lines) {
		List<Long> productIds = lines.stream().map(InternalConsumptionItem::getShelterProductId).filter(Objects::nonNull)
				.distinct().toList();
		List<Long> residentIds = lines.stream().map(InternalConsumptionItem::getResidentId).filter(Objects::nonNull)
				.distinct().toList();
		Map<Long, String> products = new java.util.HashMap<>();
		if (!productIds.isEmpty()) {
			for (ShelterProduct p : shelterProductRepository.findAllById(productIds)) {
				products.put(p.getId(), p.getName());
			}
		}
		Map<Long, String> residents = new java.util.HashMap<>();
		if (!residentIds.isEmpty()) {
			for (Resident r : residentRepository.findAllById(residentIds)) {
				residents.put(r.getId(), r.getName());
			}
		}
		for (InternalConsumptionItem line : lines) {
			line.setProductName(products.get(line.getShelterProductId()));
			line.setResidentName(residents.get(line.getResidentId()));
		}
	}

	private void hydrateTakenByNames(List<InternalConsumption> rows) {
		List<Long> ids = rows.stream().map(InternalConsumption::getTakenByUserId).filter(Objects::nonNull).distinct()
				.toList();
		if (ids.isEmpty()) {
			return;
		}
		Map<Long, String> names = new java.util.HashMap<>();
		for (User u : userRepository.findAllById(ids)) {
			names.put(u.getId(), u.getFullName());
		}
		for (InternalConsumption row : rows) {
			row.setTakenByName(names.get(row.getTakenByUserId()));
		}
	}

	private Warehouse shelterWarehouse(Long branchId) {
		return warehouseRepository.findByBranchIdAndType(branchId, WarehouseType.SHELTER_STORE)
				.orElseThrow(() -> new ResourceNotFoundException("Stockroom not found"));
	}

	private ShelterProduct requireProduct(Long productId) {
		return shelterProductRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
	}

	private void hydrateAvailableQty(Long warehouseId, ShelterProduct product) {
		product.setQtyOnHand(Boolean.TRUE.equals(product.getLotTracked())
				? stockBatchRepository.sumQty(warehouseId, product.getId())
				: simpleQty(warehouseId, product.getId()));
	}

	private static BigDecimal weightedAverage(BigDecimal oldQty, BigDecimal oldCost, BigDecimal buyQty, BigDecimal buyCost) {
		BigDecimal onHand = nz(oldQty);
		BigDecimal cost = nz(oldCost);
		BigDecimal newQty = onHand.add(buyQty);
		if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
			return buyCost;
		}
		return onHand.multiply(cost).add(buyQty.multiply(buyCost)).divide(newQty, 4, RoundingMode.HALF_UP);
	}

	private static BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
