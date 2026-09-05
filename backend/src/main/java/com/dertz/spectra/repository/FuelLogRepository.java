package com.dertz.spectra.repository;

import com.dertz.spectra.model.FuelLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {

	List<FuelLog> findByBranchIdOrderByFilledAtDesc(Long branchId);

	List<FuelLog> findByBranchIdAndVehicleLabelIgnoreCaseOrderByOdometerKmAsc(Long branchId, String vehicleLabel);
}
