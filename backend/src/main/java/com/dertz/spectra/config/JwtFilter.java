package com.dertz.spectra.config;

import com.dertz.spectra.Enum.EntityStatus;
import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.exception.BusinessException;
import com.dertz.spectra.model.Branch;
import com.dertz.spectra.model.Tenant;
import com.dertz.spectra.model.User;
import com.dertz.spectra.model.UserBranch;
import com.dertz.spectra.repository.BranchRepository;
import com.dertz.spectra.repository.TenantRepository;
import com.dertz.spectra.repository.UserBranchRepository;
import com.dertz.spectra.repository.UserRepository;
import com.dertz.spectra.security.JwtService;
import com.dertz.spectra.security.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final UserBranchRepository userBranchRepository;
	private final BranchRepository branchRepository;
	private final TenantRepository tenantRepository;
	private final HandlerExceptionResolver handlerExceptionResolver;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		if (request.getServletPath().equals("/auth/login")) {
			filterChain.doFilter(request, response);
			return;
		}
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			String jwt = authHeader.substring(7);
			if (!jwtService.isTokenValid(jwt)) {
				filterChain.doFilter(request, response);
				return;
			}
			Claims claims = jwtService.extractAllClaims(jwt);
			Long tenantId = jwtService.asLong(claims.get("tenant_id"));
			Long branchId = jwtService.asLong(claims.get("branch_id"));
			Long userId = jwtService.asLong(claims.get("user_id"));
			Role role = Role.valueOf(claims.get("role", String.class));
			String email = claims.getSubject();
			String tenantCode = claims.get("tenant_code", String.class);
			String fullName = claims.get("full_name", String.class);

			TenantContext.set(new TenantContext.Snapshot(tenantId, branchId, userId, role, email, tenantCode, fullName));

			if (role != Role.PLATFORM_ADMIN) {
				Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
				if (tenant == null || tenant.getStatus() != EntityStatus.ACTIVE) {
					filterChain.doFilter(request, response);
					return;
				}
				Set<Long> allowed = allowedBranches(role, userId);
				String headerBranch = request.getHeader("X-Branch-Id");
				if (headerBranch != null && !headerBranch.isBlank()) {
					Long requested = Long.parseLong(headerBranch.trim());
					if (!allowed.contains(requested)) {
						throw new BusinessException("No access to this centre", "FORBIDDEN", HttpStatus.FORBIDDEN);
					}
					branchId = requested;
				} else if (branchId != null && !allowed.contains(branchId)) {
					branchId = allowed.stream().findFirst().orElse(null);
				} else if (branchId == null) {
					branchId = allowed.stream().findFirst().orElse(null);
				}
				TenantContext.set(new TenantContext.Snapshot(tenantId, branchId, userId, role, email, tenantCode, fullName));
			}

			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
				if (user != null && user.isEnabled()) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							user, null, user.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
			filterChain.doFilter(request, response);
		} catch (Exception ex) {
			handlerExceptionResolver.resolveException(request, response, null, ex);
		} finally {
			TenantContext.clear();
		}
	}

	private Set<Long> allowedBranches(Role role, Long userId) {
		if (role == Role.NGO_ADMIN) {
			return branchRepository.findAll().stream().map(Branch::getId).collect(Collectors.toSet());
		}
		return userBranchRepository.findByUserId(userId).stream()
				.map(UserBranch::getBranchId)
				.collect(Collectors.toSet());
	}
}
