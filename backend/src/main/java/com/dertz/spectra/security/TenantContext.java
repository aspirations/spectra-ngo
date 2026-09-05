package com.dertz.spectra.security;

import com.dertz.spectra.Enum.Role;

public final class TenantContext {

	private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

	private TenantContext() {
	}

	public record Snapshot(Long tenantId, Long branchId, Long userId, Role role, String email, String tenantCode,
			String fullName) {
	}

	public static void set(Snapshot snapshot) {
		CURRENT.set(snapshot);
	}

	public static Snapshot get() {
		return CURRENT.get();
	}

	public static Snapshot require() {
		Snapshot snapshot = CURRENT.get();
		if (snapshot == null || snapshot.tenantId() == null) {
			throw new IllegalStateException("Tenant context is not set");
		}
		return snapshot;
	}

	public static Long getTenantId() {
		Snapshot snapshot = CURRENT.get();
		return snapshot == null ? null : snapshot.tenantId();
	}

	public static void clear() {
		CURRENT.remove();
	}
}
