package com.dertz.spectra.repository;

import com.dertz.spectra.model.PurchaseGrnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseGrnItemRepository extends JpaRepository<PurchaseGrnItem, Long> {

	List<PurchaseGrnItem> findByGrnId(Long grnId);
}
