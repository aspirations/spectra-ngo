package com.dertz.spectra.task;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.Tenant;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.PayrollRunRepository;
import com.dertz.spectra.repository.TenantRepository;
import com.dertz.spectra.security.TenantContext;
import com.dertz.spectra.service.PayrollService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.YearMonth;
import java.util.List;

@Slf4j
public class MonthEndPayrollJob extends QuartzJobBean {

	@Autowired
	private TenantRepository tenantRepository;
	@Autowired
	private BranchRepository branchRepository;
	@Autowired
	private PayrollRunRepository payrollRunRepository;
	@Autowired
	private PayrollService payrollService;
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	protected void executeInternal(JobExecutionContext context) {
		YearMonth period = YearMonth.now().minusMonths(1);
		for (Tenant tenant : tenantRepository.findAll()) {
			if ("PLATFORM".equalsIgnoreCase(tenant.getCode()) || tenant.getStatus() != EntityStatus.ACTIVE) {
				continue;
			}
			for (Branch branch : branchesOf(tenant)) {
				TenantContext.set(new TenantContext.Snapshot(tenant.getId(), branch.getId(), 0L, Role.NGO_ADMIN,
						"quartz", tenant.getCode(), "Month-end job"));
				try {
					if (payrollRunRepository.findByBranchIdAndPeriodYearAndPeriodMonth(branch.getId(), period.getYear(),
							period.getMonthValue()).isEmpty()) {
						payrollService.generate(period.getYear(), period.getMonthValue());
						log.info("Opened DRAFT payroll for tenant {} branch {} {}-{}", tenant.getCode(), branch.getCode(),
								period.getYear(), period.getMonthValue());
					}
				} catch (Exception ex) {
					log.error("Month-end payroll failed for branch {}", branch.getId(), ex);
				} finally {
					entityManager.clear();
					TenantContext.clear();
				}
			}
		}
	}

	private List<Branch> branchesOf(Tenant tenant) {
		TenantContext.set(new TenantContext.Snapshot(tenant.getId(), null, 0L, Role.NGO_ADMIN, "quartz",
				tenant.getCode(), "Month-end job"));
		try {
			return branchRepository.findAll();
		} finally {
			TenantContext.clear();
		}
	}
}
