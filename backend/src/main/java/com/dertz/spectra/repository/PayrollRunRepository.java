package com.dertz.spectra.repository;

import com.dertz.spectra.model.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

	List<PayrollRun> findByBranchIdOrderByPeriodYearDescPeriodMonthDesc(Long branchId);

	Optional<PayrollRun> findByBranchIdAndPeriodYearAndPeriodMonth(Long branchId, Integer year, Integer month);

	List<PayrollRun> findAllByPeriodYearAndPeriodMonth(Integer year, Integer month);
}
