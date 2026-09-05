package com.dertz.spectra.repository;

import com.dertz.spectra.model.ShelterProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShelterProductRepository extends JpaRepository<ShelterProduct, Long> {

	List<ShelterProduct> findByActiveTrueOrderByNameAsc();

	List<ShelterProduct> findByStaffSaleTrueAndActiveTrueOrderByNameAsc();

	Optional<ShelterProduct> findBySkuIgnoreCase(String sku);

	Optional<ShelterProduct> findByBarcode(String barcode);

	@Query("SELECT p FROM ShelterProduct p WHERE p.active = true AND p.staffSale = true AND (lower(p.name) LIKE lower(concat('%', :q, '%')) OR lower(p.sku) LIKE lower(concat('%', :q, '%')) OR (p.barcode IS NOT NULL AND p.barcode LIKE concat('%', :q, '%')))")
	List<ShelterProduct> searchStaffSale(@Param("q") String q);
}
