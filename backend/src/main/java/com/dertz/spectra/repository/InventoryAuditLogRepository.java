package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.AuditStatus;
import com.dertz.spectra.model.InventoryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long> {

	List<InventoryAuditLog> findByBranchIdOrderByCreatedDateDesc(Long branchId);

	List<InventoryAuditLog> findByBranchIdAndStatus(Long branchId, AuditStatus status);
}
