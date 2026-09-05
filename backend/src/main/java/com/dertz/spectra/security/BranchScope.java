package com.dertz.spectra.security;

import com.dertz.spectra.Enum.Role;
import com.dertz.spectra.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class BranchScope {

	private BranchScope() {
	}

	public static Long requireBranchId() {
		TenantContext.Snapshot snapshot = TenantContext.require();
		if (snapshot.branchId() == null) {
			throw new BusinessException("Select a working centre (X-Branch-Id).", "BRANCH_REQUIRED",
					HttpStatus.BAD_REQUEST);
		}
		return snapshot.branchId();
	}

	public static Long optionalBranchId() {
		TenantContext.Snapshot snapshot = TenantContext.require();
		return snapshot.branchId();
	}

	public static boolean isNgoAdmin() {
		return TenantContext.require().role() == Role.NGO_ADMIN;
	}

	public static boolean isPlatformAdmin() {
		return TenantContext.require().role() == Role.PLATFORM_ADMIN;
	}

	public static Long currentUserId() {
		return TenantContext.require().userId();
	}
}
