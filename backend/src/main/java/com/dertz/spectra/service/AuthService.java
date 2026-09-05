package com.dertz.spectra.service;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.dto.AuthResponse;
import com.dertz.spectra.dto.UserDTO;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.Tenant;
import com.dertz.spectra.model.User;
import com.dertz.spectra.repository.GlobalUserLookup;
import com.dertz.spectra.repository.TenantRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.request.LoginRequest;
import com.dertz.spectra.security.JwtService;
import com.dertz.spectra.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final TenantRepository tenantRepository;
	private final UserRepository userRepository;
	private final GlobalUserLookup globalUserLookup;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final OrgService orgService;

	public AuthResponse login(LoginRequest request) {
		String email = request.getEmail().trim();
		Long tenantId = globalUserLookup.findTenantIdByEmail(email)
				.orElseThrow(() -> new BusinessException("Invalid email or password", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED));
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new BusinessException("Tenant not found", "INVALID_TENANT", HttpStatus.UNAUTHORIZED));
		TenantContext.set(new TenantContext.Snapshot(tenantId, null, 0L, Role.NGO_ADMIN, email, tenant.getCode(), "login"));
		try {
			User user = userRepository.findByEmailIgnoreCase(email)
					.orElseThrow(() -> new BusinessException("Invalid email or password", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED));
			if (user.getRole() != Role.PLATFORM_ADMIN && tenant.getStatus() != EntityStatus.ACTIVE) {
				throw new BusinessException("This organisation is disabled", "TENANT_DISABLED", HttpStatus.FORBIDDEN);
			}
			if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()) || !user.isEnabled()) {
				throw new BusinessException("Invalid email or password", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
			}
			UserDTO dto = orgService.toUserDto(user);
			dto.setTenantCode(tenant.getCode());
			dto.setTenantName(tenant.getName());
			Long defaultBranch = defaultBranch(user, dto.getBranchIds());
			Map<String, Object> claims = new HashMap<>();
			claims.put("tenant_id", tenant.getId());
			claims.put("branch_id", defaultBranch);
			claims.put("user_id", user.getId());
			claims.put("role", user.getRole().name());
			claims.put("tenant_code", tenant.getCode());
			claims.put("full_name", user.getFullName());
			String token = jwtService.generateToken(claims, user.getEmail());
			if (dto.getBranchId() == null) {
				dto.setBranchId(defaultBranch);
			}
			return AuthResponse.builder().token(token).user(dto).build();
		} finally {
			TenantContext.clear();
		}
	}

	@Transactional(readOnly = true)
	public UserDTO me() {
		TenantContext.Snapshot snapshot = TenantContext.require();
		User user = userRepository.findById(snapshot.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		UserDTO dto = orgService.toUserDto(user);
		dto.setTenantCode(snapshot.tenantCode());
		tenantRepository.findById(snapshot.tenantId()).ifPresent(t -> dto.setTenantName(t.getName()));
		if (dto.getBranchId() == null) {
			dto.setBranchId(snapshot.branchId());
		}
		return dto;
	}

	private static Long defaultBranch(User user, List<Long> branchIds) {
		if (user.getRole() == Role.PLATFORM_ADMIN) {
			return null;
		}
		if (user.getBranchId() != null && branchIds != null && branchIds.contains(user.getBranchId())) {
			return user.getBranchId();
		}
		return branchIds == null || branchIds.isEmpty() ? null : branchIds.get(0);
	}
}
