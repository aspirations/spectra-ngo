package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.AdvanceStatus;
import com.dertz.spectra.model.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {

	List<SalaryAdvance> findByBranchIdOrderByAdvancedAtDesc(Long branchId);

	List<SalaryAdvance> findByUserIdAndStatusAndPayrollRunIdIsNull(Long userId, AdvanceStatus status);

	List<SalaryAdvance> findByPayrollRunId(Long payrollRunId);
}
