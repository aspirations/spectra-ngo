package com.dertz.spectra.repository;

import com.dertz.spectra.Enum.LeaveStatus;
import com.dertz.spectra.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

	List<LeaveRequest> findByBranchIdOrderByCreatedDateDesc(Long branchId);

	List<LeaveRequest> findByUserIdOrderByCreatedDateDesc(Long userId);

	List<LeaveRequest> findByUserIdAndStatusIn(Long userId, Collection<LeaveStatus> statuses);

	List<LeaveRequest> findByBranchIdAndStatus(Long branchId, LeaveStatus status);
}
