package com.dertz.spectra.controller;

import com.dertz.spectra.dto.BranchDTO;
import com.dertz.spectra.dto.UserDTO;
import com.dertz.spectra.model.TenantSettings;
import com.dertz.spectra.request.CreateBranchRequest;
import com.dertz.spectra.request.CreateUserRequest;
import com.dertz.spectra.request.UpdateUserRequest;
import com.dertz.spectra.response.ApiResponse;
import com.dertz.spectra.service.OrgService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrgController extends BaseController {

	private final OrgService orgService;

	@GetMapping("/branches")
	public ResponseEntity<ApiResponse<List<BranchDTO>>> branches() {
		return ok(orgService.listBranches());
	}

	@GetMapping("/branches/{id}")
	public ResponseEntity<ApiResponse<BranchDTO>> branch(@PathVariable Long id) {
		return ok(orgService.getBranch(id));
	}

	@PostMapping("/branches")
	@PreAuthorize("hasRole('NGO_ADMIN')")
	public ResponseEntity<ApiResponse<BranchDTO>> createBranch(@Valid @RequestBody CreateBranchRequest request) {
		return ok(orgService.createBranch(request), "Branch provisioned");
	}

	@GetMapping("/users")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<List<UserDTO>>> users() {
		return ok(orgService.listUsers());
	}

	@GetMapping("/users/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<UserDTO>> user(@PathVariable Long id) {
		return ok(orgService.getUser(id));
	}

	@PostMapping("/users")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody CreateUserRequest request) {
		return ok(orgService.createUser(request), "User created");
	}

	@PutMapping("/users/{id}")
	@PreAuthorize("hasAnyRole('NGO_ADMIN','BRANCH_ADMIN','INVENTORY_MANAGER')")
	public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
		return ok(orgService.updateUser(id, request), "User updated");
	}

	@GetMapping("/settings")
	public ResponseEntity<ApiResponse<TenantSettings>> settings() {
		return ok(orgService.settings());
	}
}
