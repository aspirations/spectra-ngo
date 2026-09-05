package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.CareStatus;
import com.dertz.spectra.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

	List<Resident> findByBranchIdOrderByNameAsc(Long branchId);

	List<Resident> findByBranchIdAndStatus(Long branchId, CareStatus status);

	long countByBranchIdAndStatus(Long branchId, CareStatus status);
}
