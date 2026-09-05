package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.WarehouseType;
import com.dertz.spectra.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

	List<Warehouse> findByBranchId(Long branchId);

	Optional<Warehouse> findByBranchIdAndType(Long branchId, WarehouseType type);
}
