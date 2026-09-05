package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.ConsumptionStatus;
import com.dertz.spectra.model.InternalConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InternalConsumptionRepository extends JpaRepository<InternalConsumption, Long> {

	List<InternalConsumption> findByBranchIdAndConsumptionDateBetweenOrderByConsumptionDateDesc(
			Long branchId, LocalDate from, LocalDate to);

	List<InternalConsumption> findByBranchIdAndVarianceAlertTrueOrderByConsumptionDateDesc(Long branchId);

	List<InternalConsumption> findByBranchIdAndStatusOrderByCreatedDateDesc(Long branchId, ConsumptionStatus status);

	long countByBranchIdAndStatus(Long branchId, ConsumptionStatus status);
}
