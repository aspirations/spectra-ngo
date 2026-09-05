package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.PosOrderStatus;
import com.dertz.spectra.Enum.PosTender;
import com.dertz.spectra.model.StaffPosOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface StaffPosOrderRepository extends JpaRepository<StaffPosOrder, Long> {

	List<StaffPosOrder> findByEmployeeIdOrderByOrderAtDesc(Long employeeId);

	List<StaffPosOrder> findByBranchIdOrderByOrderAtDesc(Long branchId);

	List<StaffPosOrder> findByBranchIdAndOrderAtGreaterThanEqualAndOrderAtLessThan(
			Long branchId, Instant from, Instant to);

	List<StaffPosOrder> findByEmployeeIdAndTenderAndStatusAndOrderAtBetween(
			Long employeeId, PosTender tender, PosOrderStatus status, Instant from, Instant to);

	List<StaffPosOrder> findByEmployeeIdAndTenderAndStatus(
			Long employeeId, PosTender tender, PosOrderStatus status);

	@Query("SELECT COALESCE(SUM(o.total), 0) FROM StaffPosOrder o WHERE o.employeeId = :employeeId AND o.tender = :tender AND o.status = :status AND o.orderAt >= :from AND o.orderAt < :to")
	BigDecimal sumUnpaidDues(Long employeeId, PosTender tender, PosOrderStatus status, Instant from, Instant to);
}
