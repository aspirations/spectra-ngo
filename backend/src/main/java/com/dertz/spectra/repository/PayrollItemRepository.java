package com.dertz.spectra.repository;

import com.dertz.spectra.model.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {

	List<PayrollItem> findByPayrollRunId(Long payrollRunId);

	Optional<PayrollItem> findByPayrollRunIdAndUserId(Long payrollRunId, Long userId);

	Optional<PayrollItem> findByIdAndUserId(Long id, Long userId);

	List<PayrollItem> findByUserIdOrderByIdDesc(Long userId);
}
