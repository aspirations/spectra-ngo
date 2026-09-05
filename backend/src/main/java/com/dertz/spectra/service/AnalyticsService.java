package com.dertz.spectra.service;

import com.dertz.spectra.Enum.AnalyticsAlertType;
import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.LeaveStatus;
import com.dertz.spectra.Enum.LeaveType;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.AnalyticsAlert;
import com.dertz.spectra.model.FuelLog;
import com.dertz.spectra.model.LeaveRequest;
import com.dertz.spectra.model.ShelterProduct;
import com.dertz.spectra.model.StaffPosOrder;
import com.dertz.spectra.model.User;
import com.dertz.spectra.repository.AnalyticsAlertRepository;
import com.dertz.spectra.repository.DogCaseNoteRepository;
import com.dertz.spectra.repository.FuelLogRepository;
import com.dertz.spectra.repository.InternalConsumptionRepository;
import com.dertz.spectra.repository.LeaveRequestRepository;
import com.dertz.spectra.repository.StaffPosOrderRepository;
import com.dertz.spectra.repository.UserRepository;
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
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

	private static final BigDecimal MIN_KM_PER_LITRE = new BigDecimal("7.0");
	private static final BigDecimal MILEAGE_DROP = new BigDecimal("0.15");
	private static final int GHOST_IDLE_DAYS = 21;

	private final AnalyticsAlertRepository analyticsAlertRepository;
	private final FuelLogRepository fuelLogRepository;
	private final UserRepository userRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final StaffPosOrderRepository staffPosOrderRepository;
	private final InternalConsumptionRepository consumptionRepository;
	private final DogCaseNoteRepository dogCaseNoteRepository;
	private final InventoryService inventoryService;

	@Transactional
	public List<AnalyticsAlert> scanAndList() {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		scanLowStock(tenantId, branchId);
		scanFuelMileage(tenantId, branchId);
		scanGhostWorkers(tenantId, branchId);
		scanPosDuringLop(tenantId, branchId);
		return analyticsAlertRepository.findByBranchIdOrderByDetectedAtDesc(branchId);
	}

	@Transactional
	public AnalyticsAlert acknowledge(Long id) {
		AnalyticsAlert alert = analyticsAlertRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
		alert.setAcknowledged(true);
		return analyticsAlertRepository.save(alert);
	}

	@Transactional
	public void raise(AnalyticsAlertType type, String title, String detail, String severity) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		raise(tenantId, branchId, type, title, detail, severity);
	}

	private void scanLowStock(Long tenantId, Long branchId) {
		for (ShelterProduct product : inventoryService.products()) {
			if (product.getReorderLevel() == null || product.getReorderLevel().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal qty = inventoryService.availableQty(product);
			if (qty.compareTo(product.getReorderLevel()) >= 0) {
				continue;
			}
			raise(tenantId, branchId, AnalyticsAlertType.LOW_STOCK, "Low stock: " + product.getSku(),
					product.getName() + " is " + qty.stripTrailingZeros().toPlainString()
							+ " vs reorder " + product.getReorderLevel().stripTrailingZeros().toPlainString(),
					"HIGH");
		}
	}

	private void scanFuelMileage(Long tenantId, Long branchId) {
		List<FuelLog> fills = fuelLogRepository.findByBranchIdOrderByFilledAtDesc(branchId);
		java.util.LinkedHashSet<String> vehicles = new java.util.LinkedHashSet<>();
		fills.forEach(f -> vehicles.add(f.getVehicleLabel()));
		for (String vehicle : vehicles) {
			List<FuelLog> ordered = fuelLogRepository
					.findByBranchIdAndVehicleLabelIgnoreCaseOrderByOdometerKmAsc(branchId, vehicle);
			BigDecimal prevKmL = null;
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
				BigDecimal kmL = km.divide(cur.getLitres(), 2, RoundingMode.HALF_UP);
				boolean belowFloor = kmL.compareTo(MIN_KM_PER_LITRE) < 0;
				boolean dropped = prevKmL != null && prevKmL.compareTo(BigDecimal.ZERO) > 0
						&& kmL.compareTo(prevKmL.multiply(BigDecimal.ONE.subtract(MILEAGE_DROP))) < 0;
				if (belowFloor || dropped) {
					raise(tenantId, branchId, AnalyticsAlertType.FUEL_MILEAGE, "Low mileage: " + vehicle,
							vehicle + " ran " + kmL + " km/L on " + cur.getFilledAt()
									+ (dropped ? " vs prior " + prevKmL + " km/L" : " (floor " + MIN_KM_PER_LITRE + ")"),
							"MEDIUM");
				}
				prevKmL = kmL;
			}
		}
	}

	private void scanGhostWorkers(Long tenantId, Long branchId) {
		LocalDate sinceDate = LocalDate.now().minusDays(GHOST_IDLE_DAYS);
		Instant since = sinceDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
		List<User> staff = userRepository.findByBranchId(branchId).stream()
				.filter(u -> u.getStatus() == EntityStatus.ACTIVE)
				.filter(u -> u.getRole() != Role.PLATFORM_ADMIN && u.getRole() != Role.NGO_ADMIN)
				.filter(u -> u.getBaseMonthlySalary() != null && u.getBaseMonthlySalary().compareTo(BigDecimal.ZERO) > 0)
				.filter(u -> hiredLongEnough(u, since))
				.toList();
		for (User user : staff) {
			boolean leave = leaveRequestRepository.findByUserIdAndStatusIn(user.getId(), List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING))
					.stream()
					.anyMatch(l -> overlapsSince(l, sinceDate));
			boolean pos = staffPosOrderRepository.findByEmployeeIdOrderByOrderAtDesc(user.getId()).stream()
					.anyMatch(o -> o.getOrderAt() != null && !o.getOrderAt().isBefore(since));
			boolean tookStock = consumptionRepository
					.findByBranchIdAndConsumptionDateBetweenOrderByConsumptionDateDesc(branchId, sinceDate, LocalDate.now())
					.stream()
					.anyMatch(c -> user.getId().equals(c.getTakenByUserId()));
			boolean notes = dogCaseNoteRepository.findByCreatedBy(user.getId()).stream()
					.anyMatch(n -> n.getCreatedDate() != null && n.getCreatedDate().toInstant().isAfter(since));
			if (leave || pos || tookStock || notes) {
				continue;
			}
			raise(tenantId, branchId, AnalyticsAlertType.GHOST_WORKER, "Ghost worker: " + user.getFullName(),
					user.getFullName() + " is on payroll with no leave, POS, stock issue, or notes in "
							+ GHOST_IDLE_DAYS + " days.",
					"HIGH");
		}
	}

	private void scanPosDuringLop(Long tenantId, Long branchId) {
		ZoneId zone = ZoneId.of("Asia/Kolkata");
		LocalDate sinceDate = LocalDate.now().minusDays(60);
		List<User> staff = userRepository.findByBranchId(branchId).stream()
				.filter(u -> u.getStatus() == EntityStatus.ACTIVE)
				.filter(u -> u.getRole() != Role.PLATFORM_ADMIN && u.getRole() != Role.NGO_ADMIN)
				.toList();
		for (User user : staff) {
			List<LeaveRequest> lops = leaveRequestRepository
					.findByUserIdAndStatusIn(user.getId(), List.of(LeaveStatus.APPROVED))
					.stream()
					.filter(l -> l.getType() == LeaveType.UNPAID_LEAVE_LOP || l.getType() == LeaveType.HALF_DAY_LOP)
					.filter(l -> overlapsSince(l, sinceDate))
					.toList();
			if (lops.isEmpty()) {
				continue;
			}
			List<StaffPosOrder> orders = staffPosOrderRepository.findByEmployeeIdOrderByOrderAtDesc(user.getId());
			for (LeaveRequest lop : lops) {
				boolean hit = orders.stream().anyMatch(o -> {
					if (o.getOrderAt() == null) {
						return false;
					}
					LocalDate day = LocalDate.ofInstant(o.getOrderAt(), zone);
					return sameIsoWeek(day, lop.getFromDate())
							|| (!day.isBefore(lop.getFromDate()) && !day.isAfter(lop.getToDate()));
				});
				if (hit) {
					raise(tenantId, branchId, AnalyticsAlertType.GHOST_WORKER,
							"Ghost worker: " + user.getFullName() + " POS during LOP",
							user.getFullName() + " posted a store purchase in the same week as "
									+ lop.getType() + " (" + lop.getFromDate() + ").",
							"HIGH");
				}
			}
		}
	}

	private static boolean sameIsoWeek(LocalDate a, LocalDate b) {
		return a.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR) == b.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
				&& a.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR) == b.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
	}

	private boolean hiredLongEnough(User user, Instant since) {
		Date created = user.getCreatedDate();
		if (created == null) {
			return true;
		}
		return created.toInstant().isBefore(since);
	}

	private boolean overlapsSince(LeaveRequest leave, LocalDate since) {
		return leave.getToDate() != null && !leave.getToDate().isBefore(since);
	}

	private void raise(Long tenantId, Long branchId, AnalyticsAlertType type, String title, String detail, String severity) {
		if (analyticsAlertRepository.existsByBranchIdAndAlertTypeAndTitleAndAcknowledgedFalse(branchId, type, title)) {
			return;
		}
		analyticsAlertRepository.save(AnalyticsAlert.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.alertType(type)
				.severity(severity)
				.title(title)
				.detail(detail)
				.detectedAt(Instant.now())
				.acknowledged(false)
				.build());
	}
}
