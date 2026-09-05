package com.dertz.spectra.service;

import com.dertz.spectra.Enum.ExpenseCategory;
import com.dertz.spectra.Enum.PaymentSource;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.AnalyticsAlert;
import com.dertz.spectra.model.DogExpenseLink;
import com.dertz.spectra.model.FuelLog;
import com.dertz.spectra.model.OperatingExpense;
import com.dertz.spectra.model.Resident;
import com.dertz.spectra.repository.DogExpenseLinkRepository;
import com.dertz.spectra.repository.FuelLogRepository;
import com.dertz.spectra.repository.InternalConsumptionRepository;
import com.dertz.spectra.repository.OperatingExpenseRepository;
import com.dertz.spectra.repository.ResidentRepository;
import com.dertz.spectra.request.DogExpenseRequest;
import com.dertz.spectra.request.FuelLogRequest;
import com.dertz.spectra.request.OperatingExpenseRequest;
import com.dertz.spectra.security.BranchScope;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpsDomainService {

	private final OperatingExpenseRepository operatingExpenseRepository;
	private final DogExpenseLinkRepository dogExpenseLinkRepository;
	private final FuelLogRepository fuelLogRepository;
	private final ResidentRepository residentRepository;
	private final InternalConsumptionRepository consumptionRepository;
	private final AnalyticsService analyticsService;
	private final PayrollService payrollService;

	@Transactional(readOnly = true)
	public List<OperatingExpense> expenses() {
		return operatingExpenseRepository.findByBranchIdOrderByExpenseDateDesc(BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public List<DogExpenseLink> dogExpenses() {
		return dogExpenseLinkRepository.findByBranchIdOrderByExpenseDateDesc(BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public List<DogExpenseLink> dogExpensesFor(Long residentId) {
		return dogExpenseLinkRepository.findByResidentIdOrderByExpenseDateDesc(residentId);
	}

	@Transactional(readOnly = true)
	public List<FuelLog> fuelLogs() {
		return fuelLogRepository.findByBranchIdOrderByFilledAtDesc(BranchScope.requireBranchId());
	}

	@Transactional
	public List<AnalyticsAlert> alerts() {
		return analyticsService.scanAndList();
	}

	@Transactional
	public AnalyticsAlert acknowledge(Long id) {
		return analyticsService.acknowledge(id);
	}

	@Transactional
	public OperatingExpense recordExpense(OperatingExpenseRequest request) {
		if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Amount must be positive");
		}
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		OperatingExpense saved = operatingExpenseRepository.save(OperatingExpense.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.category(request.getCategory())
				.description(request.getDescription().trim())
				.amount(request.getAmount())
				.paymentSource(request.getPaymentSource())
				.expenseDate(request.getExpenseDate() == null ? LocalDate.now() : request.getExpenseDate())
				.receiptUrl(blankToNull(request.getReceiptUrl()))
				.paidByUserId(request.getPaidByUserId())
				.vendorName(blankToNull(request.getVendorName()))
				.build());
		if (request.getResidentId() != null) {
			linkDog(request.getResidentId(), saved.getId(), null, null,
					saved.getDescription(), saved.getAmount(), saved.getExpenseDate());
		}
		return saved;
	}

	@Transactional
	public FuelLog recordFuel(FuelLogRequest request) {
		if (request.getLitres().compareTo(BigDecimal.ZERO) <= 0 || request.getOdometerKm().compareTo(BigDecimal.ZERO) < 0
				|| request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Fuel litres, odometer, and amount must be valid");
		}
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		LocalDate filledAt = request.getFilledAt() == null ? LocalDate.now() : request.getFilledAt();
		OperatingExpense opex = operatingExpenseRepository.save(OperatingExpense.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.category(ExpenseCategory.FUEL)
				.description("Fuel · " + request.getVehicleLabel().trim())
				.amount(request.getAmount())
				.paymentSource(request.getPaymentSource() == null ? PaymentSource.NGO_CASH : request.getPaymentSource())
				.expenseDate(filledAt)
				.receiptUrl(blankToNull(request.getReceiptUrl()))
				.paidByUserId(request.getPaidByUserId())
				.vendorName("Fuel pump")
				.build());
		FuelLog log = fuelLogRepository.save(FuelLog.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.vehicleLabel(request.getVehicleLabel().trim())
				.odometerKm(request.getOdometerKm())
				.litres(request.getLitres())
				.amount(request.getAmount())
				.filledAt(filledAt)
				.operatingExpenseId(opex.getId())
				.build());
		analyticsService.scanAndList();
		return log;
	}

	@Transactional
	public DogExpenseLink recordDogExpense(DogExpenseRequest request) {
		if (request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessException("Amount cannot be negative");
		}
		Resident resident = requireResident(request.getResidentId());
		return linkDog(resident.getId(), request.getOperatingExpenseId(), request.getShelterProductId(), null,
				request.getLabel().trim(), request.getAmount(),
				request.getExpenseDate() == null ? LocalDate.now() : request.getExpenseDate());
	}

	@Transactional
	public Map<String, Object> snapshot() {
		List<AnalyticsAlert> alerts = analyticsService.scanAndList();
		Long branchId = BranchScope.requireBranchId();
		List<OperatingExpense> expenses = operatingExpenseRepository.findByBranchIdOrderByExpenseDateDesc(branchId);
		BigDecimal opexTotal = expenses.stream().map(OperatingExpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		List<DogExpenseLink> dogLinks = dogExpenseLinkRepository.findByBranchIdOrderByExpenseDateDesc(branchId);
		BigDecimal dogTotal = dogLinks.stream().map(DogExpenseLink::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		List<FuelLog> fuel = fuelLogRepository.findByBranchIdOrderByFilledAtDesc(branchId);
		Map<String, Object> map = new HashMap<>();
		map.put("opexTotal", opexTotal);
		map.put("opexCount", expenses.size());
		map.put("dogExpenseTotal", dogTotal);
		map.put("dogExpenseCount", dogLinks.size());
		map.put("fuelFillCount", fuel.size());
		map.put("openAlerts", alerts.stream().filter(a -> !a.isAcknowledged()).count());
		map.put("recentAlerts", alerts.stream().limit(8).toList());
		map.put("recentExpenses", expenses.stream().limit(8).toList());
		map.put("recentFuel", fuel.stream().limit(5).toList());
		return map;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> sla() {
		return payrollService.slaThisMonth();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> activity() {
		Long branchId = BranchScope.requireBranchId();
		List<Map<String, Object>> rows = new ArrayList<>();
		operatingExpenseRepository.findByBranchIdOrderByExpenseDateDesc(branchId).stream().limit(20).forEach(e -> {
			Map<String, Object> row = new HashMap<>();
			row.put("at", e.getExpenseDate().toString());
			row.put("kind", "OPEX");
			row.put("title", e.getCategory() + " · " + e.getDescription());
			row.put("detail", e.getPaymentSource() + " · " + e.getAmount());
			rows.add(row);
		});
		fuelLogRepository.findByBranchIdOrderByFilledAtDesc(branchId).stream().limit(10).forEach(f -> {
			Map<String, Object> row = new HashMap<>();
			row.put("at", f.getFilledAt().toString());
			row.put("kind", "FUEL");
			row.put("title", f.getVehicleLabel());
			row.put("detail", f.getOdometerKm() + " km · " + f.getLitres() + " L");
			rows.add(row);
		});
		consumptionRepository
				.findByBranchIdAndConsumptionDateBetweenOrderByConsumptionDateDesc(branchId, LocalDate.now().minusDays(30),
						LocalDate.now())
				.stream().limit(15).forEach(c -> {
					Map<String, Object> row = new HashMap<>();
					row.put("at", c.getConsumptionDate().toString());
					row.put("kind", Boolean.TRUE.equals(c.getVarianceAlert()) ? "BURN_RATE" : "STOCK_ISSUE");
					row.put("title", "Issue #" + c.getId());
					row.put("detail", c.getType() + (Boolean.TRUE.equals(c.getVarianceAlert()) ? " · variance flagged" : ""));
					rows.add(row);
				});
		rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("at"))).reversed());
		return rows.stream().limit(40).toList();
	}

	public DogExpenseLink linkDog(Long residentId, Long opexId, Long productId, Long consumptionId, String label,
			BigDecimal amount, LocalDate date) {
		Resident resident = requireResident(residentId);
		return dogExpenseLinkRepository.save(DogExpenseLink.builder()
				.tenantId(TenantContext.require().tenantId())
				.branchId(BranchScope.requireBranchId())
				.residentId(resident.getId())
				.operatingExpenseId(opexId)
				.shelterProductId(productId)
				.consumptionId(consumptionId)
				.label(label)
				.amount(amount)
				.expenseDate(date)
				.build());
	}

	private Resident requireResident(Long residentId) {
		Resident resident = residentRepository.findById(residentId)
				.orElseThrow(() -> new ResourceNotFoundException("Resident not found"));
		if (!resident.getBranchId().equals(BranchScope.requireBranchId())) {
			throw new BusinessException("Resident is not in this branch");
		}
		return resident;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
