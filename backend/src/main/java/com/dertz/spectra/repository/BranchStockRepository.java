package com.dertz.spectra.repository;

import com.dertz.spectra.model.BranchStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {

	Optional<BranchStock> findByWarehouseIdAndShelterProductId(Long warehouseId, Long shelterProductId);

	/** For writers only: row lock, so it must not be called from a read-only transaction. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM BranchStock s WHERE s.warehouseId = :warehouseId AND s.shelterProductId = :productId")
	Optional<BranchStock> lockByWarehouseAndProduct(Long warehouseId, Long productId);
}
