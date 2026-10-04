package com.dertz.spectra.service;

import com.dertz.spectra.Enum.PosOrderStatus;
import com.dertz.spectra.Enum.PosTender;
import com.dertz.spectra.Enum.ShelterProductCategory;
import com.dertz.spectra.Enum.UnitOfMeasure;
import com.dertz.spectra.Enum.WarehouseType;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.InternalConsumptionItem;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.StaffPosOrder;
import com.dertz.spectra.model.StaffPosOrderItem;
import com.dertz.spectra.model.StaffStorePurchase;
import com.dertz.spectra.model.StaffStorePurchaseItem;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.Warehouse;
import com.dertz.spectra.repository.ShelterProductRepository;
import com.dertz.spectra.repository.StaffPosOrderItemRepository;
import com.dertz.spectra.repository.StaffPosOrderRepository;
import com.dertz.spectra.repository.StaffStorePurchaseItemRepository;
import com.dertz.spectra.repository.StaffStorePurchaseRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.repository.WarehouseRepository;
import com.dertz.spectra.request.PosOrderRequest;
import com.dertz.spectra.request.ShelterProductRequest;
import com.dertz.spectra.request.StaffProductRequest;
import com.dertz.spectra.request.StaffPurchaseRequest;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StaffPosService {

	private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

	private final ShelterProductRepository productRepository;
	private final StaffPosOrderRepository orderRepository;
	private final StaffPosOrderItemRepository orderItemRepository;
	private final StaffStorePurchaseRepository purchaseRepository;
	private final StaffStorePurchaseItemRepository purchaseItemRepository;
	private final UserRepository userRepository;
	private final WarehouseRepository warehouseRepository;
	private final SequenceService sequenceService;
	private final InventoryService inventoryService;

	@Transactional
	public ShelterProduct createProduct(StaffProductRequest request) {
		return inventoryService.createProduct(toCatalogRequest(request));
	}

	@Transactional
	public ShelterProduct updateProduct(Long id, StaffProductRequest request) {
		ShelterProduct product = requireStaffProduct(id);
		product.setSku(request.getSku().trim());
		product.setName(request.getName().trim());
		if (request.getUnit() != null) {
			product.setUnit(request.getUnit());
		}
		product.setUnitPrice(request.getUnitPrice());
		if (request.getUnitCost() != null) {
			product.setUnitCost(request.getUnitCost());
		}
		product.setBarcode(request.getBarcode());
		product.setStaffSale(true);
		product = productRepository.save(product);
		inventoryService.adjustSimpleQty(product, request.getQtyOnHand());
		return product;
	}

	@Transactional(readOnly = true)
	public List<ShelterProduct> products(String q) {
		List<ShelterProduct> list = (q == null || q.isBlank())
				? productRepository.findByStaffSaleTrueAndActiveTrueOrderByNameAsc()
				: productRepository.searchStaffSale(q.trim());
		for (ShelterProduct product : list) {
			product.setQtyOnHand(inventoryService.availableQty(product));
		}
		return list;
	}

	@Transactional
	public Map<String, Object> receivePurchase(StaffPurchaseRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		Warehouse warehouse = stockroom(branchId);
		BigDecimal total = BigDecimal.ZERO;
		List<StaffStorePurchaseItem> lines = new ArrayList<>();
		for (StaffPurchaseRequest.Item line : request.getItems()) {
			ShelterProduct product = requireStaffProduct(line.getProductId());
			if (Boolean.TRUE.equals(product.getLotTracked())) {
				throw new BusinessException("Receive lot-tracked goods on the unified GRN, not POS purchase");
			}
			BigDecimal buyQty = line.getQty();
			BigDecimal buyCost = line.getUnitCost().setScale(2, RoundingMode.HALF_UP);
			BigDecimal lineTotal = buyQty.multiply(buyCost).setScale(2, RoundingMode.HALF_UP);
			total = total.add(lineTotal);
			inventoryService.receiveSimple(warehouse.getId(), product, buyQty, buyCost);
			lines.add(StaffStorePurchaseItem.builder()
					.tenantId(tenantId)
					.productId(product.getId())
					.qty(buyQty)
					.unitCost(buyCost)
					.lineTotal(lineTotal)
					.build());
		}
		StaffStorePurchase purchase = purchaseRepository.save(StaffStorePurchase.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.warehouseId(warehouse.getId())
				.purchaseNumber(sequenceService.nextStaffPurchaseNumber())
				.supplierName(blankToNull(request.getSupplierName()))
				.totalAmount(total)
				.purchasedAt(Instant.now())
				.notes(request.getNotes())
				.build());
		for (StaffStorePurchaseItem item : lines) {
			item.setPurchaseId(purchase.getId());
			purchaseItemRepository.save(item);
		}
		return Map.of("purchase", purchase, "items", lines);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> purchases() {
		return purchaseRepository.findByBranchIdOrderByPurchasedAtDesc(BranchScope.requireBranchId()).stream()
				.map(p -> {
					Map<String, Object> row = new HashMap<>();
					row.put("purchase", p);
					row.put("items", purchaseItemRepository.findByPurchaseId(p.getId()));
					return row;
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> profitAndLoss(LocalDate from, LocalDate to) {
		LocalDate startDate = from == null ? LocalDate.now(IST).withDayOfMonth(1) : from;
		LocalDate endDate = to == null ? LocalDate.now(IST) : to;
		Instant start = startDate.atStartOfDay(IST).toInstant();
		Instant endExclusive = endDate.plusDays(1).atStartOfDay(IST).toInstant();
		Long branchId = BranchScope.requireBranchId();

		List<StaffPosOrder> orders = orderRepository
				.findByBranchIdAndOrderAtGreaterThanEqualAndOrderAtLessThan(branchId, start, endExclusive)
				.stream()
				.filter(o -> o.getStatus() == PosOrderStatus.COMPLETED)
				.toList();
		BigDecimal revenue = BigDecimal.ZERO;
		BigDecimal cogs = BigDecimal.ZERO;
		Map<Long, ProductPnl> byProduct = new LinkedHashMap<>();
		for (StaffPosOrder order : orders) {
			revenue = revenue.add(order.getTotal());
			for (StaffPosOrderItem item : orderItemRepository.findByOrderId(order.getId())) {
				cogs = cogs.add(nz(item.getLineCost()));
				ProductPnl row = byProduct.computeIfAbsent(item.getProductId(), id -> new ProductPnl());
				row.soldQty = row.soldQty.add(item.getQty());
				row.revenue = row.revenue.add(item.getLineTotal());
				row.cogs = row.cogs.add(nz(item.getLineCost()));
			}
		}

		List<StaffStorePurchase> buys = purchaseRepository
				.findByBranchIdAndPurchasedAtGreaterThanEqualAndPurchasedAtLessThan(branchId, start, endExclusive);
		BigDecimal purchaseSpend = buys.stream().map(StaffStorePurchase::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal inventoryValue = BigDecimal.ZERO;
		List<Map<String, Object>> catalog = new ArrayList<>();
		for (ShelterProduct product : productRepository.findByStaffSaleTrueAndActiveTrueOrderByNameAsc()) {
			BigDecimal qty = inventoryService.availableQty(product);
			BigDecimal value = qty.multiply(nz(product.getUnitCost())).setScale(2, RoundingMode.HALF_UP);
			inventoryValue = inventoryValue.add(value);
			ProductPnl sold = byProduct.getOrDefault(product.getId(), new ProductPnl());
			Map<String, Object> row = new HashMap<>();
			row.put("productId", product.getId());
			row.put("name", product.getName());
			row.put("sku", product.getSku());
			row.put("qtyOnHand", qty);
			row.put("unitCost", nz(product.getUnitCost()));
			row.put("unitPrice", product.getUnitPrice());
			row.put("stockValue", value);
			row.put("soldQty", sold.soldQty);
			row.put("revenue", sold.revenue);
			row.put("cogs", sold.cogs);
			row.put("profit", sold.revenue.subtract(sold.cogs));
			catalog.add(row);
		}

		BigDecimal gross = revenue.subtract(cogs);
		BigDecimal margin = revenue.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: gross.divide(revenue, 4, RoundingMode.HALF_UP);
		Map<String, Object> result = new HashMap<>();
		result.put("from", startDate.toString());
		result.put("to", endDate.toString());
		result.put("salesCount", orders.size());
		result.put("revenue", revenue);
		result.put("cogs", cogs);
		result.put("grossProfit", gross);
		result.put("margin", margin);
		result.put("purchaseCount", buys.size());
		result.put("purchaseSpend", purchaseSpend);
		result.put("inventoryValue", inventoryValue);
		result.put("products", catalog);
		result.put("purchases", buys);
		return result;
	}

	@Transactional
	public Map<String, Object> checkout(PosOrderRequest request) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		User employee = userRepository.findById(request.getEmployeeId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
		BigDecimal total = BigDecimal.ZERO;
		List<StaffPosOrderItem> lines = new ArrayList<>();
		for (PosOrderRequest.Item line : request.getItems()) {
			ShelterProduct product = requireStaffProduct(line.getProductId());
			List<InternalConsumptionItem> issued = inventoryService.deduct(product.getId(), line.getQty());
			BigDecimal lineTotal = product.getUnitPrice().multiply(line.getQty()).setScale(2, RoundingMode.HALF_UP);
			BigDecimal lineCost = issued.stream()
					.map(i -> nz(i.getUnitCost()).multiply(i.getQty()))
					.reduce(BigDecimal.ZERO, BigDecimal::add)
					.setScale(2, RoundingMode.HALF_UP);
			BigDecimal unitCost = line.getQty().compareTo(BigDecimal.ZERO) == 0
					? BigDecimal.ZERO
					: lineCost.divide(line.getQty(), 2, RoundingMode.HALF_UP);
			total = total.add(lineTotal);
			lines.add(StaffPosOrderItem.builder()
					.tenantId(tenantId)
					.productId(product.getId())
					.qty(line.getQty())
					.unitPrice(product.getUnitPrice())
					.lineTotal(lineTotal)
					.unitCost(unitCost)
					.lineCost(lineCost)
					.build());
		}
		if (request.getTender() == PosTender.PAYROLL_CREDIT) {
			CreditMeter meter = creditMeter(employee);
			BigDecimal projected = meter.unpaidDues().add(employee.getRolledOverStoreDebt()).add(total);
			if (projected.compareTo(employee.getCreditLimit()) > 0) {
				throw new BusinessException(
						"Credit limit exceeded. Limit ₹" + employee.getCreditLimit() + ", projected ₹" + projected,
						"CREDIT_LIMIT", HttpStatus.CONFLICT);
			}
		}
		StaffPosOrder order = orderRepository.save(StaffPosOrder.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.employeeId(employee.getId())
				.cashierId(BranchScope.currentUserId())
				.orderNumber(sequenceService.nextPosNumber())
				.tender(request.getTender())
				.status(PosOrderStatus.COMPLETED)
				.total(total)
				.orderAt(Instant.now())
				.build());
		for (StaffPosOrderItem item : lines) {
			item.setOrderId(order.getId());
			orderItemRepository.save(item);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("order", order);
		result.put("items", lines);
		result.put("credit", creditSnapshot(employee.getId()));
		return result;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> passbook(Long employeeId) {
		assertCanViewEmployee(employeeId);
		User employee = userRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
		List<StaffPosOrder> orders = orderRepository.findByEmployeeIdOrderByOrderAtDesc(employeeId);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (StaffPosOrder order : orders) {
			Map<String, Object> row = new HashMap<>();
			List<StaffPosOrderItem> items = orderItemRepository.findByOrderId(order.getId());
			hydrateOrderItemNames(items);
			row.put("order", order);
			row.put("items", items);
			rows.add(row);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("employee", Map.of(
				"id", employee.getId(),
				"fullName", employee.getFullName(),
				"creditLimit", employee.getCreditLimit(),
				"rolledOverStoreDebt", employee.getRolledOverStoreDebt()));
		result.put("orders", rows);
		result.put("credit", creditSnapshot(employeeId));
		return result;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> creditSnapshot(Long employeeId) {
		User employee = userRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
		CreditMeter meter = creditMeter(employee);
		BigDecimal outstanding = meter.unpaidDues().add(employee.getRolledOverStoreDebt());
		Map<String, Object> map = new HashMap<>();
		map.put("employeeId", employeeId);
		map.put("creditLimit", employee.getCreditLimit());
		map.put("unpaidDuesThisMonth", meter.unpaidDues());
		map.put("rolledOverStoreDebt", employee.getRolledOverStoreDebt());
		map.put("outstanding", outstanding);
		map.put("available", employee.getCreditLimit().subtract(outstanding).max(BigDecimal.ZERO));
		return map;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> branchOrders() {
		List<StaffPosOrder> orders = orderRepository.findByBranchIdOrderByOrderAtDesc(BranchScope.requireBranchId());
		List<Long> employeeIds = orders.stream().map(StaffPosOrder::getEmployeeId).filter(Objects::nonNull).distinct().toList();
		Map<Long, String> names = new HashMap<>();
		if (!employeeIds.isEmpty()) {
			for (User user : userRepository.findAllById(employeeIds)) {
				names.put(user.getId(), user.getFullName());
			}
		}
		List<Map<String, Object>> rows = new ArrayList<>();
		for (StaffPosOrder order : orders) {
			List<StaffPosOrderItem> items = orderItemRepository.findByOrderId(order.getId());
			hydrateOrderItemNames(items);
			Map<String, Object> row = new HashMap<>();
			row.put("order", order);
			row.put("items", items);
			row.put("employeeName", names.get(order.getEmployeeId()));
			rows.add(row);
		}
		return rows;
	}

	private Warehouse stockroom(Long branchId) {
		return warehouseRepository.findByBranchIdAndType(branchId, WarehouseType.SHELTER_STORE)
				.orElseThrow(() -> new ResourceNotFoundException("Stockroom not found"));
	}

	private void hydrateOrderItemNames(List<StaffPosOrderItem> items) {
		List<Long> ids = items.stream().map(StaffPosOrderItem::getProductId).filter(Objects::nonNull).distinct().toList();
		if (ids.isEmpty()) {
			return;
		}
		Map<Long, String> names = new HashMap<>();
		for (ShelterProduct product : productRepository.findAllById(ids)) {
			names.put(product.getId(), product.getName());
		}
		for (StaffPosOrderItem item : items) {
			item.setProductName(names.get(item.getProductId()));
		}
	}

	private ShelterProduct requireStaffProduct(Long id) {
		ShelterProduct product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		if (!Boolean.TRUE.equals(product.getStaffSale())) {
			throw new BusinessException("Product is not enabled for staff sale");
		}
		return product;
	}

	private static ShelterProductRequest toCatalogRequest(StaffProductRequest request) {
		ShelterProductRequest catalog = new ShelterProductRequest();
		catalog.setSku(request.getSku());
		catalog.setName(request.getName());
		catalog.setUnit(request.getUnit() == null ? UnitOfMeasure.PIECE : request.getUnit());
		catalog.setCategory(ShelterProductCategory.STAFF_RETAIL);
		catalog.setBarcode(request.getBarcode());
		catalog.setLotTracked(false);
		catalog.setStaffSale(true);
		catalog.setClinicalUse(false);
		catalog.setUnitPrice(request.getUnitPrice());
		catalog.setUnitCost(request.getUnitCost());
		catalog.setQtyOnHand(request.getQtyOnHand());
		return catalog;
	}

	private static BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private void assertCanViewEmployee(Long employeeId) {
		var role = TenantContext.require().role();
		if (role == com.dertz.spectra.Enum.Role.EMPLOYEE || role == com.dertz.spectra.Enum.Role.VET_TECH_EMPLOYEE) {
			if (!employeeId.equals(BranchScope.currentUserId())) {
				throw new BusinessException("Not your passbook", "FORBIDDEN", HttpStatus.FORBIDDEN);
			}
		}
	}

	private CreditMeter creditMeter(User employee) {
		LocalDate now = LocalDate.now(IST);
		Instant from = now.withDayOfMonth(1).atStartOfDay(IST).toInstant();
		Instant to = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(IST).toInstant();
		BigDecimal unpaid = orderRepository.sumUnpaidDues(employee.getId(), PosTender.PAYROLL_CREDIT,
				PosOrderStatus.COMPLETED, from, to);
		return new CreditMeter(unpaid == null ? BigDecimal.ZERO : unpaid);
	}

	private record CreditMeter(BigDecimal unpaidDues) {
	}

	private static final class ProductPnl {
		private BigDecimal soldQty = BigDecimal.ZERO;
		private BigDecimal revenue = BigDecimal.ZERO;
		private BigDecimal cogs = BigDecimal.ZERO;
	}
}
