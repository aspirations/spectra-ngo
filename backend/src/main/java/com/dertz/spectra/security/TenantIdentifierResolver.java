package com.dertz.spectra.security;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

	@Override
	public Long resolveCurrentTenantIdentifier() {
		Long id = TenantContext.getTenantId();
		return id != null ? id : 0L;
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}
}
