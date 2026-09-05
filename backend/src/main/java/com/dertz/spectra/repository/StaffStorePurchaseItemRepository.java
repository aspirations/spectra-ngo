package com.dertz.spectra.repository;

import com.dertz.spectra.model.StaffStorePurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffStorePurchaseItemRepository extends JpaRepository<StaffStorePurchaseItem, Long> {

	List<StaffStorePurchaseItem> findByPurchaseId(Long purchaseId);
}
