package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.PurchaseOrderStatus;
import com.dertz.spectra.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	List<PurchaseOrder> findByBranchIdOrderByCreatedDateDesc(Long branchId);

	List<PurchaseOrder> findByBranchIdAndStatusInOrderByCreatedDateDesc(Long branchId, Collection<PurchaseOrderStatus> statuses);

	long countByBranchIdAndStatus(Long branchId, PurchaseOrderStatus status);

	long countByBranchIdAndStatusAndRequestedByUserId(Long branchId, PurchaseOrderStatus status, Long requestedByUserId);

	long countByBranchIdAndStatusAndApproverUserId(Long branchId, PurchaseOrderStatus status, Long approverUserId);
}
