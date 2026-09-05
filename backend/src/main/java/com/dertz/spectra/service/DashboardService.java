package com.dertz.spectra.service;

import com.dertz.spectra.Enum.AuditStatus;
import com.dertz.spectra.Enum.CareSection;
import com.dertz.spectra.Enum.ExpenseCategory;
import com.dertz.spectra.Enum.LeaveStatus;
import com.dertz.spectra.Enum.LeaveType;
import com.dertz.spectra.Enum.WarehouseType;
import com.dertz.spectra.dto.CensusDTO;
import com.dertz.spectra.model.AnalyticsAlert;
import com.dertz.spectra.model.FuelLog;
import com.dertz.spectra.model.InternalConsumption;
import com.dertz.spectra.model.LeaveRequest;
import com.dertz.spectra.model.OperatingExpense;
import com.dertz.spectra.model.StaffPosOrder;
import com.dertz.spectra.model.StockBatch;
import com.dertz.spectra.model.Warehouse;
import com.dertz.spectra.repository.AnalyticsAlertRepository;
import com.dertz.spectra.repository.FuelLogRepository;
import com.dertz.spectra.repository.InternalConsumptionRepository;
import com.dertz.spectra.repository.InventoryAuditLogRepository;
import com.dertz.spectra.repository.LeaveRequestRepository;
import com.dertz.spectra.repository.OperatingExpenseRepository;
import com.dertz.spectra.repository.StaffPosOrderRepository;
import com.dertz.spectra.repository.StockBatchRepository;
import com.dertz.spectra.repository.WarehouseRepository;
import com.dertz.spectra.security.BranchScope;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

	private final ResidentService residentService;
	private final InventoryService inventoryService;
	private final StaffPosService staffPosService;
	private final InternalConsumptionRepository consumptionRepository;
	private final InventoryAuditLogRepository auditLogRepository;
	private final StaffPosOrderRepository orderRepository;
	private final OperatingExpenseRepository operatingExpenseRepository;
	private final AnalyticsAlertRepository analyticsAlertRepository;
	private final FuelLogRepository fuelLogRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final WarehouseRepository warehouseRepository;
	private final StockBatchRepository stockBatchRepository;
	private final PurchaseOrderService purchaseOrderService;

	@Transactional(readOnly = true)
	public Map<String, Object> snapshot() {
		Long branchId = BranchScope.requireBranchId();
		LocalDate today = LocalDate.now(ZONE);
		LocalDate from = today.minusDays(13);

		CensusDTO census = residentService.census(branchId);
		List<?> treatmentsDue = residentService.dueToday();
		List<InternalConsumption> alerts = inventoryService.alerts();
		var openAudits = auditLogRepository.findByBranchIdAndStatus(branchId, AuditStatus.FLAGGED_UNCERTAIN);
		List<StaffPosOrder> recentOrders = orderRepository.findByBranchIdOrderByOrderAtDesc(branchId).stream()
				.limit(8)
				.toList();
		Long userId = TenantContext.require().userId();

		List<OperatingExpense> expenses = operatingExpenseRepository.findByBranchIdOrderByExpenseDateDesc(branchId);
		List<AnalyticsAlert> analyticsAlerts = analyticsAlertRepository.findByBranchIdOrderByDetectedAtDesc(branchId);
		List<FuelLog> fuelLogs = fuelLogRepository.findByBranchIdOrderByFilledAtDesc(branchId);
		List<InternalConsumption> consumptions = consumptionRepository
				.findByBranchIdAndConsumptionDateBetweenOrderByConsumptionDateDesc(branchId, from, today);
		List<LeaveRequest> leaves = leaveRequestRepository.findByBranchIdAndStatus(branchId, LeaveStatus.APPROVED);
		Instant orderFrom = from.atStartOfDay(ZONE).toInstant();
		List<StaffPosOrder> periodOrders = orderRepository.findByBranchIdAndOrderAtGreaterThanEqualAndOrderAtLessThan(
				branchId, orderFrom, today.plusDays(1).atStartOfDay(ZONE).toInstant());

		BigDecimal opexTotal = expenses.stream().map(OperatingExpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		long openAlertCount = analyticsAlerts.stream().filter(a -> !a.isAcknowledged()).count();
		List<Map<String, Object>> expiring = expiringBatches(branchId, today);
		long onLeaveToday = leaves.stream()
				.filter(l -> !today.isBefore(l.getFromDate()) && !today.isAfter(l.getToDate()))
				.map(LeaveRequest::getUserId)
				.distinct()
				.count();

		Map<String, Object> data = new HashMap<>();
		data.put("census", census);
		data.put("treatmentsDue", treatmentsDue);
		data.put("consumptionAlerts", alerts);
		data.put("openAudits", openAudits);
		data.put("recentOrders", recentOrders);
		data.put("myCredit", staffPosService.creditSnapshot(userId));
		data.put("kpis", Map.of(
				"openAudits", openAudits.size(),
				"openAlerts", openAlertCount,
				"opexTotal", opexTotal,
				"treatmentsDue", treatmentsDue.size(),
				"expiringBatches", expiring.size(),
				"onLeaveToday", onLeaveToday,
				"pendingIssues", inventoryService.pendingIssueCount(),
				"pendingPoApprovals", purchaseOrderService.pendingApprovalsCount(),
				"pendingPoWaiting", purchaseOrderService.pendingSubmittedCount()
		));
		data.put("charts", Map.of(
				"censusByCategory", censusSeries(census),
				"feedTrend", feedTrend(consumptions, census.getTheoreticalFeedKg(), from, today),
				"opexByCategory", opexByCategory(expenses),
				"leaveMix", leaveMix(leaves, today.withDayOfMonth(1), today),
				"staffSpendTrend", staffSpendTrend(periodOrders, from, today),
				"alertSeverity", alertSeverity(analyticsAlerts),
				"fuelEfficiency", fuelEfficiency(fuelLogs)
		));
		data.put("expiringBatches", expiring.stream().limit(6).toList());
		data.put("opsAlerts", analyticsAlerts.stream().filter(a -> !a.isAcknowledged()).limit(5).toList());
		return data;
	}

	private List<Map<String, Object>> censusSeries(CensusDTO census) {
		Map<CareSection, Long> byCat = census.getByCategory() == null ? Map.of() : census.getByCategory();
		List<Map<String, Object>> series = new ArrayList<>();
		byCat.forEach((k, v) -> series.add(Map.of("name", k.name(), "value", v.intValue())));
		return series;
	}

	private List<Map<String, Object>> feedTrend(List<InternalConsumption> rows, BigDecimal theoretical,
			LocalDate from, LocalDate today) {
		BigDecimal theory = theoretical == null ? BigDecimal.ZERO : theoretical;
		Map<LocalDate, BigDecimal> actualByDay = new HashMap<>();
		for (InternalConsumption c : rows) {
			if (c.getStatus() != null && c.getStatus() != com.dertz.spectra.Enum.ConsumptionStatus.ISSUED) {
				continue;
			}
			if (c.getActualQty() == null) {
				continue;
			}
			actualByDay.merge(c.getConsumptionDate(), c.getActualQty(), BigDecimal::add);
		}
		List<Map<String, Object>> series = new ArrayList<>();
		for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
			Map<String, Object> point = new LinkedHashMap<>();
			point.put("date", d.format(DAY));
			point.put("actual", actualByDay.getOrDefault(d, BigDecimal.ZERO));
			point.put("theoretical", theory);
			series.add(point);
		}
		return series;
	}

	private List<Map<String, Object>> opexByCategory(List<OperatingExpense> expenses) {
		Map<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
		for (OperatingExpense e : expenses) {
			totals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
		}
		return totals.entrySet().stream()
				.sorted(Map.Entry.<ExpenseCategory, BigDecimal>comparingByValue().reversed())
				.map(e -> Map.<String, Object>of("name", e.getKey().name(), "value", e.getValue()))
				.toList();
	}

	private List<Map<String, Object>> leaveMix(List<LeaveRequest> leaves, LocalDate from, LocalDate to) {
		Map<LeaveType, Long> counts = new EnumMap<>(LeaveType.class);
		for (LeaveRequest leave : leaves) {
			if (leave.getToDate().isBefore(from) || leave.getFromDate().isAfter(to)) {
				continue;
			}
			counts.merge(leave.getType(), 1L, Long::sum);
		}
		return counts.entrySet().stream()
				.map(e -> Map.<String, Object>of("name", e.getKey().name(), "value", e.getValue()))
				.toList();
	}

	private List<Map<String, Object>> staffSpendTrend(List<StaffPosOrder> orders, LocalDate from, LocalDate today) {
		Map<LocalDate, BigDecimal> byDay = new HashMap<>();
		for (StaffPosOrder o : orders) {
			LocalDate day = LocalDate.ofInstant(o.getOrderAt(), ZONE);
			byDay.merge(day, o.getTotal(), BigDecimal::add);
		}
		List<Map<String, Object>> series = new ArrayList<>();
		for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
			series.add(Map.of("date", d.format(DAY), "total", byDay.getOrDefault(d, BigDecimal.ZERO)));
		}
		return series;
	}

	private List<Map<String, Object>> alertSeverity(List<AnalyticsAlert> alerts) {
		Map<String, Long> counts = alerts.stream()
				.filter(a -> !a.isAcknowledged())
				.collect(Collectors.groupingBy(
						a -> a.getSeverity() == null ? "UNKNOWN" : a.getSeverity(),
						Collectors.counting()));
		return counts.entrySet().stream()
				.map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
				.toList();
	}

	private List<Map<String, Object>> fuelEfficiency(List<FuelLog> logs) {
		List<FuelLog> ordered = logs.stream()
				.sorted(Comparator.comparing(FuelLog::getFilledAt).thenComparing(FuelLog::getId))
				.toList();
		List<Map<String, Object>> series = new ArrayList<>();
		for (int i = 1; i < ordered.size(); i++) {
			FuelLog prev = ordered.get(i - 1);
			FuelLog cur = ordered.get(i);
			if (prev.getOdometerKm() == null || cur.getOdometerKm() == null || cur.getLitres() == null
					|| cur.getLitres().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal km = cur.getOdometerKm().subtract(prev.getOdometerKm());
			if (km.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal kmpl = km.divide(cur.getLitres(), 2, RoundingMode.HALF_UP);
			series.add(Map.of(
					"date", cur.getFilledAt().format(DAY),
					"kmPerLitre", kmpl,
					"vehicle", cur.getVehicleLabel() == null ? "Vehicle" : cur.getVehicleLabel()));
		}
		return series;
	}

	private List<Map<String, Object>> expiringBatches(Long branchId, LocalDate today) {
		Warehouse warehouse = warehouseRepository.findByBranchIdAndType(branchId, WarehouseType.SHELTER_STORE)
				.orElse(null);
		if (warehouse == null) {
			return List.of();
		}
		LocalDate horizon = today.plusDays(45);
		return stockBatchRepository.findByWarehouseIdOrderByExpiryDateAsc(warehouse.getId()).stream()
				.filter(b -> b.getQtyOnHand().compareTo(BigDecimal.ZERO) > 0)
				.filter(b -> !b.getExpiryDate().isAfter(horizon))
				.map(this::batchRow)
				.toList();
	}

	private Map<String, Object> batchRow(StockBatch b) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("batchNumber", b.getBatchNumber());
		row.put("shelterProductId", b.getShelterProductId());
		row.put("qtyOnHand", b.getQtyOnHand());
		row.put("expiryDate", b.getExpiryDate().format(DAY));
		row.put("unitLandedCost", b.getUnitLandedCost());
		return row;
	}
}
