package com.dertz.spectra.repository;

import com.dertz.spectra.model.StaffStorePurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface StaffStorePurchaseRepository extends JpaRepository<StaffStorePurchase, Long> {

	List<StaffStorePurchase> findByBranchIdOrderByPurchasedAtDesc(Long branchId);

	List<StaffStorePurchase> findByBranchIdAndPurchasedAtGreaterThanEqualAndPurchasedAtLessThan(
			Long branchId, Instant from, Instant to);
}
