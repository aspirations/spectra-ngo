package com.dertz.spectra.repository;

import com.dertz.spectra.model.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

	List<StockBatch> findByWarehouseIdAndShelterProductIdAndQtyOnHandGreaterThanOrderByExpiryDateAscReceivedAtAsc(
			Long warehouseId, Long shelterProductId, BigDecimal qty);

	List<StockBatch> findByWarehouseIdOrderByExpiryDateAsc(Long warehouseId);

	@Query("SELECT COALESCE(SUM(b.qtyOnHand), 0) FROM StockBatch b WHERE b.warehouseId = :warehouseId AND b.shelterProductId = :productId")
	BigDecimal sumQty(Long warehouseId, Long productId);
}
