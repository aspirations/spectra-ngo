package com.dertz.spectra.repository;

import com.dertz.spectra.model.StaffStoreProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffStoreProductRepository extends JpaRepository<StaffStoreProduct, Long> {

	List<StaffStoreProduct> findByBranchIdAndActiveTrueOrderByNameAsc(Long branchId);

	Optional<StaffStoreProduct> findByBranchIdAndSkuIgnoreCase(Long branchId, String sku);

	Optional<StaffStoreProduct> findByBranchIdAndBarcode(Long branchId, String barcode);

	@Query("SELECT p FROM StaffStoreProduct p WHERE p.branchId = :branchId AND p.active = true AND (lower(p.name) LIKE lower(concat('%', :q, '%')) OR lower(p.sku) LIKE lower(concat('%', :q, '%')) OR (p.barcode IS NOT NULL AND p.barcode LIKE concat('%', :q, '%')))")
	List<StaffStoreProduct> search(@Param("branchId") Long branchId, @Param("q") String q);
}
