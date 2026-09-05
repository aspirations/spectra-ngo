package com.dertz.spectra.controller;

import com.dertz.spectra.dto.StaffPayslip;
import com.dertz.spectra.model.PayrollItem;
import com.dertz.spectra.model.PayrollRun;
import com.dertz.spectra.model.SalaryAdvance;
import com.dertz.spectra.request.AdvanceRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PayrollController extends BaseController {

	private final PayrollService payrollService;

	@GetMapping("/payroll/runs")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<PayrollRun>>> runs() {
		return ok(payrollService.list());
	}

	@PostMapping("/payroll/runs/generate")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<PayrollRun>> generate(
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month) {
		YearMonth ym = YearMonth.now().minusMonths(1);
		int y = year == null ? ym.getYear() : year;
		int m = month == null ? ym.getMonthValue() : month;
		return ok(payrollService.generate(y, m), "Draft payroll generated");
	}

	@GetMapping("/payroll/runs/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable Long id) {
		return ok(Map.of("run", payrollService.getRun(id), "items", payrollService.items(id)));
	}

	@PostMapping("/payroll/runs/{id}/approve")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<PayrollRun>> approve(@PathVariable Long id) {
		return ok(payrollService.approve(id), "Payroll approved");
	}

	@PostMapping("/payroll/runs/{id}/disburse")
	@PreAuthorize("hasAnyRole('NGO_ADMIN')")
	public ResponseEntity<ApiResponse<PayrollRun>> disburse(@PathVariable Long id) {
		return ok(payrollService.disburse(id), "Payroll disbursed");
	}

	@GetMapping("/payroll/staff/{userId}/current")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<StaffPayslip>> currentMonth(@PathVariable Long userId) {
		return ok(payrollService.currentMonth(userId));
	}

	@GetMapping("/payroll/staff/{userId}/payslip-pdf")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<byte[]> payslipPdf(@PathVariable Long userId) {
		StaffPayslip slip = payrollService.currentMonth(userId);
		String file = "payslip-" + slip.getPeriodYear() + "-" + String.format("%02d", slip.getPeriodMonth()) + ".pdf";
		return pdfFile(payrollService.payslipPdf(userId), file);
	}

	@GetMapping("/payroll/payslips/{id}")
	public ResponseEntity<ApiResponse<PayrollItem>> payslip(@PathVariable Long id) {
		return ok(payrollService.payslip(id));
	}

	@GetMapping("/payroll/payslips")
	public ResponseEntity<ApiResponse<List<PayrollItem>>> myPayslips() {
		return ok(payrollService.myPayslips());
	}

	@GetMapping("/payroll/advances")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<SalaryAdvance>>> advances() {
		return ok(payrollService.advances());
	}

	@PostMapping("/payroll/advances")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<SalaryAdvance>> recordAdvance(@Valid @RequestBody AdvanceRequest request) {
		return ok(payrollService.recordAdvance(request), "Advance recorded");
	}
}
