package com.dertz.spectra.repository;

import com.dertz.spectra.model.PurchaseGrn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseGrnRepository extends JpaRepository<PurchaseGrn, Long> {

	List<PurchaseGrn> findByBranchIdOrderByReceivedAtDesc(Long branchId);
}
