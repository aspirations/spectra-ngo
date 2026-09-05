package com.dertz.spectra.repository;

import com.dertz.spectra.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

	List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

	@Modifying
	void deleteByPurchaseOrderId(Long purchaseOrderId);
}
