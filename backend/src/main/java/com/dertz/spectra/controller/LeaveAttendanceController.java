package com.dertz.spectra.controller;

import com.dertz.spectra.model.LeaveRequest;
import com.dertz.spectra.request.LeaveRequestDto;
import com.dertz.spectra.request.LeaveReviewRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.LeaveAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LeaveAttendanceController extends BaseController {

	private final LeaveAttendanceService leaveAttendanceService;

	@GetMapping("/leave")
	public ResponseEntity<ApiResponse<List<LeaveRequest>>> leave() {
		return ok(leaveAttendanceService.list());
	}

	@PostMapping("/leave")
	public ResponseEntity<ApiResponse<LeaveRequest>> apply(@Valid @RequestBody LeaveRequestDto request) {
		return ok(leaveAttendanceService.apply(request), "Leave recorded");
	}

	@PostMapping("/leave/{id}/review")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<LeaveRequest>> review(@PathVariable Long id,
			@Valid @RequestBody LeaveReviewRequest request) {
		return ok(leaveAttendanceService.review(id, request), "Leave reviewed");
	}
}
