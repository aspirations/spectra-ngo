package com.dertz.spectra.service;

import com.dertz.spectra.Enum.LeaveStatus;
import com.dertz.spectra.Enum.LeaveType;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.LeaveRequest;
import com.dertz.spectra.model.User;
import com.dertz.spectra.repository.LeaveRequestRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.request.LeaveRequestDto;
import com.dertz.spectra.request.LeaveReviewRequest;
import com.dertz.spectra.security.BranchScope;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveAttendanceService {

	private final LeaveRequestRepository leaveRequestRepository;
	private final UserRepository userRepository;

	@Transactional
	public LeaveRequest apply(LeaveRequestDto request) {
		if (request.getToDate().isBefore(request.getFromDate())) {
			throw new BusinessException("toDate must be on or after fromDate");
		}
		Role role = TenantContext.require().role();
		boolean manager = role == Role.NGO_ADMIN || role == Role.BRANCH_ADMIN || role == Role.INVENTORY_MANAGER;
		Long userId = BranchScope.currentUserId();
		if (request.getUserId() != null && !request.getUserId().equals(userId)) {
			if (!manager) {
				throw new BusinessException("Cannot record leave for another person");
			}
			User target = userRepository.findById(request.getUserId())
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));
			userId = target.getId();
		}
		long calendarDays = ChronoUnit.DAYS.between(request.getFromDate(), request.getToDate()) + 1;
		BigDecimal days = request.getType() == LeaveType.HALF_DAY_LOP
				? BigDecimal.valueOf(calendarDays).multiply(new BigDecimal("0.5"))
				: BigDecimal.valueOf(calendarDays);
		LeaveRequest leave = LeaveRequest.builder()
				.tenantId(TenantContext.require().tenantId())
				.branchId(BranchScope.requireBranchId())
				.userId(userId)
				.type(request.getType())
				.fromDate(request.getFromDate())
				.toDate(request.getToDate())
				.days(days)
				.status(manager ? LeaveStatus.APPROVED : LeaveStatus.PENDING)
				.reason(request.getReason())
				.reviewedBy(manager ? BranchScope.currentUserId() : null)
				.build();
		return leaveRequestRepository.save(leave);
	}

	@Transactional
	public LeaveRequest review(Long id, LeaveReviewRequest request) {
		LeaveRequest leave = leaveRequestRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
		if (leave.getStatus() != LeaveStatus.PENDING) {
			throw new BusinessException("Leave already reviewed");
		}
		if (request.getStatus() != LeaveStatus.APPROVED && request.getStatus() != LeaveStatus.REJECTED) {
			throw new BusinessException("Status must be APPROVED or REJECTED");
		}
		leave.setStatus(request.getStatus());
		leave.setReviewedBy(BranchScope.currentUserId());
		return leaveRequestRepository.save(leave);
	}

	@Transactional(readOnly = true)
	public List<LeaveRequest> list() {
		Role role = TenantContext.require().role();
		if (role == Role.EMPLOYEE || role == Role.VET_TECH_EMPLOYEE) {
			return leaveRequestRepository.findByUserIdOrderByCreatedDateDesc(BranchScope.currentUserId());
		}
		return leaveRequestRepository.findByBranchIdOrderByCreatedDateDesc(BranchScope.requireBranchId());
	}
}
