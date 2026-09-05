package com.dertz.spectra.service;

import com.dertz.spectra.dto.StaffPayslip;
import com.dertz.spectra.Enum.AdvanceStatus;
import com.dertz.spectra.Enum.LeaveStatus;
import com.dertz.spectra.Enum.LeaveType;
import com.dertz.spectra.Enum.PaymentSource;
import com.dertz.spectra.Enum.PayrollStatus;
import com.dertz.spectra.Enum.PosOrderStatus;
import com.dertz.spectra.Enum.PosTender;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.Enum.WorkingDaysMode;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.LeaveRequest;
import com.dertz.spectra.model.OperatingExpense;
import com.dertz.spectra.model.PayrollItem;
import com.dertz.spectra.model.PayrollRun;
import com.dertz.spectra.model.SalaryAdvance;
import com.dertz.spectra.model.StaffPosOrder;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.model.User;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.LeaveRequestRepository;
import com.dertz.spectra.repository.OperatingExpenseRepository;
import com.dertz.spectra.repository.PayrollItemRepository;
import com.dertz.spectra.repository.PayrollRunRepository;
import com.dertz.spectra.repository.SalaryAdvanceRepository;
import com.dertz.spectra.repository.StaffPosOrderRepository;
import com.dertz.spectra.repository.TenantSettingsRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.request.AdvanceRequest;
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
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayrollService {

	private final PayrollRunRepository payrollRunRepository;
	private final PayrollItemRepository payrollItemRepository;
	private final UserRepository userRepository;
	private final BranchRepository branchRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final StaffPosOrderRepository orderRepository;
	private final TenantSettingsRepository tenantSettingsRepository;
	private final SalaryAdvanceRepository salaryAdvanceRepository;
	private final OperatingExpenseRepository operatingExpenseRepository;
	private final PdfDocuments pdfDocuments;

	@Transactional
	public PayrollRun generate(int year, int month) {
		Long tenantId = TenantContext.require().tenantId();
		Long branchId = BranchScope.requireBranchId();
		payrollRunRepository.findByBranchIdAndPeriodYearAndPeriodMonth(branchId, year, month).ifPresent(existing -> {
			if (existing.getStatus() != PayrollStatus.DRAFT) {
				throw new BusinessException("Payroll for this period is already " + existing.getStatus());
			}
			for (SalaryAdvance advance : salaryAdvanceRepository.findByPayrollRunId(existing.getId())) {
				advance.setPayrollRunId(null);
				salaryAdvanceRepository.save(advance);
			}
			for (PayrollItem item : payrollItemRepository.findByPayrollRunId(existing.getId())) {
				for (OperatingExpense expense : operatingExpenseRepository.findByReimbursedInPayrollItemId(item.getId())) {
					expense.setReimbursedInPayrollItemId(null);
					operatingExpenseRepository.save(expense);
				}
				payrollItemRepository.delete(item);
			}
			payrollRunRepository.delete(existing);
		});
		PayrollRun run = payrollRunRepository.save(PayrollRun.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.periodYear(year)
				.periodMonth(month)
				.status(PayrollStatus.DRAFT)
				.build());
		List<User> employees = userRepository.findByBranchId(branchId).stream()
				.filter(u -> u.getRole() != Role.NGO_ADMIN && u.getRole() != Role.PLATFORM_ADMIN)
				.toList();
		YearMonth ym = YearMonth.of(year, month);
		for (User user : employees) {
			PayCalc c = calc(user, ym);
			String breakdown = """
					{"baseSalary":%s,"workingDays":%d,"unpaidLeaveDays":%s,"dailyWage":%s,"lopDeduction":%s,"storeDues":%s,"otherDeductions":%s,"advanceDeduction":%s,"reimbursementCredit":%s,"netPayout":%s,"rolledOverStoreDebt":%s}
					""".formatted(c.base(), c.workingDays(), c.unpaidDays(), c.dailyWage(),
					c.lop(), c.storeDues(), c.other(), c.advanceDeduction(), c.reimbursement(), c.net(), c.roll());
			PayrollItem saved = payrollItemRepository.save(PayrollItem.builder()
					.tenantId(tenantId)
					.payrollRunId(run.getId())
					.userId(user.getId())
					.baseSalary(c.base())
					.workingDays(c.workingDays())
					.unpaidLeaveDays(c.unpaidDays())
					.lopDeduction(c.lop())
					.storeDues(c.storeDues())
					.otherDeductions(c.other())
					.advanceDeduction(c.advanceDeduction())
					.reimbursementCredit(c.reimbursement())
					.netPayout(c.net())
					.rolledOverStoreDebt(c.roll())
					.breakdownJson(breakdown)
					.build());
			for (SalaryAdvance advance : c.advances()) {
				advance.setPayrollRunId(run.getId());
				salaryAdvanceRepository.save(advance);
			}
			for (OperatingExpense expense : c.pockets()) {
				expense.setReimbursedInPayrollItemId(saved.getId());
				operatingExpenseRepository.save(expense);
			}
		}
		return run;
	}

	@Transactional
	public PayrollRun approve(Long id) {
		PayrollRun run = getRun(id);
		if (run.getStatus() != PayrollStatus.DRAFT) {
			throw new BusinessException("Only DRAFT runs can be approved");
		}
		YearMonth ym = YearMonth.of(run.getPeriodYear(), run.getPeriodMonth());
		ZoneId zone = ZoneId.of("Asia/Kolkata");
		Instant from = ym.atDay(1).atStartOfDay(zone).toInstant();
		Instant to = ym.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant();
		for (PayrollItem item : payrollItemRepository.findByPayrollRunId(run.getId())) {
			List<StaffPosOrder> orders = orderRepository.findByEmployeeIdAndTenderAndStatusAndOrderAtBetween(
					item.getUserId(), PosTender.PAYROLL_CREDIT, PosOrderStatus.COMPLETED, from, to);
			for (StaffPosOrder order : orders) {
				order.setStatus(PosOrderStatus.DEDUCTED_IN_PAYROLL);
				order.setPayrollRunId(run.getId());
				orderRepository.save(order);
			}
			User user = userRepository.findById(item.getUserId())
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));
			user.setRolledOverStoreDebt(item.getRolledOverStoreDebt());
			userRepository.save(user);
		}
		for (SalaryAdvance advance : salaryAdvanceRepository.findByPayrollRunId(run.getId())) {
			advance.setStatus(AdvanceStatus.DEDUCTED_IN_PAYROLL);
			salaryAdvanceRepository.save(advance);
		}
		run.setStatus(PayrollStatus.APPROVED);
		run.setApprovedBy(BranchScope.currentUserId());
		return payrollRunRepository.save(run);
	}

	@Transactional
	public PayrollRun disburse(Long id) {
		PayrollRun run = getRun(id);
		if (run.getStatus() != PayrollStatus.APPROVED) {
			throw new BusinessException("Only APPROVED runs can be disbursed");
		}
		run.setStatus(PayrollStatus.DISBURSED);
		run.setDisbursedBy(BranchScope.currentUserId());
		run.setDisbursedAt(Instant.now());
		return payrollRunRepository.save(run);
	}

	@Transactional(readOnly = true)
	public List<PayrollRun> list() {
		return payrollRunRepository.findByBranchIdOrderByPeriodYearDescPeriodMonthDesc(BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public PayrollRun getRun(Long id) {
		return payrollRunRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payroll run not found"));
	}

	@Transactional(readOnly = true)
	public List<PayrollItem> items(Long runId) {
		getRun(runId);
		return payrollItemRepository.findByPayrollRunId(runId);
	}

	@Transactional(readOnly = true)
	public PayrollItem payslip(Long itemId) {
		PayrollItem item = payrollItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));
		Role role = TenantContext.require().role();
		if (role == Role.EMPLOYEE || role == Role.VET_TECH_EMPLOYEE) {
			if (!item.getUserId().equals(BranchScope.currentUserId())) {
				throw new BusinessException("Not your payslip", "FORBIDDEN", org.springframework.http.HttpStatus.FORBIDDEN);
			}
		}
		return item;
	}

	@Transactional(readOnly = true)
	public List<PayrollItem> myPayslips() {
		return payrollItemRepository.findByUserIdOrderByIdDesc(BranchScope.currentUserId());
	}

	@Transactional
	public SalaryAdvance recordAdvance(AdvanceRequest request) {
		if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Advance must be positive");
		}
		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Long branchId = BranchScope.requireBranchId();
		if (user.getBranchId() != null && !user.getBranchId().equals(branchId)) {
			throw new BusinessException("Staff is not in this branch");
		}
		return salaryAdvanceRepository.save(SalaryAdvance.builder()
				.tenantId(TenantContext.require().tenantId())
				.branchId(branchId)
				.userId(user.getId())
				.amount(request.getAmount())
				.advancedAt(request.getAdvancedAt() == null ? LocalDate.now() : request.getAdvancedAt())
				.notes(request.getNotes())
				.status(AdvanceStatus.OPEN)
				.build());
	}

	@Transactional(readOnly = true)
	public List<SalaryAdvance> advances() {
		return salaryAdvanceRepository.findByBranchIdOrderByAdvancedAtDesc(BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> slaThisMonth() {
		YearMonth ym = YearMonth.now();
		int workingDays = workingDays(ym.getYear(), ym.getMonthValue());
		LocalDate from = ym.atDay(1);
		LocalDate to = ym.atEndOfMonth();
		List<Map<String, Object>> rows = new ArrayList<>();
		List<User> employees = userRepository.findByBranchId(BranchScope.requireBranchId()).stream()
				.filter(u -> u.getRole() != Role.NGO_ADMIN && u.getRole() != Role.PLATFORM_ADMIN)
				.toList();
		for (User user : employees) {
			BigDecimal unpaid = unpaidDays(user.getId(), from, to);
			BigDecimal paidLeave = leaveDays(user.getId(), from, to, true);
			BigDecimal present = BigDecimal.valueOf(workingDays).subtract(unpaid).subtract(paidLeave).max(BigDecimal.ZERO);
			BigDecimal slaPct = workingDays == 0
					? BigDecimal.ZERO
					: present.multiply(new BigDecimal("100")).divide(BigDecimal.valueOf(workingDays), 1, RoundingMode.HALF_UP);
			Map<String, Object> row = new HashMap<>();
			row.put("userId", user.getId());
			row.put("fullName", user.getFullName());
			row.put("workingDays", workingDays);
			row.put("presentDays", present);
			row.put("paidLeaveDays", paidLeave);
			row.put("unpaidLeaveDays", unpaid);
			row.put("slaPercent", slaPct);
			rows.add(row);
		}
		return rows;
	}

	@Transactional(readOnly = true)
	public StaffPayslip currentMonth(Long userId) {
		User user = requireStaff(userId);
		YearMonth ym = YearMonth.now(ZoneId.of("Asia/Kolkata"));
		return payslipFor(user, ym);
	}

	@Transactional(readOnly = true)
	public byte[] payslipPdf(Long userId) {
		StaffPayslip slip = currentMonth(userId);
		return pdfDocuments.payslip(slip, orgLabel(), centreLabel());
	}

	private StaffPayslip payslipFor(User user, YearMonth ym) {
		Optional<PayrollRun> run = payrollRunRepository.findByBranchIdAndPeriodYearAndPeriodMonth(
				BranchScope.requireBranchId(), ym.getYear(), ym.getMonthValue());
		if (run.isPresent()) {
			Optional<PayrollItem> item = payrollItemRepository.findByPayrollRunIdAndUserId(run.get().getId(), user.getId());
			if (item.isPresent()) {
				return fromItem(user, item.get(), ym, run.get().getStatus().name(), true);
			}
		}
		return fromCalc(user, calc(user, ym), ym, false, null);
	}

	private User requireStaff(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Role actor = TenantContext.require().role();
		boolean manager = actor == Role.NGO_ADMIN || actor == Role.BRANCH_ADMIN || actor == Role.INVENTORY_MANAGER;
		if (!manager && !BranchScope.currentUserId().equals(userId)) {
			throw new BusinessException("Not your payslip", "FORBIDDEN", HttpStatus.FORBIDDEN);
		}
		return user;
	}

	private PayCalc calc(User user, YearMonth ym) {
		int workingDays = workingDays(ym.getYear(), ym.getMonthValue());
		LocalDate from = ym.atDay(1);
		LocalDate to = ym.atEndOfMonth();
		ZoneId zone = ZoneId.of("Asia/Kolkata");
		Instant fromTs = from.atStartOfDay(zone).toInstant();
		Instant toTs = to.plusDays(1).atStartOfDay(zone).toInstant();
		BigDecimal unpaidDays = unpaidDays(user.getId(), from, to);
		BigDecimal base = nz(user.getBaseMonthlySalary());
		BigDecimal dailyWage = workingDays == 0
				? BigDecimal.ZERO
				: base.divide(BigDecimal.valueOf(workingDays), 4, RoundingMode.HALF_UP);
		BigDecimal lop = dailyWage.multiply(unpaidDays).setScale(2, RoundingMode.HALF_UP);
		BigDecimal monthDues = nz(orderRepository.sumUnpaidDues(user.getId(), PosTender.PAYROLL_CREDIT,
				PosOrderStatus.COMPLETED, fromTs, toTs));
		BigDecimal storeDues = monthDues.add(nz(user.getRolledOverStoreDebt()));
		BigDecimal other = nz(user.getOtherFixedDeductions());
		List<SalaryAdvance> advances = salaryAdvanceRepository
				.findByUserIdAndStatusAndPayrollRunIdIsNull(user.getId(), AdvanceStatus.OPEN);
		BigDecimal advanceDeduction = advances.stream().map(SalaryAdvance::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		List<OperatingExpense> pockets = operatingExpenseRepository
				.findByPaidByUserIdAndPaymentSourceAndReimbursedInPayrollItemIdIsNull(user.getId(), PaymentSource.WORKER_PAID);
		BigDecimal reimbursement = pockets.stream().map(OperatingExpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal available = base.subtract(lop).subtract(other).subtract(advanceDeduction).max(BigDecimal.ZERO);
		BigDecimal storeApplied = storeDues.min(available);
		BigDecimal net = available.subtract(storeApplied).add(reimbursement).max(BigDecimal.ZERO)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal roll = storeDues.subtract(storeApplied).setScale(2, RoundingMode.HALF_UP);
		return new PayCalc(workingDays, unpaidDays, dailyWage.setScale(2, RoundingMode.HALF_UP), lop, storeDues, other,
				advanceDeduction, reimbursement, net, roll, base, advances, pockets);
	}

	private StaffPayslip fromCalc(User user, PayCalc c, YearMonth ym, boolean locked, String runStatus) {
		return StaffPayslip.builder()
				.userId(user.getId())
				.fullName(user.getFullName())
				.email(user.getEmail())
				.role(user.getRole() == null ? "" : user.getRole().name())
				.periodYear(ym.getYear())
				.periodMonth(ym.getMonthValue())
				.periodLabel(periodLabel(ym))
				.locked(locked)
				.runStatus(runStatus)
				.baseSalary(c.base())
				.workingDays(c.workingDays())
				.unpaidLeaveDays(c.unpaidDays())
				.dailyWage(c.dailyWage())
				.lopDeduction(c.lop())
				.storeDues(c.storeDues())
				.otherDeductions(c.other())
				.advanceDeduction(c.advanceDeduction())
				.reimbursementCredit(c.reimbursement())
				.netPayout(c.net())
				.rolledOverStoreDebt(c.roll())
				.build();
	}

	private StaffPayslip fromItem(User user, PayrollItem item, YearMonth ym, String runStatus, boolean locked) {
		BigDecimal daily = item.getWorkingDays() == null || item.getWorkingDays() == 0
				? BigDecimal.ZERO
				: nz(item.getBaseSalary()).divide(BigDecimal.valueOf(item.getWorkingDays()), 2, RoundingMode.HALF_UP);
		return StaffPayslip.builder()
				.userId(user.getId())
				.fullName(user.getFullName())
				.email(user.getEmail())
				.role(user.getRole() == null ? "" : user.getRole().name())
				.periodYear(ym.getYear())
				.periodMonth(ym.getMonthValue())
				.periodLabel(periodLabel(ym))
				.locked(locked)
				.runStatus(runStatus)
				.baseSalary(item.getBaseSalary())
				.workingDays(item.getWorkingDays())
				.unpaidLeaveDays(item.getUnpaidLeaveDays())
				.dailyWage(daily)
				.lopDeduction(item.getLopDeduction())
				.storeDues(item.getStoreDues())
				.otherDeductions(item.getOtherDeductions())
				.advanceDeduction(nz(item.getAdvanceDeduction()))
				.reimbursementCredit(nz(item.getReimbursementCredit()))
				.netPayout(item.getNetPayout())
				.rolledOverStoreDebt(item.getRolledOverStoreDebt())
				.build();
	}

	private String orgLabel() {
		String name = TenantContext.require().tenantCode();
		return name == null || name.isBlank() ? "Spectra" : name;
	}

	private String centreLabel() {
		return branchRepository.findById(BranchScope.requireBranchId()).map(Branch::getName).orElse("Centre");
	}

	private static String periodLabel(YearMonth ym) {
		return ym.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + ym.getYear();
	}

	private record PayCalc(
			int workingDays,
			BigDecimal unpaidDays,
			BigDecimal dailyWage,
			BigDecimal lop,
			BigDecimal storeDues,
			BigDecimal other,
			BigDecimal advanceDeduction,
			BigDecimal reimbursement,
			BigDecimal net,
			BigDecimal roll,
			BigDecimal base,
			List<SalaryAdvance> advances,
			List<OperatingExpense> pockets) {
	}

	private BigDecimal unpaidDays(Long userId, LocalDate from, LocalDate to) {
		return leaveDays(userId, from, to, false);
	}

	private BigDecimal leaveDays(Long userId, LocalDate from, LocalDate to, boolean paidOnly) {
		TenantSettings settings = tenantSettingsRepository.findByTenantId(TenantContext.require().tenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
		List<LeaveRequest> leaves = leaveRequestRepository.findByUserIdAndStatusIn(userId,
				List.of(LeaveStatus.APPROVED));
		BigDecimal days = BigDecimal.ZERO;
		for (LeaveRequest leave : leaves) {
			if (leave.getToDate().isBefore(from) || leave.getFromDate().isAfter(to)) {
				continue;
			}
			boolean unpaidType = leave.getType() == LeaveType.UNPAID_LEAVE_LOP || leave.getType() == LeaveType.HALF_DAY_LOP;
			if (paidOnly == unpaidType) {
				continue;
			}
			LocalDate start = leave.getFromDate().isBefore(from) ? from : leave.getFromDate();
			LocalDate end = leave.getToDate().isAfter(to) ? to : leave.getToDate();
			for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
				if (settings.getWorkingDaysMode() == WorkingDaysMode.EXCLUDE_SUNDAYS
						&& date.getDayOfWeek().getValue() == 7) {
					continue;
				}
				days = days.add(leave.getType() == LeaveType.HALF_DAY_LOP ? new BigDecimal("0.5") : BigDecimal.ONE);
			}
		}
		return days;
	}

	private int workingDays(int year, int month) {
		TenantSettings settings = tenantSettingsRepository.findByTenantId(TenantContext.require().tenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
		YearMonth ym = YearMonth.of(year, month);
		int count = 0;
		for (int d = 1; d <= ym.lengthOfMonth(); d++) {
			LocalDate date = ym.atDay(d);
			if (settings.getWorkingDaysMode() == WorkingDaysMode.EXCLUDE_SUNDAYS
					&& date.getDayOfWeek().getValue() == 7) {
				continue;
			}
			count++;
		}
		return count;
	}

	private static BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
